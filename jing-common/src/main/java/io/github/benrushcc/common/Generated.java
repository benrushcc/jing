package io.github.benrushcc.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Mark a class as generated by annotation processor
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface Generated {
    Class<?> value();
}
