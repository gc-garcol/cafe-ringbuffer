package gc.garcol.libcore;

/**
 * Utility class for checking preconditions.
 * Provides methods to validate arguments and state conditions.
 * Throws appropriate exceptions if conditions are not met.
 *
 * @author thaivc
 * @since 2024
 */
public class Preconditions
{
    /**
     * Checks if the provided argument condition is true.
     * Throws an IllegalArgumentException with the specified message if the condition is false.
     *
     * @param success the condition to check
     * @param message the exception message to use if the check fails
     * @throws IllegalArgumentException if the condition is false
     */
    public static void checkArgument(boolean success, String message)
    {
        if (!success)
        {
            throw new IllegalArgumentException(message);
        }
    }
}