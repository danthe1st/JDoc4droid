package io.github.danthe1st.jdoc4droid.util.parsing;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import io.github.danthe1st.jdoc4droid.model.ClassInformation;
import io.github.danthe1st.jdoc4droid.model.SimpleClassDescription;

public class CacheTest extends AbstractParserTest {
    private File cacheFile;

    @Before
    public void setUpTest() throws IOException {
        cacheFile = new File(outputDir.toFile(), "cachetest.cache");
        if (cacheFile.exists()) {
            Files.delete(cacheFile.toPath());
        }
    }

    @Test
    public void testIndexCache() throws IOException, ClassNotFoundException {
        List<SimpleClassDescription> descs = IndexParser.parseClasses(outputDir.toFile());
        JavaDocParser.saveClassesToCache(descs, cacheFile);
        List<SimpleClassDescription> fromCache = JavaDocParser.loadClassesFromCache(cacheFile);
        assertEquals(descs, fromCache);
    }

    @Test
    public void testClassCache() throws IOException, ClassNotFoundException {
        ClassInformation classInformation = ClassParserTest.loadExampleClass();
        JavaDocParser.saveInformationToCache(classInformation, cacheFile);
        System.out.println("----------------------");
        ClassInformation fromCache = JavaDocParser.loadInformationFromCache(cacheFile);
        assertEquals(classInformation, fromCache);
    }
}
