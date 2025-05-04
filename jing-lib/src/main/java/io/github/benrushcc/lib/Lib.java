package io.github.benrushcc.lib;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface Lib {
    String[] value() default {}; // Library names, looking inside jvm by default

    String[] relyOn() default {}; // Relied on library names, must be loaded before this library
}
