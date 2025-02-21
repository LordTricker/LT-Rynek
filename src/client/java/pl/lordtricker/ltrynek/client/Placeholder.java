package pl.lordtricker.ltrynek.client;

import java.util.HashMap;
import java.util.Map;

public class Placeholder {
    private final String key;
    private final String value;

    public Placeholder(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public static Map<String, String> toMap(Placeholder... placeholders) {
        Map<String, String> map = new HashMap<>();
        for (Placeholder p : placeholders) {
            map.put(p.key, p.value);
        }
        return map;
    }
}
