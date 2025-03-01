package pl.lordtricker.ltrynek.client.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CompositeKeyUtil {

    /**
     * Regex umożliwia dopasowanie:
     *   - samej nazwy,
     *   - nazwy + (lore),
     *   - nazwy + [material],
     *   - nazwy + (lore) + [material].
     *
     * Grupy:
     *   1: baseName  (zawsze wymagane)
     *   2: lore      (opcjonalne, w nawiasach okrągłych)
     *   3: material  (opcjonalne, w nawiasach kwadratowych)
     */
    private static final Pattern PATTERN = Pattern.compile(
            "^(.*?)" +                   // 1) baseName (dowolne znaki, chciwe, ale minimalnie dopasujemy przez kolejne grupy)
                    "(?:\\(([^)]*)\\))?" +       // 2) (optional) (lore)
                    "(?:\\[([^\\]]*)\\])?" +     // 3) (optional) [material]
                    "$"
    );

    /**
     * Tworzy klucz kompozytowy w formacie: "baseName|lore|material" (małymi literami).
     * Przed parsowaniem usuwa ewentualne zbędne cudzysłowy wewnątrz nawiasów.
     */
    public static String createCompositeKey(String rawInput) {
        // Usuwamy nadmiarowe cudzysłowy z wnętrza nawiasów (jeśli gracz wpisze np. "coś w lore" z cudzysłowami)
        String normalized = rawInput
                .replaceAll("\\(\"", "(")
                .replaceAll("\"\\)", ")")
                .replaceAll("\\[\"", "[")
                .replaceAll("\"\\]", "]")
                .trim();

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
            // Jeśli nie pasuje do wzorca (np. ktoś użyje innych znaków),
            // to traktujemy cały input jako baseName.
            baseName = normalized;
        }

        // Dodanie prefiksu "minecraft:" jeśli user podał np. netherite_sword bez prefixu
        if (!material.isEmpty() && !material.startsWith("minecraft:")) {
            material = "minecraft:" + material;
        }

        return (baseName + "|" + lore + "|" + material).toLowerCase();
    }

    /**
     * Generuje "przyjazną" nazwę w stylu: baseName(lore)[material].
     */
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