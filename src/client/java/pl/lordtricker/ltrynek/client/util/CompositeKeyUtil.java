package pl.lordtricker.ltrynek.client.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CompositeKeyUtil {

    // Regex z opcjonalnymi spacjami między grupami:
    // Grupa 1: baseName
    // Grupa 2: lore (opcjonalne, w nawiasach okrągłych)
    // Grupa 3: material (opcjonalne, w nawiasach kwadratowych)
    private static final Pattern PATTERN = Pattern.compile(
            "^(.*?)\\s*(?:\\(([^)]*)\\))?\\s*(?:\\[([^\\]]*)\\])?\\s*$"
    );

    public static String createCompositeKey(String rawInput) {
        // Usuwamy zbędne cudzysłowy wewnątrz nawiasów
        String normalized = rawInput
                .replaceAll("\\(\"", "(")
                .replaceAll("\"\\)", ")")
                .replaceAll("\\[\"", "[")
                .replaceAll("\"\\]", "]")
                .trim();

        // Jeśli użytkownik podał tylko materiał (bez spacji i innych znaków)
        if (normalized.matches("(?i)^(minecraft:)?[a-z0-9_]+$")) {
            String material = normalized;
            if (!material.toLowerCase().startsWith("minecraft:")) {
                material = "minecraft:" + material;
            }
            // Klucz: pusta nazwa i lore, tylko materiał
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
            // Jeśli nie pasuje do wzorca, traktujemy cały input jako nazwę
            baseName = normalized;
        }

        // Dodanie prefiksu "minecraft:" do materiału, jeśli nie został podany
        if (!material.isEmpty() && !material.toLowerCase().startsWith("minecraft:")) {
            material = "minecraft:" + material;
        }

        return (baseName + "|" + lore + "|" + material).toLowerCase();
    }

    public static String getFriendlyName(String compositeKey) {
        String[] parts = compositeKey.split("\\|", -1);
        String baseName = parts[0];
        String lore = parts[1];
        String material = parts[2];
        StringBuilder sb = new StringBuilder(baseName);
        if (!lore.isEmpty()) {
            sb.append("(").append(lore).append(")");
        }
        if (!material.isEmpty()) {
            sb.append("[").append(material).append("]");
        }
        return sb.toString();
    }
}
