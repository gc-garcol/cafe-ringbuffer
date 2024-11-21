package gc.garcol.libcore;

import java.nio.ByteBuffer;

/**
 * Utility class for working with ByteBuffers.
 *
 * @author thaivc
 * @since 2024
 */
public class ByteBufferUtil
{
    /**
     * Puts the given byte array into the specified ByteBuffer at the given index.
     * Updates the buffer's position to the end of the inserted data.
     *
     * @param buffer      the ByteBuffer to put the byte array into
     * @param bufferIndex the index in the buffer to start putting the byte array
     * @param src         the byte array to put into the buffer
     */
    public static void put(ByteBuffer buffer, int bufferIndex, byte[] src)
    {
        buffer.put(bufferIndex, src);
        buffer.position(bufferIndex + src.length);
    }
}