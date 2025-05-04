#include "common.h"
#include "mem.h"

#include "../thirdparty/rpmalloc/rpmalloc/rpmalloc.h"
#include <stdlib.h>
#include <string.h>

// system malloc series functions

void *jing_malloc(size_t size) {
    return malloc(size);
}

void jing_free(void *ptr) {
    free(ptr);
}

void *jing_realloc(void *ptr, size_t size) {
    return realloc(ptr, size);
}

void *jing_aligned_alloc(size_t alignment, size_t size) {
#ifdef OS_WINDOWS
    return _aligned_malloc(size, alignment);
#else
    return aligned_alloc(alignment, size);
#endif
}

void jing_aligned_free(void *ptr) {
#ifdef OS_WINDOWS
    _aligned_free(ptr);
#else
    free(ptr);
#endif
}

// rpmalloc series functions

int jing_rp_initialize(void) {
    return rpmalloc_initialize();
}

void jing_rp_thread_initialize(void) {
    rpmalloc_thread_initialize();
}

void jing_rp_thread_finalize(void) {
    rpmalloc_thread_finalize(1);
}

void jing_rp_finalize(void) {
    rpmalloc_finalize();
}

void *jing_rp_malloc(size_t size) {
    return rpmalloc(size);
}

void jing_rp_free(void *ptr) {
    rpfree(ptr);
}

void *jing_rp_realloc(void *ptr, size_t size) {
    return rprealloc(ptr, size);
}

void *jing_rp_aligned_alloc(size_t alignment, size_t size) {
    return rpaligned_alloc(alignment, size);
}

// mem series functions

int jing_memchr(const void* ptr, char ch, size_t count) {
    void* r = memchr(ptr, ch, count);
    if(r == NULL) {
        return -1;
    }
    return ((char*)r) - ((char*)ptr);
}

int jing_memcpy(void* dest, size_t destsz, const void* src, size_t count) {
    return memcpy_s(dest, destsz, src, count);
}

int jing_memmove(void* dest, size_t destsz, const void* src, size_t count) {
    return memmove_s(dest, destsz, src, count);
}




