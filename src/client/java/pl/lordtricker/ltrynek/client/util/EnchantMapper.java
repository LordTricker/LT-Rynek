package pl.lordtricker.ltrynek.client.util;

import java.util.HashMap;
import java.util.Map;

public class EnchantMapper {
    private static final Map<String, String> pre120Map = new HashMap<>();
    private static final Map<String, String> post121Map = new HashMap<>();

    static {
        // Pre 1.21 (starsze wersje) – skrócone aliasy
        pre120Map.put("prot", "prot");
        pre120Map.put("protection", "prot");

        pre120Map.put("sharp", "sharp");
        pre120Map.put("sharpness", "sharp");

        pre120Map.put("infi", "infi");
        pre120Map.put("infinity", "infi");

        pre120Map.put("flame", "flame");

        pre120Map.put("fire", "fire");
        pre120Map.put("fire_aspect", "fire");

        pre120Map.put("knock", "knock");
        pre120Map.put("knockback", "knock");

        pre120Map.put("unbr", "unbr");
        pre120Map.put("unbreaking", "unbr");

        pre120Map.put("sweep", "sweep");
        pre120Map.put("sweeping", "sweep");

        pre120Map.put("bane", "bane");
        pre120Map.put("bane_of_arthropods", "bane");

        pre120Map.put("mend", "mend");
        pre120Map.put("mending", "mend");

        pre120Map.put("effi", "effi");
        pre120Map.put("efficiency", "effi");

        pre120Map.put("fort", "fort");
        pre120Map.put("fortune", "fort");

        pre120Map.put("silk", "silk");
        pre120Map.put("silk_touch", "silk");

        pre120Map.put("thorn", "thorn");
        pre120Map.put("thorns", "thorn");

        // Post 1.21 (nowsze wersje) – używamy tych samych skrótów
        post121Map.put("prot", "prot");
        post121Map.put("protection", "prot");

        post121Map.put("sharp", "sharp");
        post121Map.put("sharpness", "sharp");

        post121Map.put("infi", "infi");
        post121Map.put("infinity", "infi");

        post121Map.put("flame", "flame");

        post121Map.put("fire", "fire");
        post121Map.put("fire_aspect", "fire");

        post121Map.put("knock", "knock");
        post121Map.put("knockback", "knock");

        post121Map.put("unbr", "unbr");
        post121Map.put("unbreaking", "unbr");

        post121Map.put("sweep", "sweep");
        post121Map.put("sweeping", "sweep");

        post121Map.put("bane", "bane");
        post121Map.put("bane_of_arthropods", "bane");

        post121Map.put("mend", "mend");
        post121Map.put("mending", "mend");

        post121Map.put("effi", "effi");
        post121Map.put("efficiency", "effi");

        post121Map.put("fort", "fort");
        post121Map.put("fortune", "fort");

        post121Map.put("silk", "silk");
        post121Map.put("silk_touch", "silk");

        post121Map.put("thorn", "thorn");
        post121Map.put("thorns", "thorn");
    }

    /**
     * Mapuje skróconą nazwę enchantu (może zawierać numer poziomu, np. "unbreaking3") do właściwego aliasu.
     * W wyniku np. "unbreaking3" przy mapowaniu dla wersji post121 zostanie przekształcone na "unbr3".
     *
     * @param shortName skrócona nazwa enchantu (z poziomem, np. "sharpness5")
     * @param post121   true dla mapowania w stylu 1.21+, false dla starszych wersji
     * @return skrócony alias enchantu z dołączonym poziomem (np. "sharp5") lub "unknown", jeśli brak mapowania
     */
    public static String mapEnchant(String shortName, boolean post121) {
        String baseName = shortName.toLowerCase();
        String levelPart = "";
        int index = baseName.length() - 1;
        while (index >= 0 && Character.isDigit(baseName.charAt(index))) {
            index--;
        }
        if (index < baseName.length() - 1) {
            levelPart = baseName.substring(index + 1);
            baseName = baseName.substring(0, index + 1);
        }
        String mapped;
        if (post121) {
            mapped = post121Map.getOrDefault(baseName, "unknown");
        } else {
            mapped = pre120Map.getOrDefault(baseName, "unknown");
        }
        if (!mapped.equals("unknown") && !levelPart.isEmpty()) {
            return mapped + levelPart;
        }
        return mapped;
    }
}