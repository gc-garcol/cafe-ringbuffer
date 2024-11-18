package gc.garcol.libbenchmark;

import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * @author thaivc
 * @since 2024
 */
public class Pipeline1P3C_OneToManyRingBufferRunner
{

    public static void main(String[] args) throws RunnerException
    {
        Options options = new OptionsBuilder()
            .include(Pipeline1P3C_OneToManyRingBufferBechmark.class.getSimpleName())
            .resultFormat(ResultFormatType.JSON)
            .result("benchmark-result.Pipeline1P3C_one-to-many-ring-buffer.json")
            .jvmArgs("--add-opens", "java.base/java.nio=ALL-UNNAMED") // Add JVM argument
            .build();
        new Runner(options).run();
    }

}
