package io.github.danthe1st.jdoc4droid.util.parsing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import io.github.danthe1st.jdoc4droid.model.SimpleClassDescription;
import io.github.danthe1st.jdoc4droid.tests.example.ExampleClass;

public class IndexParserTest extends AbstractParserTest {
    @Test
    public void testExample() throws IOException {
        List<SimpleClassDescription> descs = IndexParser.parseClasses(outputDir.toFile());
        Optional<SimpleClassDescription> exampleClassDescription = descs.stream().filter(desc -> ExampleClass.class.getSimpleName().equals(desc.getName())).findAny();
        assertTrue("No SimpleClassDescription found for ExampleClass", exampleClassDescription.isPresent());
        SimpleClassDescription expected = new SimpleClassDescription(ExampleClass.class.getSimpleName(), majorVersionNumber < 11 ? "" : "Example class description", "Class", ExampleClass.class.getPackage().getName(), EXAMPLE_CLASS_PATH);
        assertEquals("Loaded SimpleClassDescription does not match expected one", expected, exampleClassDescription.get());
    }

    @Test
    public void testNonExistingJavadoc() {
        assertThrows(IOException.class, () -> IndexParser.parseClasses(new File("thisdoesnotexist")));
    }
}
