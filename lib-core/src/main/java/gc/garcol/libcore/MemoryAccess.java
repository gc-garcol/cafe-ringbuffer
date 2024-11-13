package gc.garcol.libcore;

/**
 * @author thaivc
 * @since 2024
 */
public class MemoryAccess
{

    public static void loadFence()
    {
        UnsafeHelper.UNSAFE.loadFence();
    }

    public static void storeFence()
    {
        UnsafeHelper.UNSAFE.storeFence();
    }

    public static void fullFence()
    {
        UnsafeHelper.UNSAFE.fullFence();
    }
}
