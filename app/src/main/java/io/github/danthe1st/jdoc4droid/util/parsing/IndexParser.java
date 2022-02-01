package io.github.danthe1st.jdoc4droid.util.parsing;

import androidx.annotation.WorkerThread;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.github.danthe1st.jdoc4droid.model.SimpleClassDescription;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@WorkerThread
class IndexParser {
    @NonNull
    private final Element summaryTable;

    static List<SimpleClassDescription> parseClasses(File javaDocDir) throws IOException {
        File index = new File(javaDocDir, "allclasses-index.html");
        File noFrameFile = new File(javaDocDir, "allclasses-noframe.html");
        if (index.exists()) {
            return parseClassesFromAllclassesIndexFile(index);
        } else if (noFrameFile.exists()) {
            return parseClassesFromAllclassesNoframeFile(noFrameFile);
        } else {
            throw new IOException("neither allclasses-index.html nor allclasses-noframe.html found");
        }
    }

    private static List<SimpleClassDescription> parseClassesFromAllclassesIndexFile(File allclassesIndex) throws IOException {
        IndexParser parser = getParser(JavaDocParser.parseFile(allclassesIndex));
        if ("table".equals(parser.summaryTable.tagName())) {
            return parser.parseClassesFromHTMLTable();
        } else {
            return parser.parseClassesFromVirtualClassListTable();
        }
    }

    private static IndexParser getParser(Document doc) throws IOException {
        return new IndexParser(getSummaryTable(doc));
    }

    private static Element getSummaryTable(Document doc) throws IOException {
        Elements table = doc.getElementsByClass("summary-table");
        if (table.isEmpty()) {
            table = doc.getElementsByTag("table");
        }
        if (table.isEmpty()) {
            throw new IOException("summary table not found");
        }
        return table.first();
    }

    private static List<SimpleClassDescription> parseClassesFromAllclassesNoframeFile(File allclassesNoframe) throws IOException {
        return JavaDocParser.parseFile(allclassesNoframe)
                .select(".indexContainer ul li")
                .stream()
                .map(li -> loadSimpleClassDescription(li, ""))
                .collect(Collectors.toList());
    }

    private static SimpleClassDescription loadSimpleClassDescription(Element link, String description) {
        link = link.children().stream().filter(c -> "a".equals(c.tagName())).findFirst().orElseThrow(() -> new IllegalStateException("Cannot load class description as no class description was found"));
        return new SimpleClassDescription(
                link.text(),
                description,
                capitalize(link.attr("title").split(" ")[0]),
                link.attr("title").split(" ")[2],
                link.attr("href"));
    }

    private static String capitalize(String toCapitalize) {
        char[] chars = toCapitalize.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (i == 0 || Character.isWhitespace(chars[i - 1])) {
                chars[i] = Character.toUpperCase(chars[i]);
            }
        }
        return String.valueOf(chars);
    }

    private List<SimpleClassDescription> parseClassesFromHTMLTable() {
        //TODO do this partially and add it to the list
        return summaryTable.children().parallelStream()
                .filter(elem -> "tbody".equals(elem.tagName()))
                .filter(elem -> !elem.select("td").isEmpty())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("table does not have tbody"))
                .children()
                .parallelStream()
                .filter(elem -> elem.children().stream().anyMatch(c -> "td".equals(c.tagName())))
                .map(elem -> loadSimpleClassDescription(elem.child(0), elem.child(1).text()))
                .collect(Collectors.toList());
    }

    private List<SimpleClassDescription> parseClassesFromVirtualClassListTable() {
        SimpleClassDescription temp = null;
        List<SimpleClassDescription> descList = new ArrayList<>();
        for (Element child : summaryTable.children()) {
            if (child.hasClass("col-first") && child.childrenSize() > 0) {
                temp = loadSimpleClassDescription(child, "");
                descList.add(temp);
            } else if (child.hasClass("col-last") && temp != null) {
                temp.setDescription(child.text());
            }
        }
        return descList;
    }
}
