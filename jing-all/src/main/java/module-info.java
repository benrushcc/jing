module jing.all {
    requires transitive jing.common;
    requires transitive jing.lib;
    requires transitive jing.serde;
    requires transitive jing.std;

    requires static jing.libprocessor;
    requires static jing.serdeprocessor;
}