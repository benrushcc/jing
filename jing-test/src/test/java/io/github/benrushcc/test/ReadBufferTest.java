package io.github.benrushcc.test;

import io.github.benrushcc.std.ReadBuffer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySegment;
import java.nio.ByteOrder;
import java.util.List;

public class ReadBufferTest {
    @Test
    public void testReadBuffer() {
        byte[] b1 = new byte[1];
        byte[] b2 = new byte[1];
        byte[] b3 = new byte[1];
        byte[] b4 = new byte[1];
        b1[0] = 1;
        b2[0] = 1;
        b3[0] = 1;
        b4[0] = 1;
        List<MemorySegment> ms = List.of(MemorySegment.ofArray(b1), MemorySegment.ofArray(b2), MemorySegment.ofArray(b3), MemorySegment.ofArray(b4));
        ReadBuffer rb = ReadBuffer.as(ms);
        int i = rb.readInt(ByteOrder.nativeOrder());
        Assertions.assertEquals(0x01010101, i);
    }

}
