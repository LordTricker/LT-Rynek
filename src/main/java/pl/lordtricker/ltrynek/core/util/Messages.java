<<<<<<<< HEAD:src/main/java/pl/lordtricker/ltrynek/core/util/Messages.java
package pl.lordtricker.ltrynek.core.util;
========
package pl.lordtricker.ltrynek.client.util;
>>>>>>>> d541205 (Big update: market analyzer, price formatter, sounds alert):src/client/java/pl/lordtricker/ltrynek/client/util/Messages.java

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Messages {
    private static final Map<String, List<String>> messages = new HashMap<>();

    static {
        try (InputStream in = Messages.class.getResourceAsStream("/assets/ltrynek/messages/messages.json")) {
            if (in == null) {
                System.err.println("messages.json not found in resources!");
            } else {
                Type type = new TypeToken<Map<String, List<String>>>() {}.getType();
                Map<String, List<String>> loaded = new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), type);
                if (loaded != null) {
                    messages.putAll(loaded);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String get(String key) {
        List<String> lines = messages.get(key);
        if (lines == null) {
            return "Missing message for key: " + key;
        }
        return String.join("\n", lines);
    }

    public static String format(String key, Map<String, String> placeholders) {
        List<String> lines = messages.get(key);
        if (lines == null) {
            return "Missing message for key: " + key;
        }
        List<String> replaced = new ArrayList<>();
        for (String line : lines) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                line = line.replace("%" + entry.getKey() + "%", entry.getValue());
            }
            replaced.add(line);
        }
        return String.join("\n", replaced);
    }
}
