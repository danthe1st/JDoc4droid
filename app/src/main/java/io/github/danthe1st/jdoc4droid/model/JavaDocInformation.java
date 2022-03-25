package io.github.danthe1st.jdoc4droid.model;


import java.io.File;
import java.util.Objects;

public class JavaDocInformation {
    private String name;
    private String onlineDocUrl;
    private File directory;
    private JavaDocType type;
    private String baseDownloadUrl;
    private int order;

    public JavaDocInformation(String name, String onlineDocUrl, File directory, JavaDocType type, int order) {
        this(name, onlineDocUrl, directory, type, "", order);
    }

    //region boilerplate
    public JavaDocInformation(String name, String onlineDocUrl, File directory, JavaDocType type, String baseDownloadUrl, int order) {
        this.name = name;
        this.onlineDocUrl = onlineDocUrl;
        this.directory = directory;
        this.type = type;
        this.baseDownloadUrl = baseDownloadUrl;
        this.order = order;
    }

    public String getName() {
        return this.name;
    }

    public String getOnlineDocUrl() {
        return this.onlineDocUrl;
    }

    public File getDirectory() {
        return this.directory;
    }

    public JavaDocType getType() {
        return this.type;
    }

    public String getBaseDownloadUrl() {
        return this.baseDownloadUrl;
    }

    public int getOrder() {
        return this.order;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setOnlineDocUrl(String onlineDocUrl) {
        this.onlineDocUrl = onlineDocUrl;
    }

    public void setDirectory(File directory) {
        this.directory = directory;
    }

    public void setType(JavaDocType type) {
        this.type = type;
    }

    public void setBaseDownloadUrl(String baseDownloadUrl) {
        this.baseDownloadUrl = baseDownloadUrl;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        JavaDocInformation that = (JavaDocInformation) o;
        return order == that.order && Objects.equals(name, that.name) && Objects.equals(onlineDocUrl, that.onlineDocUrl) && Objects.equals(directory, that.directory) && type == that.type && Objects.equals(baseDownloadUrl, that.baseDownloadUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, onlineDocUrl, directory, type, baseDownloadUrl, order);
    }

    @Override
    public String toString() {
        return "JavaDocInformation{" +
                "name='" + name + '\'' +
                ", onlineDocUrl='" + onlineDocUrl + '\'' +
                ", directory=" + directory +
                ", type=" + type +
                ", baseDownloadUrl='" + baseDownloadUrl + '\'' +
                ", order=" + order +
                '}';
    }
    //endregion
}
