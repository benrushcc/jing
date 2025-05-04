package io.github.benrushcc.std;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public abstract sealed class WriteBuffer implements AutoCloseable permits WriteBuffer.NativeWriteBuffer, WriteBuffer.ReservedWriteBuffer, WriteBuffer.HeapWriteBuffer {

    private static final long DEFAULT_NATIVE_BUFFER_SIZE = Long.getLong("jing.writeBuffer.native.size", 4096L);

    private static final int DEFAULT_HEAP_BUFFER_SIZE = Integer.getInteger("jing.writeBuffer.heap.size", 4096);

    public static WriteBuffer nativeBuffer(Mem m, long size) {
        return new NativeWriteBuffer(m, size);
    }

    public static WriteBuffer nativeBuffer(Mem m) {
        return new NativeWriteBuffer(m, DEFAULT_NATIVE_BUFFER_SIZE);
    }

    public static WriteBuffer reserved(Mem m, MemorySegment segment) {
        return new ReservedWriteBuffer(m, segment);
    }

    public static WriteBuffer heap(int size) {
        return new HeapWriteBuffer(size);
    }

    public static WriteBuffer heap() {
        return new HeapWriteBuffer(DEFAULT_HEAP_BUFFER_SIZE);
    }

    protected MemorySegment segment = MemorySegment.NULL;
    protected long writeIndex = 0L;

    abstract MemorySegment resize(long nextIndex);

    @Override
    public abstract void close();

    public void writeByte(byte b) {
        long nextIndex = Math.addExact(writeIndex, ValueLayout.JAVA_BYTE.byteSize());
        if(nextIndex > segment.byteSize()) {
            segment = resize(nextIndex);
        }
        MemAccess.setByte(segment, writeIndex, b);
        writeIndex = nextIndex;
    }

    public void writeBytes(byte b1, byte b2) {
        long nextIndex = Math.addExact(writeIndex, Math.multiplyExact(ValueLayout.JAVA_BYTE.byteSize(), 2));
        if(nextIndex > segment.byteSize()) {
            segment = resize(nextIndex);
        }
        MemAccess.setByte(segment, writeIndex, b1);
        MemAccess.setByte(segment, Math.addExact(writeIndex, ValueLayout.JAVA_BYTE.byteSize()), b2);
        writeIndex = nextIndex;
    }

    public void writeBytes(byte b1, byte b2, byte b3) {
        long nextIndex = Math.addExact(writeIndex, Math.multiplyExact(ValueLayout.JAVA_BYTE.byteSize(), 3));
        if(nextIndex > segment.byteSize()) {
            segment = resize(nextIndex);
        }
        MemAccess.setByte(segment, writeIndex, b1);
        MemAccess.setByte(segment, Math.addExact(writeIndex, ValueLayout.JAVA_BYTE.byteSize()), b2);
        MemAccess.setByte(segment, Math.addExact(writeIndex, Math.multiplyExact(ValueLayout.JAVA_BYTE.byteSize(), 2)), b3);
        writeIndex = nextIndex;
    }

    public void writeBytes(byte[] bytes, int off, int len) {
        long nextIndex = Math.addExact(writeIndex, len);
        if(nextIndex > segment.byteSize()) {
            segment = resize(nextIndex);
        }
        MemorySegment src = MemorySegment.ofArray(bytes).asSlice(off, len);
        MemorySegment.copy(src, 0L, segment, writeIndex, len);
        writeIndex = nextIndex;
    }

    public void writeBytes(byte[] bytes) {
        long nextIndex = Math.addExact(writeIndex, bytes.length);
        if(nextIndex > segment.byteSize()) {
            segment = resize(nextIndex);
        }
        MemorySegment src = MemorySegment.ofArray(bytes);
        MemorySegment.copy(src, 0L, segment, writeIndex, bytes.length);
        writeIndex = nextIndex;
    }

    public void writeShort(short s, ByteOrder byteOrder) {
        long nextIndex = Math.addExact(writeIndex, ValueLayout.JAVA_SHORT.byteSize());
        if(nextIndex > segment.byteSize()) {
            segment = resize(nextIndex);
        }
        MemAccess.setShort(segment, writeIndex, s, byteOrder);
        writeIndex = nextIndex;
    }

    public void writeShort(short s) {
        writeShort(s, ByteOrder.nativeOrder());
    }

    public void writeChar(char c, ByteOrder byteOrder) {
        long nextIndex = Math.addExact(writeIndex, ValueLayout.JAVA_CHAR.byteSize());
        if (nextIndex > segment.byteSize()) {
            segment = resize(nextIndex);
        }
        MemAccess.setChar(segment, writeIndex, c, byteOrder);
        writeIndex = nextIndex;
    }

    public void writeChar(char c) {
        writeChar(c, ByteOrder.nativeOrder());
    }

    public void writeInt(int i, ByteOrder byteOrder) {
        long nextIndex = Math.addExact(writeIndex, ValueLayout.JAVA_INT.byteSize());
        if (nextIndex > segment.byteSize()) {
            segment = resize(nextIndex);
        }
        MemAccess.setInt(segment, writeIndex, i, byteOrder);
        writeIndex = nextIndex;
    }

    public void writeInt(int i) {
        writeInt(i, ByteOrder.nativeOrder());
    }

    public void writeLong(long l, ByteOrder byteOrder) {
        long nextIndex = Math.addExact(writeIndex, ValueLayout.JAVA_LONG.byteSize());
        if (nextIndex > segment.byteSize()) {
            segment = resize(nextIndex);
        }
        MemAccess.setLong(segment, writeIndex, l, byteOrder);
        writeIndex = nextIndex;
    }

    public void writeLong(long l) {
        writeLong(l, ByteOrder.nativeOrder());
    }

    public void writeFloat(float f, ByteOrder byteOrder) {
        long nextIndex = Math.addExact(writeIndex, ValueLayout.JAVA_FLOAT.byteSize());
        if (nextIndex > segment.byteSize()) {
            segment = resize(nextIndex);
        }
        MemAccess.setFloat(segment, writeIndex, f, byteOrder);
        writeIndex = nextIndex;
    }

    public void writeFloat(float f) {
        writeFloat(f, ByteOrder.nativeOrder());
    }

    public void writeDouble(double d, ByteOrder byteOrder) {
        long nextIndex = Math.addExact(writeIndex, ValueLayout.JAVA_DOUBLE.byteSize());
        if (nextIndex > segment.byteSize()) {
            segment = resize(nextIndex);
        }
        MemAccess.setDouble(segment, writeIndex, d, byteOrder);
        writeIndex = nextIndex;
    }

    public void writeDouble(double d) {
        writeDouble(d, ByteOrder.nativeOrder());
    }

    public void writeUtf8Str(String str) {
        MemorySegment m = MemorySegment.ofArray(str.getBytes(StandardCharsets.UTF_8));
        long nextIndex = Math.addExact(Math.addExact(writeIndex, m.byteSize()), 1);
        if(nextIndex > segment.byteSize()) {
            segment = resize(nextIndex);
        }
        MemorySegment.copy(m, 0L, segment, writeIndex, m.byteSize());
        MemAccess.setByte(segment, Math.addExact(writeIndex, m.byteSize()), (byte) '\0');
        writeIndex = nextIndex;
    }

    public void writeUtf8Data(int data) {
        if(data < 0x80) {
            writeByte((byte) data);
        }else if(data < 0x800) {
            writeBytes((byte) (0xC0 | (data >> 6)), (byte) (0x80 | (data & 0x3F)));
        }else {
            writeBytes((byte) (0xE0 | (data >> 12)), (byte) (0x80 | ((data >> 6) & 0x3F)), (byte) (0x80 | (data & 0x3F)));
        }
    }

    public void writeSegment(MemorySegment m) {
        long nextIndex = Math.addExact(writeIndex, m.byteSize());
        if(nextIndex > segment.byteSize()) {
            segment = resize(nextIndex);
        }
        MemorySegment.copy(m, 0L, segment, writeIndex, m.byteSize());
        writeIndex = nextIndex;
    }

    public MemorySegment content() {
        if(writeIndex == 0L) {
            return MemorySegment.NULL;
        }else if(writeIndex == segment.byteSize()) {
            return segment;
        }else {
            return segment.asSlice(0L, writeIndex);
        }
    }

    public static long grow(long cap) {
        long newCap = 1L << (Long.SIZE - Long.numberOfLeadingZeros(cap));
        if(newCap < 0L) {
            throw new ArithmeticException("Capacity overflow");
        }
        return newCap;
    }

    private static final class NativeWriteBuffer extends WriteBuffer {
        private final Mem mem;
        private final long initialSize;

        NativeWriteBuffer(Mem m, long size) {
            mem = m;
            initialSize = size;
        }

        @Override
        public void close() {
            mem.freeMemory(segment);
        }

        @Override
        MemorySegment resize(long nextIndex) {
            MemorySegment newSegment;
            if(segment.address() == 0L) {
                newSegment = mem.allocateMemory(Math.max(initialSize, grow(nextIndex)));
            } else {
                newSegment = mem.reallocMemory(segment, grow(nextIndex));
            }
            if(newSegment.address() == 0L) {
                throw new OutOfMemoryError();
            }
            return newSegment;
        }
    }

    private static final class ReservedWriteBuffer extends WriteBuffer {
        private final Mem mem;
        private final MemorySegment initialSegment;

        ReservedWriteBuffer(Mem m, MemorySegment reserved) {
            if(!reserved.isNative() || reserved.address() == 0L) {
                throw new RuntimeException("Invalid reserved segment");
            }
            mem = m;
            initialSegment = reserved;
            segment = reserved;
        }

        @Override
        MemorySegment resize(long nextIndex) {
            MemorySegment newSegment;
            if(segment.address() == initialSegment.address()) {
                newSegment = mem.allocateMemory(grow(nextIndex));
                if(writeIndex >= MemAccess.MEMCPY_THRESHOLD) {
                    MemAccess.memcpy(segment, 0L, newSegment, 0L, writeIndex);
                } else {
                    MemorySegment.copy(segment, 0L, newSegment, 0L, writeIndex);
                }
            } else {
                newSegment = mem.reallocMemory(segment, grow(nextIndex));
            }
            if(newSegment.address() == 0L) {
                throw new OutOfMemoryError();
            }
            return newSegment;
        }

        @Override
        public void close() {
            if(segment.address() != initialSegment.address()) {
                mem.freeMemory(segment);
            }
        }
    }

    private static final class HeapWriteBuffer extends WriteBuffer {
        private final int initialSize;

        HeapWriteBuffer(int size) {
            initialSize = size;
        }

        @Override
        MemorySegment resize(long nextIndex) {
            MemorySegment newSegment;
            if(segment.address() == 0L) {
                newSegment = MemorySegment.ofArray(new byte[Math.max(initialSize, Math.toIntExact(grow(nextIndex)))]);
            } else {
                byte[] base = (byte[]) segment.heapBase().orElseThrow();
                byte[] newArray = new byte[Math.toIntExact(grow(nextIndex))];
                System.arraycopy(base, 0, newArray, 0, Math.toIntExact(writeIndex));
                newSegment = MemorySegment.ofArray(newArray);
            }
            return newSegment;
        }

        @Override
        public void close() {
            // No external close operation needed for HeapWriteBuffer
        }
    }
}
