package io.github.benrushcc.std.lib;

import io.github.benrushcc.lib.Lib;
import io.github.benrushcc.lib.Link;

import java.lang.foreign.MemorySegment;

@Lib("jing")
public interface SysMemLib {

    @Link(name = "jing_malloc", critical = true)
    MemorySegment malloc(long size);

    @Link(name = "jing_realloc",  critical = true)
    MemorySegment realloc(MemorySegment seg, long newSize);

    @Link(name = "jing_free", critical = true)
    void free(MemorySegment seg);

    @Link(name = "jing_aligned_alloc", critical = true)
    MemorySegment alignedAlloc(long alignment, long size);

    @Link(name = "jing_aligned_free", critical = true)
    void alignedFree(MemorySegment seg);

    @Link(name = "jing_memchr", critical = true)
    int memchr(MemorySegment ptr, byte ch, long size);

    @Link(name = "jing_memcpy", critical = true)
    int memcpy(MemorySegment dst, long dstSize, MemorySegment src, long count);

    @Link(name = "jing_memmove", critical = true)
    int memmove(MemorySegment dst, long dstSize, MemorySegment src, long count);
}
