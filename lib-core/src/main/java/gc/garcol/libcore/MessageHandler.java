package gc.garcol.libcore;

/**
 * Functional interface for handling messages read from a buffer.
 *
 * @author thaivc
 * @since 2024
 */
@FunctionalInterface
public interface MessageHandler
{

    /**
     * Called for the processing of each message read from a buffer in turn.
     *
     * @param msgTypeId the type identifier of the message
     * @param buffer    the buffer containing the message
     * @param index     the starting index of the message in the buffer
     * @param length    the length of the message in the buffer
     * @return true if the consumed position should be committed, false otherwise
     */
    boolean onMessage(int msgTypeId, UnsafeBuffer buffer, int index, int length);
}
