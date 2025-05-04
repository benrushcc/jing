package io.github.benrushcc.codegen;

import javax.annotation.processing.Filer;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


public final class Source {
    private static final String INDENT = fetchingIntent();
    private static final int SPACE_SIZE = 4;

    private static String fetchingIntent() {
        String indent = System.getProperty("jing.codegen.indent");
        return switch (indent) {
            case "tab" -> "\t";
            case "space" -> {
                Integer i = Integer.getInteger("jing.codegen.spaceSize");
                yield " ".repeat(i == null || i < 0 ? SPACE_SIZE : i);
            }
            case null -> " ".repeat(SPACE_SIZE);
            default -> throw new IllegalStateException("Unexpected indent value: " + indent);
        };
    }

    private static String generateSourceClassName(String className, String suffix) {
        return "_" + className + "$$" + suffix;
    }

    private final Set<String> imports = new LinkedHashSet<>();
    private final Set<String> refers = new LinkedHashSet<>();
    private final String moduleName;
    private final String packageName;
    private final String className;
    private final List<Line> lines = new ArrayList<>();
    private int indent = 0;

    public Source(Elements elements, TypeElement classElement, String suffix) {
        ModuleElement moduleElement = elements.getModuleOf(classElement);
        PackageElement packageElement = elements.getPackageOf(classElement);
        if(moduleElement.isUnnamed()) {
            throw new RuntimeException("ModuleElement is unnamed");
        }
        moduleName = moduleElement.getQualifiedName().toString();
        packageName = packageElement.getQualifiedName().toString();
        className = generateSourceClassName(classElement.getSimpleName().toString(), suffix);
    }

    public String className() {
        return className;
    }

    public void addBlock(Block b) {
        List<Line> l = b.lines();
        for (Line line : l) {
            lines.add(new Line(line.content(), line.indent() + indent));
        }
        Line last = l.getLast();
        if(last != null) {
            indent += last.indent();
            if(indent < 0) {
                throw new RuntimeException("Invalid indentation occurred");
            }
        }
    }

    public void addBlocks(List<Block> blocks) {
        for (Block b : blocks) {
            addBlock(b);
        }
    }

    public String register(Elements elements, TypeElement typeElement) {
        if(typeElement.getNestingKind() != NestingKind.TOP_LEVEL) {
            throw new RuntimeException("Registered element must be top-level : " + typeElement.getSimpleName());
        }
        String packageName = elements.getPackageOf(typeElement).getQualifiedName().toString();
        String fullName = typeElement.getQualifiedName().toString();
        String simpleName = typeElement.getSimpleName().toString();
        return register(packageName, fullName, simpleName);
    }

    public String register(Class<?> clazz) {
        if(clazz.isMemberClass()) {
            throw new RuntimeException("Registered class must be top-level : " + clazz.getSimpleName());
        }
        String packageName = clazz.getPackageName();
        String fullName = clazz.getName();
        String simpleName = clazz.getSimpleName();
        return register(packageName, fullName, simpleName);
    }

    /// p -> packageName
    /// f -> fullName
    /// s -> shortName
    private String register(String p, String f, String s) {
        if(p.equals(packageName)) {
            // same package, direct import
            refers.add(s);
            return s;
        }
        if(imports.contains(f)) {
            // already exists
            return s;
        }
        if(refers.contains(s)) {
            // duplicate named class, using full name
            return f;
        }
        imports.add(f);
        refers.add(s);
        return s;
    }

    public String sourceFileName() {
        return moduleName + "/" + packageName + "." + className;
    }

    public void writeToFiler(Filer filer) {
        try{
            JavaFileObject sourceFile = filer.createSourceFile(sourceFileName());
            try(PrintWriter out = new PrintWriter(sourceFile.openWriter())) {
                out.println("package %s;\n".formatted(packageName));
                for(String s : imports) {
                    out.println("import %s;".formatted(s));
                }
                out.println();
                for(Line line : lines) {
                    out.print(INDENT.repeat(line.indent()));
                    out.println(line.content());
                }
            } catch (IOException e) {
                throw new IllegalStateException("Can't write to target source file", e);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Can't create target source file", e);
        }
    }
}
