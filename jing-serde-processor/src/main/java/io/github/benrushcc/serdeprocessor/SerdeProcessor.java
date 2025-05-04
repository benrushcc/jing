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
    private static final int INITIAL_SIZE = 1024;
    private static final List<String> GENERATED_CLASS_NAMES = new ArrayList<>(INITIAL_SIZE);

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(Serde.class.getName());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if(roundEnv.processingOver()) {
            Filer filer = env().getFiler();
            try {
                FileObject fo = filer.createResource(StandardLocation.CLASS_OUTPUT, "", "serde.txt");
                Path p = Paths.get(fo.toUri());
                Files.deleteIfExists(p);
                try(BufferedWriter writer = Files.newBufferedWriter(p, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                    for (String s : GENERATED_CLASS_NAMES) {
                        writer.write(s);
                        writer.newLine();
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to create serde resources", e);
            }
        } else {
            Set<? extends Element> serdes = roundEnv.getElementsAnnotatedWith(Serde.class);
            for (Element e : serdes) {
                // TODO
            }
        }
        return true;
    }
}
