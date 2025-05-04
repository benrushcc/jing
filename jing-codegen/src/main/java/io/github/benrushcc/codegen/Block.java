package io.github.benrushcc.codegen;

import java.util.ArrayList;
import java.util.List;

/// Block represents a bunch of lines
public final class Block {
    private static final String EMPTY_STRING = "";
    private final List<Line> lines = new ArrayList<>();
    private int currentIndent = 0;

    public Block addLine(String content) {
        lines.add(new Line(content, currentIndent));
        return this;
    }

    public Block newLine() {
        lines.add(new Line(EMPTY_STRING, currentIndent));
        return this;
    }

    public Block indent() {
        currentIndent++;
        return this;
    }

    public Block unindent() {
        currentIndent--;
        return this;
    }

    public List<Line> lines() {
        return List.copyOf(lines);
    }
}
