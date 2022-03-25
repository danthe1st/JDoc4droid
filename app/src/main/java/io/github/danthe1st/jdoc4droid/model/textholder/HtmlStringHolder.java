package io.github.danthe1st.jdoc4droid.model.textholder;

import android.text.Html;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Objects;

import io.github.danthe1st.jdoc4droid.util.LongStringSerializer;


public class HtmlStringHolder implements TextHolder, Externalizable {
    private static final long serialVersionUID = 2033469939306675188L;

    private String html;

    private int flags;

    private String mainName;

    private String anchor = "";
    private CharSequence spanned;

    public HtmlStringHolder(String html, int flags, String mainName) {
        this.html = html;
        this.flags = flags;
        this.mainName = mainName;
    }

    public HtmlStringHolder(String html, int flags, String mainName, String anchor) {
        this.html = html;
        this.flags = flags;
        this.mainName = mainName;
        this.anchor = anchor;
    }

    public HtmlStringHolder() {
    }


    @Override
    public CharSequence getText() {
        if (spanned == null) {
            spanned = Html.fromHtml(html, flags);
        }
        return spanned;
    }

    @Override
    public String getRawText() {
        return html;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        LongStringSerializer.serialize(out, html);
        if (mainName == null) {
            out.writeUTF("");
        } else {
            out.writeUTF(mainName);
        }
        out.writeInt(flags);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException {
        html = LongStringSerializer.deSerialize(in);
        mainName = in.readUTF();
        if ("".equals(mainName)) {
            mainName = null;
        }
        flags = in.readInt();
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

    public int getFlags() {
        return this.flags;
    }

    public String getMainName() {
        return this.mainName;
    }

    public String getAnchor() {
        return this.anchor;
    }
}
