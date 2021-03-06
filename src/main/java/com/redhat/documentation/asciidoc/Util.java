package com.redhat.documentation.asciidoc;

import org.asciidoctor.ast.ContentNode;

public class Util {

    public static final String MODULE_TYPE_ATTRIBUTE = "module-type";

    public static String getFullId(ContentNode node) {
        StringBuilder buf = new StringBuilder(node.getId());

        while (node.getParent() != null) {
            buf.insert(0, node.getId() + "/");
            node = node.getParent();
        }

        return buf.toString();
    }

    public static String getModuleType(ContentNode node) {
        if (node.getAttributes().containsKey(MODULE_TYPE_ATTRIBUTE))
            return node.getAttributes().get(MODULE_TYPE_ATTRIBUTE).toString();

        var id = node.getId();

        if (id.startsWith("con-"))
            return "con";

        if (id.startsWith("proc-"))
            return "proc";

        if (id.startsWith("ref-"))
            return "ref";

        return "unknown"; // punt, we don't know
    }

    public static String fixIncludes(String source) {
        return source.replaceAll("(?<include>include::)(?<path>(\\w|\\/|-)*)chap-(?<filename>.+)\\.(?<extension>.+)\\[]",
                                        "${include}assemblies/assembly-${filename}.${extension}[]")
                     .replaceAll("(?<include>include::)\\{asciidoc-dir}\\/(?<path>(\\w|\\/|-)*)\\/(?<filename>.*)\\[tags=(?<module>.+)]",
                             "${include}modules/${path}/${module}.adoc[leveloffset=+1]");
    }

}
