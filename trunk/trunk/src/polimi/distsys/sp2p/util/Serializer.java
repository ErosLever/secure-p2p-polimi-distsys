/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package polimi.distsys.sp2p.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.DatatypeConverter;

/**
 *
 * @author eros
 */
public class Serializer {

    public static void serialize(Object o, OutputStream out) throws IOException{
        synchronized(o){
            byte[] serialized = serialize(o);
            out.write(serialized);
            out.flush();
            System.out.println("Written obj: "+o);
        }
    }

    public static void serialize(Object o, SocketChannel out) throws IOException{
        byte[] buf = serialize(o);
        ByteBuffer bb = ByteBuffer.wrap(buf);
        bb.flip(); // needed?
        while(bb.hasRemaining())
            out.write(bb);
    }

    public static byte[] serialize(Object o) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.reset();
        synchronized(o){
            oos.writeObject(o);
            oos.flush();
            oos.close();
        }
        baos.close();
        return baos.toByteArray();
    }

    public static <E>E deserialize(byte[] serialized, Class<E> type) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bais = new ByteArrayInputStream(serialized);
        ObjectInputStream ois = new ObjectInputStream(bais);
        Object o = ois.readObject();
        ois.close();
        bais.close();
        return type.cast(o);
    }

    public static <E>E deserialize(InputStream in, Class<E> type) throws IOException, ClassNotFoundException {
    	ObjectInputStream ois = new ObjectInputStream(
    			new FilterCloseInputStream(in));
        Object o = ois.readObject();
        System.out.println("Received obj: "+o);
        ois.close();
        return type.cast(o);
    }

    public static String toXML(Object o){
        Set<String> fields = new HashSet<String>();
        for(Field f : o.getClass().getDeclaredFields()){
            fields.add(f.getName());
        }
        return toXML(o,fields.toArray(new String[0]));
    }

    public static String toXML(Object o, String... fields){
        Set<String> fieldset = new HashSet<String>(Arrays.asList(fields));
        StringBuilder sb = new StringBuilder();
        sb.append("<"+o.getClass().getName()+">");
        if(o != null){
            for(Field f : o.getClass().getDeclaredFields()){
                if(fieldset.contains( f.getName() )){
                    boolean acc = f.isAccessible();
                    if(!acc) // getting rid of "private" modifiers
                        f.setAccessible(true);
                    try {
                        Object of = f.get(o);
                        String s = of == null ? "null" : of.toString();
                        if(!s.startsWith("<")){
                            String type = f.getType().toString();
                            if(type.startsWith("class "))
                                type = type.substring(6);
                            else if(type.startsWith("interface "))
                                type = type.substring(10);
                            if(type.startsWith("java.lang."))
                                type = type.substring(10);
                            s = "<"+type+">"+s+"</"+type+">";
                        }
                        sb.append(s);
                    } catch (IllegalArgumentException ex) {
                        Logger.getLogger(Serializer.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IllegalAccessException ex) {
                        Logger.getLogger(Serializer.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    if(!acc)
                        f.setAccessible(false);
                }
            }
        } else {
            sb.append("null");
        }
        sb.append("</"+o.getClass().getName()+">");
        return sb.toString();
    }
    
    public static String byteArrayToHexString(byte[] buf){
        //return new javax.xml.bind.annotation.adapters.HexBinaryAdapter().marshal(buf);
    	return DatatypeConverter.printHexBinary(buf);
    }
    
    public static byte[] hexStringToByteArray(String s){
        //return new javax.xml.bind.annotation.adapters.HexBinaryAdapter().unmarshal(s);
    	return DatatypeConverter.parseHexBinary(s);
    }
    
    public static String base64Encode(byte[] buf){
    	return DatatypeConverter.printBase64Binary(buf);
    }
    
    public static byte[] base64Decode(String encoded){
    	return DatatypeConverter.parseBase64Binary(encoded);
    }

	public static class FilterCloseInputStream extends FilterInputStream {
		
		public FilterCloseInputStream(InputStream in) {
			super(in);
			// TODO Auto-generated constructor stub
		}

		public void close(){
			//do nothing
		}
	}
	
	public static class FilterCloseOutputStream extends FilterOutputStream {
		
		public FilterCloseOutputStream(OutputStream in) {
			super(in);
			// TODO Auto-generated constructor stub
		}

		public void close(){
			//do nothing
		}
	}
}
