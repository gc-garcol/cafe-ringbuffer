package gc.garcol.libcore;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * @author thaivc
 * @since 2024
 */
public class OneToManyRingBufferTest
{

    OneToManyRingBuffer oneToManyRingBuffer;
    ByteBuffer messageBufferWriter = ByteBuffer.allocate(1 << 10);
    ByteBuffer messageBufferReader = ByteBuffer.allocate(1 << 10);

    @BeforeEach
    public void setUp()
    {
        messageBufferWriter.clear();
        messageBufferReader.clear();
    }

    @Test
    public void shouldWriteSuccessfully_1P3C_10()
    {
        String message = "Hello, world!";
        oneToManyRingBuffer = new OneToManyRingBuffer(10, 3);

        ByteBufferUtil.put(messageBufferWriter, 0, message.getBytes());
        messageBufferWriter.flip();
        oneToManyRingBuffer.write(1, messageBufferWriter);
    }

    @Test
    public void shouldConsumeSuccessfully_1P3C_10()
    {
        String message = "Hello, world!";
        oneToManyRingBuffer = new OneToManyRingBuffer(10, 3);

        ByteBufferUtil.put(messageBufferWriter, 0, message.getBytes());
        messageBufferWriter.flip();
        oneToManyRingBuffer.write(1, messageBufferWriter);

        MessageHandler handler = (msgTypeId, buffer, index, length) -> {
            System.out.println("msgTypeId: " + msgTypeId);
            System.out.println("index: " + index);
            System.out.println("length: " + length);

            messageBufferReader.clear();

            buffer.getBytes(index, messageBufferReader, 0, length);
            messageBufferReader.position(length);
            messageBufferReader.flip();

            byte[] messageBytes = new byte[length];
            messageBufferReader.get(messageBytes);

            System.out.println("message: " + new String(messageBytes));
            Assertions.assertEquals(message, new String(messageBytes));

            return true;
        };

        oneToManyRingBuffer.read(0, handler);
        oneToManyRingBuffer.read(1, handler);
        oneToManyRingBuffer.read(2, handler);
    }

    @Test
    public void shouldConsumeWithLimitSuccessfully_1P3C_10()
    {
        List<String> messages = List.of(
            "Hello, world! 1",
            "Hello, world! 2",
            "Hello, world! 3"
        );

        List<AtomicInteger> consumedIndexes = List.of(new AtomicInteger(), new AtomicInteger(), new AtomicInteger());

        oneToManyRingBuffer = new OneToManyRingBuffer(10, 3);

        for (int i = 0; i < messages.size(); i++)
        {
            ByteBufferUtil.put(messageBufferWriter, 0, messages.get(i).getBytes());
            messageBufferWriter.flip();
            oneToManyRingBuffer.write(i, messageBufferWriter);
        }

        Function<Integer, MessageHandler> handlerSupplier = (Integer consumerId) ->
            (int msgTypeId, UnsafeBuffer buffer, int index, int length) -> {
                System.out.println("----------");
                System.out.println("consumerId: " + consumerId);
                System.out.println("msgTypeId: " + msgTypeId);
                System.out.println("index: " + index);
                System.out.println("length: " + length);

                messageBufferReader.clear();

                buffer.getBytes(index, messageBufferReader, 0, length);
                messageBufferReader.position(length);
                messageBufferReader.flip();

                byte[] messageBytes = new byte[length];
                messageBufferReader.get(messageBytes);

                System.out.println("message: " + new String(messageBytes));

                Assertions.assertEquals(messages.get(msgTypeId), new String(messageBytes));

                consumedIndexes.get(consumerId).incrementAndGet();
                return true;
            };

        oneToManyRingBuffer.read(0, handlerSupplier.apply(0), 3);
        oneToManyRingBuffer.read(1, handlerSupplier.apply(1), 2);
        oneToManyRingBuffer.read(2, handlerSupplier.apply(2), 1);

        Assertions.assertEquals(3, consumedIndexes.get(0).get());
        Assertions.assertEquals(2, consumedIndexes.get(1).get());
        Assertions.assertEquals(1, consumedIndexes.get(2).get());
    }

    @Test
    public void shouldConsumeThreadSafe_1P2C_10()
    {
        List<String> messages = IntStream.range(0, 10).boxed().map(i -> "Hello, world! " + i).toList();

        List<AtomicInteger> consumedIndexes = List.of(new AtomicInteger(), new AtomicInteger(), new AtomicInteger());

        oneToManyRingBuffer = new OneToManyRingBuffer(10, 2);

        for (int i = 0; i < messages.size(); i++)
        {
            ByteBufferUtil.put(messageBufferWriter, 0, messages.get(i).getBytes());
            messageBufferWriter.flip();
            oneToManyRingBuffer.write(i, messageBufferWriter);
        }

        Function<Integer, MessageHandler> handlerSupplier = (Integer consumerId) ->
            (int msgTypeId, UnsafeBuffer buffer, int index, int length) -> {
                System.out.println("---------- " + Thread.currentThread().getName());
                System.out.println("consumerId: " + consumerId);
                System.out.println("msgTypeId: " + msgTypeId);
                System.out.println("index: " + index);
                System.out.println("length: " + length);

                messageBufferReader.clear();

                buffer.getBytes(index, messageBufferReader, 0, length);
                messageBufferReader.position(length);
                messageBufferReader.flip();

                byte[] messageBytes = new byte[length];
                messageBufferReader.get(messageBytes);

                System.out.println("message: " + new String(messageBytes));

                Assertions.assertEquals(messages.get(msgTypeId), new String(messageBytes));

                consumedIndexes.get(consumerId).incrementAndGet();
                return true;
            };

        final AtomicBoolean end = new AtomicBoolean(false);

        Thread thread0 = new Thread(() -> {
            for (int i = 0; i < messages.size(); i++)
            {
                UncheckUtil.run(() -> Thread.sleep(15));
                oneToManyRingBuffer.read(0, handlerSupplier.apply(0), 1);
            }
            end.set(true);
        });
        Thread thread1 = new Thread(() -> {
            while (!end.get())
            {
                UncheckUtil.run(() -> Thread.sleep(50));
                oneToManyRingBuffer.read(1, handlerSupplier.apply(1));
            }
        });

        thread0.start();
        thread1.start();
        UncheckUtil.run(thread0::join);
        UncheckUtil.run(thread1::join);

        Assertions.assertEquals(messages.size(), consumedIndexes.get(0).get());
        Assertions.assertEquals(messages.size(), consumedIndexes.get(1).get());
    }
}
