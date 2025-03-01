package pl.lordtricker.ltrynek.client.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CompositeKeyUtil {

    private static final Pattern PATTERN = Pattern.compile(
            "^(.*?)\\s*(?:\\(([^)]*)\\))?\\s*(?:\\[([^\\]]*)\\])?\\s*$"
    );

    /**
     * Tworzy klucz kompozytowy w formacie: "baseName|lore|material" (wszystko małymi literami).
     * Jeśli podany ciąg odpowiada tylko identyfikatorowi przedmiotu (np. "minecraft:netherite_sword"
     * lub "netherite_sword"), to traktuje go jako materiał.
     */
    public static String createCompositeKey(String rawInput) {
        String normalized = rawInput
                .replaceAll("\\(\"", "(")
                .replaceAll("\"\\)", ")")
                .replaceAll("\\[\"", "[")
                .replaceAll("\"\\]", "]")
                .trim();

        if (normalized.matches("(?i)^(minecraft:)?[a-z0-9_]+$")) {
            String material = normalized;
            if (!material.toLowerCase().startsWith("minecraft:")) {
                material = "minecraft:" + material;
            }
            return "|" + "|" + material.toLowerCase();
        }

        Matcher matcher = PATTERN.matcher(normalized);
        String baseName;
        String lore = "";
        String material = "";

        if (matcher.matches()) {
            baseName = matcher.group(1).trim();
            if (matcher.group(2) != null) {
                lore = matcher.group(2).trim();
            }
            if (matcher.group(3) != null) {
                material = matcher.group(3).trim();
            }
        } else {
            baseName = normalized;
        }

        if (!material.isEmpty() && !material.toLowerCase().startsWith("minecraft:")) {
            material = "minecraft:" + material;
        }

        return (baseName + "|" + lore + "|" + material).toLowerCase();
    }

    /**
     * Generuje "przyjazną" nazwę w stylu: baseName(lore)[material].
     * Jeśli baseName jest pusty, zwraca wartość materiału.
     */
    public static String getFriendlyName(String compositeKey) {
        String[] parts = compositeKey.split("\\|", -1);
        String baseName = parts[0];
        String lore = parts[1];
        String material = parts[2];

        if (!baseName.isEmpty()) {
            StringBuilder sb = new StringBuilder(baseName);
            if (!lore.isEmpty()) {
                sb.append("(").append(lore).append(")");
            }
            if (!material.isEmpty()) {
                sb.append("[").append(material).append("]");
            }
            return sb.toString();
        } else if (!material.isEmpty()) {
            return material;
        }
        return "";
    }
}
