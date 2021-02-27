package io.github.danthe1st.jdoc4droid.model.textholder;

import androidx.annotation.Nullable;

import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@AllArgsConstructor
public class StringHolder implements TextHolder{
    @NonNull
    private final String text;
    @Getter
    private String mainName;

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

    @Override
    public int compareTo(TextHolder o) {
        return 0;
    }
}
