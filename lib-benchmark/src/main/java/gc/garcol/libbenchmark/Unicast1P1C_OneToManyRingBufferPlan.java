package gc.garcol.libbenchmark;

import gc.garcol.libcore.MessageHandler;
import gc.garcol.libcore.OneToManyRingBuffer;
import gc.garcol.libcore.UnsafeBuffer;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Reference to https://github.com/LMAX-Exchange/disruptor/blob/master/src/jmh/java/com/lmax/disruptor/BlockingQueueBenchmark.java
 *
 * @author thaivc
 * @since 2024
 */
@State(Scope.Benchmark)
public class Unicast1P1C_OneToManyRingBufferPlan
{

    OneToManyRingBuffer ringBuffer;
    byte[] data = "Hello, World!".getBytes();
    ByteBuffer writeBuffer = ByteBuffer.allocate(1 << 16);
    MessageHandler messageHandler;
    AtomicBoolean consumerRunning = new AtomicBoolean(true);

    @Setup(Level.Trial)
    public void setUp(Blackhole blackhole) throws InterruptedException
    {
        ringBuffer = new OneToManyRingBuffer(10, 1);

        messageHandler = new MessageHandler()
        {
            public boolean onMessage(final int msgTypeId, final UnsafeBuffer buffer, final int index, final int length)
            {
                blackhole.consume(msgTypeId);
                return true;
            }
        };

        final CountDownLatch consumerStartedLatch = new CountDownLatch(1);

        final Thread eventHandler = new Thread(() ->
        {
            consumerStartedLatch.countDown();
            while (consumerRunning.get())
            {
                ringBuffer.read(0, messageHandler);
                Thread.yield();
            }
        });
        eventHandler.start();
        consumerStartedLatch.await();
    }

    @TearDown
    public void tearDown()
    {
        consumerRunning.set(true);
    }
}
