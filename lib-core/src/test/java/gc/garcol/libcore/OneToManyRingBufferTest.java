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
            Assertions.assertEquals(message, new String(messageBytes), "Message not match");

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
            messageBufferWriter.clear();
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

                Assertions.assertEquals(messages.get(msgTypeId), new String(messageBytes), "Message not match");

                consumedIndexes.get(consumerId).incrementAndGet();
                return true;
            };

        oneToManyRingBuffer.read(0, handlerSupplier.apply(0), 3);
        oneToManyRingBuffer.read(1, handlerSupplier.apply(1), 2);
        oneToManyRingBuffer.read(2, handlerSupplier.apply(2), 1);

        Assertions.assertEquals(3, consumedIndexes.get(0).get(), "Consumer 0 not consume 3 messages");
        Assertions.assertEquals(2, consumedIndexes.get(1).get(), "Consumer 1 not consume 2 messages");
        Assertions.assertEquals(1, consumedIndexes.get(2).get(), "Consumer 2 not consume 1 message");
    }

    @Test
    public void shouldConsumeThreadSafe_1P2C_10()
    {
        List<String> messages = IntStream.range(0, 10).boxed().map(i -> "Hello, world! " + i).toList();

        List<AtomicInteger> consumedIndexes = List.of(new AtomicInteger(), new AtomicInteger());

        oneToManyRingBuffer = new OneToManyRingBuffer(10, 2);

        for (int i = 0; i < messages.size(); i++)
        {
            messageBufferWriter.clear();
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

                Assertions.assertEquals(messages.get(msgTypeId), new String(messageBytes), "Message not match");

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

        Assertions.assertEquals(messages.size(), consumedIndexes.get(0).get(), "Consumer 0 not consume all messages");
        Assertions.assertEquals(messages.size(), consumedIndexes.get(1).get(), "Consumer 1 not consume all messages");
    }

    @Test
    public void shouldPublishWithLimit_1P1C_10()
    {
        List<String> messages = IntStream.range(0, 50).boxed().map(i -> "Hello, world! " + i).toList();
        oneToManyRingBuffer = new OneToManyRingBuffer(10, 1);
        for (int i = 0; i < messages.size(); i++)
        {
            messageBufferWriter.clear();
            ByteBufferUtil.put(messageBufferWriter, 0, messages.get(i).getBytes());
            messageBufferWriter.flip();
            boolean success = oneToManyRingBuffer.write(i, messageBufferWriter);
            System.out.println("write message " + i + " with length " + messageBufferWriter.limit() + " and align-length " + BitUtil.align(messageBufferWriter.limit() + OneToManyRingBuffer.HEADER_LENGTH, OneToManyRingBuffer.HEADER_LENGTH));
            if (!success)
            {
                System.out.println("Failed to write message: " + i);
                break;
            }
        }
    }

    @Test
    public void shouldPublishInNextCircle_1P1C_10()
    {
        List<String> messages = IntStream.range(0, 50).boxed().map(i -> "Hello, world! " + i).toList();
        oneToManyRingBuffer = new OneToManyRingBuffer(10, 1);

        AtomicInteger publishedMessages = new AtomicInteger(0);

        Runnable publishMessages = () -> {
            for (int i = 0; i < messages.size(); i++)
            {
                messageBufferWriter.clear();
                ByteBufferUtil.put(messageBufferWriter, 0, messages.get(i).getBytes());
                messageBufferWriter.flip();
                boolean success = oneToManyRingBuffer.write(i, messageBufferWriter);
                System.out.println("write message " + i + " with length " + messageBufferWriter.limit() + " and align-length " + BitUtil.align(messageBufferWriter.limit() + OneToManyRingBuffer.HEADER_LENGTH, OneToManyRingBuffer.HEADER_LENGTH));
                if (!success)
                {
                    System.out.println("Failed to write message: " + i);
                    break;
                }
                publishedMessages.incrementAndGet();
            }
        };

        publishMessages.run();

        MessageHandler handler = (msgTypeId, buffer, index, length) -> {
            System.out.println("--------------------");
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
            return true;
        };

        oneToManyRingBuffer.read(0, handler, 1);
        publishedMessages.decrementAndGet();

        messageBufferWriter.clear();
        ByteBufferUtil.put(messageBufferWriter, 0, "Hello, world! 1".getBytes());
        messageBufferWriter.flip();
        System.out.println("--------");
        System.out.println("write message 100 with length " + messageBufferWriter.limit() + " and align-length " + BitUtil.align(messageBufferWriter.limit() + OneToManyRingBuffer.HEADER_LENGTH, OneToManyRingBuffer.HEADER_LENGTH));

        boolean success = oneToManyRingBuffer.write(100, messageBufferWriter);
        publishedMessages.incrementAndGet();

        Assertions.assertTrue(success, "Failed to write message: 100");

        int readMessages = oneToManyRingBuffer.read(0, handler);
        Assertions.assertEquals(readMessages, publishedMessages.get(), "Read messages not equal to published messages");
    }
}
