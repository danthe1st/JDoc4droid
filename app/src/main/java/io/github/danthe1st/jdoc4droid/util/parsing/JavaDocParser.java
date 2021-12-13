package io.github.danthe1st.jdoc4droid.util.parsing;

import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import io.github.danthe1st.jdoc4droid.model.ClassInformation;
import io.github.danthe1st.jdoc4droid.model.SimpleClassDescription;
import lombok.experimental.UtilityClass;

@UtilityClass
public class JavaDocParser {

    Document parseFile(File file) throws IOException {
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
        classes = IndexParser.parseClasses(javaDocDir);
        saveClassesToCache(classes, cacheFile);
        return classes;
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
        ClassInformation info = ClassParser.parseClassInformation(classFile, selectedId);
        saveInformationToCache(info, cacheFile);
        return info;
    }

    void saveClassesToCache(List<SimpleClassDescription> classes, File cacheFile) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(cacheFile)))) {
            oos.writeObject(classes);
        }
    }

    List<SimpleClassDescription> loadClassesFromCache(File cacheFile) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(cacheFile)))) {
            return (List<SimpleClassDescription>) ois.readObject();
        }
    }

    ClassInformation loadInformationFromCache(File cacheFile) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(cacheFile)))) {
            return (ClassInformation) ois.readObject();
        }
    }

    void saveInformationToCache(ClassInformation info, File cacheFile) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(cacheFile)))) {
            oos.writeObject(info);
        }
    }
}
