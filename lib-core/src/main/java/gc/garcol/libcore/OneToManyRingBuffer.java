package gc.garcol.libcore;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

/**
 * @author thaivc
 * @since 2024
 */
public class OneToManyRingBuffer
{
    private final UnsafeBuffer unsafeBuffer;
    private final AtomicLong producerPosition = new AtomicLong();
    private final AtomicLongArray consumerPositions;

    private final int capacity;
    private final int maxMsgLength;
    private final int lastConsumerIndex;

    public static final int HEADER_LENGTH = Integer.BYTES * 2; // length, type

    /**
     * Alignment as a multiple of bytes for each record.
     */
    private static final int ALIGNMENT = HEADER_LENGTH;

    private static final int MIN_RECORD_LENGTH = BitUtil.align(Integer.BYTES + HEADER_LENGTH, ALIGNMENT);

    public OneToManyRingBuffer(int powSize, int consumerSize)
    {

        if (powSize < 10)
        {
            throw new IllegalArgumentException("Ring buffer size must be greater than 1024");
        }

        if (consumerSize < 1)
        {
            throw new IllegalArgumentException("Consumer size must be greater than 0");
        }

        capacity = 1 << powSize;
        unsafeBuffer = new UnsafeBuffer(capacity);
        consumerPositions = new AtomicLongArray(consumerSize);
        lastConsumerIndex = consumerSize - 1;

        maxMsgLength = capacity >> 3;
    }

    /**
     * Write a message to the ring buffer
     *
     * @param msgTypeId
     * @param message   the message to write, the limit must be equals to the message length
     * @return the message was written successfully or not
     */
    public boolean write(int msgTypeId, ByteBuffer message)
    {
        int msgLength = message.limit();
        checkMsgLength(msgLength);

        // [1] happen-before guarantee for reads
        long currentProducerPosition = producerPosition.get();
        long firstConsumerPosition = consumerPositions.get(0);
        long lastConsumerPosition = consumerPositions.get(lastConsumerIndex);

        int currentProducerOffset = offset(currentProducerPosition);
        boolean currentProducerFlip = flip(currentProducerPosition);
        int firstConsumerOffset = offset(firstConsumerPosition);
        boolean firstConsumerFlip = flip(firstConsumerPosition);
        int lastConsumerOffset = offset(lastConsumerPosition);
        boolean lastConsumerFlip = flip(lastConsumerPosition);

        final int recordLength = msgLength + HEADER_LENGTH;
        final int alignedRecordLength = BitUtil.align(recordLength, ALIGNMENT);

        final int expectedEndOffsetOfRecord = currentProducerOffset + alignedRecordLength - 1;

        boolean sameCircleWithFirstConsumer = sameCircle(currentProducerFlip, firstConsumerFlip);
        boolean sameCircleWithLastConsumer = sameCircle(currentProducerFlip, lastConsumerFlip);

        int realStartOfRecord = currentProducerOffset;

        // C3 . . C2 . . C1 . . P . . x
        if (sameCircleWithFirstConsumer && sameCircleWithLastConsumer)
        {
            //                R E C O R D
            // C3 . C2 . C1 . P . . x
            if (expectedEndOffsetOfRecord >= capacity)
            {
                // R E C O R D
                // . . . . . . C3 . C2 . C1 . . P . . x
                if (lastConsumerOffset > alignedRecordLength - 1)
                {
                    realStartOfRecord = 0; // jump to the beginning of the buffer
                }
                else
                {
                    return false;
                }
            }
            // else keeping the realStartOfRecord = producerOffset.get();

            //                      R E C O R D
            // C3 . . C2 . . C1 . . P . . . . x
        }
        else if (!sameCircleWithLastConsumer) // !sameCircleWithFirstConsumer, the producer is on the next circle which is higher than last consumer
        {
            //             R  E  C  O  R  D
            // .  C1  .  . P  .  .  .  C3 .  .  . C2  .  .  x
            if (expectedEndOffsetOfRecord >= lastConsumerOffset)
            {
                return false;
            }

            // else keeping the realStartOfRecord = producerOffset.get();

            //             R  E  C  O  R  D
            // .  C1  .  . P  .  .  .  .  .  C3 .  .  . C2  .  .  x
        }

        UnsafeBuffer buffer = this.unsafeBuffer;

        int nextProducerOffset = expectedEndOffsetOfRecord + 1;

        boolean shouldFlip = nextProducerOffset + MIN_RECORD_LENGTH > capacity - 1;
        if (shouldFlip)
        {
            nextProducerOffset = 0;
        }

        // when [2] happened, the [2] ensures that the these instructions are synchronized into main memory as well
        buffer.putBytes(realStartOfRecord + HEADER_LENGTH, message, 0, msgLength);
        buffer.putInt(realStartOfRecord, msgLength);
        buffer.putInt(realStartOfRecord + Integer.BYTES, msgTypeId);

        boolean newProducerFlip = shouldFlip != currentProducerFlip;
        long newProducerPosition = position(nextProducerOffset, newProducerFlip);
        // [2]: happen-before guarantee for writes
        producerPosition.set(newProducerPosition);

        return true;
    }

    /**
     * Read message from the ring buffer, apply for consumeIndex-th consumer
     */
    public int read(int consumerIndex, final MessageHandler handler)
    {
        return read(consumerIndex, handler, Integer.MAX_VALUE);
    }

    /**
     * Read message from the ring buffer, apply for consumeIndex-th consumer
     *
     * @param consumerIndex the index of the consumer
     * @param handler       the handler to process the message
     * @param limit         the maximum number of messages to read
     * @return the number of messages read
     */
    public int read(int consumerIndex, final MessageHandler handler, int limit)
    {
        for (int i = 0; i < limit; i++)
        {
            if (!readOne(consumerIndex, handler))
            {
                return i;
            }
        }
        return limit;
    }

    /**
     * Read one message from the ring buffer, apply for consumeIndex-th consumer
     *
     * @param consumerIndex the index of the consumer
     * @param handler       the handler to process the message
     * @return true if a message was read otherwise false
     */
    public boolean readOne(int consumerIndex, final MessageHandler handler)
    {
        // [1] happen-before guarantee for reads
        long barrierPosition = consumerIndex == 0 ? producerPosition.get() : consumerPositions.get(consumerIndex - 1);

        int previousBarrier = offset(barrierPosition);
        boolean previousFlip = flip(barrierPosition);

        long currentConsumerPosition = consumerPositions.get(consumerIndex);
        int currentConsumerOffset = offset(currentConsumerPosition);
        boolean currentConsumerFlip = flip(currentConsumerPosition);

        if (sameCircle(currentConsumerFlip, previousFlip))
        {

            if (currentConsumerOffset >= previousBarrier)
            {
                return false;
            }

            // R  E C O R D
            // C  . . . . . P . . . . . x

            // or

            // R  E C O R D
            // C1 . . . . . C2 . . . . . x
        }

        // when [1] happened, the [1] ensures that the these instructions are loaded from the main memory as well
        int messageLength = unsafeBuffer.getInt(currentConsumerOffset);

        boolean isLastMessage = false;
        if (messageLength == 0)
        {

            //  R E C O R D
            //  . . . . . . C2 0 0 0
            //              C1
            //              P
            if (sameCircle(currentConsumerFlip, previousFlip))
            {
                return false;
            }

            // R  E C O R D
            // C1 . . . . . . C2 0 0 0
            isLastMessage = true;
        }

        int messageTypeId = unsafeBuffer.getInt(currentConsumerOffset + Integer.BYTES);

        boolean consumeSuccess = handler.onMessage(messageTypeId, unsafeBuffer, currentConsumerOffset + HEADER_LENGTH, messageLength);

        if (!consumeSuccess)
        {
            return false;
        }

        int recordLength = messageLength + HEADER_LENGTH;
        int alignedRecordLength = BitUtil.align(recordLength, ALIGNMENT);
        int nextConsumerOffset = isLastMessage ? 0 : currentConsumerOffset + alignedRecordLength;

        boolean newFlip = isLastMessage != currentConsumerFlip;
        long newConsumerPosition = position(nextConsumerOffset, newFlip);

        if (consumerIndex == lastConsumerIndex)
        {
            int endClearOffset = isLastMessage ? capacity - 1 : nextConsumerOffset - 1;
            unsafeBuffer.clearBytes(currentConsumerOffset, endClearOffset);
        }

        // [2] happen-before guarantee for writes
        consumerPositions.set(consumerIndex, newConsumerPosition);

        return true;
    }

    private void checkMsgLength(final int length)
    {
        if (length < 0)
        {
            throw new IllegalArgumentException("invalid message length=" + length);
        }
        else if (length > maxMsgLength)
        {
            throw new IllegalArgumentException(
                "encoded message exceeds maxMsgLength=" + maxMsgLength + ", length=" + length);
        }
    }

    private boolean sameCircle(boolean firstFlip, boolean secondFlip)
    {
        return firstFlip == secondFlip;
    }

    private int offset(long position)
    {
        return (int)(position >> 32);
    }

    private boolean flip(long position)
    {
        return (int)(position & 0xFFFFFFFFL) == 1;
    }

    private long position(int offset, boolean flip)
    {
        return ((long)offset << 32) | (flip ? 1 : 0);
    }
}
