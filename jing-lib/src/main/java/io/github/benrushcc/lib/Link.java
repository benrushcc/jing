package io.github.benrushcc.lib;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface Link {
    String[] name(); // Function names, could be multiple due to macro issues

    String[] capturedCallState() default {};

    boolean critical() default false;

    boolean allowHeapAccess() default false; // will only make sense if critical is true

    int firstVariadicArg() default -1; // will only make sense if above 0
}
