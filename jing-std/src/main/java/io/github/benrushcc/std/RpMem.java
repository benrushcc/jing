package io.github.benrushcc.std;

import io.github.benrushcc.common.Utils;
import io.github.benrushcc.lib.LibContext;
import io.github.benrushcc.std.lib.RpMemLib;

import java.lang.foreign.MemorySegment;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@SuppressWarnings("unused")
public final class RpMem {
    private RpMem() {
        Utils.unsupportedInstantiated();
    }

    private static final Lock lock = new ReentrantLock();

    private static int state = 0;

    private static final RpMemLib RP_MEM_LIB = LibContext.acquire(RpMemLib.class);

    private static final Mem MEM = new Mem() {
        @Override
        public long alignmentBoundary() {
            return 16;
        }

        @Override
        public MemorySegment allocateMemory(long size) {
            return RP_MEM_LIB.rpMalloc(size).reinterpret(size);
        }

        @Override
        public MemorySegment reallocMemory(MemorySegment seg, long newSize) {
            return RP_MEM_LIB.rpRealloc(seg, newSize).reinterpret(newSize);
        }

        @Override
        public void freeMemory(MemorySegment seg) {
            RP_MEM_LIB.rpFree(seg);
        }

        @Override
        public MemorySegment alignedAllocateMemory(long alignment, long size) {
            return RP_MEM_LIB.rpAlignedAlloc(alignment, size).reinterpret(size);
        }

        @Override
        public void alignedFreeMemory(MemorySegment seg) {
            RP_MEM_LIB.rpFree(seg);
        }
    };

    public static Mem rpInitialize() {
        lock.lock();
        try {
            if(state++ == 0) {
                int r = RP_MEM_LIB.rpInitialize();
                if(r < 0) {
                    throw new RuntimeException("Failed to initialize RpMem");
                }
            }
            RP_MEM_LIB.rpThreadInitialize();
            return MEM;
        } finally {
            lock.unlock();
        }
    }

    public static void rpFinalize() {
        lock.lock();
        try {
            RpMemLib lib = RP_MEM_LIB;
            lib.rpThreadFinalize();
            if(--state == 0) {
                lib.rpFinalize();
            }
        } finally {
            lock.unlock();
        }
    }
}
