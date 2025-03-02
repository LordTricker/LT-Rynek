package pl.lordtricker.ltrynek.client.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;

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

    /**
     * Zwraca wszystkie linie komunikatu połączone znakiem nowej linii.
     */
    public static String get(String key) {
        List<String> lines = messages.get(key);
        if (lines == null) {
            return "Missing message for key: " + key;
        }
        return String.join("\n", lines);
    }

    /**
     * Formatowanie placeholderów. Zastępuje %placeholder% wartościami w mapie.
     */
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
