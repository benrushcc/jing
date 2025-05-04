package io.github.benrushcc.lib;

import io.github.benrushcc.common.Utils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Supplier;
import java.util.stream.Collectors;


public final class LibContext {
    private static final Map<Class<?>, Supplier<?>> LIB_MAP;

    static {
        Map<Class<?>, Supplier<?>> m = new HashMap<>();
        for (LibRegistry registry : ServiceLoader.load(LibRegistry.class)) {
            m.put(registry.target(), registry.supplier());
        }
        LIB_MAP = Map.copyOf(m);
    }

    private LibContext() {
        Utils.unsupportedInstantiated();
    }

    public static <T> T acquire(Class<T> clazz) {
        Supplier<?> s = LIB_MAP.get(clazz);
        if(s == null) {
            throw new RuntimeException("Lib not found: " + clazz.getName());
        }
        return clazz.cast(s.get());
    }
}
