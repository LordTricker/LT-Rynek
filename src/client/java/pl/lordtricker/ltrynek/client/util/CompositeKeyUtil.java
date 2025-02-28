package pl.lordtricker.ltrynek.client.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CompositeKeyUtil {

    // Regex do parsowania formatu: bazowa nazwa (opcjonalne lore w nawiasach) oraz opcjonalny materiał w kwadratowych nawiasach
    private static final Pattern PATTERN = Pattern.compile("^(.*?)\\(([^)]+)\\)(?:\\[([^\\]]+)\\])?$");

    /**
     * Tworzy klucz kompozytowy na podstawie surowego wejścia.
     * Normalizuje wejście usuwając zbędne cudzysłowy z wnętrza nawiasów.
     * Klucz ma postać: "baseName|lore|material" (wszystko małymi literami).
     */
    public static String createCompositeKey(String rawInput) {
        // Normalizacja: usuwamy cudzysłowy z wnętrza nawiasów
        String normalized = rawInput
                .replaceAll("\\(\"", "(")
                .replaceAll("\"\\)", ")")
                .replaceAll("\\[\"", "[")
                .replaceAll("\"\\]", "]");
        Matcher matcher = PATTERN.matcher(normalized);
        String baseName;
        String lore = "";
        String material = "";
        if (matcher.matches()) {
            baseName = matcher.group(1).trim();
            lore = matcher.group(2).trim();
            if (matcher.group(3) != null) {
                material = matcher.group(3).trim();
            }
        } else {
            baseName = normalized.trim();
        }
        if (!material.isEmpty() && !material.startsWith("minecraft:")) {
            material = "minecraft:" + material;
        }
        return (baseName + "|" + lore + "|" + material).toLowerCase();
    }

    /**
     * Generuje przyjazny dla użytkownika tekst na podstawie composite key.
     * Przykładowo: "Anarchiczny miecz(Dodatkowe obrażenia 24)[minecraft:netherite_sword]"
     */
    public static String getFriendlyName(String compositeKey) {
        String[] parts = compositeKey.split("\\|", -1);
        String baseName = parts[0];
        String lore = parts[1];
        String material = parts[2];
        String friendly = baseName;
        if (!lore.isEmpty()) {
            friendly += "(" + lore + ")";
        }
        if (!material.isEmpty()) {
            friendly += "[" + material + "]";
        }
        return friendly;
    }
}
