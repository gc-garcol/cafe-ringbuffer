package gc.garcol.libcore;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

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
}
