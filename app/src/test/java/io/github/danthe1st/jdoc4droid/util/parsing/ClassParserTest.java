package io.github.danthe1st.jdoc4droid.util.parsing;

import static org.junit.Assert.assertEquals;
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
import io.github.danthe1st.jdoc4droid.model.textholder.StringHolder;
import io.github.danthe1st.jdoc4droid.model.textholder.TextHolder;
import io.github.danthe1st.jdoc4droid.tests.example.ExampleClass;

public class ClassParserTest extends AbstractParserTest {
    @Test
    public void testClassParser() throws IOException {
        ClassInformation classInformation = loadExampleClass();
        String rawHeader = classInformation.getHeader().getRawText();
        assertEquals("Package "+ Objects.requireNonNull(ExampleClass.class.getPackage()).getName()+" Class "+ExampleClass.class.getSimpleName(), getTextFromHTML(rawHeader));
    }

    @Test
    public void checkDescription() throws IOException{
        ClassInformation classInformation = loadExampleClass();
        TextHolder data=getSection(classInformation, new StringHolder(majorVersionNumber>14?"class-description":"description"), TextHolder.EMPTY, TextHolder.EMPTY);
        assertEquals("public class "+ExampleClass.class.getSimpleName()+(majorVersionNumber>14?" ":System.lineSeparator())+"extends "+(majorVersionNumber>14?"":"java.lang.")+"Object Example class description",getTextFromHTML(data.getRawText()));
    }

    @Test
    public void checkMethodDetails() throws IOException {
        ClassInformation classInformation = loadExampleClass();
        String normalized=Normalizer.normalize(Jsoup.parse("public&nbsp;void&nbsp;someMethod()").text(),Normalizer.Form.NFKC);
        String innerKey=majorVersionNumber>11&&majorVersionNumber<15?"someMethod":normalized;
        TextHolder data=getSection(classInformation,new StringHolder("details"),new StringHolder(majorVersionNumber>11?"Method Details":"Method Detail"),new StringHolder(innerKey));
        assertEquals("someMethod "+normalized+" Example method description",Normalizer.normalize(getTextFromHTML(data.getRawText()),Normalizer.Form.NFKC));
    }

    private TextHolder getSection(ClassInformation classInformation, TextHolder outerKey, TextHolder middleKey, TextHolder innerKey){
        Map<TextHolder, Map<TextHolder, Map<TextHolder, TextHolder>>> outer = classInformation.getSections();
        assertTrue(outer.containsKey(outerKey));
        Map<TextHolder, Map<TextHolder, TextHolder>> middle = outer.get(outerKey);
        Objects.requireNonNull(middle);
        assertTrue(middle.containsKey(middleKey));
        Map<TextHolder, TextHolder> inner = middle.get(middleKey);
        Objects.requireNonNull(inner);
        assertTrue(inner.containsKey(innerKey));
        TextHolder data = inner.get(innerKey);
        return Objects.requireNonNull(data);
    }

    static ClassInformation loadExampleClass() throws IOException {
        return ClassParser.parseClassInformation(new File(outputDir.toFile(), AbstractParserTest.EXAMPLE_CLASS_PATH), null);
    }

    private String getTextFromHTML(String text){
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
