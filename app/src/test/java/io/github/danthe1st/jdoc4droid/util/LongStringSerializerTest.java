package io.github.danthe1st.jdoc4droid.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class LongStringSerializerTest {
    @Test(timeout = 5000)//max ObjectOutputStream buffer size=1024
    public void testWithMaxOOSBufferSize() throws IOException {
        testWithRandomString(LongStringSerializer.MAX_SUPPLY_SIZE);
    }
    @Test(timeout = 5000)
    public void testWithHalfOOSBufferSize() throws IOException {
        testWithRandomString(LongStringSerializer.MAX_SUPPLY_SIZE/2);
    }
    @Test(timeout = 5000)
    public void testWithSmallerThanOOSBufferSize() throws IOException {
        testWithRandomString(LongStringSerializer.MAX_SUPPLY_SIZE-1);
    }
    @Test(timeout = 5000)
    public void testWithHigherThanOOSBufferSize() throws IOException {
        testWithRandomString(LongStringSerializer.MAX_SUPPLY_SIZE+1);
    }
    @Test(timeout = 5000)
    public void testWithMultipleOfMaxOOSBufferSize() throws IOException {
        testWithRandomString(LongStringSerializer.MAX_SUPPLY_SIZE*8);
    }
    @Test(timeout = 5000)
    public void testWithNonMultipleOfMaxOOSBufferSize() throws IOException {
        testWithRandomString(LongStringSerializer.MAX_SUPPLY_SIZE*8+LongStringSerializer.MAX_SUPPLY_SIZE/2);
    }

    private void testWithRandomString(int len) throws IOException {
        String str=generateString(len);
        try(PipedOutputStream pos=new PipedOutputStream();
            PipedInputStream pis=new PipedInputStream(pos,len*2+1);
            ObjectOutputStream oos=new ObjectOutputStream(pos);
            ObjectInput ois=new ObjectInputStream(pis)){
            LongStringSerializer.serialize(oos,str);
            oos.flush();
            String read=LongStringSerializer.deSerialize(ois);
            assertEquals(str,read);
        }
    }

    private String generateString(int len){
        Random rand= ThreadLocalRandom.current();
        StringBuilder sb=new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append((char)(rand.nextInt(32+26)+'A'));
        }
        return sb.toString();
    }
}
