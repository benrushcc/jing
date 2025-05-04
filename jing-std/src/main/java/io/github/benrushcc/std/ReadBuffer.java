package io.github.benrushcc.std;

import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.VectorMask;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("Duplicates")
public sealed abstract class ReadBuffer permits ReadBuffer.SingleReadBuffer, ReadBuffer.MultipleReadBuffer {

    public static ReadBuffer as(MemorySegment segment) {
        return new SingleReadBuffer(segment);
    }

    public static ReadBuffer as(List<MemorySegment> segments) {
        Objects.requireNonNull(segments);
        if(segments.isEmpty()) {
            throw new IllegalArgumentException("segments must not be empty");
        } else if(segments.size() == 1) {
            return new SingleReadBuffer(segments.getFirst());
        } else {
            return new MultipleReadBuffer(segments);
        }
    }

    public abstract long readIndex();

    public abstract void setIndex(long index);

    public abstract long available();

    public abstract long size();

    public abstract byte[] copy(long startIndex, long endIndex);

    public abstract byte readByte();

    public abstract short readShort(ByteOrder byteOrder);

    public short readShort() {
        return readShort(ByteOrder.nativeOrder());
    }

    public abstract char readChar(ByteOrder byteOrder);

    public char readChar() {
        return readChar(ByteOrder.nativeOrder());
    }

    public abstract int readInt(ByteOrder byteOrder);

    public int readInt() {
        return readInt(ByteOrder.nativeOrder());
    }

    public abstract long readLong(ByteOrder byteOrder);

    public long readLong() {
        return readLong(ByteOrder.nativeOrder());
    }

    public abstract float readFloat(ByteOrder byteOrder);

    public float readFloat() {
        return readFloat(ByteOrder.nativeOrder());
    }

    public abstract double readDouble(ByteOrder byteOrder);

    public double readDouble() {
        return readDouble(ByteOrder.nativeOrder());
    }

    public abstract long search(byte b);

    public abstract long search(ByteMatcher matcher);

    public abstract long vectorSearch(ByteVectorMatcher matcher);

    private static long compileSwarPattern(byte b) {
        long pattern = b & 0xFFL;
        return pattern
                | (pattern << 8)
                | (pattern << 16)
                | (pattern << 24)
                | (pattern << 32)
                | (pattern << 40)
                | (pattern << 48)
                | (pattern << 56);
    }

    private static int swarCheck(long data, long pattern) {
        long mask = data ^ pattern; // this single line is the major cost that we are about 10% slower than the JDK version
        long match = (mask - 0x0101010101010101L) & ~mask & 0x8080808080808080L;
        ByteOrder o = ByteOrder.nativeOrder();
        if(o == ByteOrder.BIG_ENDIAN) {
            return Long.numberOfLeadingZeros(match) >>> 3;
        }else if(o == ByteOrder.LITTLE_ENDIAN) {
            return Long.numberOfTrailingZeros(match) >>> 3;
        }else {
            throw new RuntimeException("unreached");
        }
    }

    private static long linearSearch(long startIndex, MemorySegment segment, ByteMatcher matcher) {
        final int start = Math.toIntExact(startIndex);
        final int end = Math.toIntExact(segment.byteSize());
        for(int i = start; i < end; i++) {
            byte b = MemAccess.getByte(segment, i);
            if(matcher.match(b)) {
                return startIndex + i + Byte.BYTES;
            }
        }
        return -1L;
    }

    private static long swarSearch(long startIndex, MemorySegment segment, byte b) {
        final int start = Math.toIntExact(startIndex);
        final int end = Math.toIntExact(segment.byteSize());
        final long pattern = ReadBuffer.compileSwarPattern(b);
        // fallback to linearSearch if data length is too small
        if(end - start < Long.BYTES) {
            return linearSearch(startIndex, segment, target -> target == b);
        }
        // check the first part
        int r = swarCheck(MemAccess.getLong(segment, start), pattern);
        if(r < Long.BYTES) {
            return startIndex + r + Byte.BYTES;
        }
        // check the middle part
        int index = Math.toIntExact(Long.BYTES - ((segment.address() + start) & (Long.BYTES - 1)) + start);
        int tail = Math.toIntExact(end - Long.BYTES);
        for( ; index <= tail; index += Long.BYTES) {
            r = swarCheck(MemAccess.getLong(segment, index), pattern);
            if(r < Long.BYTES) {
                return startIndex + index + r + Byte.BYTES;
            }
        }
        // check the tail part
        if(index < end) {
            r = swarCheck(MemAccess.getLong(segment, tail), pattern);
            if(r < Long.BYTES) {
                return startIndex + index + r + Byte.BYTES;
            }
        }
        return -1L;
    }

    private static long vectorSearch(long startIndex, MemorySegment segment, ByteVectorMatcher matcher) {
        final int start = Math.toIntExact(startIndex);
        final int end = Math.toIntExact(segment.byteSize());
        for(int index = start; index < end; index += ByteVector.SPECIES_PREFERRED.length()) {
            VectorMask<Byte> mask = ByteVector.SPECIES_PREFERRED.indexInRange(index, end);
            ByteVector vec = ByteVector.fromMemorySegment(ByteVector.SPECIES_PREFERRED, segment, index, ByteOrder.nativeOrder(), mask);
            int r = matcher.match(vec).firstTrue();
            if(r < ByteVector.SPECIES_PREFERRED.length()) {
                return startIndex + index + r + Byte.BYTES;
            }
        }
        return -1L;
    }

    private static final class SingleReadBuffer extends ReadBuffer {
        private final MemorySegment segment;
        private long readIndex = 0L;

        private SingleReadBuffer(MemorySegment m) {
            if(m == null || m.byteSize() == 0L) {
                throw new IllegalArgumentException("segment must not be empty");
            }
            this.segment = m.isReadOnly() ? m : m.asReadOnly();
        }

        @Override
        public long readIndex() {
            return readIndex;
        }

        @Override
        public void setIndex(long index) {
            if(index >= 0L && index <= segment.byteSize()) {
                readIndex = index;
            }
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + segment.byteSize());
        }

        @Override
        public long available() {
            return Math.subtractExact(segment.byteSize(), readIndex);
        }

        @Override
        public long size() {
            return segment.byteSize();
        }

        @Override
        public byte[] copy(long startIndex, long endIndex) {
            return segment.asSlice(startIndex, endIndex).toArray(ValueLayout.JAVA_BYTE);
        }

        @Override
        public byte readByte() {
            byte r = MemAccess.getByte(segment, readIndex);
            readIndex = Math.addExact(readIndex, ValueLayout.JAVA_BYTE.byteSize());
            return r;
        }

        @Override
        public short readShort(ByteOrder byteOrder) {
            short r = MemAccess.getShort(segment, readIndex, byteOrder);
            readIndex = Math.addExact(readIndex, ValueLayout.JAVA_SHORT.byteSize());
            return r;
        }

        @Override
        public char readChar(ByteOrder byteOrder) {
            char r = MemAccess.getChar(segment, readIndex, byteOrder);
            readIndex = Math.addExact(readIndex, ValueLayout.JAVA_CHAR.byteSize());
            return r;
        }

        @Override
        public int readInt(ByteOrder byteOrder) {
            int r = MemAccess.getInt(segment, readIndex, byteOrder);
            readIndex = Math.addExact(readIndex, ValueLayout.JAVA_INT.byteSize());
            return r;
        }

        @Override
        public long readLong(ByteOrder byteOrder) {
            long r = MemAccess.getLong(segment, readIndex, byteOrder);
            readIndex = Math.addExact(readIndex, ValueLayout.JAVA_LONG.byteSize());
            return r;
        }

        @Override
        public float readFloat(ByteOrder byteOrder) {
            float r = MemAccess.getFloat(segment, readIndex, byteOrder);
            readIndex = Math.addExact(readIndex, ValueLayout.JAVA_FLOAT.byteSize());
            return r;
        }

        @Override
        public double readDouble(ByteOrder byteOrder) {
            double r = MemAccess.getDouble(segment, readIndex, byteOrder);
            readIndex = Math.addExact(readIndex, ValueLayout.JAVA_DOUBLE.byteSize());
            return r;
        }

        @Override
        public long search(byte b) {
            long nextIndex = ReadBuffer.swarSearch(readIndex, segment, b);
            if(nextIndex >= 0L) {
                readIndex = nextIndex;
            }
            return nextIndex;
        }

        @Override
        public long search(ByteMatcher matcher) {
            long nextIndex = ReadBuffer.linearSearch(readIndex, segment, matcher);
            if(nextIndex >= 0L) {
                readIndex = nextIndex;
            }
            return nextIndex;
        }

        @Override
        public long vectorSearch(ByteVectorMatcher matcher) {
            long nextIndex = ReadBuffer.vectorSearch(readIndex, segment, matcher);
            if(nextIndex >= 0L) {
                readIndex = nextIndex;
            }
            return nextIndex;
        }
    }

    private static final class MultipleReadBuffer extends ReadBuffer {
        private final List<MemorySegment> segments;
        private final long size;
        private int listIndex = 0;
        private long segmentIndex = 0L;
        private long readIndex = 0L;

        private MultipleReadBuffer(List<MemorySegment> ms) {
            long s = 0L;
            segments = new ArrayList<>(ms.size());
            for (MemorySegment m : ms) {
                if(m == null || m.byteSize() == 0L) {
                    throw new IllegalArgumentException("segment must not be empty");
                }
                s = Math.addExact(s, m.byteSize());
                segments.add(m.isReadOnly() ? m : m.asReadOnly());
            }
            size = s;
        }

        @Override
        public long readIndex() {
            return readIndex;
        }

        @Override
        public void setIndex(long index) {
            if(index >= 0L && index <= size) {
                readIndex = index;
            }
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }

        @Override
        public long available() {
            return Math.subtractExact(size, readIndex);
        }

        @Override
        public long size() {
            return size;
        }

        @Override
        public byte[] copy(long startIndex, long endIndex) {
            if(startIndex < 0L || endIndex > size || startIndex > endIndex) {
                throw new IndexOutOfBoundsException();
            }
            long total = Math.subtractExact(endIndex, startIndex);
            if(total < 0L) {
                throw new IndexOutOfBoundsException();
            }else if(total == 0L) {
                return new byte[0];
            }else {
                byte[] bytes = new byte[Math.toIntExact(total)];
                MemorySegment m = MemorySegment.ofArray(bytes);
                long index = 0L;
                for (MemorySegment target : segments) {
                    long len = target.byteSize();
                    if (len <= startIndex) {
                        startIndex = Math.subtractExact(startIndex, len);
                        endIndex = Math.subtractExact(endIndex, len);
                    } else if (len <= endIndex) {
                        long copied = Math.subtractExact(len, startIndex);
                        MemorySegment.copy(target, startIndex, m, index, copied);
                        startIndex = 0L;
                        endIndex = Math.subtractExact(endIndex, len);
                    } else {
                        MemorySegment.copy(target, startIndex, m, index, endIndex);
                        return bytes;
                    }
                }
                throw new RuntimeException("unreached");
            }
        }

        private MemorySegment joinSegment(int size) {
            MemorySegment heap = MemorySegment.ofArray(new byte[size]);
            long heapIndex = 0L;
            long offset = segmentIndex;
            int currentIndex = listIndex;
            long required;
            long available;
            do {
                MemorySegment target = segments.get(currentIndex);
                required = Math.subtractExact(heap.byteSize(), heapIndex);
                available = Math.subtractExact(target.byteSize(), offset);
                if(available >= required) {
                    MemorySegment.copy(target, offset, heap, heapIndex, required);
                    segmentIndex = required;
                    break ;
                } else {
                    MemorySegment.copy(target, offset, heap, heapIndex, available);
                    heapIndex = Math.addExact(heapIndex, available);
                    currentIndex = Math.addExact(currentIndex, 1);
                }
            } while (currentIndex < segments.size());
            listIndex = currentIndex;
            return heap;
        }

        @Override
        public byte readByte() {
            long nextReadIndex = Math.addExact(readIndex, ValueLayout.JAVA_BYTE.byteSize());
            if(nextReadIndex > size) {
                throw new IndexOutOfBoundsException();
            }
            readIndex = nextReadIndex;
            // fast path
            MemorySegment m = segments.get(listIndex);
            long nextIndex = Math.addExact(segmentIndex, ValueLayout.JAVA_BYTE.byteSize());
            if(nextIndex <= m.byteSize()) {
                byte r = MemAccess.getByte(m, nextIndex);
                segmentIndex = nextIndex;
                return r;
            }
            // slow path
            return MemAccess.getByte(joinSegment(Byte.BYTES), 0L);
        }

        @Override
        public short readShort(ByteOrder byteOrder) {
            long nextReadIndex = Math.addExact(readIndex, ValueLayout.JAVA_SHORT.byteSize());
            if(nextReadIndex > size) {
                throw new IndexOutOfBoundsException();
            }
            readIndex = nextReadIndex;
            // fast path
            MemorySegment m = segments.get(listIndex);
            long nextIndex = Math.addExact(segmentIndex, ValueLayout.JAVA_SHORT.byteSize());
            if(nextIndex <= m.byteSize()) {
                short r = MemAccess.getShort(m, nextIndex, byteOrder);
                segmentIndex = nextIndex;
                return r;
            }
            // slow path
            return MemAccess.getShort(joinSegment(Short.BYTES), 0L, byteOrder);
        }

        @Override
        public char readChar(ByteOrder byteOrder) {
            long nextReadIndex = Math.addExact(readIndex, ValueLayout.JAVA_CHAR.byteSize());
            if(nextReadIndex > size) {
                throw new IndexOutOfBoundsException();
            }
            readIndex = nextReadIndex;
            // fast path
            MemorySegment m = segments.get(listIndex);
            long nextIndex = Math.addExact(segmentIndex, ValueLayout.JAVA_CHAR.byteSize());
            if(nextIndex <= m.byteSize()) {
                char r = MemAccess.getChar(m, nextIndex, byteOrder);
                segmentIndex = nextIndex;
                return r;
            }
            // slow path
            return MemAccess.getChar(joinSegment(Character.BYTES), 0L, byteOrder);
        }

        @Override
        public int readInt(ByteOrder byteOrder) {
            long nextReadIndex = Math.addExact(readIndex, ValueLayout.JAVA_INT.byteSize());
            if(nextReadIndex > size) {
                throw new IndexOutOfBoundsException();
            }
            readIndex = nextReadIndex;
            // fast path
            MemorySegment m = segments.get(listIndex);
            long nextIndex = Math.addExact(segmentIndex, ValueLayout.JAVA_INT.byteSize());
            if(nextIndex <= m.byteSize()) {
                int r = MemAccess.getInt(m, nextIndex, byteOrder);
                segmentIndex = nextIndex;
                return r;
            }
            // slow path
            return MemAccess.getInt(joinSegment(Integer.BYTES), 0L, byteOrder);
        }

        @Override
        public long readLong(ByteOrder byteOrder) {
            long nextReadIndex = Math.addExact(readIndex, ValueLayout.JAVA_LONG.byteSize());
            if(nextReadIndex > size) {
                throw new IndexOutOfBoundsException();
            }
            readIndex = nextReadIndex;
            // fast path
            MemorySegment m = segments.get(listIndex);
            long nextIndex = Math.addExact(segmentIndex, ValueLayout.JAVA_LONG.byteSize());
            if(nextIndex <= m.byteSize()) {
                long r = MemAccess.getLong(m, nextIndex, byteOrder);
                segmentIndex = nextIndex;
                return r;
            }
            // slow path
            return MemAccess.getLong(joinSegment(Long.BYTES), 0L, byteOrder);
        }

        @Override
        public float readFloat(ByteOrder byteOrder) {
            long nextReadIndex = Math.addExact(readIndex, ValueLayout.JAVA_FLOAT.byteSize());
            if(nextReadIndex > size) {
                throw new IndexOutOfBoundsException();
            }
            readIndex = nextReadIndex;
            // fast path
            MemorySegment m = segments.get(listIndex);
            long nextIndex = Math.addExact(segmentIndex, ValueLayout.JAVA_FLOAT.byteSize());
            if(nextIndex <= m.byteSize()) {
                float r = MemAccess.getFloat(m, nextIndex, byteOrder);
                segmentIndex = nextIndex;
                return r;
            }
            // slow path
            return MemAccess.getFloat(joinSegment(Float.BYTES), 0L, byteOrder);
        }

        @Override
        public double readDouble(ByteOrder byteOrder) {
            long nextReadIndex = Math.addExact(readIndex, ValueLayout.JAVA_DOUBLE.byteSize());
            if(nextReadIndex > size) {
                throw new IndexOutOfBoundsException();
            }
            readIndex = nextReadIndex;
            // fast path
            MemorySegment m = segments.get(listIndex);
            long nextIndex = Math.addExact(segmentIndex, ValueLayout.JAVA_DOUBLE.byteSize());
            if(nextIndex <= m.byteSize()) {
                double r = MemAccess.getDouble(m, nextIndex, byteOrder);
                segmentIndex = nextIndex;
                return r;
            }
            // slow path
            return MemAccess.getDouble(joinSegment(Double.BYTES), 0L, byteOrder);
        }

        @Override
        public long search(byte b) {
            long s = segmentIndex;
            long r = readIndex;
            for(int i = listIndex; i < segments.size(); i++) {
                MemorySegment target = segments.get(i);
                long nextIndex = ReadBuffer.swarSearch(s, target, b);
                if(nextIndex >= 0L) {
                    readIndex = Math.addExact(r, Math.subtractExact(nextIndex, s));
                    segmentIndex = nextIndex;
                    listIndex = i;
                    return readIndex;
                } else {
                    r = Math.addExact(r, Math.subtractExact(target.byteSize(), s));
                    s = 0L;
                }
            }
            return -1L;
        }

        @Override
        public long search(ByteMatcher matcher) {
            long s = segmentIndex;
            long r = readIndex;
            for(int i = listIndex; i < segments.size(); i++) {
                MemorySegment target = segments.get(i);
                long nextIndex = ReadBuffer.linearSearch(s, target, matcher);
                if(nextIndex >= 0L) {
                    readIndex = Math.addExact(r, Math.subtractExact(nextIndex, s));
                    segmentIndex = nextIndex;
                    listIndex = i;
                    return readIndex;
                } else {
                    r = Math.addExact(r, Math.subtractExact(target.byteSize(), s));
                    s = 0L;
                }
            }
            return -1L;
        }

        @Override
        public long vectorSearch(ByteVectorMatcher matcher) {
            long s = segmentIndex;
            long r = readIndex;
            for(int i = listIndex; i < segments.size(); i++) {
                MemorySegment target = segments.get(i);
                long nextIndex = ReadBuffer.vectorSearch(s, target, matcher);
                if(nextIndex >= 0L) {
                    readIndex = Math.addExact(r, Math.subtractExact(nextIndex, s));
                    segmentIndex = nextIndex;
                    listIndex = i;
                    return readIndex;
                } else {
                    r = Math.addExact(r, Math.subtractExact(target.byteSize(), s));
                    s = 0L;
                }
            }
            return -1L;
        }
    }
}
