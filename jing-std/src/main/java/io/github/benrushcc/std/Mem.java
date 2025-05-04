package io.github.benrushcc.std;

import java.lang.foreign.MemorySegment;

@SuppressWarnings("unused")
public interface Mem {
    /// Minimal alignment boundary, for 64-bit machine, it's usually 8
    long alignmentBoundary();

    MemorySegment allocateMemory(long size);

    MemorySegment reallocMemory(MemorySegment seg, long newSize);

    void freeMemory(MemorySegment seg);

    MemorySegment alignedAllocateMemory(long alignment, long size);

    void alignedFreeMemory(MemorySegment seg);
}
