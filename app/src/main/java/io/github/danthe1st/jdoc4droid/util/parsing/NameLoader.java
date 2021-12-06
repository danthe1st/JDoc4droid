package io.github.danthe1st.jdoc4droid.util.parsing;

import android.text.Html;
import android.util.Log;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

import io.github.danthe1st.jdoc4droid.model.textholder.HtmlStringHolder;
import io.github.danthe1st.jdoc4droid.model.textholder.StringHolder;
import io.github.danthe1st.jdoc4droid.model.textholder.TextHolder;
import lombok.experimental.UtilityClass;

@UtilityClass
class NameLoader {
    static final String SELECTOR_NAME_HEADER = "h1,h2,h3,h4,h5";

    TextHolder findName(Element elem, Set<TextHolder> currentNames) {
        return findName(elem, currentNames, new String[]{SELECTOR_NAME_HEADER});
    }

    TextHolder findName(Element elem, Set<TextHolder> currentNames, String[] selectorNameHeaders) {
        String name;
        Element root = elem.parents().last();
        TextHolder ret=tryFindNameFromFirstHeader(elem, currentNames, selectorNameHeaders);
        if(ret!=null){
            return ret;
        }
        name = elem.attr("id");
        if (!name.isEmpty()) {
            return findFirstNameFromId(name,root);
        }
        name = elem.attr("class");
        TextHolder nameHolder = new StringHolder(name);
        if (name.isEmpty() || currentNames.contains(nameHolder)) {
            //fallback if nothing else works
            generateName();
        } else {
            currentNames.add(nameHolder);
        }
        return nameHolder;
    }
    private TextHolder tryFindNameFromFirstHeader(Element elem,Set<TextHolder> currentNames,String[] selectorNameHeaders){
        Element firstHeader = null;
        for (int i = 0; firstHeader == null && i < selectorNameHeaders.length; i++) {
            String selectorNameHeader = selectorNameHeaders[i];
            firstHeader = elem.selectFirst(selectorNameHeader);
        }
        if (firstHeader != null) {
            return tryFindNameFromHeader(firstHeader,elem,currentNames);
        }
        return null;
    }

    private TextHolder tryFindNameFromHeader(Element header,Element elem,Set<TextHolder> currentNames){
        String name = header.text();
        TextHolder nameHolder = new StringHolder(name);
        if (!name.isEmpty() && !currentNames.contains(nameHolder)) {
            TextHolder ret = nameHolder;
            currentNames.add(nameHolder);
            Elements mainNameElems = elem.getElementsByClass("member-name");
            if (!mainNameElems.isEmpty()) {
                String mainName = mainNameElems.first().text();
                int mainNameIndex;
                if ((mainNameIndex = name.indexOf(mainName)) != -1) {
                    name = name.substring(0, mainNameIndex) + "<b>" + mainName + "</b>" + name.substring(mainNameIndex + mainName.length());
                    ret = new HtmlStringHolder(name, Html.FROM_HTML_MODE_COMPACT, mainName);
                }
            }
            return ret;
        }
        return null;
    }
    private TextHolder findFirstNameFromId(String id,Element root){
        String encoded;
        try {
            encoded = URLEncoder.encode(id, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("encoding UTF-8 not supported",e);
        }
        Elements referencers = root.getElementsByAttributeValue("href", "#" + encoded);
        return new StringHolder(referencers.stream().map(Element::text).findFirst().orElse(id));
    }
    private TextHolder generateName(){
        String name = UUID.randomUUID().toString();
        Log.e(JavaDocParser.class.getName(), "Need to generate UUID");
        return new StringHolder(name);
    }



}
