package io.github.danthe1st.jdoc4droid.model;

import java.io.Serializable;
import java.util.Map;

import io.github.danthe1st.jdoc4droid.model.textholder.TextHolder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClassInformation implements Serializable {
    private static final long serialVersionUID = 7618988607928849792L;

    private TextHolder header;
    private Map<TextHolder,Map<TextHolder,Map<TextHolder,TextHolder>>> sections;
    private TextHolder selectedOuterSection;
    private TextHolder selectedMiddleSection;
    private TextHolder selectedInnerSection;
}
