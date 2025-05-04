package io.github.benrushcc.common;

import io.github.benrushcc.common.experimental.ExStableValue;

import java.util.function.Supplier;

@SuppressWarnings("unused")
public final class Utils {
    private static final Supplier<StackWalker> WALKER_SUPPLIER = ExStableValue.supplier(() -> StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE));

    private Utils() {
        unsupportedInstantiated();
    }

    public static void unsupportedInstantiated() {
        String callerClassName = WALKER_SUPPLIER.get().walk(
                        stackFrames -> stackFrames.skip(1).findFirst().map(StackWalker.StackFrame::getClassName))
                .orElseThrow(() -> new RuntimeException("StackFrames not unwinded"));
        throw new UnsupportedOperationException(callerClassName + " should not be instantiated");
    }

    public static long current() {
        return System.currentTimeMillis();
    }

    public static long nano() {
        return System.nanoTime();
    }

    public static long elapsed(long nano) {
        return nano() - nano;
    }
}
