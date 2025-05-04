package io.github.benrushcc.std;

import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.VectorMask;

@FunctionalInterface
public interface ByteVectorMatcher {
    VectorMask<Byte> match(ByteVector vector);
}
