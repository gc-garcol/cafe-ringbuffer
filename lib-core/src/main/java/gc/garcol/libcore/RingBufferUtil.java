package gc.garcol.libcore;

/**
 * Utility class for operations related to ring buffers.
 * Provides methods for checking message lengths, comparing circles, and manipulating positions.
 *
 * @since 2024
 */
public class RingBufferUtil
{
    /**
     * Checks if the message length is within the valid range.
     *
     * @param length       the length of the message
     * @param maxMsgLength the maximum allowed message length
     * @throws IllegalArgumentException if the message length is invalid
     */
    public static void checkMsgLength(int length, int maxMsgLength)
    {
        Preconditions.checkArgument(length >= 0, "invalid message length=" + length);
        Preconditions.checkArgument(length <= maxMsgLength, "encoded message exceeds maxMsgLength=" + maxMsgLength + ", length=" + length);
    }

    /**
     * Checks if two positions are in the same circle.
     *
     * @param firstFlip  the first flip
     * @param secondFlip the second flip
     * @return true if both flips are in the same circle, false otherwise
     */
    public static boolean sameCircle(boolean firstFlip, boolean secondFlip)
    {
        return firstFlip == secondFlip;
    }

    /**
     * Extracts the offset from a position.
     *
     * @param position the position
     * @return the offset part of the position
     */
    public static int offset(long position)
    {
        return (int)(position >> 32);
    }

    /**
     * Extracts the flip bit from a position.
     *
     * @param position the position
     * @return true if the flip bit is set, false otherwise
     */
    public static boolean flip(long position)
    {
        return (int)(position & 0xFFFFFFFFL) == 1;
    }

    /**
     * Combines an offset and a flip bit into a position.
     *
     * @param offset the offset
     * @param flip   the flip bit
     * @return the combined position
     */
    public static long position(int offset, boolean flip)
    {
        return ((long)offset << 32) | (flip ? 1 : 0);
    }
}