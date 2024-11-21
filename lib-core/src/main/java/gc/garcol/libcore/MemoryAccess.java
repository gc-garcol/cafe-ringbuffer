package gc.garcol.libcore;

/**
 * Utility class for memory access operations using Unsafe.
 *
 * @author thaivc
 * @since 2024
 */
public class MemoryAccess
{
    /**
     * Ensures that all previous loads are visible to subsequent loads.
     * This method uses Unsafe's loadFence to provide the memory fence.
     */
    public static void loadFence()
    {
        UnsafeHelper.UNSAFE.loadFence();
    }

    /**
     * Ensures that all previous stores are visible to subsequent stores.
     * This method uses Unsafe's storeFence to provide the memory fence.
     */
    public static void storeFence()
    {
        UnsafeHelper.UNSAFE.storeFence();
    }

    /**
     * Ensures that all previous loads and stores are visible to subsequent loads and stores.
     * This method uses Unsafe's fullFence to provide the memory fence.
     */
    public static void fullFence()
    {
        UnsafeHelper.UNSAFE.fullFence();
    }
}