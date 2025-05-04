package io.github.benrushcc.lib;

import io.github.benrushcc.common.Utils;

import java.io.File;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/// Dynamic library management utilities
@SuppressWarnings("unused")
public final class Dyn {
    private Dyn() {
        Utils.unsupportedInstantiated();
    }

    private static final String JING_LIBRARY_PATH = searchCustomLibraryPath();

    private static final List<String> DEFAULT_LIBRARY_PATHS = searchDefaultLibraryPaths();

    private static final Map<String, Path> LIBRARY_CACHE = new HashMap<>();

    private static final Lock LIBRARY_CACHE_LOCK = new ReentrantLock();

    private static String searchCustomLibraryPath() {
        String customPath = System.getenv("JING_LIBRARY_PATH");
        if(customPath != null && !customPath.isBlank()) {
            return customPath;
        }
        String fallbackPath = System.getProperty("jing.library.path");
        if(fallbackPath != null && !fallbackPath.isBlank()) {
            return fallbackPath;
        }
        return null;
    }

    private static List<String> searchDefaultLibraryPaths() {
        String libPaths = System.getProperty("java.library.path");
        if(libPaths == null || libPaths.isBlank()) {
            if(JING_LIBRARY_PATH != null) {
                // Always using JING_LIBRARY_PATH then
                return List.of();
            }
            throw new IllegalStateException("java.library.path not found, couldn't determine the default library path");
        }
        return List.of(libPaths.split(File.pathSeparator));
    }

    private static SymbolLookup load(String libraryName) {
        // Empty string means vm internal symbolLookup
        if(libraryName.isEmpty()) {
            return Linker.nativeLinker().defaultLookup();
        }
        String realLibraryName = System.mapLibraryName(libraryName);
        // Hit cache
        Path cached = LIBRARY_CACHE.get(realLibraryName);
        if(cached != null) {
            return SymbolLookup.libraryLookup(cached, Arena.global());
        }
        // Fast path
        if(JING_LIBRARY_PATH != null) {
            Path p = Paths.get(JING_LIBRARY_PATH, realLibraryName);
            if(Files.exists(p)) {
                LIBRARY_CACHE.putIfAbsent(realLibraryName, p);
                return SymbolLookup.libraryLookup(p, Arena.global());
            }
        }
        // Slow path
        for (String s : DEFAULT_LIBRARY_PATHS) {
            Path p = Paths.get(s, realLibraryName);
            if(Files.exists(p)) {
                LIBRARY_CACHE.putIfAbsent(realLibraryName, p);
                return SymbolLookup.libraryLookup(p, Arena.global());
            }
        }
        return null;
    }

    public static SymbolLookup loadLibrary(String libraryName) {
        LIBRARY_CACHE_LOCK.lock();
        try {
            SymbolLookup sm = load(libraryName);
            if(sm == null) {
                throw new RuntimeException("Unable to load library " + libraryName);
            }
            return sm;
        } finally {
            LIBRARY_CACHE_LOCK.unlock();
        }
    }

    public static SymbolLookup loadLibrary(List<String> libraryNames) {
        LIBRARY_CACHE_LOCK.lock();
        try {
            for (String libraryName : libraryNames) {
                SymbolLookup sm = load(libraryName);
                if(sm != null) {
                    return sm;
                }
            }
            throw new RuntimeException("Unable to load library " + String.join(", ", libraryNames));
        } finally {
            LIBRARY_CACHE_LOCK.unlock();
        }
    }

    public static MethodHandle mh(Linker linker, SymbolLookup lookup, String methodName, FunctionDescriptor functionDescriptor, Linker.Option... options) {
        MemorySegment m = lookup.find(methodName).orElseThrow(() -> new RuntimeException("Target method not found: " + methodName));
        return linker.downcallHandle(m, functionDescriptor, options);
    }

    public static MethodHandle mh(Linker linker, SymbolLookup lookup, List<String> methodNames, FunctionDescriptor functionDescriptor, Linker.Option... options) {
        for (String methodName : methodNames) {
            Optional<MemorySegment> ptr = lookup.find(methodName);
            if(ptr.isPresent()) {
                return linker.downcallHandle(ptr.get(), functionDescriptor, options);
            }
        }
        throw new RuntimeException("Target method not found: " + String.join(", ", methodNames));
    }
}
