package io.github.benrushcc.bench;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@BenchmarkMode(value = Mode.AverageTime)
@Warmup(iterations = 3, time = 400, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 1200, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public abstract class BaseBench {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private static final BiConsumer<Class<?>, ChainedOptionsBuilder> DEFAULT_CONFIGURATION = (launchClass, builder) -> builder.include(launchClass.getSimpleName())
            .detectJvmArgs()
            .resultFormat(ResultFormatType.TEXT)
            .result("%s_%s.txt".formatted(launchClass.getSimpleName(), LocalDateTime.now().format(FORMATTER)));

    protected static void run(Consumer<ChainedOptionsBuilder> consumer) {
        ChainedOptionsBuilder builder = new OptionsBuilder();
        if(consumer != null) {
            consumer.accept(builder);
        }
        Options options = builder.build();
        try {
            new Runner(options).run();
        }catch (RunnerException e) {
            throw new RuntimeException("Failed to run Benchmark", e);
        }
    }

    protected static void run(Class<?> launchClass) {
        run(b -> DEFAULT_CONFIGURATION.accept(launchClass, b));
    }
}
