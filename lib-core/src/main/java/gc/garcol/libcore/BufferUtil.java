package gc.garcol.libcore;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;

/**
 * Utility class for working with ByteBuffers using VarHandles.
 * Provides methods to access the underlying byte array and its offset.
 *
 * @author thaivc
 * @since 2024
 */
public class BufferUtil
{

    /**
     * Private constructor to prevent instantiation.
     */
    private BufferUtil()
    {
    }

    /**
     * The base offset of the byte array in memory.
     * Used for calculating the memory address of array elements.
     */
    public static final long ARRAY_BASE_OFFSET = UnsafeHelper.UNSAFE.arrayBaseOffset(byte[].class);

    /**
     * VarHandle for accessing the "hb" field in ByteBuffer.
     * This field represents the underlying byte array of the ByteBuffer.
     */
    public static final VarHandle BYTE_BUFFER_HB_HANDLE;

    /**
     * VarHandle for accessing the "offset" field in ByteBuffer.
     * This field represents the offset of the underlying byte array in the ByteBuffer.
     */
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

    /**
     * Returns the underlying byte array of the given ByteBuffer.
     *
     * @param buffer the ByteBuffer to extract the byte array from
     * @return the underlying byte array
     * @throws IllegalArgumentException if the buffer is direct
     */
    public static byte[] array(final ByteBuffer buffer)
    {
        if (buffer.isDirect())
        {
            throw new IllegalArgumentException("buffer must wrap an array");
        }

        return (byte[])BYTE_BUFFER_HB_HANDLE.get(buffer);
    }

    /**
     * Returns the offset of the underlying byte array in the given ByteBuffer.
     *
     * @param buffer the ByteBuffer to extract the offset from
     * @return the offset of the underlying byte array
     */
    public static int arrayOffset(final ByteBuffer buffer)
    {
        return (int)BYTE_BUFFER_OFFSET_HANDLE.get(buffer);
    }
}