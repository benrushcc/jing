#ifndef COMMON_H
#define COMMON_H


#define likely(x)   __builtin_expect(!!(x), 1)
#define unlikely(x) __builtin_expect(!!(x), 0)


#if defined(_WIN32)
    #define EXPORT_SYMBOL __declspec(dllexport)
#else
    #define EXPORT_SYMBOL __attribute__((visibility("default")))
#endif


#if defined(_WIN32) || defined(_WIN64)
    #define OS_WINDOWS 1
#elif defined(__APPLE__) && defined(__MACH__)
    #define OS_MACOS 1
#elif defined(__linux__)
    #define OS_LINUX 1
#else
    #error "Unknown compiler/platform"
#endif

#endif
