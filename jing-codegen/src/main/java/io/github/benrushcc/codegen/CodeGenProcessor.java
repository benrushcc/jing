package io.github.benrushcc.codegen;

import io.github.benrushcc.common.experimental.ExStableValue;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.SourceVersion;
import java.util.Set;

public abstract class CodeGenProcessor extends AbstractProcessor {
    private static final ExStableValue<ProcessingEnvironment> ENV = ExStableValue.of();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        ENV.setOrThrow(processingEnv);
    }

    @Override
    public Set<String> getSupportedOptions() {
        return Set.of();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    protected static ProcessingEnvironment env() {
        return ENV.orElseThrow();
    }
}
