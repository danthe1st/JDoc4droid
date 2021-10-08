package io.github.danthe1st.jdoc4droid.util;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import io.github.danthe1st.jdoc4droid.model.JavaDocInformation;
import io.github.danthe1st.jdoc4droid.model.JavaDocType;
import lombok.experimental.UtilityClass;

@UtilityClass
public class JavaDocDownloader {

    private final ExecutorService downloader = Executors.newSingleThreadExecutor();

    private static final Map<String, String> ORACLE_SUBDIR_SUFFIXES_MAPPING;
    private static final String METADATA_FILE_NAME = ".metadata";
    private static final Pattern JDK_JAVADOC_MAJOR_VERSION_PATTERN = Pattern.compile("[^0-9]*([0-9]+)([^0-9].*)?");

    static {
        Map<String, String> suffixSubdirMapping = new HashMap<>();
        suffixSubdirMapping.put("-apidocs.zip", "api");
        suffixSubdirMapping.put("_doc-all.zip", "docs/api");
        suffixSubdirMapping.put("-docs-all.zip", "docs/api");
        ORACLE_SUBDIR_SUFFIXES_MAPPING = Collections.unmodifiableMap(suffixSubdirMapping);
    }

    public boolean downloadJavaApiDocs(Context ctx, String url, Consumer<JavaDocInformation> onSuccess) {
        int queryStartIndex = url.indexOf('?');
        if (queryStartIndex == -1) {
            queryStartIndex = url.length();
        }
        int fileNameStartIndex = url.lastIndexOf('/', queryStartIndex);
        if (queryStartIndex > fileNameStartIndex) {
            String fileName = url.substring(fileNameStartIndex + 1, queryStartIndex);
            for (Map.Entry<String, String> suffixMappingEntry : ORACLE_SUBDIR_SUFFIXES_MAPPING.entrySet()) {
                String suffix = suffixMappingEntry.getKey();
                String subDirToUnzip = suffixMappingEntry.getValue();
                if (fileName.endsWith(suffix)) {
                    String name = fileName.substring(0, fileName.length() - suffix.length());
                    File javaDocDir = getJavaDocDir(ctx, name);
                    if (javaDocDir.exists()) {
                        downloader.execute(()->{
                            try {
                                onSuccess.accept(readMetadataFile(javaDocDir));
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        });

                        return false;
                    } else {
                        JavaDocInformation javaDocInfo = new JavaDocInformation(name, getJavaJavadocUrl(name), javaDocDir, JavaDocType.JDK);
                        downloadAndUnzipAsync(url, javaDocInfo, ()->onSuccess.accept(javaDocInfo), subDirToUnzip);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static String getJavaJavadocUrl(String name){
        int majorVersion=getJavaJavadocMajorVersion(name);
        String type=name.contains("javafx")?"javafx":"docs";
        String prefix=majorVersion>=11?"/en/java":"";
        return "https://docs.oracle.com"+prefix+"/javase/"+majorVersion+"/"+type+"/api/";
        //https://docs.oracle.com/en/java/javase/11/docs/api/
    }

    private static int getJavaJavadocMajorVersion(String fullVersionName){
        Matcher matcher = JDK_JAVADOC_MAJOR_VERSION_PATTERN.matcher(fullVersionName);
        if(matcher.matches()){
            return Integer.parseInt(matcher.group(1));
        }else{
            throw new IllegalArgumentException("invalid JDK version");//TODO handle somehow
        }
    }

    public static void downloadFromMavenRepo(Context ctx, String repoUrl, String groupId, String artefactId, String version, Consumer<JavaDocInformation> onSuccess) {
        String effectiveUrl = repoUrl + "/" + groupId.replace('.', '/') + "/" + artefactId + "/" + version + "/" + artefactId + "-" + version + "-javadoc.jar";
        String src;
        if("https://repo1.maven.org/maven2".equalsIgnoreCase(repoUrl)){
            src="https://javadoc.io/doc/"+groupId+"/"+artefactId+"/"+version+"/";
        }else {
            src="";
        }
        JavaDocInformation javaDocInfo = new JavaDocInformation(artefactId + " " + version, src, getJavaDocDir(ctx, fileNameFromString(repoUrl) + "_" + fileNameFromString(artefactId) + "_" + fileNameFromString(version)), JavaDocType.MAVEN);
        //TODO javadoc.io if central
        downloadAndUnzipAsync(effectiveUrl, javaDocInfo, ()->onSuccess.accept(javaDocInfo), "");
    }

    public static void downloadFromUri(Context ctx, Uri uri,Consumer<JavaDocInformation> onSuccess) {
        String targetFileName = getLastOfSplitted(uri.getLastPathSegment(), "/");
        JavaDocInformation javaDocInfo = new JavaDocInformation("", "", getJavaDocDir(ctx, targetFileName), JavaDocType.ZIP);
        downloadAndUnzipAsync(() -> ctx.getContentResolver().openInputStream(uri), javaDocInfo, ()->onSuccess.accept(javaDocInfo), "");
    }

    private static String getLastOfSplitted(String toSplit, String delimitor) {
        String[] split = toSplit.split(delimitor);
        return split[split.length - 1];
    }

    public File[] getAllSavedJavaDocDirs(Context ctx) {
        return getJavaDocBaseDir(ctx).listFiles();
    }

    private String fileNameFromString(String toConvert) {
        return toConvert.replaceAll("[^A-Za-z0-9]", "_");
    }

    private static void downloadAndUnzipAsync(String url, JavaDocInformation javaDocInfo, Runnable onSuccess, String subDirToUnzip) {
        downloadAndUnzipAsync(() -> {
            URL javaUrl = new URL(url);
            return javaUrl.openStream();
        }, javaDocInfo, onSuccess, subDirToUnzip);
    }

    private static void downloadAndUnzipAsync(Callable<InputStream> inputStreamSupplier, JavaDocInformation javaDocInfo, Runnable onSuccess, String subDirToUnzip) {
        downloader.execute(() -> downloadAndUnzip(inputStreamSupplier, javaDocInfo, onSuccess, subDirToUnzip));
    }

    private void downloadAndUnzip(Callable<InputStream> inputStreamSupplier, JavaDocInformation javaDocInfo, Runnable onSuccess, String subDirToUnzip) {
        try {
            File tempDir = getTempDir();
            unzip(inputStreamSupplier, tempDir, subDirToUnzip);
            if(javaDocInfo.getName().isEmpty()){
                javaDocInfo.setName(JavaDocParser.loadName(tempDir));
            }
            createMetadataFile(javaDocInfo, tempDir);
            Files.move(tempDir.toPath(), javaDocInfo.getDirectory().toPath());
            onSuccess.run();
        } catch (Exception e) {
            Log.e(JavaDocDownloader.class.getName(), "cannot download javadoc", e);
        }
    }

    private void unzip(Callable<InputStream> inputStreamLoader, File dest, String subDirToUnzip) throws Exception {
        //
        //
        //long len = con.getContentLengthLong();
        //Log.i(JavaDocDownloader.class.getName(), "downloading javadoc: " + con.getContentLengthLong() + " bytes");
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(inputStreamLoader.call()))) {
            ZipEntry entry;
            //long byteCount = 0;
            while ((entry = zis.getNextEntry()) != null) {
                //byteCount += entry.getCompressedSize();
                if (entry.getName().startsWith(subDirToUnzip)) {
                    String name = entry.getName().substring(subDirToUnzip.length());
                    File dFile = new File(dest, name);
                    if (entry.isDirectory()) {
                        dFile.mkdirs();
                    } else {
                        dFile.getParentFile().mkdirs();
                        Files.copy(zis, dFile.toPath());
                    }
                }
                //Log.i(JavaDocDownloader.class.getName(), byteCount + "/" + len + " downloaded");//TODO show somehow
            }
        }
    }

    private File getTempDir() throws IOException {
        return Files.createTempDirectory("javadoc").toFile();
    }

    private File getJavaDocDir(Context ctx, String name) {
        return new File(getJavaDocBaseDir(ctx), name);
    }

    private File getJavaDocBaseDir(Context ctx) {
        File dir = new File(ctx.getDataDir(), "docs");
        if (!dir.exists()) {
            dir.mkdir();
        }
        return dir;
    }

    public static List<JavaDocInformation> getAllSavedJavaDocInfos(Context context) {
        return Arrays.stream(getAllSavedJavaDocDirs(context)).map(JavaDocDownloader::readMetadataFileUnchecked).collect(Collectors.toList());
    }

    private void createMetadataFile(JavaDocInformation javaDocInfo, File javaDocDir) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(javaDocDir, METADATA_FILE_NAME)), StandardCharsets.UTF_8))) {
            bw.write(javaDocInfo.getName());
            bw.newLine();
            bw.write(javaDocInfo.getType().name());
            bw.newLine();
            bw.write(javaDocInfo.getSource());
            bw.newLine();
        }
    }

    private JavaDocInformation readMetadataFileUnchecked(File javaDocDir) {
        try {
            return readMetadataFile(javaDocDir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private JavaDocInformation readMetadataFile(File javaDocDir) throws IOException {
        File metadataFile=new File(javaDocDir, METADATA_FILE_NAME);
        if (metadataFile.exists()) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(metadataFile), StandardCharsets.UTF_8))) {
                String name = br.readLine();
                JavaDocType type = JavaDocType.valueOf(br.readLine());
                String source = br.readLine();
                return new JavaDocInformation(name, source, javaDocDir, type);
            }
        } else {
            return new JavaDocInformation(javaDocDir.getName(), "", javaDocDir, JavaDocType.ZIP);
        }
    }
}
