package gc.garcol.libcore;

import java.nio.ByteBuffer;

/**
 * @author thaivc
 * @since 2024
 */
public class ByteBufferUtil
{
    public static void put(ByteBuffer buffer, int bufferIndex, byte[] src)
    {
        buffer.put(bufferIndex, src);
        buffer.position(bufferIndex + src.length);
    }
}
