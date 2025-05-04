open module jing.bench {
    requires transitive jing.all;
    requires jdk.unsupported;
    requires jmh.core;
    requires static jmh.generator.annprocess;
}