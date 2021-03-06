package com.redhat.documentation.asciidoc.extraction;

import java.util.Collections;
import java.util.List;

import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.Preprocessor;
import org.asciidoctor.extension.PreprocessorReader;

/**
 * Preprocessor to get the lines of the source document.
 */
public class ReaderPreprocessor extends Preprocessor {
    private List<String> lines;

    @Override
    public void process(Document document, PreprocessorReader reader) {
        lines = reader.lines();
    }

    public List<String> getLines() {
        return Collections.unmodifiableList(lines);
    }
}
