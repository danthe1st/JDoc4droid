package io.github.danthe1st.jdoc4droid.model.textholder;

import java.util.Objects;

public class StringHolder implements TextHolder {

    private final String text;

    private String mainName;

    public StringHolder(String text) {
        this.text = text;
    }

    public StringHolder(String text, String mainName) {
        this.text = text;
        this.mainName = mainName;
    }

    @Override
    public String getRawText() {
        return text;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TextHolder that = (TextHolder) o;
        return Objects.equals(text, that.getRawText());
    }

    @Override
    public int hashCode() {
        return text.hashCode();
    }

    @Override
    public String toString() {
        return text;
    }

    public String getMainName() {
        return this.mainName;
    }
}
