package ham_fisted;


/** 
 * Static 32 bit and 64 bit integer operations specific to the bitmap trie.
 */
public final class IntegerOps {
  /** Return the next power of 2 >= value.*/
  public static final int nextPow2(int value) {
    int highestOneBit = Integer.highestOneBit(value);
    if (value == highestOneBit) {
      return value;
    }
    return highestOneBit << 1;
  }
  /**
   * 1. unsigned shifted hash right by shift bits.
   * 2. return last 5 bits.
   *
   * @return A number from 0-31 inclusive.
   */
  public static final int mask(int shift, int hash) {
    
    return (hash >>> shift) & 0x01f;
  }
  /**
   * @return An integer with 1 bit high that indicates which value
   * from 0-31 inclusive is indicate by the shifted and masked hash.
   */
  public static final int bitpos(int shift, int hash) {
    return 1 << mask(shift, hash);
  }
  /**
   * @return The number of raised bits in bitmap behind
   * position bit.  This indicates the index in the node's packed 
   * array where data for this bit should be stored.
   */
  public static final int index(int bitmap, int bit){
    return Integer.bitCount(bitmap & (bit - 1));
  }
  /** 
   * @return unchecked shift incremented by 5 to indicate
   * the next 5 bits of the hashcode should be used for the bitmap.
   */
  public static final int incShift(int shift) {
    return shift + 5;
  }
  /** 
   * @return unchecked shift incremented by 5 to indicate
   * the next 5 bits of the hashcode should be used for the bitmap.
   */
  public static final int checkedIncShift(int shift) {
    if (shift >= 30)
      throw new RuntimeException("Invalid shift amount - already at max shift - "
				 + String.valueOf(shift));
    return shift + 5;
  }
  /**
   * Ripped directly from HashMap.java in openjdk source code -
   *
   * Computes key.hashCode() and spreads (XORs) higher bits of hash
   * to lower.  Because the table uses power-of-two masking, sets of
   * hashes that vary only in bits above the current mask will
   * always collide. (Among known examples are sets of Float keys
   * holding consecutive whole numbers in small tables.)  So we
   * apply a transform that spreads the impact of higher bits
   * downward. There is a tradeoff between speed, utility, and
   * quality of bit-spreading. Because many common sets of hashes
   * are already reasonably distributed (so don't benefit from
   * spreading), and because we use trees to handle large sets of
   * collisions in bins, we just XOR some shifted bits in the
   * cheapest possible way to reduce systematic lossage, as well as
   * to incorporate impact of the highest bits that would otherwise
   * never be used in index calculations because of table bounds.
   */
  public static final int mixhash(int h) {
    return h ^ (h >>> 16);
  }

  public static final int mixhash(Object key) {
    int h;
    return (key == null) ? 0 : mixhash(key.hashCode());
  }

  /**
   * Set bits starting at sidx and ending at *but including* eidx.
   */
  public static final int highBits(int sidx, int eidx) {
    int retval = 0;
    for (int idx = sidx; idx <= eidx; ++idx)
      retval |= 1 << idx;
    return retval;
  }

  /** Untested long versions of some of the above
  public static final long mask(long hash, long shift) {
    // Return the last 10 bits of hash right shifted shift bits
    return (hash >>> shift) & 0x03F;
  }
  public static final long bitpos(long hash, long shift) {
    return 1 << mask(hash, shift);
  }
  public static final long index(long bitmap, long bit){
    return Long.bitCount(bitmap & (bit - 1));
  }
  public static final long incShift(long shift) {
    return shift + 6;
  }
  */
}
