package io.github.benrushcc.std;

import io.github.benrushcc.common.Utils;
import io.github.benrushcc.common.experimental.ExStableValue;
import io.github.benrushcc.lib.LibContext;
import io.github.benrushcc.std.lib.SysMemLib;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public final class SysMem {
    private SysMem() {
        Utils.unsupportedInstantiated();
    }

    private static final Supplier<Mem> MEM_SUPPLIER = ExStableValue.supplier(() -> new Mem() {
        private static final SysMemLib lib = LibContext.acquire(SysMemLib.class);
        @Override
        public long alignmentBoundary() {
            return ValueLayout.ADDRESS.byteAlignment();
        }

        @Override
        public MemorySegment allocateMemory(long size) {
            return lib.malloc(size).reinterpret(size);
        }

        @Override
        public MemorySegment reallocMemory(MemorySegment seg, long newSize) {
            return lib.realloc(seg, newSize).reinterpret(newSize);
        }

        @Override
        public void freeMemory(MemorySegment seg) {
            lib.free(seg);
        }

        @Override
        public MemorySegment alignedAllocateMemory(long alignment, long size) {
            return lib.alignedAlloc(alignment, size).reinterpret(size);
        }

        @Override
        public void alignedFreeMemory(MemorySegment seg) {
            lib.alignedFree(seg);
        }
    });

    public static Mem instance() {
        return MEM_SUPPLIER.get();
    }
}
