package io.github.danthe1st.jdoc4droid.util;

import androidx.annotation.WorkerThread;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.charset.StandardCharsets;

public final class LongStringSerializer {
    static final int MAX_SUPPLY_SIZE = 1024;

    private LongStringSerializer() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    @WorkerThread
    public static void serialize(ObjectOutput out, String toWrite) throws IOException {
        byte[] rawDataBytes = toWrite.getBytes(StandardCharsets.UTF_8);
        out.writeInt(rawDataBytes.length);
        if (rawDataBytes.length != 0) {
            out.flush();
            for (int i = 0; i <= (rawDataBytes.length - 1) / MAX_SUPPLY_SIZE; i++) {
                out.write(rawDataBytes, i * MAX_SUPPLY_SIZE, Math.min(MAX_SUPPLY_SIZE, rawDataBytes.length - i * MAX_SUPPLY_SIZE));
                out.flush();
            }
        }
    }

    @WorkerThread
    public static String deSerialize(ObjectInput in) throws IOException {
        int rawDataLength = in.readInt();
        if (rawDataLength == 0) {
            return "";
        }
        byte[] rawDataBytes = new byte[rawDataLength];
        for (int i = 0; i <= (rawDataBytes.length - 1) / MAX_SUPPLY_SIZE; i++) {
            in.read(rawDataBytes, i * MAX_SUPPLY_SIZE, Math.min(MAX_SUPPLY_SIZE, rawDataBytes.length - i * MAX_SUPPLY_SIZE));
        }
        return new String(rawDataBytes, StandardCharsets.UTF_8);
    }
}
