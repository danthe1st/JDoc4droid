package io.github.danthe1st.jdoc4droid.util;

import android.content.Context;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import io.github.danthe1st.jdoc4droid.activities.list.javadocs.ListJavadocsActivity;
import io.github.danthe1st.jdoc4droid.model.JavaDocInformation;
import io.github.danthe1st.jdoc4droid.model.JavaDocType;
import lombok.experimental.UtilityClass;

@UtilityClass
public class JavaDocDownloader {

    private final ExecutorService downloader = new ThreadPoolExecutor(1, 3,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>());

    private final Map<String, String> ORACLE_SUBDIR_SUFFIXES_MAPPING;
    private final String METADATA_FILE_NAME = ".metadata";
    private final Pattern JDK_JAVADOC_MAJOR_VERSION_PATTERN = Pattern.compile("[^0-9]*([0-9]+)([^0-9].*)?");

    static {
        Map<String, String> suffixSubdirMapping = new HashMap<>();
        suffixSubdirMapping.put("-apidocs.zip", "api");
        suffixSubdirMapping.put("_doc-all.zip", "docs/api");
        suffixSubdirMapping.put("-docs-all.zip", "docs/api");
        ORACLE_SUBDIR_SUFFIXES_MAPPING = Collections.unmodifiableMap(suffixSubdirMapping);
    }

    @AnyThread
    public CompletableFuture<JavaDocInformation> downloadJavaApiDocs(Context ctx, String url, InputStream is, int currentNumberOfJavadocs) {
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
                if (javaDocDir.exists()) {
                    return CompletableFuture.supplyAsync(()->{
                        try {
                            return readMetadataFile(javaDocDir);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    },downloader);
                } else {
                    String remoteUrl;
                    try{
                        remoteUrl = getJavaJavadocUrl(name);
                    }catch (IllegalArgumentException e){
                        remoteUrl="";
                    }
                    JavaDocInformation javaDocInfo = new JavaDocInformation(name, remoteUrl , javaDocDir, JavaDocType.JDK, currentNumberOfJavadocs);
                    return downloadAndUnzipAsync(()->is, javaDocInfo, subDirToUnzip);
                }
            }
        }
        return null;
    }
    @AnyThread
    private String getJavaJavadocUrl(String name){
        int majorVersion=getJavaJavadocMajorVersion(name);
        String type=name.contains("javafx")?"javafx":"docs";
        String prefix=majorVersion>=11?"/en/java":"";
        return "https://docs.oracle.com"+prefix+"/javase/"+majorVersion+"/"+type+"/api/";
        //https://docs.oracle.com/en/java/javase/11/docs/api/
    }
    @AnyThread
    private int getJavaJavadocMajorVersion(String fullVersionName){
        Matcher matcher = JDK_JAVADOC_MAJOR_VERSION_PATTERN.matcher(fullVersionName);
        if(matcher.matches()){
            return Integer.parseInt(matcher.group(1));
        }else{
            throw new IllegalArgumentException("invalid JDK version");
        }
    }

    @AnyThread
    public CompletableFuture<JavaDocInformation> downloadFromMavenRepo(Context ctx, String repoUrl, String groupId, String artefactId, String version, int numberOfJavadocs) {
        if(version.isEmpty()){
            version="RELEASE";
        }
        String artifactBaseUrl=repoUrl + "/" + groupId.replace('.', '/') + "/" + artefactId + "/";
        String effectiveUrl = artifactBaseUrl + version + "/" + artefactId + "-" + version + "-javadoc.jar";
        String src;
        if("https://repo1.maven.org/maven2".equalsIgnoreCase(repoUrl)){
            src="https://javadoc.io/doc/"+groupId+"/"+artefactId+"/"+version+"/";
        }else {
            src="";
        }
        JavaDocInformation javaDocInfo = new JavaDocInformation(
                artefactId + " " + version, src,
                getJavaDocDir(ctx, fileNameFromString(repoUrl) + "_" + fileNameFromString(artefactId) + "_" + fileNameFromString(version)),
                JavaDocType.MAVEN,artifactBaseUrl,numberOfJavadocs);
        Function<String,JavaDocInformation> downloadAction= url->downloadAndUnzip(url, javaDocInfo);
        if("LATEST".equals(version)||"RELEASE".equals(version)){
            return CompletableFuture.supplyAsync(()->getLatestMavenVersionUnchecked(javaDocInfo))
                    .thenApply(downloadAction);
        }
        return CompletableFuture.supplyAsync(()->downloadAction.apply(effectiveUrl),downloader);
    }

    @AnyThread
    public CompletableFuture<JavaDocInformation> updateMavenJavadoc(JavaDocInformation javaDocInfo) {
        return CompletableFuture.supplyAsync(()->{
            try {
                JavaDocInformation newJavaDocInfo=new JavaDocInformation(javaDocInfo.getName(),javaDocInfo.getOnlineDocUrl(),javaDocInfo.getDirectory(),JavaDocType.MAVEN, javaDocInfo.getBaseDownloadUrl(), javaDocInfo.getOrder());
                String url=getLatestMavenVersion(newJavaDocInfo);
                if(javaDocInfo.getName().equals(newJavaDocInfo.getName())){//nothing to update
                    return null;
                }
                JavaDocInformation ret = downloadAndUnzip(url, newJavaDocInfo);
                try {
                    ListJavadocsActivity.deleteRecursive(javaDocInfo.getDirectory());
                } catch (IOException e) {
                    Log.e(JavaDocDownloader.class.getName(), "cannot delete old version after updating javadoc", e);
                }
                return ret;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        },downloader);
    }
    @WorkerThread
    private String getLatestMavenVersionUnchecked(JavaDocInformation javaDocInfo) {
        try {
            return getLatestMavenVersion(javaDocInfo);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    @WorkerThread
    private String getLatestMavenVersion(JavaDocInformation javaDocInfo) throws IOException {
        try(BufferedInputStream is=new BufferedInputStream(new URL(javaDocInfo.getBaseDownloadUrl()+"maven-metadata.xml").openStream())){
            String gId=null;
            String aId=null;
            String version=null;
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(is, null);
            Set<String> allVersions=new HashSet<>();
            int tagType;
            do{
                tagType = parser.next();
                if(tagType==XmlPullParser.START_TAG) {
                    if ("release".equals(parser.getName())) {
                        if(parser.next()==XmlPullParser.TEXT){
                            version = parser.getText();
                            allVersions.add(version);
                        }

                    }else if ("groupId".equals(parser.getName())) {
                        if(parser.next()==XmlPullParser.TEXT){
                            gId = parser.getText();
                        }
                    }else if ("artifactId".equals(parser.getName())) {
                        if(parser.next()==XmlPullParser.TEXT){
                            aId = parser.getText();
                        }
                    }else if("version".equals(parser.getName())){
                        if(parser.next()==XmlPullParser.TEXT){
                            allVersions.add(parser.getText());
                        }
                    }
                }
            }while(tagType!=XmlPullParser.END_DOCUMENT);
            if(gId==null||aId==null||version==null){
                throw new IOException("invalid maven-metadata.xml");
            }
            String newOnlineUrl=javaDocInfo.getOnlineDocUrl();
            String newDirectory=javaDocInfo.getDirectory().getPath();
            String newName=javaDocInfo.getName();
            allVersions.add("LATEST");
            allVersions.add("RELEASE");
            for (String v : allVersions) {
                newOnlineUrl=newOnlineUrl.replace(v,version);
                newName=newName.replace(v,version);
                newDirectory=newDirectory.replace(fileNameFromString(v),fileNameFromString(version));
            }
            javaDocInfo.setOnlineDocUrl(newOnlineUrl);
            File newDirectoryAsFile=new File(newDirectory);
            if(newDirectoryAsFile.equals(javaDocInfo.getDirectory())){
                newDirectoryAsFile=new File(newDirectoryAsFile+"_");
            }
            javaDocInfo.setDirectory(newDirectoryAsFile);
            javaDocInfo.setName(newName);
            return javaDocInfo.getBaseDownloadUrl() + version + "/" + aId + "-" + version + "-javadoc.jar";
        } catch (XmlPullParserException e) {
            throw new IOException(e);
        }
    }

    @UiThread
    public CompletableFuture<JavaDocInformation> downloadFromUri(Context ctx, Uri uri, int numberOfJavadocs) {
        String targetFileName = getLastOfSplitted(uri.getLastPathSegment(), "/");
        JavaDocInformation javaDocInfo = new JavaDocInformation("", "", getJavaDocDir(ctx, targetFileName), JavaDocType.ZIP,numberOfJavadocs);
        return downloadAndUnzipAsync(() -> ctx.getContentResolver().openInputStream(uri), javaDocInfo, "");
    }

    private String getLastOfSplitted(String toSplit, String delimitor) {
        String[] split = toSplit.split(delimitor);
        return split[split.length - 1];
    }

    public File[] getAllSavedJavaDocDirs(Context ctx) {
        return getJavaDocBaseDir(ctx).listFiles();
    }

    private String fileNameFromString(String toConvert) {
        return toConvert.replaceAll("[^A-Za-z0-9]", "_");
    }

    @AnyThread
    private CompletableFuture<JavaDocInformation> downloadAndUnzipAsync(Callable<InputStream> inputStreamSupplier, JavaDocInformation javaDocInfo, String subDirToUnzip) {
        return CompletableFuture.supplyAsync(() -> downloadAndUnzip(inputStreamSupplier, javaDocInfo, subDirToUnzip),downloader);
    }

    @WorkerThread
    private JavaDocInformation downloadAndUnzip(String url, JavaDocInformation javaDocInfo) {
        return downloadAndUnzip(() -> {
            URL javaUrl = new URL(url);
            return javaUrl.openStream();
        }, javaDocInfo, "");
    }
    @WorkerThread
    private JavaDocInformation downloadAndUnzip(Callable<InputStream> inputStreamSupplier, JavaDocInformation javaDocInfo, String subDirToUnzip) {
        try {
            File tempDir = getTempDir();
            unzip(inputStreamSupplier, tempDir, subDirToUnzip);
            if(javaDocInfo.getName().isEmpty()){
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
    public String loadName(File tempDir) throws IOException {
        Document doc = Jsoup.parse(new File(tempDir,"index.html"), StandardCharsets.UTF_8.name());
        return doc.title();
    }

    @WorkerThread
    private void unzip(Callable<InputStream> inputStreamLoader, File dest, String subDirToUnzip) throws IOException {
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
                        Files.createDirectories(dFile.toPath());
                    } else {
                        Files.createDirectories(dFile.getParentFile().toPath());
                        Files.copy(zis, dFile.toPath());
                    }
                }
                //Log.i(JavaDocDownloader.class.getName(), byteCount + "/" + len + " downloaded");//TODO show somehow
            }
        }catch (IOException e){
            throw e;
        }catch (Exception e) {
            throw new IOException(e);
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

    @WorkerThread
    public List<JavaDocInformation> getAllSavedJavaDocInfos(Context context) throws IOException {
        try{
            List<JavaDocInformation> ret = Arrays.stream(getAllSavedJavaDocDirs(context)).map(JavaDocDownloader::readMetadataFileUnchecked).sorted(Comparator.comparingInt(JavaDocInformation::getOrder)).collect(Collectors.toList());
            for (int i = 0; i < ret.size(); i++) {
                if(ret.get(i).getOrder()!=i){
                    ret.get(i).setOrder(i);
                    createMetadataFile(ret.get(i),ret.get(i).getDirectory());
                }
            }
            return ret;
        }catch (UncheckedIOException e){
            IOException cause=e.getCause();
            throw cause==null?new IOException(e):cause;
        }
    }

    @AnyThread
    public CompletableFuture<Void> saveMetadata(JavaDocInformation javaDocInfo){
        return CompletableFuture.supplyAsync(()->{
            try {
                createMetadataFile(javaDocInfo,javaDocInfo.getDirectory());
                return null;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        },downloader);
    }

    @WorkerThread
    private void createMetadataFile(JavaDocInformation javaDocInfo, File javaDocDir) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(javaDocDir, METADATA_FILE_NAME)), StandardCharsets.UTF_8))) {
            bw.write(javaDocInfo.getName());
            bw.newLine();
            bw.write(javaDocInfo.getType().name());
            bw.newLine();
            bw.write(javaDocInfo.getOnlineDocUrl());
            bw.newLine();
            if(!javaDocInfo.getBaseDownloadUrl().isEmpty()){
                bw.write(javaDocInfo.getBaseDownloadUrl());
            }
            bw.newLine();
            bw.write(""+javaDocInfo.getOrder());
            bw.newLine();
        }
    }

    @WorkerThread
    private JavaDocInformation readMetadataFileUnchecked(File javaDocDir) {
        try {
            return readMetadataFile(javaDocDir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @WorkerThread
    private JavaDocInformation readMetadataFile(File javaDocDir) throws IOException {
        File metadataFile=new File(javaDocDir, METADATA_FILE_NAME);
        if (metadataFile.exists()) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(metadataFile), StandardCharsets.UTF_8))) {
                String name = br.readLine();
                JavaDocType type = JavaDocType.valueOf(br.readLine());
                String onlineUrl = br.readLine();
                String downloadSrc=br.readLine();
                String javadocNumber=br.readLine();
                return new JavaDocInformation(name, onlineUrl, javaDocDir, type,downloadSrc==null?"":downloadSrc,(javadocNumber==null||javadocNumber.isEmpty())?-1:Integer.parseInt(javadocNumber));
            }
        } else {
            return new JavaDocInformation(javaDocDir.getName(), "", javaDocDir, JavaDocType.ZIP, -1);
        }
    }


}
