package gc.garcol.libcore;

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
    ByteBuffer writeMessageBuffer = ByteBuffer.allocate(1 << 10);

    @BeforeEach
    public void setUp()
    {
        writeMessageBuffer.clear();
    }

    @Test
    public void shouldWriteSuccessfully_1P3C_10()
    {
        String message = "Hello, world!";
        oneToManyRingBuffer = new OneToManyRingBuffer(10, 3);

        writeMessageBuffer.put(0, message.getBytes());
        writeMessageBuffer.flip();
        oneToManyRingBuffer.write(1, writeMessageBuffer);
    }
}
