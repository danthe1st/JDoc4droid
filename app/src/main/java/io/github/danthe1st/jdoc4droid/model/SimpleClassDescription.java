package io.github.danthe1st.jdoc4droid.model;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SimpleClassDescription implements Serializable {
    private String name;
    private String description;
    private String classType;
    private String packageName;
    private String path;
}
