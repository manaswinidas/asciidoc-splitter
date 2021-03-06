package com.redhat.documentation.asciidoc.extraction;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.redhat.documentation.asciidoc.Util;
import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.Section;
import org.asciidoctor.ast.StructuralNode;

/**
 * An Assembly is essentially a full document from an asciidoc perspective.
 * An Assembly can contain links to modules and also have some additional text, typically before the includes, sort of like a preamble.
 */
public class Assembly {

    private String id;
    private String idWithoutContext;
    private String context;
    private List<ExtractedModule> modules;
    private StringBuilder source;
    private boolean createAssembly;

    public Assembly(Document doc, List<String> lines) {
        this.id = (doc.getId() == null ? doc.getBlocks().get(0).getId() : doc.getId());
        // If there isn't an explicit id, it starts with an _
        if (this.id.startsWith("_")) {
            // Don't use the first character (an underscore) and replace underscore with hyphen
            this.id = this.id.substring(1).replaceAll("_", "-");
        }

        if (this.id.contains("{context}")) {
            this.idWithoutContext = this.id.substring(0, this.id.lastIndexOf("{context}") - 1);
        } else {
            this.idWithoutContext = this.id;
        }

        this.context = this.id;
        this.modules = new ArrayList<>();
        this.source = new StringBuilder();

        this.createAssembly = Boolean.parseBoolean(doc.getAttribute("assembly", "true").toString());

        // Adding the id of the module
        this.source.append("[id=\"").append(this.idWithoutContext).append("_{context}\"]\n")
                // Adding the section title
                .append("= ").append(doc.getTitle()).append("\n")
                .append(":context: ").append(this.idWithoutContext).append("\n\n");

        // Grab the preamble
        if (doc.findBy(Map.of("context", ":preamble")).size() > 0) {
            for (int i = lines.indexOf("= " + doc.getTitle()); i < getPreambleEndLineNumber(doc, lines); i++) {
                this.source.append(Util.fixIncludes(lines.get(i))).append("\n");
            }
            this.source.append("\n");
        }

        List<SectionWrapper> moduleSources = new ArrayList<>();

        var modules = doc.findBy(Map.of("context", ":section")).stream()
                .filter(ExtractedModule::isNodeAModule)
                .map(Section.class::cast)
                .collect(Collectors.toList());

        var modulesItr = modules.listIterator();
        while (modulesItr.hasNext()) {
            var section = modulesItr.next();

            if (modulesItr.hasNext()) {
                var nextSection = modules.get(modulesItr.nextIndex());
                var sectionEndLineNumber = nextSection.getSourceLocation().getLineNumber() -1;

                var nextSectionLine = lines.get(sectionEndLineNumber);
                // We have to find the end of this section by looking at the next section and going back looking for
                // a blank or empty string
                while (!(nextSectionLine.isEmpty() || nextSectionLine.isBlank())) {
                    sectionEndLineNumber -= 1;
                    nextSectionLine = lines.get(sectionEndLineNumber);
                }

                // Add it to the list
                moduleSources.add(new SectionWrapper(section, getSectionSource(lines, section, sectionEndLineNumber)));

            } else {
                // Add it to the list
                moduleSources.add(new SectionWrapper(section, getSectionSource(lines, section, lines.size())));
            }
        }

        moduleSources.forEach(wrapper -> {
            var extractedModule = new ExtractedModule(wrapper.getSection(), wrapper.getSource());

            // Additional resources special case
            if (extractedModule.isAdditonalResources()) {
                this.source.append("=".repeat(extractedModule.getLeveloffset() + 1))
                        .append(" ")
                        .append(extractedModule.getSection().getTitle())
                        .append("\n\n")
                        .append(extractedModule.getSource());
                return;
            }

            this.modules.add(extractedModule);
            this.source.append("include::modules/")
                    .append(extractedModule.getFolder())
                    .append(File.separator)
                    .append(extractedModule.getFileName())
                    .append("[leveloffset=+" + extractedModule.getLeveloffset() + "]")
                    .append("\n\n");
        });
    }

    private int getPreambleEndLineNumber(StructuralNode doc, List<String> lines) {
        var nextSection = doc.getBlocks().get(1);
        var sectionEndLineNumber = nextSection.getSourceLocation().getLineNumber() -1;

        var nextSectionLine = lines.get(sectionEndLineNumber);
        // We have to find the end of this section by looking at the next section and going back looking for
        // a blank or empty string
        while (!(nextSectionLine.isEmpty() || nextSectionLine.isBlank())) {
            sectionEndLineNumber -= 1;
            nextSectionLine = lines.get(sectionEndLineNumber);
        }
        return sectionEndLineNumber;
    }

    private String getSectionSource(List<String> lines, StructuralNode section, int nextSectionStart) {
        var startingLine = section.getSourceLocation().getLineNumber();
        StringBuilder sectionSource = new StringBuilder();
        for (int i = startingLine; i < nextSectionStart; i++) {
            sectionSource.append(Util.fixIncludes(lines.get(i))).append("\n");
        }
        return sectionSource.toString();
    }

    public String getId() {
        return id;
    }

    public String getContext() {
        return context;
    }

    public List<ExtractedModule> getModules() {
        return Collections.unmodifiableList(this.modules);
    }

    public String getSource() {
        return source.toString();
    }

    public String getFilename() {
        return "assembly-" + idWithoutContext + ".adoc".toLowerCase();
    }

    public boolean shouldCreateAssembly() {
        return this.createAssembly;
    }

    @Override
    public String toString() {
        return "Assembly{" +
               "id='" + id + '\'' +
               ", module size=" + modules.size() +
               ", createAssembly=" + createAssembly +
               '}';
    }
}

