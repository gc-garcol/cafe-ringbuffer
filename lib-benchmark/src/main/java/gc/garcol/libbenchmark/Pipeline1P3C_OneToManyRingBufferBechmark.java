package gc.garcol.libbenchmark;

import gc.garcol.libcore.ByteBufferUtil;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @author thaivc
 * @since 2024
 */
@State(Scope.Thread)
@BenchmarkMode({ Mode.Throughput, Mode.AverageTime })
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(1)
public class Pipeline1P3C_OneToManyRingBufferBechmark
{
    @Benchmark
    @Timeout(time = 60)
    @Measurement(iterations = 1, time = 60)
    @Warmup(iterations = 1, time = 10)
    public void publish(Pipeline1P3C_OneToManyRingBufferPlan ringBufferPlan, Blackhole blackhole) throws IOException
    {
        ringBufferPlan.writeBuffer.clear();
        ByteBufferUtil.put(ringBufferPlan.writeBuffer, 0, ringBufferPlan.data);
        ringBufferPlan.writeBuffer.flip();

        boolean publishSuccess = false;
        while (!publishSuccess)
        {
            publishSuccess = ringBufferPlan.ringBuffer.write(1, ringBufferPlan.writeBuffer);
        }
    }

}
