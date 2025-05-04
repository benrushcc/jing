module jing.std {
    requires transitive jing.common;
    requires transitive jing.lib;
    requires static jing.libprocessor;

    exports io.github.benrushcc.std;
    exports io.github.benrushcc.std.lib;

    provides io.github.benrushcc.lib.LibRegistry with io.github.benrushcc.std.lib._SysMemLib$$Lib, io.github.benrushcc.std.lib._RpMemLib$$Lib;
}