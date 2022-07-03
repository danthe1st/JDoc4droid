package io.github.danthe1st.jdoc4droid.util.parsing;

import static org.junit.Assert.*;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import io.github.danthe1st.jdoc4droid.model.SimpleClassDescription;
import io.github.danthe1st.jdoc4droid.tests.example.ExampleClass;
import io.github.danthe1st.jdoc4droid.tests.example.SomeEnum;

public class IndexParserTest extends AbstractParserTest {
	@Test
	public void testExampleClass() throws IOException {
		testClass(ExampleClass.class, "Class", "Classes", "Example class description");
	}

	@Test
	public void testEnum() throws IOException {
		testClass(SomeEnum.class, "Enum", "Enum Classes", "");
	}

	private void testClass(Class<?> cl, String oldType, String newType, String description) throws IOException {
		List<SimpleClassDescription> descs = IndexParser.parseClasses(outputDir.toFile());
		Optional<SimpleClassDescription> classDescription = descs.stream().filter(desc -> cl.getSimpleName().equals(desc.getName())).findAny();
		assertTrue("No SimpleClassDescription found for " + cl.getSimpleName(), classDescription.isPresent());
		SimpleClassDescription expected = new SimpleClassDescription(cl.getSimpleName(), majorVersionNumber < 11 ? "" : description, majorVersionNumber >= 17 ? newType : oldType, cl.getPackage().getName(), getPathOfClass(cl));
		assertEquals("Loaded SimpleClassDescription does not match expected one", expected, classDescription.get());
	}

	@Test
	public void testNonExistingJavadoc() {
		assertThrows(IOException.class, () -> IndexParser.parseClasses(new File("thisdoesnotexist")));
	}
}
