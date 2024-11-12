package gc.garcol.libcore;

import sun.misc.Unsafe;

import java.nio.ByteBuffer;

/**
 * @author thaivc
 * @since 2024
 */
public class BufferUtil
{
    public static final Unsafe UNSAFE = Unsafe.getUnsafe();

    public static final long ARRAY_BASE_OFFSET = UNSAFE.arrayBaseOffset(byte[].class);
    public static final long BYTE_BUFFER_HB_FIELD_OFFSET;
    public static final long BYTE_BUFFER_OFFSET_FIELD_OFFSET;

    static
    {
        try
        {
            BYTE_BUFFER_HB_FIELD_OFFSET = UNSAFE.objectFieldOffset(
                ByteBuffer.class.getDeclaredField("hb"));

            BYTE_BUFFER_OFFSET_FIELD_OFFSET = UNSAFE.objectFieldOffset(
                ByteBuffer.class.getDeclaredField("offset"));
        }
        catch (final Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }

    public static byte[] array(final ByteBuffer buffer)
    {
        if (buffer.isDirect())
        {
            throw new IllegalArgumentException("buffer must wrap an array");
        }

        return (byte[])UNSAFE.getObject(buffer, BYTE_BUFFER_HB_FIELD_OFFSET);
    }

    public static int arrayOffset(final ByteBuffer buffer)
    {
        return UNSAFE.getInt(buffer, BYTE_BUFFER_OFFSET_FIELD_OFFSET);
    }
}
