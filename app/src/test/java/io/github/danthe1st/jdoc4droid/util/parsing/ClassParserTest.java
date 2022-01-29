package io.github.danthe1st.jdoc4droid.util.parsing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.jsoup.Jsoup;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.text.Normalizer;
import java.util.Map;
import java.util.Objects;

import io.github.danthe1st.jdoc4droid.model.ClassInformation;
import io.github.danthe1st.jdoc4droid.model.textholder.HtmlStringHolder;
import io.github.danthe1st.jdoc4droid.model.textholder.TextHolder;
import io.github.danthe1st.jdoc4droid.tests.example.ExampleClass;

public class ClassParserTest extends AbstractParserTest {
    @Test
    public void testClassParser() throws IOException {
        ClassInformation classInformation = loadExampleClass();
        String rawHeader = classInformation.getHeader().getRawText();
        assertEquals((majorVersionNumber<9?"":"Package ")+ Objects.requireNonNull(ExampleClass.class.getPackage()).getName()+" Class "+ExampleClass.class.getSimpleName(), getTextFromHTML(rawHeader));
    }

    @Test
    public void checkDescription() throws IOException{
        ClassInformation classInformation = loadExampleClass();
        TextHolder data=getSection(classInformation, majorVersionNumber>16?"class-description":"description", "","");
        assertEquals("public class "+ExampleClass.class.getSimpleName()+(majorVersionNumber>15?" ":System.lineSeparator())+"extends "+(majorVersionNumber>15?"":"java.lang.")+"Object Example class description",getTextFromHTML(data.getRawText()));
    }

    @Test
    public void checkMethodDetails() throws IOException {
        ClassInformation classInformation = loadExampleClass();
        String signature="public void someMethod()";
        String innerKey=majorVersionNumber>12&&majorVersionNumber<15?"someMethod":signature;
        TextHolder data=getSection(classInformation,"details",majorVersionNumber>12?"Method Details":"Method Detail",innerKey);
        assertEquals("someMethod "+signature+" Example method description",cleanUp(getTextFromHTML(data.getRawText())));
        assertTrue(data instanceof HtmlStringHolder);
        assertEquals("someMethod--",((HtmlStringHolder)data).getAnchor().replaceAll("[()]","-"));
    }

    private TextHolder getSection(ClassInformation classInformation, String outerKey, String middleKey, String innerKey){
        Map<TextHolder, Map<TextHolder, Map<TextHolder, TextHolder>>> outer = classInformation.getSections();
        Map<TextHolder, Map<TextHolder, TextHolder>> middle = getValueFromRoughRawKeyMatch(outer,outerKey);
        assertNotNull(middle);
        Map<TextHolder, TextHolder> inner = getValueFromRoughRawKeyMatch(middle,middleKey);
        assertNotNull(inner);
        TextHolder data = getValueFromRoughRawKeyMatch(inner,innerKey);
        assertNotNull(data);
        return data;
    }

    private static <T> T getValueFromRoughRawKeyMatch(Map<TextHolder,T> map, String key){
        return map.entrySet()
                .stream()
                .filter(e->
                        cleanUp(e.getKey())
                                .equals(key)
                )
                .map(Map.Entry::getValue)
                .findAny().orElse(null);
    }

    private static String cleanUp(TextHolder holder){
        String txt=holder.getRawText();
        if(holder instanceof HtmlStringHolder){
            txt=getTextFromHTML(txt);
        }
        return cleanUp(txt);
    }

    private static String cleanUp(String toClean){
        return Normalizer.normalize(
                toClean
                        .replace("\u200B","")
                        .replace("\u00A0"," ")
                ,Normalizer.Form.NFKC);
    }

    static ClassInformation loadExampleClass() throws IOException {
        return ClassParser.parseClassInformation(new File(outputDir.toFile(), AbstractParserTest.EXAMPLE_CLASS_PATH), null);
    }

    private static String getTextFromHTML(String text){
        return Jsoup.parse(text).text();
    }

    @Test
    public void testNonExistantClass(){
        assertThrows(IOException.class,()-> ClassParser.parseClassInformation(new File("thisfiledoesnotexist"), null));
    }
    @Test
    public void testNonClassHtmlFIle(){
        assertThrows(IOException.class,()-> ClassParser.parseClassInformation(new File(outputDir.toFile(), "index.html"), null));
    }
}
