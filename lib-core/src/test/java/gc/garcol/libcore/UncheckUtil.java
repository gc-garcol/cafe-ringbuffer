package gc.garcol.libcore;

/**
 * @author thaivc
 * @since 2024
 */
public class UncheckUtil
{

    public interface RunnableT
    {
        void run() throws Exception;
    }

    public static void run(RunnableT runnable)
    {
        try
        {
            runnable.run();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
