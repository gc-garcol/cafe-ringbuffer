package gc.garcol.libcore;

import java.nio.ByteBuffer;

import static gc.garcol.libcore.RingBufferUtil.*;

/**
 * A ring buffer that supports one producer and multiple consumers.
 * Provides methods to write messages to the buffer and read messages from the buffer.
 * Ensures memory visibility guarantees using happen-before relationships.
 *
 * @author thaivc
 * @since 2024
 */
public class OneToManyRingBuffer
{
    private final UnsafeBuffer unsafeBuffer;

    /**
     * The pointers buffer contains the producer position and consumer positions.
     * <p>
     * [64 padding bytes] | producer position: 8 bytes |  [(64 - 8) padding bytes] | consumer position 1: 8 bytes | ... | [(64 - 8) padding bytes] | consumer position n: 8 bytes | 64 padding bytes
     */
    private final UnsafeBuffer pointers;

    private final int capacity;
    private final int maxRecordLength;
    private final int lastConsumerIndex;
    private final int consumerSize;

    private final int producerPointerIndex;
    private final int[] consumerPointerIndexes;

    /**
     * The length of the header in bytes.
     * The header contains the length and type of the message.
     */
    public static final int HEADER_LENGTH = Integer.BYTES * 2; // length, type

    public static final int EXTRA_PADDING_LENGTH = Long.BYTES * 8;

    /**
     * Alignment as a multiple of bytes for each record.
     * Padding to align the record in order to prevent false sharing.
     */
    public static final int ALIGNMENT = Long.BYTES * 8; // padding to align the record in order to prevent false sharing

    /**
     * Constructs a OneToManyRingBuffer with the specified size and number of consumers.
     *
     * @param powSize      the power of two size for the ring buffer
     * @param consumerSize the number of consumers
     */
    public OneToManyRingBuffer(int powSize, int consumerSize)
    {
        Preconditions.checkArgument(powSize >= 10, "Ring buffer size must be greater than 1024");
        Preconditions.checkArgument(powSize <= 31, "Ring buffer size must be less than 2^31");
        Preconditions.checkArgument(consumerSize >= 1, "Consumer size must be greater than 0");

        capacity = 1 << powSize;
        unsafeBuffer = new UnsafeBuffer(capacity);
        pointers = new UnsafeBuffer(Long.BYTES * 8 + Long.BYTES + (Long.BYTES * 7 + Long.BYTES) * consumerSize + Long.BYTES * 8);
        lastConsumerIndex = consumerSize - 1;

        producerPointerIndex = Long.BYTES * 8;
        consumerPointerIndexes = new int[consumerSize];
        this.consumerSize = consumerSize;

        consumerPointerIndexes[0] = Long.BYTES * 8 + Long.BYTES + Long.BYTES * 7; // padding + producer-pointer-block + padding
        for (int i = 1; i < consumerSize; i++)
        {
            consumerPointerIndexes[i] = consumerPointerIndexes[i - 1] + Long.BYTES + Long.BYTES * 7; // padding + consumer-pointer-block + padding
        }

        maxRecordLength = capacity >> 3;
    }

    /**
     * Writes a message to the ring buffer.
     *
     * @param msgTypeId the type identifier of the message
     * @param message   the message to write, the limit must be equal to the message length
     * @return true if the message was written successfully, false otherwise
     */
    public boolean write(int msgTypeId, ByteBuffer message)
    {
        int messageLength = message.limit();
        final int recordLength = calculateRecordLength(messageLength);
        final int alignedRecordLength = BitUtil.align(recordLength, ALIGNMENT);
        checkMsgLength(alignedRecordLength, maxRecordLength);

        // [1] happen-before guarantee for reads
        long currentProducerPosition = pointers.getLongVolatile(producerPointerIndex);

        // when [1] happened, the [1] ensures that the these instructions are loaded from the main memory as well
        long firstConsumerPosition = pointers.getLong(consumerPointerIndexes[0]);
        long lastConsumerPosition = consumerSize == 1 ? firstConsumerPosition : pointers.getLong(consumerPointerIndexes[lastConsumerIndex]);

        int currentProducerOffset = offset(currentProducerPosition);
        boolean currentProducerFlip = flip(currentProducerPosition);
        int firstConsumerOffset = offset(firstConsumerPosition);
        boolean firstConsumerFlip = flip(firstConsumerPosition);
        int lastConsumerOffset = offset(lastConsumerPosition);
        boolean lastConsumerFlip = flip(lastConsumerPosition);

        final int expectedEndOffsetOfRecord = currentProducerOffset + alignedRecordLength - 1;

        boolean sameCircleWithFirstConsumer = sameCircle(currentProducerFlip, firstConsumerFlip);
        boolean sameCircleWithLastConsumer = sameCircle(currentProducerFlip, lastConsumerFlip);

        int realStartOfRecord = currentProducerOffset;

        boolean shouldFlip = false;

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
                    shouldFlip = true;
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

        int nextProducerOffset = (realStartOfRecord + alignedRecordLength) % capacity; // maybe nextProducerOffset == 0

        // when [2] happened, the [2] ensures that the these instructions are synchronized into main memory as well
        buffer.putBytes(realStartOfRecord + HEADER_LENGTH, message, 0, messageLength);
        buffer.putInt(realStartOfRecord, messageLength);
        buffer.putInt(realStartOfRecord + Integer.BYTES, msgTypeId);

        shouldFlip |= nextProducerOffset == 0;
        boolean newProducerFlip = shouldFlip != currentProducerFlip;
        long newProducerPosition = position(nextProducerOffset, newProducerFlip);
        // [2]: happen-before guarantee for writes
        pointers.putLongVolatile(producerPointerIndex, newProducerPosition);

        return true;
    }

    /**
     * Reads messages from the ring buffer for the specified consumer.
     *
     * @param consumerIndex the index of the consumer
     * @param handler       the handler to process the messages
     * @return the number of messages read
     */
    public int read(int consumerIndex, final MessageHandler handler)
    {
        return read(consumerIndex, handler, Integer.MAX_VALUE);
    }

    /**
     * Reads messages from the ring buffer for the specified consumer with a limit.
     *
     * @param consumerIndex the index of the consumer
     * @param handler       the handler to process the messages
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
     * Reads one message from the ring buffer for the specified consumer.
     *
     * @param consumerIndex the index of the consumer
     * @param handler       the handler to process the message
     * @return true if a message was read, false otherwise
     */
    public boolean readOne(int consumerIndex, final MessageHandler handler)
    {
        // [1] happen-before guarantee for reads
        long barrierPosition = consumerIndex == 0
            ? pointers.getLongVolatile(producerPointerIndex)
            : pointers.getLongVolatile(consumerPointerIndexes[consumerIndex - 1]);

        int previousBarrier = offset(barrierPosition);
        boolean previousFlip = flip(barrierPosition);

        long currentConsumerPosition = pointers.getLong(consumerPointerIndexes[consumerIndex]);
        int currentConsumerOffset = offset(currentConsumerPosition);
        boolean currentConsumerFlip = flip(currentConsumerPosition);

        if (sameCircle(currentConsumerFlip, previousFlip))
        {

            // . . . . . P . . . . . x
            //           C1

            // or

            // . . . . . C1 . . . . . x
            //           C2
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

        if (messageLength == 0)
        {

            //  . . . . . . C2 0 0 0
            //              C1
            //              P
            if (sameCircle(currentConsumerFlip, previousFlip))
            {
                return false;
            }

            // R  E C O R D
            // C1 . . . . . P . . . . .C2 0 0 0

            // or

            //
            // P . . . . . . C1 0 0 0

            // when [2] happened, the [2] ensures that the these instructions are synchronized into main memory as well
            unsafeBuffer.clearBytes(currentConsumerOffset, capacity - 1);

            int nextConsumerOffset = 0;
            boolean newFlip = !currentConsumerFlip;
            long newConsumerPosition = position(nextConsumerOffset, newFlip);

            // [2] happen-before guarantee for writes
            pointers.putLongVolatile(consumerPointerIndexes[consumerIndex], newConsumerPosition);
            return readOne(consumerIndex, handler);
        }

        int messageTypeId = unsafeBuffer.getInt(currentConsumerOffset + Integer.BYTES);

        boolean consumeSuccess = handler.onMessage(messageTypeId, unsafeBuffer, currentConsumerOffset + HEADER_LENGTH, messageLength);

        if (!consumeSuccess)
        {
            return false;
        }

        int recordLength = calculateRecordLength(messageLength);
        int alignedRecordLength = BitUtil.align(recordLength, ALIGNMENT);
        int endRecordOffset = currentConsumerOffset + alignedRecordLength - 1;

        if (consumerIndex == lastConsumerIndex)
        {
            // when [3] happened, the [3] ensures that the these instructions are synchronized into main memory as well
            unsafeBuffer.clearBytes(currentConsumerOffset, endRecordOffset);
        }

        int nextConsumerOffset = (endRecordOffset + 1) % capacity; // nextConsumerOffset == 0 if the endRecordOffset is at the end of the buffer
        boolean shouldFlip = nextConsumerOffset == 0;
        boolean newFlip = shouldFlip != currentConsumerFlip;

        long newConsumerPosition = position(nextConsumerOffset, newFlip);

        // [3] happen-before guarantee for writes
        pointers.putLongVolatile(consumerPointerIndexes[consumerIndex], newConsumerPosition);

        return true;
    }

    private int calculateRecordLength(int messageLength)
    {
        return messageLength + HEADER_LENGTH + EXTRA_PADDING_LENGTH;
    }
}
