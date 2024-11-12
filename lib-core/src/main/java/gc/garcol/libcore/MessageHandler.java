package gc.garcol.libcore;

/**
 * @author thaivc
 * @since 2024
 */
@FunctionalInterface
public interface MessageHandler
{
    void onMessage(int msgTypeId, UnsafeBuffer buffer, int index, int length);
}
