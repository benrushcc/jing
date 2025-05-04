module jing.libprocessor {
    requires transitive jing.common;
    requires transitive jing.codegen;
    requires transitive jing.lib;

    exports io.github.benrushcc.libprocessor;

    provides javax.annotation.processing.Processor with io.github.benrushcc.libprocessor.LibProcessor;
}