/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

//package org.apache.parquet.column.values.bloomfilter;
package ham_fisted;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Arrays;
import net.openhft.hashing.LongHashFunction;

/*
 * This Bloom filter is implemented using block-based Bloom filter algorithm from Putze et al.'s
 * "Cache-, Hash- and Space-Efficient Bloom filters". The basic idea is to hash the item to a tiny
 * Bloom filter which size fit a single cache line or smaller. This implementation sets 8 bits in
 * each tiny Bloom filter. Each tiny Bloom filter is 32 bytes to take advantage of 32-byte SIMD
 * instruction.
 */
public class BlockSplitBloomFilter {
  // Bytes in a tiny Bloom filter block.
  private static final int BYTES_PER_BLOCK = 32;

  // Bits in a tiny Bloom filter block.
  private static final int BITS_PER_BLOCK = 256;

  // The lower bound of bloom filter size, set to the size of a tiny Bloom filter block.
  public static final int LOWER_BOUND_BYTES = 32;

  // The upper bound of bloom filter size, set to default row group size.
  public static final int UPPER_BOUND_BYTES = 128 * 1024 * 1024;

  // The number of bits to set in a tiny Bloom filter
  private static final int BITS_SET_PER_BLOCK = 8;

  // The metadata in the header of a serialized Bloom filter is four four-byte values: the number of bytes,
  // the filter algorithm, the hash algorithm, and the compression.
  public static final int HEADER_SIZE = 16;

  // The default false positive probability value
  public static final double DEFAULT_FPP = 0.01;

  // The underlying byte array for Bloom filter bitset.
  private byte[] bitset;

  // A integer array buffer of underlying bitset to help setting bits.
  private IntBuffer intBuffer;

  private int maximumBytes = UPPER_BOUND_BYTES;
  private int minimumBytes = LOWER_BOUND_BYTES;


  private int[] mask = new int[BITS_SET_PER_BLOCK];

  // The block-based algorithm needs 8 odd SALT values to calculate eight indexes
  // of bits to set, one per 32-bit word.
  private static final int[] SALT = {
    0x47b6137b, 0x44974d91, 0x8824ad5b, 0xa2b7289d, 0x705495c7, 0x2df1424b, 0x9efc4947, 0x5c6bfb31
  };

  public BlockSplitBloomFilter(int numBytes) {
    this.minimumBytes = LOWER_BOUND_BYTES;
    this.maximumBytes = UPPER_BOUND_BYTES;
    initBitset(numBytes);
  }

  public byte[] bitset() { return bitset; }

  /**
   * Construct the Bloom filter with given bitset, it is used when reconstructing
   * Bloom filter from parquet file.
   *
   * @param bitset       The given bitset to construct Bloom filter.
   * @param hashStrategy The hash strategy Bloom filter apply.
   */
  public BlockSplitBloomFilter(byte[] bitset) {
    if (bitset == null) {
      throw new RuntimeException("Given bitset is null");
    }

    this.bitset = bitset;
    this.intBuffer = ByteBuffer.wrap(bitset).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
  }

  /**
   * Create a new bitset for Bloom filter.
   *
   * @param numBytes The number of bytes for Bloom filter bitset. The range of num_bytes should be within
   *                 [minimumBytes, maximumBytes], it will be rounded up/down
   *                 to lower/upper bound if num_bytes is out of range and also will rounded up to a power
   *                 of 2. It uses XXH64 as its default hash function and block-based algorithm
   *                 as default algorithm.
   */
  private void initBitset(int numBytes) {
    if (numBytes < minimumBytes) {
      numBytes = minimumBytes;
    }
    // Get next power of 2 if it is not power of 2.
    if ((numBytes & (numBytes - 1)) != 0) {
      numBytes = Integer.highestOneBit(numBytes) << 1;
    }
    if (numBytes > maximumBytes || numBytes < 0) {
      numBytes = maximumBytes;
    }
    this.bitset = new byte[numBytes];
    this.intBuffer = ByteBuffer.wrap(bitset).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
  }

  private int[] setMask(int key) {
    // The following three loops are written separately so that they could be
    // optimized for vectorization.
    for (int i = 0; i < BITS_SET_PER_BLOCK; ++i) {
      mask[i] = key * SALT[i];
    }

    for (int i = 0; i < BITS_SET_PER_BLOCK; ++i) {
      mask[i] = mask[i] >>> 27;
    }

    for (int i = 0; i < BITS_SET_PER_BLOCK; ++i) {
      mask[i] = 0x1 << mask[i];
    }

    return mask;
  }


  public void insertHash(long hash) {
    long numBlocks = bitset.length / BYTES_PER_BLOCK;
    long lowHash = hash >>> 32;
    int blockIndex = (int) ((lowHash * numBlocks) >> 32);
    int key = (int) hash;

    // Calculate mask for bucket.
    int[] mask = setMask(key);
    for (int i = 0; i < BITS_SET_PER_BLOCK; i++) {
      int value = intBuffer.get(blockIndex * (BYTES_PER_BLOCK / 4) + i);
      value |= mask[i];
      intBuffer.put(blockIndex * (BYTES_PER_BLOCK / 4) + i, value);
    }
  }


  public boolean findHash(long hash) {
    long numBlocks = bitset.length / BYTES_PER_BLOCK;
    long lowHash = hash >>> 32;
    int blockIndex = (int) ((lowHash * numBlocks) >> 32);
    int key = (int) hash;

    // Calculate mask for the tiny Bloom filter.
    int[] mask = setMask(key);
    for (int i = 0; i < BITS_SET_PER_BLOCK; i++) {
      if (0 == (intBuffer.get(blockIndex * (BYTES_PER_BLOCK / 4) + i) & mask[i])) {
        return false;
      }
    }

    return true;
  }

  private static void checkArgument(boolean arg, String message) {
    if(!arg) throw new RuntimeException(message);
  }
  /**
   * Calculate optimal size according to the number of distinct values and false positive probability.
   *
   * @param n: The number of distinct values.
   * @param p: The false positive probability.
   * @return optimal number of bits of given n and p.
   */
  public static int optimalNumOfBits(long n, double p) {
    checkArgument((p > 0.0 && p < 1.0), "FPP should be less than 1.0 and great than 0.0");
    final double m = -8 * n / Math.log(1 - Math.pow(p, 1.0 / 8));
    int numBits = (int) m;

    // Handle overflow.
    if (numBits > UPPER_BOUND_BYTES << 3 || m < 0) {
      numBits = UPPER_BOUND_BYTES << 3;
    }

    // Round numBits up to (k * BITS_PER_BLOCK)
    numBits = (numBits + BITS_PER_BLOCK - 1) & ~BITS_PER_BLOCK;

    if (numBits < (LOWER_BOUND_BYTES << 3)) {
      numBits = LOWER_BOUND_BYTES << 3;
    }
    return numBits;
  }


  public int getBitsetSize() {
    return this.bitset.length;
  }


  public static long hash(byte[] input) {
    return LongHashFunction.xx().hashBytes(input);
  }


  public boolean equals(Object object) {
    if (object == this) {
      return true;
    }
    if (object instanceof BlockSplitBloomFilter) {
      BlockSplitBloomFilter that = (BlockSplitBloomFilter) object;
      return Arrays.equals(this.bitset, that.bitset);
    }
    return false;
  }


  public boolean canMergeFrom(BlockSplitBloomFilter otherBloomFilter) {
    return otherBloomFilter != null
      && getBitsetSize() == otherBloomFilter.getBitsetSize();
  }

  public void merge(BlockSplitBloomFilter otherBloomFilter) throws IOException {
    checkArgument(otherBloomFilter != null, "The BloomFilter to merge shouldn't be null");
    checkArgument(
        canMergeFrom(otherBloomFilter),
        "BloomFilters must have the same size of bitset.");
    byte[] otherBits = otherBloomFilter.bitset;
    for (int i = 0; i < otherBits.length; i++) {
      bitset[i] |= otherBits[i];
    }
  }
}
