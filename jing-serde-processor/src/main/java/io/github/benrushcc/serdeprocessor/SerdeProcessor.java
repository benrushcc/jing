package io.github.benrushcc.serdeprocessor;

import io.github.benrushcc.codegen.CodeGenProcessor;
import io.github.benrushcc.serde.Serde;

import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public final class SerdeProcessor extends CodeGenProcessor {

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(Serde.class.getName());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if(!roundEnv.processingOver()) {
            Set<? extends Element> serdes = roundEnv.getElementsAnnotatedWith(Serde.class);
            for (Element e : serdes) {
                // TODO
            }
        }
        return true;
    }
}
