package io.github.danthe1st.jdoc4droid.model;


import java.io.File;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JavaDocInformation {
    private String name;
    private String onlineDocUrl;
    private File directory;
    private JavaDocType type;
    private String baseDownloadUrl;

    public JavaDocInformation(String name,String onlineDocUrl,File directory,JavaDocType type){
        this(name,onlineDocUrl,directory,type,"");
    }
}
