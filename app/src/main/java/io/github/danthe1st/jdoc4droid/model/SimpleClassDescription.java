package io.github.danthe1st.jdoc4droid.model;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SimpleClassDescription implements Serializable {
    private static final long serialVersionUID = 163640046407057922L;

    private String name;
    private String description;
    private String classType;
    private String packageName;
    private String path;
}
