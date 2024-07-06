import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.bitbylogic.apibylogic.util.HashMapUtil;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HashMapUtilTests {

    @Test
    public void testToString() {
        HashMap<String, TestObj> test = new HashMap<>();
        test.put("testKey", new TestObj("test", "testTwo"));
        String testString = HashMapUtil.mapToString(test, ";", "|", new HashMapUtil.ObjectParser<String, TestObj>() {
            @Override
            public String wrapKey(String key) {
                return key;
            }

            @Override
            public String wrapValue(TestObj value) {
                return value.getTest() + ":" + value.getTestTwo();
            }
        });

        assertEquals("testKey|test:testTwo", testString);
    }

    @Test
    public void testFromString() {
        HashMap<String, TestObj> test = new HashMap<>();
        test.put("testKey", new TestObj("test", "testTwo"));
        String testString = HashMapUtil.mapToString(test, ";", "|", new HashMapUtil.ObjectParser<String, TestObj>() {
            @Override
            public String wrapKey(String key) {
                return key;
            }

            @Override
            public String wrapValue(TestObj value) {
                return value.getTest() + ":" + value.getTestTwo();
            }
        });

        HashMapUtil.mapFromString(new HashMapUtil.ObjectWrapper<String, TestObj>() {
            @Override
            public String wrapKey(String key) {
                return key;
            }

            @Override
            public TestObj wrapValue(String value) {
                String[] data = value.split(":");
                return new TestObj(data[0], data[1]);
            }
        }, ";", "|", testString);
    }

    @RequiredArgsConstructor
    @Getter
    class TestObj {
        private final String test;
        private final String testTwo;
    }

}
