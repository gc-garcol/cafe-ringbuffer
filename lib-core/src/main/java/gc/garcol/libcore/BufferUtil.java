package gc.garcol.libcore;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;

/**
 * @author thaivc
 * @since 2024
 */
public class BufferUtil
{

    public static final long ARRAY_BASE_OFFSET = UnsafeHelper.UNSAFE.arrayBaseOffset(byte[].class);
    public static final VarHandle BYTE_BUFFER_HB_HANDLE;
    public static final VarHandle BYTE_BUFFER_OFFSET_HANDLE;

    static
    {
        try
        {
            // VarHandle for the "hb" field in ByteBuffer
            BYTE_BUFFER_HB_HANDLE = MethodHandles.privateLookupIn(ByteBuffer.class, MethodHandles.lookup())
                .findVarHandle(ByteBuffer.class, "hb", byte[].class);

            // VarHandle for the "offset" field in ByteBuffer
            BYTE_BUFFER_OFFSET_HANDLE = MethodHandles.privateLookupIn(ByteBuffer.class, MethodHandles.lookup())
                .findVarHandle(ByteBuffer.class, "offset", int.class);

        }
        catch (ReflectiveOperationException e)
        {
            throw new RuntimeException("Failed to initialize VarHandles", e);
        }
    }

    public static byte[] array(final ByteBuffer buffer)
    {
        if (buffer.isDirect())
        {
            throw new IllegalArgumentException("buffer must wrap an array");
        }

        return (byte[])BYTE_BUFFER_HB_HANDLE.get(buffer);
    }

    public static int arrayOffset(final ByteBuffer buffer)
    {
        return (int)BYTE_BUFFER_OFFSET_HANDLE.get(buffer);
    }
}
