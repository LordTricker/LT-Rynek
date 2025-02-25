package pl.lordtricker.ltrynek.client.util;

import java.util.HashMap;
import java.util.Map;

public class ColorStripUtils {
    private static final Map<Character, Character> SMALL_FONT_MAP = new HashMap<>() {{
        put('ᴀ', 'a'); put('ʙ', 'b'); put('ᴄ', 'c'); put('ᴅ', 'd');
        put('ᴇ', 'e'); put('ꜰ', 'f'); put('ɢ', 'g'); put('ʜ', 'h');
        put('ɪ', 'i'); put('ᴊ', 'j'); put('ᴋ', 'k'); put('ʟ', 'l');
        put('ᴍ', 'm'); put('ɴ', 'n'); put('ᴏ', 'o'); put('ᴘ', 'p');
        put('ʀ', 'r'); put('ѕ', 's'); put('ᴛ', 't'); put('ᴜ', 'u');
        put('ᴡ', 'w'); put('ʏ', 'y'); put('ᴢ', 'z'); put('ꜱ', 's');
        put('ғ', 'f'); put('ᴌ', 'ł'); put('ᴠ', 'v');
        put('ᵃ', 'a'); put('ᵇ', 'b'); put('ᶜ', 'c'); put('ᵈ', 'd');
        put('ᵉ', 'e'); put('ᶠ', 'f'); put('ᵍ', 'g'); put('ʰ', 'h');
        put('ᶦ', 'i'); put('ʲ', 'j'); put('ᵏ', 'k'); put('ˡ', 'l');
        put('ᵐ', 'm'); put('ⁿ', 'n'); put('ᵒ', 'o'); put('ᵖ', 'p');
        put('ʳ', 'r'); put('ˢ', 's'); put('ᵗ', 't'); put('ᵘ', 'u');
        put('ᵛ', 'v'); put('ʷ', 'w'); put('ˣ', 'x'); put('ʸ', 'y');
        put('ᶻ', 'z');
        put('ⓐ', 'a'); put('ⓑ', 'b'); put('ⓒ', 'c'); put('ⓓ', 'd');
        put('ⓔ', 'e'); put('ⓕ', 'f'); put('ⓖ', 'g'); put('ⓗ', 'h');
        put('ⓘ', 'i'); put('ⓙ', 'j'); put('ⓚ', 'k'); put('ⓛ', 'l');
        put('ⓜ', 'm'); put('ⓝ', 'n'); put('ⓞ', 'o'); put('ⓟ', 'p');
        put('ⓠ', 'q'); put('ⓡ', 'r'); put('ⓢ', 's'); put('ⓣ', 't');
        put('ⓤ', 'u'); put('ⓥ', 'v'); put('ⓦ', 'w'); put('ⓧ', 'x');
        put('ⓨ', 'y'); put('ⓩ', 'z');
        put('⁰', '0'); put('¹', '1'); put('²', '2'); put('³', '3');
        put('⁴', '4'); put('⁵', '5'); put('⁶', '6'); put('⁷', '7');
        put('⁸', '8'); put('⁹', '9'); put('₀', '0'); put('₁', '1');
        put('₂', '2'); put('₃', '3'); put('₄', '4'); put('₅', '5');
        put('₆', '6'); put('₇', '7'); put('₈', '8'); put('₉', '9');
        put('０', '0'); put('１', '1'); put('２', '2'); put('３', '3');
        put('４', '4'); put('５', '5'); put('６', '6'); put('７', '7');
        put('８', '8'); put('９', '9');
        put('⓪', '0'); put('①', '1'); put('②', '2'); put('③', '3');
        put('④', '4'); put('⑤', '5'); put('⑥', '6'); put('⑦', '7');
        put('⑧', '8'); put('⑨', '9');
    }};

    public static String stripAllColorsAndFormats(String input) {
        if (input == null || input.isEmpty()) return "";
        // 1. Zamiana małych fontów
        input = mapSmallFont(input);
        // 2. Usuwanie standardowych kodów kolorów (np. §a)
        input = input.replaceAll("§[0-9A-FK-ORa-fk-or]", "");
        // 3. Usuwanie sekwencji hex typu §x§R§R§G§G§B§B
        input = input.replaceAll("§x(§[0-9A-Fa-f]){6}", "");
        // 4. Usuwanie form typu <gradient:...> i </gradient>
        input = input.replaceAll("(?i)<gradient:[^>]*>", "");
        input = input.replaceAll("(?i)</gradient>", "");
        // 5. Usuwanie form typu <#RRGGBB> i <##RRGGBB> oraz &#RRGGBB
        input = input.replaceAll("(?i)<#?[0-9A-F]{6}>", "");
        input = input.replaceAll("(?i)<##[0-9A-F]{6}>", "");
        input = input.replaceAll("(?i)&#[0-9A-F]{6}", "");
        return input.trim();
    }

    private static String mapSmallFont(String input) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (SMALL_FONT_MAP.containsKey(c)) {
                sb.append(SMALL_FONT_MAP.get(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}