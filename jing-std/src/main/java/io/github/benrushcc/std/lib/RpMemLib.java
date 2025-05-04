package io.github.benrushcc.std.lib;

import io.github.benrushcc.lib.Lib;
import io.github.benrushcc.lib.Link;

import java.lang.foreign.MemorySegment;

@Lib("jing")
public interface RpMemLib {

    @Link(name = "jing_rp_initialize", critical = true)
    int rpInitialize();

    @Link(name = "jing_rp_thread_initialize", critical = true)
    void rpThreadInitialize();

    @Link(name = "jing_rp_thread_finalize", critical = true)
    void rpThreadFinalize();

    @Link(name = "jing_rp_finalize", critical = true)
    void rpFinalize();

    @Link(name = "jing_rp_malloc", critical = true)
    MemorySegment rpMalloc(long size);

    @Link(name = "jing_rp_realloc",  critical = true)
    MemorySegment rpRealloc(MemorySegment seg, long newSize);

    @Link(name = "jing_rp_free", critical = true)
    void rpFree(MemorySegment seg);

    @Link(name = "jing_rp_aligned_alloc", critical = true)
    MemorySegment rpAlignedAlloc(long alignment, long size);
}
