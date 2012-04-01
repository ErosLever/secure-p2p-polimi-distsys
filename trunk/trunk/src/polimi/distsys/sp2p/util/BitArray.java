package polimi.distsys.sp2p.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;

/**
 * The bit array class that functs as an array of boolean, in which
 * each elements takes one bit.  The return type of matrix comparison.
 *
 * @author Hanhua Feng - hanhua@cs.columbia.edu
 * @version $Id: BitArray.java,v 1.9 2003/05/13 00:13:13 hanhua Exp $
 */
public class BitArray implements Cloneable {
    int size;
    Integer[] barray;

    /** Constructor
     * @param n    size of the bit array
     */
    public BitArray( int n ) {
        size = n;
        barray = new Integer[(size+31)/32];
    }

    /** Constructor from existing integer array
     * @param n    size of the bit array
     * @param integers    integer array containing all bits
     */
    private BitArray( int n, Integer[] integers ) {
        size = n;
        barray = integers;
    }

    /** Return a new copy of the bit array
     * @return     copy of this bit array
     */
    public final BitArray copy() {
        return new BitArray( size, (Integer[]) barray.clone() );
    }

    /** Implements the method in Cloneable
     * @return     copy of this bit array as Object
     */
    public Object clone() {
        return copy();
    }

    /** get the length of the bit array
     */
    public final int length() {
        return size;
    }

    /** Set one bit in the bit array
     * @param bit  index of this bit
     */
    public final void set( int bit ) {
    	if( barray[bit/32] == null )
    		barray[bit/32] = 0;
        barray[bit/32] |= 1<<(bit%32);
    }

    /** Clear one bit in the bit array
     * @param bit  index of this bit
     */
    public final void clear( int bit ) {
        barray[bit/32] &= ~(1<<(bit%32));
    }

    /** Flip one bit in the bit array
     * @param bit  index of this bit
     */
    public final void flip( int bit ) {
        barray[bit/32] ^= 1<<(bit%32);
    }

    /** Clear all barray of the bit array
     */
    public final void clear() {
        for ( int i=0; i<barray.length; i++ )
            barray[i] = 0;
    }

    /** Check a bit in the bit array.
     * @param b    index of this bit
     * @return     boolean value indicating whether this bit has been set.
     */
    public final boolean get( int bit ) {
    	Integer value = barray[bit/32];
    	value = value == null ? 0 : value;
    	value &= 1<< (bit%32) ;
        return value != 0;
    }

    /** Return a new bit array whose bits are exactly the reverse.
     * @return    new bit array
     */
    public final BitArray not() {
        BitArray x = copy();
        for ( int i=0; i<barray.length; i++ )
            x.barray[i] = ~barray[i];
        return x;
    }

    /** Return a new bit array whose bits are the bitwise ors.
     * @return    new bit array
     */
    public final BitArray or( BitArray b ) {
        BitArray x, y;
        int xq, xr;
        if ( size >= b.size )
        {
            x = this.copy();
            y = b;
            xq = b.size / 32;
            xr = b.size % 32;
        }
        else
        {
            x = b.copy();
            y = this;
            xq = size / 32;
            xr = size % 32;
        }

        for ( int i=0; i<xq; i++ )
            x.barray[i] |= y.barray[i];
        if ( 0 != xr )
            x.barray[xq] |= y.barray[xq] & ((1<<xr)-1);

        return x;
    }

    /** Return a new bit array whose bits are the bitwise ands.
     * @return    new bit array
     */
    public final BitArray and( BitArray b ) {
        BitArray x, y;
        int xq, xr;
        if ( size >= b.size )
        {
            x = this.copy();
            y = b;
            xq = b.size / 32;
            xr = b.size % 32;
        }
        else
        {
            x = b.copy();
            y = this;
            xq = size / 32;
            xr = size % 32;
        }

        for ( int i=0; i<xq; i++ )
            x.barray[i] &= y.barray[i];
        if ( 0 != xr )
            x.barray[xq] &= y.barray[xq] | ~((1<<xr)-1);

        return x;
    }

    /** Return a new bit array whose bits are the bitwise exclusive or.
     * @return    new bit array
     */
    public final BitArray xor( BitArray b ) {
        BitArray x, y;
        int xq, xr;
        if ( size >= b.size )
        {
            x = this.copy();
            y = b;
            xq = b.size / 32;
            xr = b.size % 32;
        }
        else
        {
            x = b.copy();
            y = this;
            xq = size / 32;
            xr = size % 32;
        }

        for ( int i=0; i<xq; i++ )
            x.barray[i] ^= y.barray[i];
        if ( 0 != xr )
            x.barray[xq] ^= y.barray[xq] & ((1<<xr)-1);

        return x;
    }

    final static int countBits( int x ) {
        int y = x;
        y = ( ( y & 0xaaaaaaaa ) >>> 1 )
            + ( y & 0x55555555 );
        y = ( ( y & 0xcccccccc ) >>> 2 )
            + ( y & 0x33333333 );
        y = ( ( y & 0xf0f0f0f0 ) >>> 4 )
            + ( y & 0x0f0f0f0f );
        y = ( ( y & 0xff00ff00 ) >>> 8 )
            + ( y & 0x00ff00ff );
        y = ( ( y & 0xffff0000 ) >>> 16 )
            + ( y & 0x0000ffff );

        return (int) y;
    }

    /** count the number of 1's in the bit array.
     * @return    the count of 1's
     */
    public final int count() {
        int cnt = 0;
        int xq = size / 32;
        int xr = size % 32;
        for ( int i=0; i<xq; i++ )
            cnt += barray[i] == null ? 0 : countBits( barray[i] );
        if ( 0 != xr )
            cnt += barray[xq] == null ? 0 : countBits( barray[xq] & ((1<<xr)-1) );
        return cnt;
    }

    /** convert bit array to a string for printing.
     */
    public String toString() {
        StringBuffer str = new StringBuffer();
        for ( int i=0; i<size; i++ )
            if ( get(i) )
                str.append( '1' );
            else 
                str.append( '0' );
        return str.toString();
    }
    
    public void serialize( OutputStream os ) throws IOException{
    	for(int i=0;i<size;i++){
    		os.write( get(i) ? '1' : '0' );
    	}
    }
    
    public static BitArray deserialize( InputStream in ) throws IOException{
    	Vector<Integer> parsed = new Vector<Integer>();
    	int count = 0;
    	int read;
    	while( ( read = in.read() ) != -1 ){
    		if( count % 32 == 0 )
    			parsed.add( 0 );
    		if( read == (byte)'1' ){
        		int val = parsed.get( count / 32 );
    			val |= 1 << ( count % 32 );
    			parsed.set( count / 32, val );
    		}
    		count++;
    	}
    	return new BitArray( count, parsed.toArray(new Integer[0]) ); 
    }

}