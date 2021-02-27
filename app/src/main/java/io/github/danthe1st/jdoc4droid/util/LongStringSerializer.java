package io.github.danthe1st.jdoc4droid.util;

import org.jsoup.Jsoup;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import io.github.danthe1st.jdoc4droid.model.textholder.TextHolder;
import lombok.experimental.UtilityClass;

@UtilityClass
public class LongStringSerializer {
    public void serialize(ObjectOutput out,String toWrite) throws IOException {
        byte[] rawDataBytes=toWrite.getBytes(StandardCharsets.UTF_8);
        out.writeInt(rawDataBytes.length);
        out.write(rawDataBytes);
    }
    public String deSerialize(ObjectInput in) throws ClassNotFoundException, IOException {
        int rawDataLength=in.readInt();
        byte[] rawDataBytes=new byte[rawDataLength];
        in.read(rawDataBytes);
        return new String(rawDataBytes,StandardCharsets.UTF_8);

    }
}
