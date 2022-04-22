package ham_fisted;


public final class IntBitmap {
  public static final int nextPow2(int value) {
    int highestOneBit = Integer.highestOneBit(value);
    if (value == highestOneBit) {
      return value;
    }
    return highestOneBit << 1;
  }
  public static final int mask(int hash, int shift) {
    // Return the last 5 bits of hash right shifted shift bits
    return (hash >>> shift) & 0x01f;
  }
  public static final int bitpos(int hash, int shift) {
    return 1 << mask(hash, shift);
  }
  public static final int index(int bitmap, int bit){
    return Integer.bitCount(bitmap & (bit - 1));
  }
  public static final int incShift(int shift) {
    return shift + 5;
  }
  public static final long mask(long hash, long shift) {
    // Return the last 10 bits of hash right shifted shift bits
    return (hash >>> shift) & 0x3FF;
  }
  public static final long bitpos(long hash, long shift) {
    return 1 << mask(hash, shift);
  }
  public static final long index(long bitmap, long bit){
    return Long.bitCount(bitmap & (bit - 1));
  }
  public static final long incShift(long shift) {
    return shift + 10;
  }
}
