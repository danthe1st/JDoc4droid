package io.github.danthe1st.jdoc4droid.util;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.net.TrafficStats;
import android.net.Uri;
import android.util.Log;
import android.util.Xml;

import androidx.annotation.AnyThread;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.IntConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import io.github.danthe1st.jdoc4droid.activities.list.javadocs.ListJavadocsActivity;
import io.github.danthe1st.jdoc4droid.model.JavaDocInformation;
import io.github.danthe1st.jdoc4droid.model.JavaDocType;

public final class JavaDocDownloader {

    private static final LinkedBlockingQueue<Runnable> tasks = new LinkedBlockingQueue<>();
    private static final ExecutorService downloader = new ThreadPoolExecutor(1, 3,
            0L, TimeUnit.MILLISECONDS,
            tasks, r ->
            new Thread(() -> {
                TrafficStats.setThreadStatsTag((int) Thread.currentThread().getId());
                r.run();
            }));

    private static final Map<String, String> ORACLE_SUBDIR_SUFFIXES_MAPPING;
    private static final String METADATA_FILE_NAME = ".metadata";
    private static final Pattern JDK_JAVADOC_MAJOR_VERSION_PATTERN = Pattern.compile("[^0-9]*([0-9]+)([^0-9].*)?");

    private InputStreamLoader inputStreamSupplier;
    private final JavaDocInformation javaDocInfo;
    private final String subDirToUnzip;
    private long inputStreamLen = -1;
    private final IntConsumer progressUpdater;


    static {
        Map<String, String> suffixSubdirMapping = new HashMap<>();
        suffixSubdirMapping.put("-apidocs.zip", "api");
        suffixSubdirMapping.put("_doc-all.zip", "docs/api");
        suffixSubdirMapping.put("-docs-all.zip", "docs/api");
        ORACLE_SUBDIR_SUFFIXES_MAPPING = Collections.unmodifiableMap(suffixSubdirMapping);
    }

    private JavaDocDownloader(String url, JavaDocInformation javaDocInfo, IntConsumer progressUpdater) {
        this(getInputStreamSupplierFromURL(url), javaDocInfo, "", progressUpdater);
    }

    @FunctionalInterface
    private interface InputStreamLoader {
        InputStream load(JavaDocDownloader downloader) throws IOException;
    }

    private static InputStreamLoader getInputStreamSupplierFromURL(String url) {
        return downloader -> {
            URLConnection con = new URL(url).openConnection();
            downloader.inputStreamLen = con.getContentLengthLong();
            return con.getInputStream();
        };
    }

    private JavaDocDownloader(InputStream is, long len, JavaDocInformation javaDocInfo, String subDirToUnzip, IntConsumer progressUpdater) {
        this(dl -> is, javaDocInfo, subDirToUnzip, progressUpdater);
        inputStreamLen = len;
    }

    private JavaDocDownloader(InputStreamLoader inputStreamSupplier, JavaDocInformation javaDocInfo, String subDirToUnzip, IntConsumer progressUpdater) {
        this.inputStreamSupplier = inputStreamSupplier;
        this.javaDocInfo = javaDocInfo;
        this.subDirToUnzip = subDirToUnzip;
        this.progressUpdater = progressUpdater;
    }

    @AnyThread
    public static CompletableFuture<JavaDocInformation> downloadOracleJavadoc(Context ctx, String url, InputStream is, long isLen, int currentNumberOfJavadocs, IntConsumer progressUpdater) {
        int queryStartIndex = url.indexOf('?');
        if (queryStartIndex == -1) {
            queryStartIndex = url.length();
        }
        int fileNameStartIndex = url.lastIndexOf('/', queryStartIndex);
        String fileName = url.substring(fileNameStartIndex + 1, queryStartIndex);
        for (Map.Entry<String, String> suffixMappingEntry : ORACLE_SUBDIR_SUFFIXES_MAPPING.entrySet()) {
            String suffix = suffixMappingEntry.getKey();
            String subDirToUnzip = suffixMappingEntry.getValue();
            if (fileName.endsWith(suffix)) {
                String name = fileName.substring(0, fileName.length() - suffix.length());
                File javaDocDir = getJavaDocDir(ctx, name);
                return downloadOracleJavadoc(is, isLen, currentNumberOfJavadocs, subDirToUnzip, name, javaDocDir, progressUpdater);
            }
        }
        return null;
    }

    @AnyThread
    private static CompletableFuture<JavaDocInformation> downloadOracleJavadoc(InputStream is, long isLen, int currentNumberOfJavadocs, String subDirToUnzip, String name, File javaDocDir, IntConsumer progressUpdater) {
        if (javaDocDir.exists()) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return readMetadataFile(javaDocDir);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }, downloader);
        } else {
            String remoteUrl;
            try {
                remoteUrl = getJavaJavadocUrl(name);
            } catch (IllegalArgumentException e) {
                remoteUrl = "";
            }
            JavaDocInformation javaDocInfo = new JavaDocInformation(name, remoteUrl, javaDocDir, JavaDocType.JDK, currentNumberOfJavadocs);
            return new JavaDocDownloader(is, isLen, javaDocInfo, subDirToUnzip, progressUpdater).downloadAndUnzipAsync();
        }
    }

    @AnyThread
    private static String getJavaJavadocUrl(String name) {
        int majorVersion = getJavaJavadocMajorVersion(name);
        String type = name.contains("javafx") ? "javafx" : "docs";
        String prefix = majorVersion >= 11 ? "/en/java" : "";
        return "https://docs.oracle.com" + prefix + "/javase/" + majorVersion + "/" + type + "/api/";
    }

    @AnyThread
    private static int getJavaJavadocMajorVersion(String fullVersionName) {
        Matcher matcher = JDK_JAVADOC_MAJOR_VERSION_PATTERN.matcher(fullVersionName);
        if (matcher.matches() && matcher.groupCount() > 0) {
            return Integer.parseInt(Objects.requireNonNull(matcher.group(1)));
        } else {
            throw new IllegalArgumentException("invalid JDK version");
        }
    }

    @AnyThread
    public static CompletableFuture<JavaDocInformation> downloadFromMavenRepo(Context ctx, String repoUrl, String groupId, String artefactId, String version, int numberOfJavadocs, IntConsumer progressUpdater) {
        if (version == null || version.isEmpty()) {
            return downloadFromMavenRepo(ctx, repoUrl, groupId, artefactId, "RELEASE", numberOfJavadocs, progressUpdater);
        }
        String artifactBaseUrl = repoUrl + "/" + groupId.replace('.', '/') + "/" + artefactId + "/";
        String effectiveUrl = artifactBaseUrl + version + "/" + artefactId + "-" + version + "-javadoc.jar";
        String src;
        if ("https://repo1.maven.org/maven2".equalsIgnoreCase(repoUrl)) {
            src = "https://javadoc.io/doc/" + groupId + "/" + artefactId + "/" + version + "/";
        } else {
            src = "";
        }
        JavaDocInformation javaDocInfo = new JavaDocInformation(
                artefactId + " " + version, src,
                getJavaDocDir(ctx, fileNameFromString(repoUrl) + "_" + fileNameFromString(artefactId) + "_" + fileNameFromString(version)),
                JavaDocType.MAVEN, artifactBaseUrl, numberOfJavadocs);
        JavaDocDownloader javaDocDownloader = new JavaDocDownloader(effectiveUrl, javaDocInfo, progressUpdater);
        return CompletableFuture.supplyAsync(() -> {
            if ("LATEST".equals(version) || "RELEASE".equals(version)) {
                javaDocDownloader.loadLatestMavenVersionUnchecked();
            }
            return javaDocDownloader.downloadAndUnzip();
        }, downloader);
    }

    @AnyThread
    public static CompletableFuture<JavaDocInformation> updateMavenJavadoc(JavaDocInformation javaDocInfo, IntConsumer progressUpdater) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JavaDocInformation newJavaDocInfo = new JavaDocInformation(javaDocInfo.getName(), javaDocInfo.getOnlineDocUrl(), javaDocInfo.getDirectory(), JavaDocType.MAVEN, javaDocInfo.getBaseDownloadUrl(), javaDocInfo.getOrder());
                JavaDocDownloader javaDocDownloader = new JavaDocDownloader("", newJavaDocInfo, progressUpdater);
                javaDocDownloader.loadLatestMavenVersion();
                if (javaDocInfo.getName().equals(newJavaDocInfo.getName())) {//nothing to update
                    return null;
                }
                JavaDocInformation ret = javaDocDownloader.downloadAndUnzip();
                try {
                    ListJavadocsActivity.deleteRecursive(javaDocInfo.getDirectory());
                } catch (IOException e) {
                    Log.e(JavaDocDownloader.class.getName(), "cannot delete old version after updating javadoc", e);
                }
                return ret;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, downloader);
    }

    @WorkerThread
    private void loadLatestMavenVersionUnchecked() {
        try {
            loadLatestMavenVersion();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @WorkerThread
    private void loadLatestMavenVersion() throws IOException {
        try (BufferedInputStream is = new BufferedInputStream(new URL(javaDocInfo.getBaseDownloadUrl() + "maven-metadata.xml").openStream())) {
            String gId = null;
            String aId = null;
            String version = null;
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(is, null);
            Set<String> allVersions = new HashSet<>();
            int tagType;
            do {
                tagType = parser.next();
                if (tagType == XmlPullParser.START_TAG) {
                    if ("release".equals(parser.getName())) {
                        if (parser.next() == XmlPullParser.TEXT) {
                            version = parser.getText();
                            allVersions.add(version);
                        }

                    } else if ("groupId".equals(parser.getName())) {
                        if (parser.next() == XmlPullParser.TEXT) {
                            gId = parser.getText();
                        }
                    } else if ("artifactId".equals(parser.getName())) {
                        if (parser.next() == XmlPullParser.TEXT) {
                            aId = parser.getText();
                        }
                    } else if ("version".equals(parser.getName())) {
                        if (parser.next() == XmlPullParser.TEXT) {
                            allVersions.add(parser.getText());
                        }
                    }
                }
            } while (tagType != XmlPullParser.END_DOCUMENT);
            if (gId == null || aId == null || version == null) {
                throw new IOException("invalid maven-metadata.xml");
            }
            String newOnlineUrl = javaDocInfo.getOnlineDocUrl();
            String newDirectory = javaDocInfo.getDirectory().getPath();
            String newName = javaDocInfo.getName();
            allVersions.add("LATEST");
            allVersions.add("RELEASE");
            for (String v : allVersions) {
                if (newName.endsWith(" " + v)) {
                    newOnlineUrl = newOnlineUrl.replace(v, version);
                    newName = newName.replace(v, version);
                    newDirectory = newDirectory.replace(fileNameFromString(v), fileNameFromString(version));
                }
            }
            javaDocInfo.setOnlineDocUrl(newOnlineUrl);
            File newDirectoryAsFile = new File(newDirectory);
            if (newDirectoryAsFile.equals(javaDocInfo.getDirectory())) {
                newDirectoryAsFile = new File(newDirectoryAsFile + "_");
            }
            javaDocInfo.setDirectory(newDirectoryAsFile);
            javaDocInfo.setName(newName);
            String url = javaDocInfo.getBaseDownloadUrl() + version + "/" + aId + "-" + version + "-javadoc.jar";
            inputStreamSupplier = getInputStreamSupplierFromURL(url);
        } catch (XmlPullParserException e) {
            throw new IOException(e);
        }
    }

    @UiThread
    public static CompletableFuture<JavaDocInformation> downloadFromUri(Context ctx, Uri uri, int numberOfJavadocs, IntConsumer progressUpdater) {
        String targetFileName = getLastOfSplitted(uri.getLastPathSegment());
        JavaDocInformation javaDocInfo = new JavaDocInformation("", "", getJavaDocDir(ctx, targetFileName), JavaDocType.ZIP, numberOfJavadocs);
        return new JavaDocDownloader(downloader -> {
            AssetFileDescriptor fileDescriptor = ctx.getContentResolver().openAssetFileDescriptor(uri, "r");
            downloader.inputStreamLen = fileDescriptor.getLength();
            return ctx.getContentResolver().openInputStream(uri);
        }, javaDocInfo, "", progressUpdater)
                .downloadAndUnzipAsync();
    }

    private static String getLastOfSplitted(String toSplit) {
        String[] split = toSplit.split("/");
        return split[split.length - 1];
    }

    public static File[] getAllSavedJavaDocDirs(Context ctx) {
        return getJavaDocBaseDir(ctx).listFiles();
    }

    private static String fileNameFromString(String toConvert) {
        return toConvert.replaceAll("[^A-Za-z0-9]", "_");
    }

    @AnyThread
    private CompletableFuture<JavaDocInformation> downloadAndUnzipAsync() {
        return CompletableFuture.supplyAsync(this::downloadAndUnzip, downloader);
    }

    @WorkerThread
    private JavaDocInformation downloadAndUnzip() {
        try {
            File tempDir = getTempDir();
            unzip(tempDir);
            if (javaDocInfo.getName().isEmpty()) {
                javaDocInfo.setName(loadName(tempDir));
            }
            createMetadataFile(javaDocInfo, tempDir);
            Files.move(tempDir.toPath(), javaDocInfo.getDirectory().toPath());
            return javaDocInfo;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @WorkerThread
    public static String loadName(File tempDir) throws IOException {
        Document doc = Jsoup.parse(new File(tempDir, "index.html"), StandardCharsets.UTF_8.name());
        return doc.title();
    }

    @WorkerThread
    private void unzip(File dest) throws IOException {
        //long len = con.getContentLengthLong();
        //Log.i(JavaDocDownloader.class.getName(), "downloading javadoc: " + con.getContentLengthLong() + " bytes");
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(inputStreamSupplier.load(this)))) {
            ZipEntry entry;
            long byteCount = 0;
            while ((entry = zis.getNextEntry()) != null) {
                byteCount += entry.getCompressedSize();
                if (entry.getName().startsWith(subDirToUnzip)) {
                    String name = entry.getName().substring(subDirToUnzip.length());
                    File dFile = new File(dest, name);
                    if (entry.isDirectory()) {
                        Files.createDirectories(dFile.toPath());
                    } else {
                        Files.createDirectories(dFile.getParentFile().toPath());
                        Files.copy(zis, dFile.toPath());
                    }
                }
                progressUpdater.accept((int) (100 * byteCount / inputStreamLen));
                //Log.i(JavaDocDownloader.class.getName(), byteCount + "/" + len + " downloaded");//TODO show somehow
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private static File getTempDir() throws IOException {
        return Files.createTempDirectory("javadoc").toFile();
    }

    private static File getJavaDocDir(Context ctx, String name) {
        return new File(getJavaDocBaseDir(ctx), name);
    }

    private static File getJavaDocBaseDir(Context ctx) {
        File dir = new File(ctx.getDataDir(), "docs");
        if (!dir.exists()) {
            dir.mkdir();
        }
        return dir;
    }

    @WorkerThread
    public static List<JavaDocInformation> getAllSavedJavaDocInfos(Context context) throws IOException {
        try {
            List<JavaDocInformation> ret = Arrays.stream(getAllSavedJavaDocDirs(context)).map(JavaDocDownloader::readMetadataFileUnchecked).sorted(Comparator.comparingInt(JavaDocInformation::getOrder)).collect(Collectors.toList());
            for (int i = 0; i < ret.size(); i++) {
                if (ret.get(i).getOrder() != i) {
                    ret.get(i).setOrder(i);
                    createMetadataFile(ret.get(i), ret.get(i).getDirectory());
                }
            }
            return ret;
        } catch (UncheckedIOException e) {
            IOException cause = e.getCause();
            throw cause == null ? new IOException(e) : cause;
        }
    }

    @AnyThread
    public static CompletableFuture<Void> saveMetadata(JavaDocInformation javaDocInfo) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                createMetadataFile(javaDocInfo, javaDocInfo.getDirectory());
                return null;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, downloader);
    }

    @WorkerThread
    private static void createMetadataFile(JavaDocInformation javaDocInfo, File javaDocDir) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(javaDocDir, METADATA_FILE_NAME)), StandardCharsets.UTF_8))) {
            bw.write(javaDocInfo.getName());
            bw.newLine();
            bw.write(javaDocInfo.getType().name());
            bw.newLine();
            bw.write(javaDocInfo.getOnlineDocUrl());
            bw.newLine();
            if (!javaDocInfo.getBaseDownloadUrl().isEmpty()) {
                bw.write(javaDocInfo.getBaseDownloadUrl());
            }
            bw.newLine();
            bw.write("" + javaDocInfo.getOrder());
            bw.newLine();
        }
    }

    @WorkerThread
    private static JavaDocInformation readMetadataFileUnchecked(File javaDocDir) {
        try {
            return readMetadataFile(javaDocDir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @WorkerThread
    private static JavaDocInformation readMetadataFile(File javaDocDir) throws IOException {
        File metadataFile = new File(javaDocDir, METADATA_FILE_NAME);
        if (metadataFile.exists()) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(metadataFile), StandardCharsets.UTF_8))) {
                String name = br.readLine();
                JavaDocType type = JavaDocType.valueOf(br.readLine());
                String onlineUrl = br.readLine();
                String downloadSrc = br.readLine();
                String javadocNumber = br.readLine();
                return new JavaDocInformation(name, onlineUrl, javaDocDir, type, downloadSrc == null ? "" : downloadSrc, (javadocNumber == null || javadocNumber.isEmpty()) ? -1 : Integer.parseInt(javadocNumber));
            } catch (UncheckedIOException e) {
                IOException cause = e.getCause();
                throw cause == null ? new IOException(e) : cause;
            }
        } else {
            return new JavaDocInformation(javaDocDir.getName(), "", javaDocDir, JavaDocType.ZIP, -1);
        }
    }

    @WorkerThread
    public static void clearCacheIfNoDownloadInProgress(Context ctx) {
        File[] dirs = ctx.getCacheDir().listFiles();
        if (dirs == null || !tasks.isEmpty()) {
            return;
        }
        Arrays.stream(dirs)
                .filter(dir -> dir.getName().startsWith("javadoc"))
                .map(File::toPath)
                .forEach(ListJavadocsActivity::deleteRecursive);
    }
}
