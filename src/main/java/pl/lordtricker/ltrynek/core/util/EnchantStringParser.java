package pl.lordtricker.ltrynek.core.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EnchantStringParser {
    private static final Pattern NEWER_PATTERN = Pattern.compile(
            "ResourceKey\\[\\s*minecraft:enchantment\\s*/\\s*minecraft:([^\\]]+)\\]\\s*=Enchantment [^}]+}\\s*=>\\s*(\\d+)"
    );
    private static final Pattern OLDER_PATTERN = Pattern.compile(
            "\\{id:\"([^\"]+)\",lvl:(\\d+)s\\}"
    );

    private EnchantStringParser() {}

    public static String parse(String rawEnchants) {
        if (rawEnchants == null || rawEnchants.isEmpty()) {
            return "";
        }
        Matcher enchantMatcherNew = NEWER_PATTERN.matcher(rawEnchants);
        StringBuilder enchantBuilder = new StringBuilder();
        boolean foundAny = false;

        while (enchantMatcherNew.find()) {
            foundAny = true;
            String enchId = enchantMatcherNew.group(1).trim();
            String levelStr = enchantMatcherNew.group(2).trim();
            String shortEnchant = enchId + levelStr;
            String mappedEnchant = EnchantMapper.mapEnchant(shortEnchant, true);
            if (enchantBuilder.length() > 0) {
                enchantBuilder.append(",");
            }
            enchantBuilder.append(mappedEnchant);
        }

        if (!foundAny) {
            Matcher enchantMatcherOld = OLDER_PATTERN.matcher(rawEnchants);
            while (enchantMatcherOld.find()) {
                String enchId = enchantMatcherOld.group(1).trim();
                String levelStr = enchantMatcherOld.group(2).trim();
                if (enchId.startsWith("minecraft:")) {
                    enchId = enchId.substring("minecraft:".length());
                }
                String shortEnchant = enchId + levelStr;
                String mappedEnchant = EnchantMapper.mapEnchant(shortEnchant, false);
                if (enchantBuilder.length() > 0) {
                    enchantBuilder.append(",");
                }
                enchantBuilder.append(mappedEnchant);
            }
        }

        return enchantBuilder.toString();
    }
}
