package pl.lordtricker.ltrynek.client.command;

import pl.lordtricker.ltrynek.client.keybinding.ToggleScanner;
import pl.lordtricker.ltrynek.client.util.ColorUtils;
import pl.lordtricker.ltrynek.client.LtrynekClient;
import pl.lordtricker.ltrynek.client.util.CompositeKeyUtil;
import pl.lordtricker.ltrynek.client.util.Messages;
import pl.lordtricker.ltrynek.client.config.ConfigLoader;
import pl.lordtricker.ltrynek.client.config.PriceEntry;
import pl.lordtricker.ltrynek.client.config.ServerEntry;
import pl.lordtricker.ltrynek.client.manager.ClientPriceListManager;
import pl.lordtricker.ltrynek.client.manager.ClientSearchListManager;
import pl.lordtricker.ltrynek.client.util.PriceFormatter;
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

import java.util.List;
import java.util.Map;

import static pl.lordtricker.ltrynek.client.config.ConfigLoader.saveAllConfigs;

public class ClientCommandRegistration {

    public static void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register(ClientCommandRegistration::registerLtrynekCommand);
    }

    private static void registerLtrynekCommand(
            CommandDispatcher<FabricClientCommandSource> dispatcher,
            CommandRegistryAccess registryAccess
    ) {
        dispatcher.register(
                ClientCommandManager.literal("ltr")
                        // /ltr
                        .executes(ctx -> {
                            String activeProfile = ClientPriceListManager.getActiveProfile();
                            String message = Messages.format("mod.info", Map.of("profile", activeProfile));
                            ctx.getSource().sendFeedback(ColorUtils.translateColorCodes(message));
                            return 1;
                        })
                        .then(ClientCommandManager.literal("scan")
                                .executes(ctx -> {
                                    ToggleScanner.scanningEnabled = !ToggleScanner.scanningEnabled;
                                    String msgKey = ToggleScanner.scanningEnabled ? "command.scanner.toggle.on" : "command.scanner.toggle.off";
                                    String msg = Messages.get(msgKey);
                                    ctx.getSource().sendFeedback(ColorUtils.translateColorCodes(msg));
                                    return 1;
                                })
                        )
                        .then(ClientCommandManager.literal("profiles")
                                .executes(ctx -> {
                                    String allProfiles = ClientPriceListManager.listProfiles();
                                    String[] profiles = allProfiles.split(",\\s*");
                                    String headerStr = Messages.get("command.profiles.header");
                                    MutableText finalText = (MutableText) ColorUtils.translateColorCodes(headerStr);
                                    finalText.append(Text.literal("\n"));

                                    String activeProfile = ClientPriceListManager.getActiveProfile();
                                    for (String profile : profiles) {
                                        String trimmedProfile = profile.trim();
                                        String lineTemplate;
                                        if (trimmedProfile.equals(activeProfile)) {
                                            lineTemplate = Messages.format("profile.picked.line", Map.of("profile", trimmedProfile));
                                        } else {
                                            lineTemplate = Messages.format("profile.available.line", Map.of("profile", trimmedProfile));
                                        }
                                        MutableText lineText = (MutableText) ColorUtils.translateColorCodes(lineTemplate);

                                        if (!trimmedProfile.equals(activeProfile)) {
                                            Style clickableStyle = Style.EMPTY
                                                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ltr profile " + trimmedProfile))
                                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                                            Text.literal("Kliknij, aby zmienić profil na " + trimmedProfile)));
                                            lineText.setStyle(clickableStyle);
                                        }

                                        finalText.append(lineText).append(Text.literal("\n"));
                                    }
                                    ctx.getSource().sendFeedback(finalText);
                                    return 1;
                                })
                        )
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
                                                .suggests((context, builder) -> {
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
                                                })
                                                .executes(ctx -> {
                                                    String maxPriceStr = StringArgumentType.getString(ctx, "maxPrice");
                                                    double parsedPrice = PriceFormatter.parsePrice(maxPriceStr);

                                                    if (parsedPrice < 0) {
                                                        ctx.getSource().sendError(Text.literal("Invalid price format: " + maxPriceStr));
                                                        return 0;
                                                    }

                                                    String fullItemName = StringArgumentType.getString(ctx, "itemName");
                                                    ClientPriceListManager.addPriceEntry(fullItemName, parsedPrice);

                                                    String activeProfile = ClientPriceListManager.getActiveProfile();
                                                    String friendly = CompositeKeyUtil.getFriendlyName(
                                                            CompositeKeyUtil.createCompositeKey(fullItemName));
                                                    String shortPrice = PriceFormatter.formatPrice(parsedPrice);

                                                    String msg = Messages.format("command.add.success", Map.of(
                                                            "item", friendly,
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
                                            String rawItem = StringArgumentType.getString(ctx, "itemName");
                                            String activeProfile = ClientPriceListManager.getActiveProfile();
                                            ClientPriceListManager.removePriceEntry(rawItem);
                                            String friendly = CompositeKeyUtil.getFriendlyName(
                                                    CompositeKeyUtil.createCompositeKey(rawItem));
                                            String msg = Messages.format("command.remove.success", Map.of(
                                                    "item", friendly,
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
                                    List<PriceEntry> entries = ClientPriceListManager.getAllProfiles().get(activeProfile);
                                    MutableText finalText = Text.empty();

                                    if (entries != null) {
                                        for (PriceEntry pe : entries) {
                                            String compositeKey = CompositeKeyUtil.getCompositeKeyFromEntry(pe);
                                            String friendlyName = CompositeKeyUtil.getFriendlyName(compositeKey);
                                            String priceStr = PriceFormatter.formatPrice(pe.maxPrice);

                                            // Ikona edycji
                                            String editIconStr = Messages.get("pricelist.icon.edit");
                                            MutableText editIcon = (MutableText) ColorUtils.translateColorCodes(editIconStr);
                                            editIcon.setStyle(
                                                    Style.EMPTY.withClickEvent(new ClickEvent(
                                                                    ClickEvent.Action.SUGGEST_COMMAND,
                                                                    "/ltr add " + priceStr + " " + friendlyName))
                                                            .withHoverEvent(new HoverEvent(
                                                                    HoverEvent.Action.SHOW_TEXT,
                                                                    Text.literal("Kliknij aby zedytować " + friendlyName))));

                                            // Ikona usuwania
                                            String removeIconStr = Messages.get("pricelist.icon.remove");
                                            MutableText removeIcon = (MutableText) ColorUtils.translateColorCodes(removeIconStr);
                                            removeIcon.setStyle(
                                                    Style.EMPTY.withClickEvent(new ClickEvent(
                                                                    ClickEvent.Action.RUN_COMMAND,
                                                                    "/ltr remove " + friendlyName))
                                                            .withHoverEvent(new HoverEvent(
                                                                    HoverEvent.Action.SHOW_TEXT,
                                                                    Text.literal("Kliknij aby usunąć " + friendlyName))));

                                            String itemLineStr = Messages.format("pricelist.item_line", Map.of("item", friendlyName, "price", priceStr));
                                            MutableText itemLine = (MutableText) ColorUtils.translateColorCodes(itemLineStr);
                                            MutableText lineText = Text.empty()
                                                    .append(editIcon).append(Text.literal(" "))
                                                    .append(removeIcon).append(Text.literal(" "))
                                                    .append(itemLine).append(Text.literal("\n"));
                                            finalText.append(lineText);
                                        }
                                    }

                                    String msgHeader = Messages.format("command.list", Map.of("profile", activeProfile, "list", ""));
                                    MutableText header = (MutableText) ColorUtils.translateColorCodes(msgHeader);
                                    ctx.getSource().sendFeedback(header);
                                    ctx.getSource().sendFeedback(finalText);
                                    return 1;
                                })
                        )


                        .then(ClientCommandManager.literal("pomoc")
                                .executes(ctx -> {
                                    String msg = Messages.get("command.help");
                                    ctx.getSource().sendFeedback(ColorUtils.translateColorCodes(msg));
                                    return 1;
                                })
                        )
                        .then(ClientCommandManager.literal("config")
                                .then(ClientCommandManager.literal("save")
                                        .executes(ctx -> {
                                            syncMemoryToConfig();
                                            saveAllConfigs(LtrynekClient.serversConfig);
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
                        .then(ClientCommandManager.literal("sounds")
                                .executes(ctx -> {
                                    boolean current = LtrynekClient.serversConfig.soundsEnabled;
                                    String msg = current ? Messages.get("command.sounds.current_on") : Messages.get("command.sounds.current_off");
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
                        .then(ClientCommandManager.literal("search")
                                .then(ClientCommandManager.literal("add")
                                        .then(ClientCommandManager.argument("item", StringArgumentType.greedyString())
                                                .suggests((context, builder) -> {
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
                                                })
                                                .executes(ctx -> {
                                                    String rawItem = StringArgumentType.getString(ctx, "item");
                                                    ClientSearchListManager.addItem(rawItem);
                                                    String msg = Messages.format("command.searchlist.add", Map.of("item", rawItem));
                                                    ctx.getSource().sendFeedback(ColorUtils.translateColorCodes(msg));
                                                    return 1;
                                                })
                                        )
                                )
                                .then(ClientCommandManager.literal("remove")
                                        .then(ClientCommandManager.argument("item", StringArgumentType.greedyString())
                                                .executes(ctx -> {
                                                    String rawItem = StringArgumentType.getString(ctx, "item");
                                                    ClientSearchListManager.removeItem(rawItem);
                                                    String friendly = CompositeKeyUtil.getFriendlyName(
                                                            CompositeKeyUtil.createCompositeKey(rawItem));
                                                    String msg = Messages.format("command.searchlist.remove", Map.of("item", friendly));
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
                                            List<String> searchItems = ClientSearchListManager.getSearchList();
                                            if (searchItems.isEmpty()) {
                                                String emptyMsg = Messages.get("command.searchlist.list.empty");
                                                ctx.getSource().sendFeedback(ColorUtils.translateColorCodes(emptyMsg));
                                                return 1;
                                            }
                                            MutableText finalText = Text.empty();
                                            String headerRaw = Messages.get("command.searchlist.stop.header");
                                            finalText.append(ColorUtils.translateColorCodes(headerRaw)).append(Text.literal("\n"));
                                            for (String compositeKey : searchItems) {
                                                ClientSearchListManager.Stats stats = ClientSearchListManager.getStats(compositeKey);
                                                if (stats == null || stats.getCount() == 0) continue;
                                                String lineRaw = Messages.format("command.searchlist.stop.line", Map.of(
                                                        "item", CompositeKeyUtil.getFriendlyName(compositeKey),
                                                        "count", String.valueOf(stats.getCount()),
                                                        "min", PriceFormatter.formatPrice(stats.getMin()),
                                                        "max", PriceFormatter.formatPrice(stats.getMax()),
                                                        "avg", PriceFormatter.formatPrice(stats.getAverage()),
                                                        "median", PriceFormatter.formatPrice(stats.getMedian()),
                                                        "quartile1", PriceFormatter.formatPrice(stats.getQuartile1()),
                                                        "quartile3", PriceFormatter.formatPrice(stats.getQuartile3())
                                                ));
                                                finalText.append(ColorUtils.translateColorCodes(lineRaw)).append(Text.literal("\n"));
                                            }
                                            ctx.getSource().sendFeedback(finalText);
                                            return 1;
                                        })
                                )
                                .then(ClientCommandManager.literal("list")
                                        .executes(ctx -> {
                                            List<String> searchItems = ClientSearchListManager.getSearchList();
                                            if (searchItems.isEmpty()) {
                                                String emptyMsg = Messages.get("command.searchlist.list.empty");
                                                ctx.getSource().sendFeedback(ColorUtils.translateColorCodes(emptyMsg));
                                                return 1;
                                            }
                                            String header = Messages.get("command.searchlist.list.header");
                                            MutableText finalText = (MutableText) ColorUtils.translateColorCodes(header);
                                            finalText.append(Text.literal("\n"));
                                            for (String compositeKey : searchItems) {
                                                String friendly = CompositeKeyUtil.getFriendlyName(compositeKey);
                                                MutableText friendlyText = Text.literal(friendly);

                                                String removeIconStr = Messages.get("pricelist.icon.remove");
                                                MutableText removeIcon = (MutableText) ColorUtils.translateColorCodes(removeIconStr);
                                                removeIcon.setStyle(
                                                        Style.EMPTY.withClickEvent(new ClickEvent(
                                                                        ClickEvent.Action.RUN_COMMAND,
                                                                        "/ltr search remove " + friendly))
                                                                .withHoverEvent(new HoverEvent(
                                                                        HoverEvent.Action.SHOW_TEXT,
                                                                        Text.literal(Messages.get("command.searchlist.list.remove.hover")))));

                                                MutableText lineText = Text.empty()
                                                        .append(removeIcon)
                                                        .append(Text.literal(" "))
                                                        .append(friendlyText);
                                                finalText.append(lineText).append(Text.literal("\n"));
                                            }
                                            ctx.getSource().sendFeedback(finalText);
                                            return 1;
                                        })
                                )

                        )
        );
    }

    private static void syncMemoryToConfig() {
        Map<String, List<PriceEntry>> allProfiles = ClientPriceListManager.getAllProfiles();
        for (Map.Entry<String, List<PriceEntry>> profEntry : allProfiles.entrySet()) {
            String profileName = profEntry.getKey();
            List<PriceEntry> priceEntries = profEntry.getValue();
            ServerEntry se = findServerEntryByProfile(profileName);
            if (se == null) continue;
            for (PriceEntry storedPe : priceEntries) {
                boolean exists = se.prices.stream().anyMatch(pe -> pe.name.equals(storedPe.name));
                if (!exists) {
                    PriceEntry pe = new PriceEntry();
                    pe.name = storedPe.name;
                    pe.maxPrice = storedPe.maxPrice;
                    pe.lore = storedPe.lore;
                    pe.material = storedPe.material;
                    pe.enchants = storedPe.enchants;
                    se.prices.add(pe);
                }
            }
        }
        saveAllConfigs(LtrynekClient.serversConfig);
    }

    private static void reinitProfilesFromConfig() {
        for (ServerEntry entry : LtrynekClient.serversConfig.servers) {
            ClientPriceListManager.setActiveProfile(entry.profileName);
            for (PriceEntry pe : entry.prices) {
                String rawItem = pe.name;
                if (pe.lore != null && !pe.lore.isEmpty()) {
                    rawItem += "(\"" + pe.lore + "\")";
                }
                if (pe.material != null && !pe.material.isEmpty()) {
                    rawItem += "[\"" + pe.material + "\"]";
                }
                if (pe.enchants != null && !pe.enchants.isEmpty()) {
                    rawItem += "{\"" + pe.enchants + "\"}";
                }
                ClientPriceListManager.addPriceEntry(rawItem, pe.maxPrice);
            }
        }

        String address = LtrynekClient.getServerAddress();
        ServerEntry serverEntry = findServerEntryByAddress(address);
        if (serverEntry != null) {
            ClientPriceListManager.setActiveProfile(serverEntry.profileName);
        } else {
            ClientPriceListManager.setActiveProfile(LtrynekClient.serversConfig.defaultProfile);
        }
    }

    private static ServerEntry findServerEntryByAddress(String address) {
        if (LtrynekClient.serversConfig == null || LtrynekClient.serversConfig.servers == null)
            return null;
        for (ServerEntry entry : LtrynekClient.serversConfig.servers) {
            for (String domain : entry.domains) {
                if (address.equalsIgnoreCase(domain) ||
                        address.toLowerCase().endsWith("." + domain.toLowerCase())) {
                    return entry;
                }
            }
        }
        return null;
    }

    private static ServerEntry findServerEntryByProfile(String profileName) {
        if (LtrynekClient.serversConfig == null || LtrynekClient.serversConfig.servers == null)
            return null;
        for (ServerEntry entry : LtrynekClient.serversConfig.servers) {
            if (entry.profileName.equalsIgnoreCase(profileName)) {
                return entry;
            }
        }
        return null;
    }
}
