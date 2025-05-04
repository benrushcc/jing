package io.github.benrushcc.lib;

import java.util.function.Supplier;

public interface LibRegistry {
    Class<?> target();

    Supplier<?> supplier();
}
