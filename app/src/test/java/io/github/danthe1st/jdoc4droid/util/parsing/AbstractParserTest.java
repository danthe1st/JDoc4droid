package io.github.danthe1st.jdoc4droid.util.parsing;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Stream;

import io.github.danthe1st.jdoc4droid.tests.example.ExampleClass;

public abstract class AbstractParserTest {
    protected static final String EXAMPLE_CLASS_PATH = ExampleClass.class.getCanonicalName().replace(".", "/") + ".html";
    protected static Path outputDir;
    protected static int majorVersionNumber;

    @BeforeClass
    public static void setUp() throws IOException, InterruptedException {
        File javaHome = findJavaHome();
        outputDir = Files.createTempDirectory("jdoc4droid.test.parsing");
        if (!javaHome.exists()) {
            fail("Java home was not found");
        }
        Process javadocProcess = new ProcessBuilder()
                .directory(javaHome)
                .command(javaHome.getAbsolutePath() + "/bin/javadoc",
                        Objects.requireNonNull(ExampleClass.class.getPackage()).getName(),
                        "-d", outputDir.toFile().getAbsolutePath(),
                        "-sourcepath", new File("src/test/java").getAbsolutePath()
                ).start();
        majorVersionNumber = loadMajorVersionNumber(javaHome);
        if (javadocProcess.waitFor() != 0) {
            fail("Creation of Javadoc didn't work");
        }
    }

    private static File findJavaHome() {
        String javaHome = System.getenv("jdoc4droid.test.parsing.java.home");
        if (javaHome == null) {
            javaHome = System.getProperty("java.home");
        }
        assertNotNull("no Java home found for parsing tests - this can be configured using the environment variable jdoc4droid.test.parsing.java.home", javaHome);
        return new File(javaHome);
    }

    private static int loadMajorVersionNumber(File javaHome) throws IOException {
        ProcessBuilder processBuilderJavaVersion = new ProcessBuilder()
                .command(javaHome.getAbsolutePath() + "/bin/java", "-version")
                .redirectError(ProcessBuilder.Redirect.PIPE);
        processBuilderJavaVersion.redirectOutput(processBuilderJavaVersion.redirectError());
        Process versionProcess = processBuilderJavaVersion.start();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(versionProcess.getErrorStream(), StandardCharsets.UTF_8))) {
            String firstLine = br.readLine();
            int startIndex = firstLine.indexOf('"') + 1;
            int endIndex = firstLine.indexOf('.', startIndex);
            if (endIndex == -1) {
                endIndex = firstLine.indexOf('"', startIndex);
            }
            String versionString = firstLine.substring(startIndex, endIndex);
            int versionInt = Integer.parseInt(versionString);
            if (versionInt == 1) {
                startIndex = endIndex + 1;
                endIndex = firstLine.indexOf('.', startIndex);
                versionInt = Integer.parseInt(firstLine.substring(startIndex, endIndex));
            }
            return versionInt;
        }
    }

    @AfterClass
    public static void cleanUp() throws IOException {
        try (Stream<Path> walk = Files.walk(outputDir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }
}
