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
@Measurement(iterations = 3, time = 3)
@Fork(1)
public class Unicast1P1C_OneToManyRingBufferBechmark
{
    @Benchmark
    @Timeout(time = 15)
    @Warmup(iterations = 1, time = 1)
    public void publish(Unicast1P1C_OneToManyRingBufferPlan ringBufferPlan, Blackhole blackhole) throws IOException
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
