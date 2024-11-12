package gc.garcol.libcore;

import sun.misc.Unsafe;

/**
 * @author thaivc
 * @since 2024
 */
public class MemoryAccess
{
    public static final Unsafe UNSAFE = Unsafe.getUnsafe();

    public static void loadFence()
    {
        UNSAFE.loadFence();
    }

    public static void storeFence()
    {
        UNSAFE.storeFence();
    }

    public static void fullFence()
    {
        UNSAFE.fullFence();
    }
}
