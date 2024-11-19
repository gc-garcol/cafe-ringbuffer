package gc.garcol.libcore;

/**
 * @author thaivc
 * @since 2024
 */
public class Preconditions
{
    public static void checkArgument(boolean success, String message)
    {
        if (!success)
        {
            throw new IllegalArgumentException(message);
        }
    }
}
