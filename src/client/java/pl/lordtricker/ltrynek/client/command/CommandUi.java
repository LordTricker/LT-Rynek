package pl.lordtricker.ltrynek.client.command;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import pl.lordtricker.ltrynek.client.util.ColorUtils;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

public final class CommandUi {
    private CommandUi() {}

    public static MutableText colored(String text) {
        MutableText out = Text.empty();
        out.append(ColorUtils.translateColorCodes(text));
        return out;
    }

    public static MutableText clickable(String text, ClickEvent.Action action, String command, String hoverText) {
        MutableText out = colored(text);
        ClickEvent clickEvent = switch (action) {
            case RUN_COMMAND -> new ClickEvent.RunCommand(command);
            case SUGGEST_COMMAND -> new ClickEvent.SuggestCommand(command);
            case OPEN_URL -> new ClickEvent.OpenUrl(URI.create(command));
            case OPEN_FILE -> new ClickEvent.OpenFile(command);
            case COPY_TO_CLIPBOARD -> new ClickEvent.CopyToClipboard(command);
            case CHANGE_PAGE -> new ClickEvent.ChangePage(Integer.parseInt(command));
            default -> throw new IllegalArgumentException("Unsupported click action: " + action);
        };
        Style style = Style.EMPTY.withClickEvent(clickEvent);
        if (hoverText != null && !hoverText.isEmpty()) {
            style = style.withHoverEvent(new HoverEvent.ShowText(Text.literal(hoverText)));
        }
        out.setStyle(style);
        return out;
    }

    public static CompletableFuture<Suggestions> suggestItemIds(SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase();
        if (remaining.startsWith("mc:")) {
            remaining = "minecraft:" + remaining.substring(3);
        }
        if (remaining.contains("minecraft:")) {
            var allItemIds = net.minecraft.registry.Registries.ITEM.getIds();
            for (var itemId : allItemIds) {
                String asString = itemId.toString();
                if (asString.contains(remaining)) {
                    builder.suggest(asString);
                }
            }
        }
        return builder.buildFuture();
    }
}
