package pl.lordtricker.ltrynek.client.util;

import pl.lordtricker.ltrynek.client.config.PriceEntry;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CompositeKeyUtil {

    private static final Pattern PATTERN = Pattern.compile(
            "^(.*?)\\s*(?:\\(\\s*\\\"([^\\\"]+)\\\"\\s*\\))?\\s*(?:\\[\\s*\\\"([^\\\"]+)\\\"\\s*\\])?\\s*(?:\\{\\s*\\\"([^\\\"]+)\\\"\\s*\\})?\\s*$"
    );

    /**
     * Tworzy composite key na podstawie ciągu wejściowego.
     * Obsługuje formaty:
     * - Prosty materiał: "minecraft:diamond_sword" → "minecraft:diamond_sword|||"
     * - Materiał w nawiasach kwadratowych z cudzysłowami:
     *    ["minecraft:diamond_sword"] → "minecraft:diamond_sword|||"
     * - Pełny format: baseName("lore")[material]{"enchants"}
     */
    public static String createCompositeKey(String rawInput) {
        String normalized = rawInput.trim();
        if (normalized.startsWith("[\"") && normalized.endsWith("\"]")) {
            String material = normalized.substring(2, normalized.length() - 2).trim().toLowerCase();
            return material + "|||";
        }
        if (normalized.toLowerCase().startsWith("minecraft:") && normalized.matches("(?i)^minecraft:[a-z0-9_]+$")) {
            String material = normalized.toLowerCase();
            return material + "|||";
        }
        Matcher matcher = PATTERN.matcher(normalized);
        String baseName;
        String lore = "";
        String material = "";
        String enchants = "";
        if (matcher.matches()) {
            baseName = matcher.group(1).trim();
            if (matcher.group(2) != null) {
                lore = matcher.group(2).trim();
            }
            if (matcher.group(3) != null) {
                material = matcher.group(3).trim();
            }
            if (matcher.group(4) != null) {
                enchants = matcher.group(4).trim();
            }
        } else {
            baseName = normalized;
        }
        if (!material.isEmpty() && !material.toLowerCase().startsWith("minecraft:")) {
            material = "minecraft:" + material;
        }
        return (baseName + "|" + lore + "|" + material + "|" + enchants).toLowerCase();
    }

    /**
     * Generuje przyjazną nazwę w formacie:
     * baseName("lore")[material]{"enchants"}
     * – z cudzysłowami wokół parametrów, jeśli są podane.
     */
    public static String getFriendlyName(String compositeKey) {
        String[] parts = compositeKey.split("\\|", -1);
        String baseName = parts[0];
        String lore = parts[1];
        String material = parts[2];
        String enchants = parts.length > 3 ? parts[3] : "";

        StringBuilder sb = new StringBuilder();
        if (!baseName.isEmpty()) {
            sb.append(baseName);
            if (!lore.isEmpty()) {
                sb.append("(\"").append(lore).append("\")");
            }
            if (!material.isEmpty()) {
                sb.append("[\"").append(material).append("\"]");
            }
            if (!enchants.isEmpty()) {
                sb.append("{\"").append(enchants).append("\"}");
            }
            return sb.toString();
        } else if (!material.isEmpty()) {
            return material;
        }
        return "";
    }

    /**
     * Tworzy composite key z obiektu PriceEntry.
     */
    public static String getCompositeKeyFromEntry(PriceEntry entry) {
        return (entry.name + "|" +
                (entry.lore == null ? "" : entry.lore) + "|" +
                (entry.material == null ? "" : entry.material) + "|" +
                (entry.enchants == null ? "" : entry.enchants)).toLowerCase();
    }

    public static String[] splitCompositeKey(String compositeKey) {
        return compositeKey.split("\\|", -1);
    }
}