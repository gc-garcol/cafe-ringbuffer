package gc.garcol.libcore;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * Utility class for accessing the Unsafe class.
 * Provides a static reference to the Unsafe instance.
 * Ensures the Unsafe instance is loaded and accessible.
 *
 * @author thaivc
 * @since 2024
 */
public class UnsafeHelper
{
    static final Unsafe UNSAFE;

    /**
     * Private constructor to prevent instantiation.
     */
    private UnsafeHelper()
    {
    }

    static
    {
        try
        {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            UNSAFE = (Unsafe)theUnsafe.get(null);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Unable to load Unsafe", e);
        }
    }
}