package io.github.danthe1st.jdoc4droid.util;

import android.text.Html;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.github.danthe1st.jdoc4droid.model.ClassInformation;
import io.github.danthe1st.jdoc4droid.model.SimpleClassDescription;
import io.github.danthe1st.jdoc4droid.model.textholder.HtmlStringHolder;
import io.github.danthe1st.jdoc4droid.model.textholder.StringHolder;
import io.github.danthe1st.jdoc4droid.model.textholder.TextHolder;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

@UtilityClass
public class JavaDocParser {
    private static final String SELECTOR_TOP = ".description,.summary,.details";
    private static final String SELECTOR_MIDDLE = "section:not(" + SELECTOR_TOP + " section *)," + SELECTOR_TOP + ">ul>li>ul>li";
    private static final String SELECTOR_BOTTOM_RAW = "section\0,ul>li:not(:first-child)\0,ul:not(:first-child)>li\0,table>tr\0";
    private static final String SELECTOR_BOTTOM = SELECTOR_BOTTOM_RAW.replace("\0", ":not(" + SELECTOR_MIDDLE + " " + SELECTOR_BOTTOM_RAW.replace("\0", " *") + ")");
    private static final String SELECTOR_NAME_HEADER = "h1,h2,h3,h4,h5";

    public static String loadName(File tempDir) throws IOException {
        Document doc = Jsoup.parse(new File(tempDir,"index.html"), StandardCharsets.UTF_8.name());
        return doc.title();
    }

    private static class Holder<T> {
        T elem;
    }

    private Document parseFile(File file) throws IOException {
        Document doc = Jsoup.parse(file, StandardCharsets.UTF_8.name());
        doc.outputSettings().prettyPrint(false);
        return doc;
    }

    public List<SimpleClassDescription> loadClasses(File javaDocDir) throws IOException {
        File cacheFile = new File(javaDocDir, "classlist.cache");
        List<SimpleClassDescription> classes;
        if (cacheFile.exists()) {
            try {
                return loadClassesFromCache(cacheFile);
            } catch (ClassNotFoundException | IOException e) {
                Log.w(JavaDocParser.class.getName(), "Cannot load classes from cache", e);
            }
        }
        classes = parseClasses(javaDocDir);
        saveClassesToCache(classes, cacheFile);
        return classes;
    }

    private void saveClassesToCache(List<SimpleClassDescription> classes, File cacheFile) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(cacheFile)))) {
            oos.writeObject(classes);
        }
    }

    private List<SimpleClassDescription> loadClassesFromCache(File cacheFile) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(cacheFile)))) {
            return (List<SimpleClassDescription>) ois.readObject();
        }
    }

    private List<SimpleClassDescription> parseClasses(File javaDocDir) throws IOException {
        //TODO show error on exception
        File index = new File(javaDocDir, "allclasses-index.html");
        if (index.exists()) {
            Element summaryTable = getSummaryTable(parseFile(index));
            if ("table".equals(summaryTable.tagName())) {
                summaryTable = summaryTable.child(1);//table body
                //TODO do this partially and add it to the list
                return summaryTable.children()
                        .stream()
                        .skip(1)
                        .map(elem -> loadSimpleClassDescription(elem.child(0), elem.child(1).text()))
                        .collect(Collectors.toList());
            } else {
                SimpleClassDescription temp = null;
                List<SimpleClassDescription> descList = new ArrayList<>();
                for (Element child : summaryTable.children()) {
                    if (child.hasClass("col-first")&&child.childrenSize()>0) {
                        temp = loadSimpleClassDescription(child, "");
                        descList.add(temp);
                    } else if (child.hasClass("col-last")&&temp != null) {
                        temp.setDescription(child.text());
                    }
                }
                return descList;
            }
        } else if ((index = new File(javaDocDir, "allclasses-noframe.html")).exists()) {
            return parseFile(index)
                    .select(".indexContainer ul li")
                    .stream()
                    .map(li -> loadSimpleClassDescription(li,""))
                    .collect(Collectors.toList());
        } else {
            throw new IOException("neither allclasses-index.html nor allclasses-noframe.html found");
        }
    }

    private static SimpleClassDescription loadSimpleClassDescription(Element link, String description) {
        link = link.child(0);
        return new SimpleClassDescription(
                link.text(),
                description,
                link.attr("title").split(" ")[0],
                link.attr("title").split(" ")[2],
                link.attr("href"));
    }

    private Element getSummaryTable(Document doc) throws IOException {
        Elements table = doc.getElementsByClass("summary-table");
        if (table.isEmpty()) {
            table = doc.getElementsByTag("table");
        }
        if (table.isEmpty()) {
            throw new IOException("summary table not found");
        }
        return table.first();
    }

    private <T> Map<TextHolder, T> findAllSections(Element elem, Set<TextHolder> namesCallback, Function<TextHolder, T> converter, Elements selectedElemParents, Holder<TextHolder> selectedSection, String selector, BiFunction<Element, Set<TextHolder>, T> adder) {

        Map<TextHolder, T> sections = new LinkedHashMap<>();
        Elements elements = elem.select(selector);

        elements.stream().filter(e -> e != elem).forEach(outerChild -> {
            Set<TextHolder> innerNames = new HashSet<>();
            TextHolder sectionName;

            if (adder != null) {
                sectionName = findName(outerChild, innerNames);
                Set<TextHolder> onlyInnerNames = new HashSet<>();
                T inner = adder.apply(outerChild, onlyInnerNames);
                innerNames.addAll(onlyInnerNames);
                if (isMapWithProperSubElements(inner)) {
                    if (innerNames.contains(sectionName)) {
                        innerNames = onlyInnerNames;
                        sectionName = findName(outerChild, innerNames);
                    }
                    sections.put(sectionName, inner);
                } else {
                    T converted = converter.apply(convertToHtmlStringHolder(outerChild));
                    sections.put(sectionName, converted);
                }
            } else {
                sectionName = findName(outerChild, innerNames, new String[]{"li.blockList>h4+pre", ".member-signature", SELECTOR_NAME_HEADER});
                T converted = converter.apply(convertToHtmlStringHolder(outerChild));
                sections.put(sectionName, converted);
            }
            if (namesCallback != null) {
                namesCallback.addAll(innerNames);
            }
            if (selectedElemParents != null && selectedElemParents.contains(outerChild)) {
                selectedSection.elem = sectionName;
            }
        });
        return sections;
    }

    private HtmlStringHolder convertToHtmlStringHolder(Element element) {
        for (Element elem : element.select("dt")) {
            elem.tagName("b");
        }
        for (Element elem : element.select("dd")) {
            elem.tagName("p");
        }

        Element idLookup=element;
        String id;
        while((id=idLookup.id()).isEmpty()&&idLookup.childrenSize()>0&&idLookup.ownText().replaceAll("\\s+","").isEmpty()){
            idLookup=idLookup.child(0);
        }
        return new HtmlStringHolder(element.html(), Html.FROM_HTML_SEPARATOR_LINE_BREAK_PARAGRAPH,null,id);
    }

    private boolean isMapWithProperSubElements(Object toTest) {
        if (!(toTest instanceof Map)) {
            return false;
        }
        Map<?, ?> mapToTest = (Map<?, ?>) toTest;
        if (mapToTest.isEmpty()) {
            return false;
        }
        if (mapToTest.size() > 1) {
            return true;
        }
        return !mapToTest.containsKey(TextHolder.EMPTY);
    }


    public ClassInformation loadClassInformation(File classFile, String selectedId) throws IOException {
        File cacheFile = new File(classFile.getParentFile(), classFile.getName() + ".cache");
        if (cacheFile.exists()) {
            try {
                return loadInformationFromCache(cacheFile);
            } catch (IOException | ClassNotFoundException | OutOfMemoryError e) {
                Log.w(JavaDocParser.class.getName(), "Cannot load classes from cache", e);
            }
        }
        ClassInformation info = parseClassInformation(classFile, selectedId);
        saveInformationToCache(info, cacheFile);
        return info;
    }

    private ClassInformation loadInformationFromCache(File cacheFile) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(cacheFile)))) {
            return (ClassInformation) ois.readObject();
        }
    }

    private void saveInformationToCache(ClassInformation info, File cacheFile) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(cacheFile)))) {
            oos.writeObject(info);
        }
    }

    private ClassInformation parseClassInformation(File classFile, String selectedId) throws IOException {
        Element elem = parseFile(classFile);
        Holder<TextHolder> selectedOuterSection = new Holder<>();
        Holder<TextHolder> selectedMiddleSection = new Holder<>();
        Holder<TextHolder> selectedInnerSection = new Holder<>();
        Element selectedElement = selectedId == null ? null : elem.getElementById(selectedId);
        Elements selectedElemParents = selectedElement == null ? null : selectedElement.parents();
        TextHolder header = new HtmlStringHolder(elem.getElementsByClass("header").get(0).html(), Html.FROM_HTML_MODE_COMPACT);
        Map<TextHolder, Map<TextHolder, Map<TextHolder, TextHolder>>> outerSections =
                findAllSections(elem, null, data -> Collections.singletonMap(TextHolder.EMPTY, Collections.singletonMap(TextHolder.EMPTY, data)), selectedElemParents, selectedOuterSection, SELECTOR_TOP, (middleElem, middleNames) ->
                        findAllSections(middleElem, middleNames, data -> Collections.singletonMap(TextHolder.EMPTY, data), selectedElemParents, selectedMiddleSection, SELECTOR_MIDDLE, (innerElem, innerNames) ->
                                findAllSections(innerElem, innerNames, data -> data, selectedElemParents, selectedInnerSection, SELECTOR_BOTTOM, null))
                );
        return new ClassInformation(header, outerSections, selectedOuterSection.elem, selectedMiddleSection.elem, selectedInnerSection.elem);
    }


    private TextHolder findName(Element elem, Set<TextHolder> currentNames) {
        return findName(elem, currentNames, new String[]{SELECTOR_NAME_HEADER});
    }

    @SneakyThrows
    private TextHolder findName(Element elem, Set<TextHolder> currentNames, String[] selectorNameHeaders) {
        String name;
        Element root = elem.parents().last();
        Element firstHeader = null;
        for (int i = 0; firstHeader == null && i < selectorNameHeaders.length; i++) {
            String selectorNameHeader = selectorNameHeaders[i];
            firstHeader = elem.selectFirst(selectorNameHeader);
        }

        if (firstHeader != null) {
            name = firstHeader.text();
            TextHolder nameHolder = new StringHolder(name);
            if (name != null && !name.isEmpty() && !currentNames.contains(nameHolder)) {
                TextHolder ret = nameHolder;
                currentNames.add(nameHolder);
                Elements mainNameElems = elem.getElementsByClass("member-name");
                if (!mainNameElems.isEmpty()) {
                    String mainName = mainNameElems.first().text();
                    int mainNameIndex;
                    if ((mainNameIndex = name.indexOf(mainName)) != -1) {
                        name = name.substring(0, mainNameIndex) + "<b>" + mainName + "</b>" + name.substring(mainNameIndex + mainName.length());
                        ret = new HtmlStringHolder(name, Html.FROM_HTML_MODE_COMPACT, mainName);
                    }
                }
                return ret;
            }
        }
        name = elem.attr("id");
        if (name != null && !name.isEmpty()) {
            Elements referencers = root.getElementsByAttributeValue("href", "#" + URLEncoder.encode(name, StandardCharsets.UTF_8.name()));
            return new StringHolder(referencers.stream().map(Element::text).findFirst().orElse(name));
        }
        name = elem.attr("class");
        TextHolder nameHolder = new StringHolder(name);
        if (name.isEmpty() || currentNames.contains(nameHolder)) {
            //fallback if nothing else works
            name = UUID.randomUUID().toString();
            nameHolder = new StringHolder(name);
            Log.e(JavaDocParser.class.getName(), "Need to generate UUID");
        } else {
            currentNames.add(nameHolder);
        }
        return nameHolder;
    }
}
