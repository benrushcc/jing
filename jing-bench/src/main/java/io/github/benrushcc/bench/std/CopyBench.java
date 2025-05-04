package io.github.benrushcc.bench.std;

import io.github.benrushcc.bench.BaseBench;
import io.github.benrushcc.std.MemAccess;
import org.openjdk.jmh.annotations.*;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Arrays;

public class CopyBench extends BaseBench {
    private byte[] src;
    private byte[] dst;
    private Arena arena;
    private MemorySegment mc;
    private MemorySegment mt;

    @Param({"32", "64", "256", "1024", "4096"})
    private int count;

    @Setup(Level.Iteration)
    public void setup() {
        src = new byte[count];
        dst = new byte[count];
        arena = Arena.ofConfined();
        mc = arena.allocate(count);
        mt = arena.allocate(count);
        Arrays.fill(src, (byte) 0);
        mc.fill((byte) 0);
    }

    @TearDown(Level.Iteration)
    public void tearDown() {
        arena.close();
    }

    @Benchmark
    public void arrayToArrayCopy() {
        System.arraycopy(src, 0, dst, 0, count);
    }

    @Benchmark
    public void heapCopy() {
        MemorySegment m1 = MemorySegment.ofArray(src);
        MemorySegment m2 = MemorySegment.ofArray(dst);
        MemorySegment.copy(m1, 0L, m2, 0L, count);
    }

    @Benchmark
    public void arrayToSegmentCopy() {
        MemorySegment.copy(src, 0, mt, ValueLayout.JAVA_BYTE, 0L, count);
    }

    @Benchmark
    public void segmentToArrayCopy() {
        MemorySegment.copy(mc, ValueLayout.JAVA_BYTE, 0L, dst, 0, count);
    }

    @Benchmark
    public void segmentToSegmentCopy() {
        MemorySegment.copy(mc, 0L, mt, 0L, count);
    }

    @Benchmark
    public void directCopy() {
        MemAccess.memcpy(mc, 0L, mt, 0L, count);
    }

    void main() {
        run(CopyBench.class);
    }
}
