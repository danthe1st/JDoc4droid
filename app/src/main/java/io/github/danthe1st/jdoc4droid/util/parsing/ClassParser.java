package io.github.danthe1st.jdoc4droid.util.parsing;

import android.text.Html;
import android.util.Log;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

import io.github.danthe1st.jdoc4droid.model.ClassInformation;
import io.github.danthe1st.jdoc4droid.model.textholder.HtmlStringHolder;
import io.github.danthe1st.jdoc4droid.model.textholder.StringHolder;
import io.github.danthe1st.jdoc4droid.model.textholder.TextHolder;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ClassParser {
    private static final String SELECTOR_TOP = ".class-description,.description,.summary,.details";
    private static final String SELECTOR_MIDDLE = "section:not(" + SELECTOR_TOP + " section *)," + SELECTOR_TOP + ">ul>li>ul>li";
    private static final String SELECTOR_BOTTOM_RAW = "section\0,ul>li:not(:first-child)\0,ul:not(:first-child)>li\0,table>tr\0";
    private static final String SELECTOR_BOTTOM = SELECTOR_BOTTOM_RAW.replace("\0", ":not(" + SELECTOR_MIDDLE + " " + SELECTOR_BOTTOM_RAW.replace("\0", " *") + ")");


    private static class Holder<T> {
        T elem;
    }

    ClassInformation parseClassInformation(File classFile, String selectedId) throws IOException {
        Element elem = JavaDocParser.parseFile(classFile);
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
    private <T> Map<TextHolder, T> findAllSections(Element elem, Set<TextHolder> namesCallback, Function<TextHolder, T> converter, Elements selectedElemParents, Holder<TextHolder> selectedSection, String selector, BiFunction<Element, Set<TextHolder>, T> adder) {

        Map<TextHolder, T> sections = new LinkedHashMap<>();
        Elements elements = elem.select(selector);

        elements.stream().filter(e -> e != elem).forEach(outerChild -> {
            Set<TextHolder> innerNames = new HashSet<>();
            TextHolder sectionName;

            if (adder != null) {
                sectionName = NameLoader.findName(outerChild, innerNames);
                Set<TextHolder> onlyInnerNames = new HashSet<>();
                T inner = adder.apply(outerChild, onlyInnerNames);
                innerNames.addAll(onlyInnerNames);
                if (isMapWithProperSubElements(inner)) {
                    if (innerNames.contains(sectionName)) {
                        innerNames = onlyInnerNames;
                        sectionName = NameLoader.findName(outerChild, innerNames);
                    }
                    sections.put(sectionName, inner);
                } else {
                    for (Element e : outerChild.select(".summary-table>.table-header,.caption")) {
                        e.remove();
                    }
                    T converted = converter.apply(convertToHtmlStringHolder(outerChild));
                    sections.put(sectionName, converted);
                }
            } else {
                sectionName = NameLoader.findName(outerChild, innerNames, new String[]{"li.blockList>h4+pre", ".member-signature", NameLoader.SELECTOR_NAME_HEADER});
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

}
