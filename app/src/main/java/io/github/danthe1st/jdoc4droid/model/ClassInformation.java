package io.github.danthe1st.jdoc4droid.model;

import android.text.SpannableStringBuilder;
import android.text.Spanned;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import io.github.danthe1st.jdoc4droid.model.textholder.TextHolder;
import io.github.danthe1st.jdoc4droid.util.LongStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClassInformation implements Externalizable {
    private TextHolder header;
    private Map<TextHolder,Map<TextHolder,Map<TextHolder,TextHolder>>> sections;
    private TextHolder selectedOuterSection;
    private TextHolder selectedMiddleSection;
    private TextHolder selectedInnerSection;

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(header);
        out.writeObject(sections);
        out.writeObject(selectedOuterSection);
        out.writeObject(selectedMiddleSection);
        out.writeObject(selectedInnerSection);
    }

    @Override
    public void readExternal(ObjectInput in) throws ClassNotFoundException, IOException {
        header= (TextHolder) in.readObject();
        sections= (Map<TextHolder, Map<TextHolder, Map<TextHolder, TextHolder>>>) in.readObject();
        selectedOuterSection= (TextHolder) in.readObject();
        selectedMiddleSection= (TextHolder) in.readObject();
        selectedInnerSection= (TextHolder) in.readObject();

    }
}
