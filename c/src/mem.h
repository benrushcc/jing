#ifndef MEM_H
#define MEM_H

#include "common.h"
#include <stddef.h>

EXPORT_SYMBOL void *jing_malloc(size_t size);

EXPORT_SYMBOL void jing_free(void *ptr);

EXPORT_SYMBOL void *jing_realloc(void *ptr, size_t size);

EXPORT_SYMBOL void *jing_aligned_alloc(size_t alignment, size_t size);

EXPORT_SYMBOL void jing_aligned_free(void *ptr);

EXPORT_SYMBOL int jing_rp_initialize(void);

EXPORT_SYMBOL void jing_rp_thread_initialize(void);

EXPORT_SYMBOL void jing_rp_thread_finalize(void);

EXPORT_SYMBOL void jing_rp_finalize(void);

EXPORT_SYMBOL void *jing_rp_malloc(size_t size);

EXPORT_SYMBOL void jing_rp_free(void *ptr);

EXPORT_SYMBOL void *jing_rp_realloc(void *ptr, size_t size);

EXPORT_SYMBOL void *jing_rp_aligned_alloc(size_t alignment, size_t size);

EXPORT_SYMBOL int jing_memchr(const void* ptr, char ch, size_t count);

EXPORT_SYMBOL int jing_memcpy(void* dest, size_t destsz, const void* src, size_t count);

EXPORT_SYMBOL int jing_memmove(void* dest, size_t destsz, const void* src, size_t count);

#endif
