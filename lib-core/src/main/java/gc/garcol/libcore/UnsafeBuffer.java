package gc.garcol.libcore;

import java.nio.ByteBuffer;

import static gc.garcol.libcore.BufferUtil.ARRAY_BASE_OFFSET;

/**
 * A buffer that uses the Unsafe class for fast memory operations.
 * Provides methods to put and get primitive types and byte arrays.
 * Ensures memory visibility guarantees using Unsafe operations.
 *
 * @author thaivc
 * @since 2024
 */
public class UnsafeBuffer
{

    private final byte[] buffer;

    /**
     * Constructs an UnsafeBuffer with the specified initial capacity.
     *
     * @param initialCapacity the initial capacity of the buffer
     */
    public UnsafeBuffer(int initialCapacity)
    {
        buffer = new byte[initialCapacity];
    }

    /**
     * Puts an integer value at the specified index.
     *
     * @param index the index at which the value will be put
     * @param value the integer value to put
     */
    public void putInt(final int index, final int value)
    {
        UnsafeHelper.UNSAFE.putInt(buffer, ARRAY_BASE_OFFSET + index, value);
    }

    /**
     * Gets an integer value from the specified index.
     *
     * @param index the index from which the value will be retrieved
     * @return the integer value at the specified index
     */
    public int getInt(final int index)
    {
        return UnsafeHelper.UNSAFE.getInt(buffer, ARRAY_BASE_OFFSET + index);
    }

    /**
     * Puts a long value at the specified index.
     *
     * @param index the index at which the value will be put
     * @param value the long value to put
     */
    public void putLong(final int index, final long value)
    {
        UnsafeHelper.UNSAFE.putLong(buffer, ARRAY_BASE_OFFSET + index, value);
    }

    /**
     * Gets a long value from the specified index.
     *
     * @param index the index from which the value will be retrieved
     * @return the long value at the specified index
     */
    public long getLong(final int index)
    {
        return UnsafeHelper.UNSAFE.getLong(buffer, ARRAY_BASE_OFFSET + index);
    }

    /**
     * Puts bytes from the specified ByteBuffer into this buffer.
     *
     * @param index     the index at which the bytes will be put
     * @param srcBuffer the source ByteBuffer
     * @param srcIndex  the index in the source ByteBuffer from which the bytes will be read
     * @param length    the number of bytes to put
     */
    public void putBytes(final int index, final ByteBuffer srcBuffer, final int srcIndex, final int length)
    {
        final byte[] srcByteArray = BufferUtil.array(srcBuffer);
        final long srcBaseOffset = ARRAY_BASE_OFFSET + BufferUtil.arrayOffset(srcBuffer);
        UnsafeHelper.UNSAFE.copyMemory(srcByteArray, srcBaseOffset + srcIndex, buffer, ARRAY_BASE_OFFSET + index, length);
    }

    /**
     * Gets bytes from this buffer into the specified ByteBuffer.
     *
     * @param index     the index from which the bytes will be read
     * @param dstBuffer the destination ByteBuffer
     * @param dstOffset the offset in the destination ByteBuffer at which the bytes will be put
     * @param length    the number of bytes to get
     */
    public void getBytes(final int index, final ByteBuffer dstBuffer, final int dstOffset, final int length)
    {
        final byte[] dstByteArray = BufferUtil.array(dstBuffer);
        final long dstBaseOffset = ARRAY_BASE_OFFSET + BufferUtil.arrayOffset(dstBuffer);
        UnsafeHelper.UNSAFE.copyMemory(buffer, ARRAY_BASE_OFFSET + index, dstByteArray, dstBaseOffset + dstOffset, length);
    }

    /**
     * Clears bytes in the specified range by setting them to zero.
     *
     * @param fromIndex the starting index of the range to clear
     * @param toIndex   the ending index of the range to clear
     */
    public void clearBytes(final int fromIndex, final int toIndex)
    {
        int messageLength = toIndex - fromIndex + 1;
        UnsafeHelper.UNSAFE.setMemory(buffer, ARRAY_BASE_OFFSET + fromIndex, messageLength, (byte)0);
    }
}