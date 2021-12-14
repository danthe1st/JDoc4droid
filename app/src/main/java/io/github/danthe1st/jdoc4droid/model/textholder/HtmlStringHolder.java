package io.github.danthe1st.jdoc4droid.model.textholder;

import android.text.Html;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Objects;

import io.github.danthe1st.jdoc4droid.util.LongStringSerializer;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@NoArgsConstructor
@RequiredArgsConstructor
public class HtmlStringHolder implements TextHolder, Externalizable {
    private static final long serialVersionUID = 2033469939306675188L;

    @NonNull
    private String html;
    @NonNull
    @Getter
    private int flags;
    @Getter
    private String mainName;
    @Getter
    private String id="";
    private CharSequence spanned;

    public HtmlStringHolder(String html, int flags,String mainName) {
        this.html=html;
        this.flags=flags;
        this.mainName=mainName;
    }

    public HtmlStringHolder(String html, int flags,String mainName, String id) {
        this.html=html;
        this.flags=flags;
        this.mainName=mainName;
        this.id=id;
    }

    @Override
    public CharSequence getText() {
        if(spanned==null){
            spanned= Html.fromHtml(html,flags);
        }
        return spanned;
    }

    @Override
    public String getRawText() {
        return html;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        LongStringSerializer.serialize(out,html);
        if(mainName==null){
            out.writeUTF("");
        }else{
            out.writeUTF(mainName);
        }
        out.writeInt(flags);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException {
        html=LongStringSerializer.deSerialize(in);
        mainName=in.readUTF();
        if("".equals(mainName)){
            mainName=null;
        }
        flags=in.readInt();
    }

    @Override
    public String toString() {
        return getText().toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HtmlStringHolder that = (HtmlStringHolder) o;
        return Objects.equals(getRawText(), that.getRawText());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getRawText());
    }
}
