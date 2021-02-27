package io.github.danthe1st.jdoc4droid.util;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
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
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import io.github.danthe1st.jdoc4droid.model.JavaDocInformation;
import lombok.experimental.UtilityClass;

@UtilityClass
public class JavaDocDownloader {

    private final ExecutorService downloader = Executors.newSingleThreadExecutor();

    private static final Map<String,String> ORACLE_SUBDIR_SUFFIXES_MAPPING;

    static{
        Map<String,String> suffixSubdirMapping=new HashMap<>();
        suffixSubdirMapping.put("-apidocs.zip","api");
        suffixSubdirMapping.put("_doc-all.zip","docs/api");
        suffixSubdirMapping.put("-docs-all.zip","docs/api");
        ORACLE_SUBDIR_SUFFIXES_MAPPING = Collections.unmodifiableMap(suffixSubdirMapping);
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

    public boolean downloadJavaApiDocs(Context ctx,String url, Consumer<File> onSuccess) {
        int queryStartIndex = url.indexOf('?');
        if (queryStartIndex == -1) {
            queryStartIndex = url.length();
        }
        int fileNameStartIndex = url.lastIndexOf('/', queryStartIndex);
        if (queryStartIndex > fileNameStartIndex) {
            String fileName = url.substring(fileNameStartIndex + 1, queryStartIndex);
            for (Map.Entry<String, String> suffixMappingEntry : ORACLE_SUBDIR_SUFFIXES_MAPPING.entrySet()) {
                String suffix=suffixMappingEntry.getKey();
                String subDirToUnzip=suffixMappingEntry.getValue();
                if (fileName.endsWith(suffix)) {
                    File javaDocDir = getJavaDocDir(ctx, fileName.substring(0, fileName.length() - suffix.length()));
                    if (javaDocDir.exists()) {
                        onSuccess.accept(javaDocDir);
                        return false;
                    } else {
                        downloadAndUnzipAsync(url, javaDocDir, onSuccess, subDirToUnzip);
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static void downloadFromMavenRepo(Context ctx, String repoUrl, String groupId, String artefactId, String version, Consumer<File> onSuccess) {
        String effectiveUrl = repoUrl + "/" + groupId.replace('.', '/') + "/" + artefactId + "/" + version + "/" + artefactId + "-" + version + "-javadoc.jar";
        downloadAndUnzipAsync(effectiveUrl, getJavaDocDir(ctx, fileNameFromString(repoUrl) + "_" + fileNameFromString(artefactId) + "_" + fileNameFromString(version)), onSuccess, "");
    }

    public static void downloadFromUri(Context ctx, Uri uri, Consumer<File> onSuccess) {
        String targetFileName=getLastOfSplitted(uri.getLastPathSegment(),"/");
        downloadAndUnzipAsync(()->ctx.getContentResolver().openInputStream(uri),getJavaDocDir(ctx,targetFileName),onSuccess,"");
    }

    private static String getLastOfSplitted(String toSplit,String delimitor){
        String[] split=toSplit.split(delimitor);
        return split[split.length-1];
    }

    public File[] getAllSavedJavaDocDirs(Context ctx) {
        return getJavaDocBaseDir(ctx).listFiles();
    }

    private String fileNameFromString(String toConvert) {
        return toConvert.replaceAll("[^A-Za-z0-9]", "_");
    }

    private static void downloadAndUnzipAsync(String url, File javaDocDir, Consumer<File> onSuccess, String subDirToUnzip) {
        downloadAndUnzipAsync(()->{
            URL javaUrl = new URL(url);
            return javaUrl.openStream();
        },javaDocDir,onSuccess,subDirToUnzip);
    }
    private static void downloadAndUnzipAsync(Callable<InputStream> inputStreamSupplier, File javaDocDir, Consumer<File> onSuccess, String subDirToUnzip) {
        downloader.execute(() -> {
            downloadAndUnzip(inputStreamSupplier, javaDocDir, onSuccess, subDirToUnzip);
        });
    }

    private void downloadAndUnzip(Callable<InputStream> inputStreamSupplier, File dest, Consumer<File> onSuccess, String subDirToUnzip) {
        try {
            File tempDir = getTempDir();
            unzip(inputStreamSupplier, tempDir, subDirToUnzip);
            Files.move(tempDir.toPath(), dest.toPath());
            onSuccess.accept(dest);
        } catch (Exception e) {
            Log.e(JavaDocDownloader.class.getName(), "cannot download javadoc", e);
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
        return Arrays.stream(getAllSavedJavaDocDirs(context)).map(dir -> new JavaDocInformation(dir.getName(), dir.getName(), dir)).collect(Collectors.toList());
    }


}
