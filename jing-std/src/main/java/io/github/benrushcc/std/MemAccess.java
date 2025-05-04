package io.github.benrushcc.std;

import io.github.benrushcc.common.Utils;
import io.github.benrushcc.lib.LibContext;
import io.github.benrushcc.std.lib.SysMemLib;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

public final class MemAccess {

    public static final long MEMCPY_THRESHOLD = Long.getLong("jing.memcpy.threshold", 32L);

    private MemAccess() {
        Utils.unsupportedInstantiated();
    }

    private static final VarHandle BYTE_HANDLE = ValueLayout.JAVA_BYTE.varHandle().withInvokeExactBehavior();
    private static final VarHandle SHORT_HANDLE = ValueLayout.JAVA_SHORT_UNALIGNED.varHandle().withInvokeExactBehavior();
    private static final VarHandle CHAR_HANDLE = ValueLayout.JAVA_CHAR_UNALIGNED.varHandle().withInvokeExactBehavior();
    private static final VarHandle INT_HANDLE = ValueLayout.JAVA_INT_UNALIGNED.varHandle().withInvokeExactBehavior();
    private static final VarHandle LONG_HANDLE = ValueLayout.JAVA_LONG_UNALIGNED.varHandle().withInvokeExactBehavior();
    private static final VarHandle FLOAT_HANDLE = ValueLayout.JAVA_FLOAT_UNALIGNED.varHandle().withInvokeExactBehavior();
    private static final VarHandle DOUBLE_HANDLE = ValueLayout.JAVA_DOUBLE_UNALIGNED.varHandle().withInvokeExactBehavior();
    private static final VarHandle ADDRESS_HANDLE = ValueLayout.ADDRESS_UNALIGNED.varHandle().withInvokeExactBehavior();

    private static final ByteOrder OPPOSITE_BYTE_ORDER = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
    private static final VarHandle SHORT_OPPOSITE_HANDLE = ValueLayout.JAVA_SHORT_UNALIGNED.withOrder(OPPOSITE_BYTE_ORDER).varHandle().withInvokeExactBehavior();
    private static final VarHandle CHAR_OPPOSITE_HANDLE = ValueLayout.JAVA_CHAR_UNALIGNED.withOrder(OPPOSITE_BYTE_ORDER).varHandle().withInvokeExactBehavior();
    private static final VarHandle INT_OPPOSITE_HANDLE = ValueLayout.JAVA_INT_UNALIGNED.withOrder(OPPOSITE_BYTE_ORDER).varHandle().withInvokeExactBehavior();
    private static final VarHandle LONG_OPPOSITE_HANDLE = ValueLayout.JAVA_LONG_UNALIGNED.withOrder(OPPOSITE_BYTE_ORDER).varHandle().withInvokeExactBehavior();
    private static final VarHandle FLOAT_OPPOSITE_HANDLE = ValueLayout.JAVA_FLOAT_UNALIGNED.withOrder(OPPOSITE_BYTE_ORDER).varHandle().withInvokeExactBehavior();
    private static final VarHandle DOUBLE_OPPOSITE_HANDLE = ValueLayout.JAVA_DOUBLE_UNALIGNED.withOrder(OPPOSITE_BYTE_ORDER).varHandle().withInvokeExactBehavior();
    private static final VarHandle ADDRESS_OPPOSITE_HANDLE = ValueLayout.ADDRESS_UNALIGNED.withOrder(OPPOSITE_BYTE_ORDER).varHandle().withInvokeExactBehavior();

    private static final SysMemLib SYS_MEM_LIB = LibContext.acquire(SysMemLib.class);

    public static byte getByte(MemorySegment m, long offset) {
        return (byte) BYTE_HANDLE.get(m, offset);
    }

    public static void setByte(MemorySegment m, long offset, byte b) {
        BYTE_HANDLE.set(m, offset, b);
    }

    public static short getShort(MemorySegment m, long offset) {
        return getShort(m, offset, ByteOrder.nativeOrder());
    }

    public static void setShort(MemorySegment m, long offset, short s) {
        setShort(m, offset, s, ByteOrder.nativeOrder());
    }

    public static short getShort(MemorySegment m, long offset, ByteOrder order) {
        if(order == ByteOrder.nativeOrder()) {
            return (short) SHORT_HANDLE.get(m, offset);
        } else {
            return (short) SHORT_OPPOSITE_HANDLE.get(m, offset);
        }
    }

    public static void setShort(MemorySegment m, long offset, short s, ByteOrder order) {
        if (order == ByteOrder.nativeOrder()) {
            SHORT_HANDLE.set(m, offset, s);
        } else {
            SHORT_OPPOSITE_HANDLE.set(m, offset, s);
        }
    }

    public static char getChar(MemorySegment m, long offset) {
        return getChar(m, offset, ByteOrder.nativeOrder());
    }

    public static void setChar(MemorySegment m, long offset, char c) {
        setChar(m, offset, c, ByteOrder.nativeOrder());
    }

    public static char getChar(MemorySegment m, long offset, ByteOrder order) {
        if(order == ByteOrder.nativeOrder()) {
            return (char) CHAR_HANDLE.get(m, offset);
        } else {
            return (char) CHAR_OPPOSITE_HANDLE.get(m, offset);
        }
    }

    public static void setChar(MemorySegment m, long offset, char c, ByteOrder order) {
        if (order == ByteOrder.nativeOrder()) {
            CHAR_HANDLE.set(m, offset, c);
        } else {
            CHAR_OPPOSITE_HANDLE.set(m, offset, c);
        }
    }

    public static int getInt(MemorySegment m, long offset) {
        return getInt(m, offset, ByteOrder.nativeOrder());
    }

    public static void setInt(MemorySegment m, long offset, int i) {
        setInt(m, offset, i, ByteOrder.nativeOrder());
    }

    public static int getInt(MemorySegment m, long offset, ByteOrder order) {
        if(order == ByteOrder.nativeOrder()) {
            return (int) INT_HANDLE.get(m, offset);
        } else {
            return (int) INT_OPPOSITE_HANDLE.get(m, offset);
        }
    }

    public static void setInt(MemorySegment m, long offset, int i, ByteOrder order) {
        if (order == ByteOrder.nativeOrder()) {
            INT_HANDLE.set(m, offset, i);
        } else {
            INT_OPPOSITE_HANDLE.set(m, offset, i);
        }
    }

    public static long getLong(MemorySegment m, long offset) {
        return getLong(m, offset, ByteOrder.nativeOrder());
    }

    public static void setLong(MemorySegment m, long offset, long l) {
        setLong( m, offset, l, ByteOrder.nativeOrder());
    }

    public static long getLong(MemorySegment m, long offset, ByteOrder order) {
        if(order == ByteOrder.nativeOrder()) {
            return (long) LONG_HANDLE.get(m, offset);
        } else {
            return (long) LONG_OPPOSITE_HANDLE.get(m, offset);
        }
    }

    public static void setLong(MemorySegment m, long offset, long l, ByteOrder order) {
        if (order == ByteOrder.nativeOrder()) {
            LONG_HANDLE.set(m, offset, l);
        } else {
            LONG_OPPOSITE_HANDLE.set(m, offset, l);
        }
    }

    public static float getFloat(MemorySegment m, long offset) {
        return getFloat(m, offset, ByteOrder.nativeOrder());
    }

    public static void setFloat(MemorySegment m, long offset, float f) {
        setFloat( m, offset, f, ByteOrder.nativeOrder());
    }

    public static float getFloat(MemorySegment m, long offset, ByteOrder order) {
        if(order == ByteOrder.nativeOrder()) {
            return (float) FLOAT_HANDLE.get(m, offset);
        } else {
            return (float) FLOAT_OPPOSITE_HANDLE.get(m, offset);
        }
    }

    public static void setFloat(MemorySegment m, long offset, float f, ByteOrder order) {
        if (order == ByteOrder.nativeOrder()) {
            FLOAT_HANDLE.set(m, offset, f);
        } else {
            FLOAT_OPPOSITE_HANDLE.set(m, offset, f);
        }
    }

    public static double getDouble(MemorySegment m, long offset) {
        return getDouble(m, offset, ByteOrder.nativeOrder());
    }

    public static void setDouble(MemorySegment m, long offset, double d) {
        setDouble( m, offset, d, ByteOrder.nativeOrder());
    }

    public static double getDouble(MemorySegment m, long offset, ByteOrder order) {
        if(order == ByteOrder.nativeOrder()) {
            return (double) DOUBLE_HANDLE.get(m, offset);
        } else {
            return (double) DOUBLE_OPPOSITE_HANDLE.get(m, offset);
        }
    }

    public static void setDouble(MemorySegment m, long offset, double d, ByteOrder order) {
        if (order == ByteOrder.nativeOrder()) {
            DOUBLE_HANDLE.set(m, offset, d);
        } else {
            DOUBLE_OPPOSITE_HANDLE.set(m, offset, d);
        }
    }

    public static MemorySegment getAddress(MemorySegment m, long offset) {
        return getAddress(m, offset, ByteOrder.nativeOrder());
    }

    public static void setAddress(MemorySegment m, long offset, MemorySegment address) {
        setAddress(m, offset, address, ByteOrder.nativeOrder());
    }

    public static MemorySegment getAddress(MemorySegment m, long offset, ByteOrder order) {
        if (order == ByteOrder.nativeOrder()) {
            return (MemorySegment) ADDRESS_HANDLE.get(m, offset);
        } else {
            return (MemorySegment) ADDRESS_OPPOSITE_HANDLE.get(m, offset);
        }
    }

    public static void setAddress(MemorySegment m, long offset, MemorySegment address, ByteOrder order) {
        if (order == ByteOrder.nativeOrder()) {
            ADDRESS_HANDLE.set(m, offset, address);
        } else {
            ADDRESS_OPPOSITE_HANDLE.set(m, offset, address);
        }
    }

    /// Below are some unsafe memory manipulate methods, use at your own risk

    public static int memchr(MemorySegment m, byte ch) {
        assert m.isNative() && m.address() != 0L;
        return SYS_MEM_LIB.memchr(m, ch, m.byteSize());
    }

    public static void memcpy(MemorySegment src, long srcOffset, MemorySegment dest, long destOffset, long count) {
        assert src.isNative() && dest.isNative() && src.address() != 0L && dest.address() != 0L;
        MemorySegment s = MemorySegment.ofAddress(Math.addExact(src.address(), srcOffset));
        MemorySegment d = MemorySegment.ofAddress(Math.addExact(dest.address(), destOffset));
        int errno = SYS_MEM_LIB.memcpy(d, Math.subtractExact(dest.byteSize(), destOffset), s, count);
        if(errno != 0) {
            throw new RuntimeException("Failed to memcpy, errno : " + errno);
        }
    }

    public static void memmove(MemorySegment src, long srcOffset, MemorySegment dest, long destOffset, long count) {
        assert src.isNative() && dest.isNative() && src.address() != 0L && dest.address() != 0L;
        MemorySegment s = MemorySegment.ofAddress(Math.addExact(src.address(), srcOffset));
        MemorySegment d = MemorySegment.ofAddress(Math.addExact(dest.address(), destOffset));
        int errno = SYS_MEM_LIB.memmove(d, Math.subtractExact(dest.byteSize(), destOffset), s, count);
        if(errno != 0) {
            throw new RuntimeException("Failed to memmove, errno : " + errno);
        }
    }
}
