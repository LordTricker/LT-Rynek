package pl.lordtricker.ltrynek.client.command;

import pl.lordtricker.ltrynek.client.search.ClientSearchListManager;
import pl.lordtricker.ltrynek.client.util.ColorUtils;
import pl.lordtricker.ltrynek.client.LtrynekClient;
import pl.lordtricker.ltrynek.client.util.Messages;
import pl.lordtricker.ltrynek.client.config.ConfigLoader;
import pl.lordtricker.ltrynek.client.config.PriceEntry;
import pl.lordtricker.ltrynek.client.config.ServerEntry;
import pl.lordtricker.ltrynek.client.price.ClientPriceListManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import pl.lordtricker.ltrynek.client.util.PriceFormatter;

import java.util.Map;

public class ClientCommandRegistration {

    public static void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                registerLtrynekCommand(dispatcher, registryAccess));
    }

    private static void registerLtrynekCommand(
            CommandDispatcher<FabricClientCommandSource> dispatcher,
            CommandRegistryAccess registryAccess
    ) {
        dispatcher.register(
                ClientCommandManager.literal("ltr")
                        // /ltr – wyświetlenie informacji o aktualnym profilu
                        .executes(ctx -> {
                            String activeProfile = ClientPriceListManager.getActiveProfile();
                            String message = Messages.format("mod.info", Map.of("profile", activeProfile));
                            ctx.getSource().sendFeedback(ColorUtils.translateColorCodes(message));
                            return 1;
                        })

                        // /ltr defaultprofile <profile>
                        .then(ClientCommandManager.literal("defaultprofile")
                                .then(ClientCommandManager.argument("profile", StringArgumentType.word())
                                        .executes(ctx -> {
                                            String profile = StringArgumentType.getString(ctx, "profile");
                                            ClientPriceListManager.setActiveProfile(profile);
                                            String msg = Messages.format("command.defaultprofile.success", Map.of("profile", profile));
                                            ctx.getSource().sendFeedback(ColorUtils.translateColorCodes(msg));
                                            return 1;
                                        })
                                )
                        )

                        // /ltr profiles – listowanie dostępnych profili
                        .then(ClientCommandManager.literal("profiles")
                                .executes(ctx -> {
                                    String allProfiles = ClientPriceListManager.listProfiles();
                                    String[] profiles = allProfiles.split(",\\s*");
                                    String headerStr = Messages.get("command.profiles.header");
                                    MutableText finalText = (MutableText) ColorUtils.translateColorCodes(headerStr);
                                    finalText.append(Text.literal("\n"));
                                    for (String profile : profiles) {
                                        String lineTemplate = Messages.format("profile.available.line", Map.of("profile", profile.trim()));
                                        MutableText lineText = (MutableText) ColorUtils.translateColorCodes(lineTemplate);
                                        Style clickableStyle = Style.EMPTY
                                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ltr profile " + profile))
                                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                                        Text.literal("Kliknij aby zmienić profil " + profile)));
                                        lineText.setStyle(clickableStyle);
                                        finalText.append(lineText).append(Text.literal("\n"));
                                    }
                                    ctx.getSource().sendFeedback(finalText);
                                    return 1;
                                })
                        )

                        // /ltr profile <profile>
                        .then(ClientCommandManager.literal("profile")
                                .then(ClientCommandManager.argument("profile", StringArgumentType.word())
                                        .executes(ctx -> {
                                            String profile = StringArgumentType.getString(ctx, "profile");
                                            ClientPriceListManager.setActiveProfile(profile);
                                            String msg = Messages.format("command.profile.change", Map.of("profile", profile));
                                            ctx.getSource().sendFeedback(ColorUtils.translateColorCodes(msg));
                                            return 1;
                                        })
                                )
                        )

                        // /ltr add <maxPrice> <itemName>
                        .then(ClientCommandManager.literal("add")
                                .then(ClientCommandManager.argument("maxPrice", StringArgumentType.word())
                                        .then(ClientCommandManager.argument("itemName", StringArgumentType.greedyString())
                                                .executes(ctx -> {
                                                    String maxPriceStr = StringArgumentType.getString(ctx, "maxPrice");
                                                    // Parsujemy wpisaną cenę (np. "10k" lub "100000")
                                                    double parsedPrice = PriceFormatter.parsePrice(maxPriceStr);
                                                    if (parsedPrice < 0) {
                                                        ctx.getSource().sendError(Text.literal("Invalid price format: " + maxPriceStr));
                                                        return 0;
                                                    }
                                                    String itemName = StringArgumentType.getString(ctx, "itemName");
                                                    String activeProfile = ClientPriceListManager.getActiveProfile();
                                                    ClientPriceListManager.addPriceEntry(itemName, parsedPrice);
                                                    // Wyświetlamy cenę w skróconej formie, np. "1.50k"
                                                    String shortPrice = PriceFormatter.formatPrice(parsedPrice);
                                                    String msg = Messages.format("command.add.success", Map.of(
                                                            "item", itemName,
                                                            "price", shortPrice,
                                                            "profile", activeProfile
                                                    ));
                                                    ctx.getSource().sendFeedback(ColorUtils.translateColorCodes(msg));
                                                    return 1;
                                                })
                                        )
                                )
                        )

                        // /ltr remove <itemName>
                        .then(ClientCommandManager.literal("remove")
                                .then(ClientCommandManager.argument("itemName", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            String itemName = StringArgumentType.getString(ctx, "itemName");
                                            String activeProfile = ClientPriceListManager.getActiveProfile();
                                            ClientPriceListManager.removePriceEntry(itemName);
                                            String msg = Messages.format("command.remove.success", Map.of(
                                                    "item", itemName,
                                                    "profile", activeProfile
                                            ));
                                            ctx.getSource().sendFeedback(ColorUtils.translateColorCodes(msg));
                                            return 1;
                                        })
                                )
                        )

                        // /ltr list
                        .then(ClientCommandManager.literal("list")
                                .executes(ctx -> {
                                    String activeProfile = ClientPriceListManager.getActiveProfile();
                                    String listRaw = ClientPriceListManager.getPriceListAsString();
                                    String[] lines = listRaw.split("\n");
                                    MutableText finalText = Text.empty();
                                    for (String line : lines) {
                                        String[] parts = line.split("\\s+", 2);
                                        if (parts.length < 2) continue;
                                        String priceStr = parts[0];
                                        String itemName = parts[1];
                                        double parsed = Double.parseDouble(priceStr);
                                        String shortPrice = PriceFormatter.formatPrice(parsed);
                                        // Ikona edycji
                                        String editIconStr = Messages.get("pricelist.icon.edit");
                                        MutableText editIcon = (MutableText) ColorUtils.translateColorCodes(editIconStr);
                                        editIcon.setStyle(
                                                Style.EMPTY.withClickEvent(new ClickEvent(
                                                                ClickEvent.Action.SUGGEST_COMMAND,
                                                                "/ltr add " + priceStr + " " + itemName))
                                                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                                                Text.literal("Kliknij aby zedytować " + itemName))));
                                        // Ikona usuwania
                                        String removeIconStr = Messages.get("pricelist.icon.remove");
                                        MutableText removeIcon = (MutableText) ColorUtils.translateColorCodes(removeIconStr);
                                        removeIcon.setStyle(
                                                Style.EMPTY.withClickEvent(new ClickEvent(
                                                                ClickEvent.Action.RUN_COMMAND,
                                                                "/ltr remove " + itemName))
                                                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                                                Text.literal("Kliknij aby usunąć " + itemName))));
                                        String itemLineStr = Messages.format("pricelist.item_line", Map.of("item", itemName, "price", shortPrice));
                                        MutableText itemLine = (MutableText) ColorUtils.translateColorCodes(itemLineStr);
                                        MutableText lineText = Text.empty()
                                                .append(editIcon).append(Text.literal(" "))
                                                .append(removeIcon).append(Text.literal(" "))
                                                .append(itemLine).append(Text.literal("\n"));
                                        finalText.append(lineText);
                                    }
                                    String msgHeader = Messages.format("command.list", Map.of("profile", activeProfile, "list", ""));
                                    MutableText header = (MutableText) ColorUtils.translateColorCodes(msgHeader);
                                    ctx.getSource().sendFeedback(header);
                                    ctx.getSource().sendFeedback(finalText);
                                    return 1;
                                })
                        )

                        // /ltr pomoc
                        .then(ClientCommandManager.literal("pomoc")
                                .executes(ctx -> {
                                    String msg = Messages.get("command.help");
                                    ctx.getSource().sendFeedback(ColorUtils.translateColorCodes(msg));
                                    return 1;
                                })
                        )

                        // /ltr config (save, reload)
                        .then(ClientCommandManager.literal("config")
                                .then(ClientCommandManager.literal("save")
                                        .executes(ctx -> {
                                            syncMemoryToConfig();
                                            ConfigLoader.saveConfig(LtrynekClient.serversConfig);
                                            String msg = Messages.get("command.config.save.success");
                                            ctx.getSource().sendFeedback(ColorUtils.translateColorCodes(msg));
                                            return 1;
                                        })
                                )
                                .then(ClientCommandManager.literal("reload")
                                        .executes(ctx -> {
                                            LtrynekClient.serversConfig = ConfigLoader.loadConfig();
                                            ClientPriceListManager.clearAllProfiles();
                                            reinitProfilesFromConfig();
                                            String msg = Messages.get("command.config.reload.success");
                                            ctx.getSource().sendFeedback(ColorUtils.translateColorCodes(msg));
                                            return 1;
                                        })
                                )
                        )

                        // /ltr sounds (on, off)
                        .then(ClientCommandManager.literal("sounds")
                                .executes(ctx -> {
                                    boolean current = LtrynekClient.serversConfig.soundsEnabled;
                                    String msg = current
                                            ? Messages.get("command.sounds.current_on")
                                            : Messages.get("command.sounds.current_off");
                                    ctx.getSource().sendFeedback(ColorUtils.translateColorCodes(msg));
                                    return 1;
                                })
                                .then(ClientCommandManager.literal("on")
                                        .executes(ctx -> {
                                            LtrynekClient.serversConfig.soundsEnabled = true;
                                            String msg = Messages.get("command.sounds.enabled");
                                            ctx.getSource().sendFeedback(ColorUtils.translateColorCodes(msg));
                                            return 1;
                                        })
                                )
                                .then(ClientCommandManager.literal("off")
                                        .executes(ctx -> {
                                            LtrynekClient.serversConfig.soundsEnabled = false;
                                            String msg = Messages.get("command.sounds.disabled");
                                            ctx.getSource().sendFeedback(ColorUtils.translateColorCodes(msg));
                                            return 1;
                                        })
                                )
                        )

                        // /ltr searchlist (add, remove, start, stop, list)
                        .then(ClientCommandManager.literal("searchlist")
                                .then(ClientCommandManager.literal("add")
                                        .then(ClientCommandManager.argument("item", StringArgumentType.greedyString())
                                                .executes(ctx -> {
                                                    String item = StringArgumentType.getString(ctx, "item");
                                                    ClientSearchListManager.addItem(item);
                                                    String msg = Messages.format("command.searchlist.add", Map.of("item", item));
                                                    ctx.getSource().sendFeedback(ColorUtils.translateColorCodes(msg));
                                                    return 1;
                                                })
                                        )
                                )
                                .then(ClientCommandManager.literal("remove")
                                        .then(ClientCommandManager.argument("item", StringArgumentType.greedyString())
                                                .executes(ctx -> {
                                                    String item = StringArgumentType.getString(ctx, "item");
                                                    ClientSearchListManager.removeItem(item);
                                                    String msg = Messages.format("command.searchlist.remove", Map.of("item", item));
                                                    ctx.getSource().sendFeedback(ColorUtils.translateColorCodes(msg));
                                                    return 1;
                                                })
                                        )
                                )
                                .then(ClientCommandManager.literal("start")
                                        .executes(ctx -> {
                                            ClientSearchListManager.startSearch();
                                            String msg = Messages.get("command.searchlist.start");
                                            ctx.getSource().sendFeedback(ColorUtils.translateColorCodes(msg));
                                            return 1;
                                        })
                                )
                                .then(ClientCommandManager.literal("stop")
                                        .executes(ctx -> {
                                            ClientSearchListManager.stopSearch();
                                            java.util.List<String> searchItems = ClientSearchListManager.getSearchList();
                                            if (searchItems.isEmpty()) {
                                                String emptyMsg = Messages.get("command.searchlist.list.empty");
                                                ctx.getSource().sendFeedback(ColorUtils.translateColorCodes(emptyMsg));
                                                return 1;
                                            }
                                            MutableText finalText = Text.empty();
                                            String headerRaw = Messages.get("command.searchlist.stop.header");
                                            finalText.append(ColorUtils.translateColorCodes(headerRaw)).append(Text.literal("\n"));
                                            for (String item : searchItems) {
                                                ClientSearchListManager.Stats stats = ClientSearchListManager.getStats(item);
                                                if (stats == null || stats.getCount() == 0) continue;
                                                String lineRaw = Messages.format("command.searchlist.stop.line", Map.of(
                                                        "item", item,
                                                        "count", String.valueOf(stats.getCount()),
                                                        "min", PriceFormatter.formatPrice(stats.getMin()),
                                                        "max", PriceFormatter.formatPrice(stats.getMax()),
                                                        "avg", PriceFormatter.formatPrice(stats.getAverage())
                                                ));
                                                finalText.append(ColorUtils.translateColorCodes(lineRaw)).append(Text.literal("\n"));
                                            }
                                            ctx.getSource().sendFeedback(finalText);
                                            return 1;
                                        })
                                )
                                .then(ClientCommandManager.literal("list")
                                        .executes(ctx -> {
                                            java.util.List<String> searchItems = ClientSearchListManager.getSearchList();
                                            if (searchItems.isEmpty()) {
                                                String emptyMsg = Messages.get("command.searchlist.list.empty");
                                                ctx.getSource().sendFeedback(ColorUtils.translateColorCodes(emptyMsg));
                                                return 1;
                                            }
                                            String header = Messages.get("command.searchlist.list.header");
                                            MutableText finalText = (MutableText) ColorUtils.translateColorCodes(header);
                                            finalText.append(Text.literal("\n"));
                                            for (String item : searchItems) {
                                                String lineTemplate = Messages.format("command.searchlist.list.line", Map.of("item", item));
                                                MutableText lineText = (MutableText) ColorUtils.translateColorCodes(lineTemplate);
                                                Style clickableStyle = Style.EMPTY.withClickEvent(new ClickEvent(
                                                                ClickEvent.Action.RUN_COMMAND,
                                                                "/ltr searchlist remove " + item))
                                                        .withHoverEvent(new HoverEvent(
                                                                HoverEvent.Action.SHOW_TEXT,
                                                                Text.literal(Messages.get("command.searchlist.list.remove.hover"))
                                                        ));
                                                lineText.setStyle(clickableStyle);
                                                finalText.append(lineText).append(Text.literal("\n"));
                                            }
                                            ctx.getSource().sendFeedback(finalText);
                                            return 1;
                                        })
                                )
                        )
        );
    }

    /**
     * Synchronizuje dane z pamięci (ClientPriceListManager) do obiektu LtrynekClient.serversConfig,
     * tak aby /ltrynek config save mógł zapisać te zmiany do pliku.
     */
    private static void syncMemoryToConfig() {
        for (ServerEntry entry : LtrynekClient.serversConfig.servers) {
            entry.prices.clear();
        }
        Map<String, Map<String, Double>> allProfiles = ClientPriceListManager.getAllProfiles();
        for (Map.Entry<String, Map<String, Double>> profEntry : allProfiles.entrySet()) {
            String profileName = profEntry.getKey();
            Map<String, Double> items = profEntry.getValue();

            ServerEntry se = findServerEntryByProfile(profileName);
            if (se == null) {
                continue;
            }

            for (Map.Entry<String, Double> itemEntry : items.entrySet()) {
                String itemName = itemEntry.getKey();
                double maxPrice = itemEntry.getValue();

                PriceEntry pe = new PriceEntry();
                pe.name = itemName;
                pe.maxPrice = maxPrice;
                se.prices.add(pe);
            }
        }
    }

    /**
     * Czyści i na nowo inicjalizuje ClientPriceListManager z LtrynekClient.serversConfig.
     */
    private static void reinitProfilesFromConfig() {
        for (ServerEntry entry : LtrynekClient.serversConfig.servers) {
            ClientPriceListManager.setActiveProfile(entry.profileName);
            for (PriceEntry pe : entry.prices) {
                ClientPriceListManager.addPriceEntry(pe.name, pe.maxPrice);
            }
        }
        ClientPriceListManager.setActiveProfile(LtrynekClient.serversConfig.defaultProfile);
    }

    /**
     * Znajduje ServerEntry w LtrynekClient.serversConfig dla danego profileName.
     */
    private static ServerEntry findServerEntryByProfile(String profileName) {
        for (ServerEntry entry : LtrynekClient.serversConfig.servers) {
            if (entry.profileName.equals(profileName)) {
                return entry;
            }
        }
        return null;
    }
}
