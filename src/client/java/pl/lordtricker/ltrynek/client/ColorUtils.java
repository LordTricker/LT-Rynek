package pl.lordtricker.ltrynek.client;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ColorUtils {
    public static Text translateColorCodes(String input) {
        if (input == null || input.isEmpty()) {
            return Text.of("");
        }
        MutableText result = (MutableText) Text.of("");
        StringBuilder currentSegment = new StringBuilder();
        Style currentStyle = Style.EMPTY;

        char[] chars = input.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c == '&' && i + 1 < chars.length) {
                if (currentSegment.length() > 0) {
                    MutableText segment = (MutableText) Text.of(currentSegment.toString());
                    segment.setStyle(currentStyle);
                    result.append(segment);
                    currentSegment.setLength(0);
                }
                char code = Character.toLowerCase(chars[++i]);
                currentStyle = applyColorCode(code);
            } else {
                currentSegment.append(c);
            }
        }
        if (currentSegment.length() > 0) {
            MutableText segment = (MutableText) Text.of(currentSegment.toString());
            segment.setStyle(currentStyle);
            result.append(segment);
        }
        return result;
    }

    private static Style applyColorCode(char code) {
        if (code == 'r') {
            return Style.EMPTY;
        }
        switch (code) {
            case '0': return Style.EMPTY.withColor(Formatting.BLACK);
            case '1': return Style.EMPTY.withColor(Formatting.DARK_BLUE);
            case '2': return Style.EMPTY.withColor(Formatting.DARK_GREEN);
            case '3': return Style.EMPTY.withColor(Formatting.DARK_AQUA);
            case '4': return Style.EMPTY.withColor(Formatting.DARK_RED);
            case '5': return Style.EMPTY.withColor(Formatting.DARK_PURPLE);
            case '6': return Style.EMPTY.withColor(Formatting.GOLD);
            case '7': return Style.EMPTY.withColor(Formatting.GRAY);
            case '8': return Style.EMPTY.withColor(Formatting.DARK_GRAY);
            case '9': return Style.EMPTY.withColor(Formatting.BLUE);
            case 'a': return Style.EMPTY.withColor(Formatting.GREEN);
            case 'b': return Style.EMPTY.withColor(Formatting.AQUA);
            case 'c': return Style.EMPTY.withColor(Formatting.RED);
            case 'd': return Style.EMPTY.withColor(Formatting.LIGHT_PURPLE);
            case 'e': return Style.EMPTY.withColor(Formatting.YELLOW);
            case 'f': return Style.EMPTY.withColor(Formatting.WHITE);
            default:  return Style.EMPTY;
        }
    }
}
