module jing.serdeprocessor {
    requires transitive jing.common;
    requires transitive jing.codegen;
    requires transitive jing.serde;

    exports io.github.benrushcc.serdeprocessor;

    provides javax.annotation.processing.Processor with io.github.benrushcc.serdeprocessor.SerdeProcessor;
}