package com.example.ltrynek.client.util;

import java.util.HashMap;
import java.util.Map;

public class ColorStripUtils {
    private static final Map<Character, Character> SMALL_FONT_MAP = new HashMap<>() {{
        put('ᴀ', 'a'); put('ʙ', 'b'); put('ᴄ', 'c'); put('ᴅ', 'd');
        put('ᴇ', 'e'); put('ꜰ', 'f'); put('ɢ', 'g'); put('ʜ', 'h');
        put('ɪ', 'i'); put('ᴊ', 'j'); put('ᴋ', 'k'); put('ʟ', 'l');
        put('ᴍ', 'm'); put('ɴ', 'n'); put('ᴏ', 'o'); put('ᴘ', 'p');
        put('ʀ', 'r'); put('ѕ', 's'); put('ᴛ', 't'); put('ᴜ', 'u');
        put('ᴡ', 'w'); put('ʏ', 'y'); put('ᴢ', 'z');
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
