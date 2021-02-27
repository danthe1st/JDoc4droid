package io.github.danthe1st.jdoc4droid.model.textholder;

import java.io.Serializable;

public interface TextHolder extends Serializable,Comparable<TextHolder>{
    TextHolder EMPTY=new StringHolder("");
    default CharSequence getText(){
        return getRawText();
    }
    String getRawText();
    default String getMainName(){
        return getText().toString();
    }

    @Override
    default int compareTo(TextHolder o) {
        int ret = 0;

        String aLoaded = getMainName();
        String bLoaded = o.getMainName();
        if (aLoaded != null && bLoaded != null) {
            ret = aLoaded.compareTo(bLoaded);
        }
        if (ret != 0) {
            return ret;
        }
        return toString().compareTo(o.toString());
    }
}
