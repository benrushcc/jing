package io.github.benrushcc.std;

import io.github.benrushcc.common.experimental.ExStableValue;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.util.Arrays;

@SuppressWarnings({"preview", "unused"})
public sealed interface Allocator extends SegmentAllocator,  AutoCloseable {

    ScopedValue<Mem> MEM_SCOPE = ScopedValue.newInstance();

    default ScopedValue.Carrier scoped(Mem mem) {
        return ScopedValue.where(MEM_SCOPE, mem);
    }

    ExStableValue<Allocator> HEAP_ALLOCATOR = ExStableValue.of();

    boolean isNative();

    @Override
    void close(); // remove throws exception signature

    default Allocator heapAllocator() {
        return HEAP_ALLOCATOR.orElseSet(HeapAllocator::new);
    }

    default Allocator directAllocator(int size) {
        Mem m = MEM_SCOPE.orElse(SysMem.instance());
        if(size <= 0) {
            throw new IllegalArgumentException("Size must be greater than zero");
        }
        return new DirectFixedAllocator(m, size);
    }

    default Allocator directAllocator() {
        Mem m = MEM_SCOPE.orElse(SysMem.instance());
        return new DirectGrowableAllocator(m);
    }

    final class HeapAllocator implements Allocator {
        @Override
        public MemorySegment allocate(long byteSize, long byteAlignment) {
            switch (Math.toIntExact(byteAlignment)) {
                case Byte.BYTES -> {
                    // Make sure byte array allocation always be backed by a byte[] which could be fetched by heapBase()
                    return MemorySegment.ofArray(new byte[Math.toIntExact(byteSize)]);
                }
                case Short.BYTES, Integer.BYTES, Long.BYTES -> {
                    // A fair amount of waste wouldn't harm the system
                    MemorySegment m = MemorySegment.ofArray(new long[Math.toIntExact((byteSize + 7) >> 3)]);
                    return m.byteSize() == byteSize ? m : m.asSlice(0L, byteSize);
                }
                default -> throw new RuntimeException("Unexpected alignment : " + byteAlignment);
            }
        }

        @Override
        public boolean isNative() {
            return false;
        }

        @Override
        public void close() {
            // No external operations needed for heap allocator
        }
    }

    @SuppressWarnings("Duplicates")
    final class DirectFixedAllocator implements Allocator {
        private final Mem m;
        private final long[] ptrs;
        private int index = 0;

        private DirectFixedAllocator(Mem mem, int size) {
            m = mem;
            ptrs = new long[size];
        }

        @Override
        public MemorySegment allocate(long byteSize, long byteAlignment) {
            // do allocate, rely on jvm's boundary checking
            MemorySegment r;
            if(byteAlignment <= m.alignmentBoundary()) {
                r = m.allocateMemory(byteSize);
                ptrs[index++] = r.address();
            } else {
                r = m.alignedAllocateMemory(byteAlignment, byteSize);
                ptrs[index++] = r.address() & 1;
            }
            // check address
            if(r.address() == 0) {
                throw new OutOfMemoryError();
            }
            return r;
        }

        @Override
        public boolean isNative() {
            return true;
        }

        @Override
        public void close() {
            for(int i = 0; i < index; i++) {
                long ptr = ptrs[i];
                if((ptr & 1) ==  0) {
                    m.freeMemory(MemorySegment.ofAddress(ptr));
                } else {
                    m.alignedFreeMemory(MemorySegment.ofAddress(ptr & (~1)));
                }
            }
        }
    }

    @SuppressWarnings("Duplicates")
    final class DirectGrowableAllocator implements Allocator {
        private static final int INITIAL_SIZE = 4;
        private final Mem m;
        private long[] ptrs = new long[INITIAL_SIZE];
        private int index = 0;

        private DirectGrowableAllocator(Mem mem) {
            m = mem;
        }

        @Override
        public MemorySegment allocate(long byteSize, long byteAlignment) {
            // check alignment
            if (Long.bitCount(byteAlignment) != 1) {
                throw new RuntimeException("Unexpected alignment : " + byteAlignment);
            }
            // check array size
            if(index == ptrs.length) {
                ptrs = Arrays.copyOf(ptrs, Math.multiplyExact(index, 2));
            }
            // do allocate
            MemorySegment r;
            if(byteAlignment <= m.alignmentBoundary()) {
                r = m.allocateMemory(byteSize);
                ptrs[index++] = r.address();
            } else {
                r = m.alignedAllocateMemory(byteAlignment, byteSize);
                ptrs[index++] = r.address() & 1;
            }
            // check address
            if(r.address() == 0) {
                throw new OutOfMemoryError();
            }
            return r;
        }

        @Override
        public boolean isNative() {
            return true;
        }

        @Override
        public void close() {
            for(int i = 0; i < index; i++) {
                long ptr = ptrs[i];
                if((ptr & 1) ==  0) {
                    m.freeMemory(MemorySegment.ofAddress(ptr));
                } else {
                    m.alignedFreeMemory(MemorySegment.ofAddress(ptr & (~1)));
                }
            }
        }
    }
}
