package io.github.benrushcc.test;

import io.github.benrushcc.std.MemAccess;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

public class LibTest {
    @Test
    public void testMemcpy() {
        try(Arena arena = Arena.ofConfined()) {
            MemorySegment m1 = arena.allocate(1024);
            MemorySegment m2 = arena.allocate(1024);
            m1.fill((byte) 1);
            m2.fill((byte) 2);
            MemAccess.memcpy(m1, 0L, m2, 0L, 1024);
            Assertions.assertEquals(-1, m1.mismatch(m2));
        }
    }

    @Test
    public void testMemmove() {
        try(Arena arena = Arena.ofConfined()) {
            MemorySegment m1 = arena.allocate(1024);
            m1.fill((byte) 2);
            MemorySegment m2 = m1.asSlice(0L, 512L);
            m2.fill((byte) 1);
            MemAccess.memmove(m2, 0L, m1, 256L, 512L);
            MemorySegment m3 = arena.allocate(1024);
            m3.fill((byte) 2);
            m3.asSlice(0L, 768L).fill((byte) 1);
            Assertions.assertEquals(-1, m3.mismatch(m1));
        }
    }
}
