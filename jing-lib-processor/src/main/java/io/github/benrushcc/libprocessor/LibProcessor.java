package io.github.benrushcc.libprocessor;

import io.github.benrushcc.codegen.Block;
import io.github.benrushcc.codegen.CodeGenProcessor;
import io.github.benrushcc.codegen.Source;
import io.github.benrushcc.common.Generated;
import io.github.benrushcc.common.experimental.ValueBased;
import io.github.benrushcc.lib.Dyn;
import io.github.benrushcc.lib.Lib;
import io.github.benrushcc.lib.LibRegistry;
import io.github.benrushcc.lib.Link;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class LibProcessor extends CodeGenProcessor {
    private static final int INITIAL_SIZE = 32;
    private static final List<String> GENERATED_CLASS_NAMES = new ArrayList<>(INITIAL_SIZE);

    private static final StableValue<TypeMirror> BOOLEAN_TYPE = StableValue.of();
    private static final StableValue<TypeMirror> BYTE_TYPE = StableValue.of();
    private static final StableValue<TypeMirror> SHORT_TYPE = StableValue.of();
    private static final StableValue<TypeMirror> INT_TYPE = StableValue.of();
    private static final StableValue<TypeMirror> LONG_TYPE = StableValue.of();
    private static final StableValue<TypeMirror> FLOAT_TYPE = StableValue.of();
    private static final StableValue<TypeMirror> DOUBLE_TYPE = StableValue.of();
    private static final StableValue<TypeMirror> CHAR_TYPE = StableValue.of();
    private static final StableValue<TypeMirror> VOID_TYPE = StableValue.of();
    private static final StableValue<TypeMirror> PTR_TYPE = StableValue.of();

    @Override
    public Set<String> getSupportedOptions() {
        return Set.of("jing.libprocessor.path"); // TODO remains to be decided
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(Lib.class.getName());
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        Types types = env().getTypeUtils();
        Elements elements = env().getElementUtils();
        BOOLEAN_TYPE.setOrThrow(types.getPrimitiveType(TypeKind.BOOLEAN));
        BYTE_TYPE.setOrThrow(types.getPrimitiveType(TypeKind.BYTE));
        SHORT_TYPE.setOrThrow(types.getPrimitiveType(TypeKind.SHORT));
        INT_TYPE.setOrThrow(types.getPrimitiveType(TypeKind.INT));
        LONG_TYPE.setOrThrow(types.getPrimitiveType(TypeKind.LONG));
        FLOAT_TYPE.setOrThrow(types.getPrimitiveType(TypeKind.FLOAT));
        DOUBLE_TYPE.setOrThrow(types.getPrimitiveType(TypeKind.DOUBLE));
        CHAR_TYPE.setOrThrow(types.getPrimitiveType(TypeKind.CHAR));
        VOID_TYPE.setOrThrow(types.getNoType(TypeKind.VOID));
        PTR_TYPE.setOrThrow(elements.getTypeElement(MemorySegment.class.getCanonicalName()).asType());
    }

    @ValueBased
    private record LinkData(
            ExecutableElement method,
            Link link
    ) {

    }

    @ValueBased
    private record LibData(
            TypeElement element,
            Lib lib,
            List<LinkData> dataList
    ) {

    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if(roundEnv.processingOver()) {
            Filer filer = env().getFiler();
            try {
                FileObject fo = filer.createResource(StandardLocation.CLASS_OUTPUT, "", "lib.txt");
                Path p = Paths.get(fo.toUri());
                Files.deleteIfExists(p);
                try(BufferedWriter writer = Files.newBufferedWriter(p, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                    for (String s : GENERATED_CLASS_NAMES) {
                        writer.write(s);
                        writer.newLine();
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to create lib resources", e);
            }
        } else {
            Set<? extends Element> libs = roundEnv.getElementsAnnotatedWith(Lib.class);
            for (Element e : libs) {
                TypeElement t = checkLibElement(e);
                Lib lib = Objects.requireNonNull(t.getAnnotation(Lib.class));
                List<LinkData> dataList = new ArrayList<>();
                for (Element el : e.getEnclosedElements()) {
                    if(el.getKind() == ElementKind.METHOD && el instanceof ExecutableElement ex) {
                        Link link = ex.getAnnotation(Link.class);
                        if(link != null) {
                            checkLinkMethod(ex);
                            checkLinkAnnotation(link, ex);
                            dataList.add(new LinkData(ex, link));
                        }
                    }
                }
                LibData libData = new LibData(t, lib, dataList);
                processLibData(libData);
            }
        }
        return true;
    }

    private static TypeElement checkLibElement(Element element) {
        if(element instanceof TypeElement t) {
            if(t.getKind() != ElementKind.INTERFACE) {
                throw new RuntimeException("@Lib annotation element must be an interface");
            }
            Set<Modifier> modifiers = t.getModifiers();
            if(modifiers.contains(Modifier.SEALED)) {
                throw new RuntimeException("@Lib annotation element must not be sealed");
            }
            if(modifiers.contains(Modifier.STATIC)) {
                throw new RuntimeException("@Lib annotation element must not be static");
            }
            if(modifiers.contains(Modifier.PRIVATE)) {
                throw new RuntimeException("@Lib annotation element must not be private");
            }
            if(t.getNestingKind() != NestingKind.TOP_LEVEL) {
                throw new RuntimeException("@Lib annotation element must be a top-level interface");
            }
            return t;
        }
        throw new RuntimeException("unreachable");
    }

    private static void checkLinkMethod(ExecutableElement ex) {
        if(ex.isDefault()) {
            throw new RuntimeException("@Link annotation element must not be default");
        }
        if(ex.isVarArgs()) {
            throw new RuntimeException("@Link annotation element must not be varargs");
        }
        if(!ex.getThrownTypes().isEmpty()) {
            throw new RuntimeException("@Link annotation element must not have thrown types");
        }
        if(!ex.getTypeParameters().isEmpty()) {
            throw new RuntimeException("@Link annotation element must not have type parameters");
        }
    }

    // Note: always update with jdk.internal.foreign.abi.CapturableState
    private static final List<String> ALLOWED_CAPTURED_LAYOUTS = List.of("GetLastError", "WSAGetLastError", "errno");

    private static void checkLinkAnnotation(Link link, ExecutableElement method) {
        Types t = env().getTypeUtils();
        if(link.capturedCallState().length > 0) {
            for (String c : link.capturedCallState()) {
                if(!ALLOWED_CAPTURED_LAYOUTS.contains(c)) {
                    throw new RuntimeException("@Link annotation element must contain a valid captured layout : " + c);
                }
            }
            if(link.critical()) {
                throw new RuntimeException("@Link captureState could not be used with critical access");
            }
            if(method.getParameters().isEmpty() || !t.isSameType(method.getParameters().getFirst().asType(), PTR_TYPE.orElseThrow())) {
                throw new RuntimeException("With capturedCallState, there must be a single memorySegment argument at the first place");
            }
        }
        int n = method.getParameters().size();
        if (link.firstVariadicArg() < -1 || link.firstVariadicArg() > n) {
            throw new RuntimeException("@Link variadic argument out of bounds : " + link.firstVariadicArg());
        }
    }

    private static void processLibData(LibData libData) {
        Source source = new Source(env().getElementUtils(), libData.element(), "Lib");
        source.addBlocks(List.of(
                outerDefinitionBlock(source, libData),
                innerDefinitionBlock(source, libData),
                innerStaticBlock(source, libData),
                innerImplementationBlock(source, libData),
                closureBlock(),
                outerImplementationBlock(source, libData),
                closureBlock()
        ));
        source.writeToFiler(env().getFiler());
        GENERATED_CLASS_NAMES.add(source.sourceFileName());
    }

    private static Block outerDefinitionBlock(Source source, LibData libData) {
        String target = source.register(env().getElementUtils(), libData.element());
        String libRegistry = source.register(LibRegistry.class);
        String generated = source.register(Generated.class);
        return new Block()
                .addLine("// This class was generated by LibProcessor and shouldn't be directly used")
                .addLine("@" + generated + "(" + target + ".class)")
                .addLine("public final class " + source.className() + " implements " + libRegistry + " {")
                .indent().newLine();
    }

    private static Block innerDefinitionBlock(Source source, LibData libData) {
        String target = source.register(env().getElementUtils(), libData.element());
        String list = source.register(List.class);
        String methodHandle = source.register(MethodHandle.class);
        return new Block()
                .addLine("private final class Impl implements " + target + " {")
                .indent().newLine()
                .addLine("private static final " + list + "<" + methodHandle + "> MHS;")
                .newLine();
    }

    private static Block innerStaticBlock(Source source, LibData libData) {
        String linker = source.register(Linker.class);
        String dyn = source.register(Dyn.class);
        String symbolLookup = source.register(SymbolLookup.class);
        String intFunction = source.register(IntFunction.class);
        String methodHandle = source.register(MethodHandle.class);
        String exp = source.register(RuntimeException.class);
        String stableValue = source.register(StableValue.class);
        Block b = new Block().addLine("static {")
                .indent()
                .addLine(linker + " linker = " + linker + ".nativeLinker();");
        String relyOnStr = relyOnStr(source, libData);
        if(relyOnStr != null) {
            b.addLine(symbolLookup + " _ = " + dyn + ".loadLibrary(\"" + relyOnStr + "\");");
        }
        b.addLine(symbolLookup + " lookup = " + dyn + ".loadLibrary(\"" + libStr(source, libData) + "\");");
        List<LinkData> dataList = libData.dataList();
        if(dataList.size() > 3) {
            // using switch
            b.addLine(intFunction + "<" + methodHandle + "> f = i -> switch (i) {").indent();
            for(int index = 0; index < dataList.size(); index++) {
                LinkData ld = dataList.get(index);
                b.addLine("case " + index + " -> " + mhCaseStr(source, ld) + ";");
            }
            b.addLine("default -> throw new " + exp + "(\"unreached\");");
        } else {
            // using if
            b.addLine(intFunction + "<" + methodHandle + "> f = i -> {").indent();
            for(int index = 0; index < dataList.size(); index++) {
                LinkData ld = dataList.get(index);
                if(index == 0) {
                    b.addLine("if(i == " + index + ") return " + mhCaseStr(source, ld) + ";");
                }else {
                    b.addLine("else if(i == " + index + ") return " + mhCaseStr(source, ld) + ";");
                }
            }
            b.addLine("else throw new " + exp + "(\"unreached\");");
        }
        return b.unindent()
                .addLine("};")
                .addLine("MHS = " + stableValue + ".list(" + dataList.size() + ", f);")
                .unindent()
                .addLine("}")
                .newLine();
    }

    private static String libStr(Source source, LibData libData) {
        String[] value = libData.lib().value();
        if(value.length == 0) {
            return "";
        }else if(value.length == 1) {
            return value[0];
        }else {
            String list = source.register(List.class);
            return list + ".of(" + String.join(", ", value) + ")";
        }
    }

    private static String relyOnStr(Source source, LibData libData) {
        String[] value = libData.lib().relyOn();
        if(value.length == 0) {
            return null;
        }else if(value.length == 1) {
            return value[0];
        }else {
            String list = source.register(List.class);
            return list + ".of(" + String.join(", ", value) + ")";
        }
    }

    private static String mhMethodStr(Source source, LinkData ld) {
        String[] name = ld.link().name();
        if(name.length == 1) {
            return "\"" + name[0] + "\"";
        } else {
            String list = source.register(List.class);
            return list + ".of(" + Arrays.stream(name).map(s -> "\"" + s + "\"").collect(Collectors.joining(", ")) + ")";
        }
    }

    private static String mhFunctionDescriptorStr(Source source, LinkData ld) {
        String fd = source.register(FunctionDescriptor.class);
        String vl = source.register(ValueLayout.class);
        Types t = env().getTypeUtils();
        ExecutableElement ex = ld.method();
        TypeMirror r = ex.getReturnType();
        List<? extends VariableElement> ps = ex.getParameters();
        StringBuilder sb = new StringBuilder(t.isSameType(r, VOID_TYPE.orElseThrow()) ? fd + ".ofVoid(" : fd + ".of(");
        List<String> list = new ArrayList<>();
        // add return types
        if(!t.isSameType(r, VOID_TYPE.orElseThrow())) {
            list.add(vl + nativeLayoutMapping(t, r));
        }
        // add captureStateLayout argument
        if(ld.link().capturedCallState().length > 0) {
            list.add(vl + ".ADDRESS"); // reserve an address parameter for capturedState
            ps.removeFirst();
        }
        // add function arguments
        for(VariableElement p : ps) {
            TypeMirror tm = p.asType();
            list.add(vl + nativeLayoutMapping(t, tm));
        }
        sb.append(String.join(", ", list));
        return sb.append(")").toString();
    }

    private static String nativeLayoutMapping(Types t, TypeMirror tm) {
        if(t.isSameType(BOOLEAN_TYPE.orElseThrow(), tm)) {
            return ".JAVA_BOOLEAN";
        }else if(t.isSameType(BYTE_TYPE.orElseThrow(), tm)) {
            return ".JAVA_BYTE";
        }else if(t.isSameType(SHORT_TYPE.orElseThrow(), tm)) {
            return ".JAVA_SHORT";
        }else if(t.isSameType(INT_TYPE.orElseThrow(), tm)) {
            return ".JAVA_INT";
        }else if(t.isSameType(LONG_TYPE.orElseThrow(), tm)) {
            return ".JAVA_LONG";
        }else if(t.isSameType(FLOAT_TYPE.orElseThrow(), tm)) {
            return ".JAVA_FLOAT";
        }else if(t.isSameType(DOUBLE_TYPE.orElseThrow(), tm)) {
            return ".JAVA_DOUBLE";
        }else if(t.isSameType(CHAR_TYPE.orElseThrow(), tm)) {
            return ".JAVA_CHAR";
        }else if(t.isSameType(PTR_TYPE.orElseThrow(), tm)) {
            return ".ADDRESS";
        }else {
            throw new RuntimeException("Unrecognized native layout type");
        }
    }

    private static String mhOptionStr(Source source, LinkData ld) {
        String linker = source.register(Linker.class);
        List<String> list = new ArrayList<>();
        Link link = ld.link();
        if(link.critical()) {
            list.add(linker + ".Option.critical(" + (link.allowHeapAccess() ? "true" : "false") + ")");
        }
        if(link.capturedCallState().length > 0) {
            list.add(linker + ".Option.captureCallState(" + Arrays.stream(link.capturedCallState()).map(s -> "\"" + s + "\"").collect(Collectors.joining(", ")) + ")");
        }
        if(link.firstVariadicArg() > 0) {
            list.add(linker + ".Option.firstVariadicArg(" + link.firstVariadicArg() + ")");
        }
        return list.isEmpty() ? "" : ", " + String.join(", ", list);
    }

    private static String mhCaseStr(Source source, LinkData ld) {
        String dyn = source.register(Dyn.class);
        String head = dyn + ".mh(linker, lookup, " + mhMethodStr(source, ld) + ", " + mhFunctionDescriptorStr(source, ld);
        String tail = mhOptionStr(source, ld);
        return head + tail + ")";
    }

    private static Block innerImplementationBlock(Source source, LibData libData) {
        String runtimeException = source.register(RuntimeException.class);
        String override = source.register(Override.class);
        String throwable = source.register(Throwable.class);
        Block b = new Block();
        for (int index = 0; index < libData.dataList().size(); index++) {
            LinkData ld = libData.dataList().get(index);
            ParameterStr pStr = castParameterValues(source, ld);
            String rStr = castReturnValue(source, ld);
            String mStr = ld.method().getSimpleName().toString();
            b.addLine("@" + override)
                    .addLine("public " + rStr + " " + mStr + "(" + pStr.fullParameters() + ") {")
                    .indent()
                    .addLine("try {")
                    .indent()
                    .addLine((rStr.equals("void") ? "" : "return (" + rStr + ") ") + "MHS.get(" + index + ").invokeExact(" + pStr.shortParameters() + ");")
                    .unindent()
                    .addLine("} catch (" + throwable + " e) {")
                    .indent()
                    .addLine("throw new " + runtimeException + "(\"Failed to invoke function " + mStr + "()\", e);")
                    .unindent()
                    .addLine("}")
                    .unindent()
                    .addLine("}")
                    .newLine();
        }
        return b;
    }

    private static Block outerImplementationBlock(Source source, LibData libData) {
        String cls = source.register(Class.class);
        String target = source.register(env().getElementUtils(), libData.element());
        String supplier = source.register(Supplier.class);
        String stableValue = source.register(StableValue.class);
        return new Block()
                .addLine("@Override")
                .addLine("public " + cls + "<?> target() { ")
                .indent()
                .addLine("return " + target + ".class;")
                .unindent()
                .addLine("}")
                .newLine()
                .addLine("@Override")
                .addLine("public " + supplier + "<?> supplier() { ")
                .indent()
                .addLine("return " + stableValue + ".supplier(Impl::new);")
                .unindent()
                .addLine("}")
                .newLine();
    }

    private static Block closureBlock() {
        return new Block().unindent().addLine("}");
    }

    private static String castReturnValue(Source source, LinkData ld) {
        Types t = env().getTypeUtils();
        TypeMirror tm = ld.method().getReturnType();
        if(t.isSameType(BOOLEAN_TYPE.orElseThrow(), tm)) {
            return boolean.class.getName();
        }else if(t.isSameType(BYTE_TYPE.orElseThrow(), tm)) {
            return byte.class.getName();
        }else if(t.isSameType(SHORT_TYPE.orElseThrow(), tm)) {
            return short.class.getName();
        }else if(t.isSameType(INT_TYPE.orElseThrow(), tm)) {
            return int.class.getName();
        }else if(t.isSameType(LONG_TYPE.orElseThrow(), tm)) {
            return long.class.getName();
        }else if(t.isSameType(FLOAT_TYPE.orElseThrow(), tm)) {
            return float.class.getName();
        }else if(t.isSameType(DOUBLE_TYPE.orElseThrow(), tm)) {
            return double.class.getName();
        }else if(t.isSameType(CHAR_TYPE.orElseThrow(), tm)) {
            return char.class.getName();
        }else if(t.isSameType(PTR_TYPE.orElseThrow(), tm)) {
            return source.register(MemorySegment.class);
        }else if(t.isSameType(VOID_TYPE.orElseThrow(), tm)) {
            return void.class.getName();
        }else {
            throw new RuntimeException("Matching type for return type not supported : " + tm);
        }
    }

    @ValueBased
    record ParameterStr(
            String fullParameters,
            String shortParameters
    ) {

    }

    private static ParameterStr castParameterValues(Source source, LinkData ld) {
        Types t = env().getTypeUtils();
        List<? extends VariableElement> ps = ld.method().getParameters();
        List<String> fullList = new ArrayList<>();
        List<String> shortList = new ArrayList<>();
        for(VariableElement p : ps) {
            String pName = p.getSimpleName().toString();
            TypeMirror tm = p.asType();
            String tName;
            if(t.isSameType(BOOLEAN_TYPE.orElseThrow(), tm)) {
                tName = boolean.class.getName();
            }else if(t.isSameType(BYTE_TYPE.orElseThrow(), tm)) {
                tName = byte.class.getName();
            }else if(t.isSameType(SHORT_TYPE.orElseThrow(), tm)) {
                tName = short.class.getName();
            }else if(t.isSameType(INT_TYPE.orElseThrow(), tm)) {
                tName = int.class.getName();
            }else if(t.isSameType(LONG_TYPE.orElseThrow(), tm)) {
                tName = long.class.getName();
            }else if(t.isSameType(FLOAT_TYPE.orElseThrow(), tm)) {
                tName = float.class.getName();
            }else if(t.isSameType(DOUBLE_TYPE.orElseThrow(), tm)) {
                tName = double.class.getName();
            }else if(t.isSameType(CHAR_TYPE.orElseThrow(), tm)) {
                tName = char.class.getName();
            }else if(t.isSameType(PTR_TYPE.orElseThrow(), tm)) {
                tName = source.register(MemorySegment.class);
            }else {
                throw new RuntimeException("Matching type for parameter type not supported : " + tm);
            }
            shortList.add(pName);
            fullList.add(tName + " " + pName);
        }
        return new ParameterStr(String.join(", ", fullList), String.join(", ", shortList));
    }
}
