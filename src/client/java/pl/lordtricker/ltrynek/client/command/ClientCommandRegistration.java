package pl.lordtricker.ltrynek.client.command;

import pl.lordtricker.ltrynek.client.keybinding.ToggleScanner;
import pl.lordtricker.ltrynek.client.LtrynekClient;
import pl.lordtricker.ltrynek.client.util.SearchAutomationController;
import pl.lordtricker.ltrynek.core.util.CompositeKeyUtil;
import pl.lordtricker.ltrynek.core.util.Messages;
import pl.lordtricker.ltrynek.client.config.ConfigLoader;
import pl.lordtricker.ltrynek.core.config.PriceEntry;
import pl.lordtricker.ltrynek.core.config.ServerEntry;
import pl.lordtricker.ltrynek.core.manager.ClientPriceListManager;
import pl.lordtricker.ltrynek.core.manager.ClientSearchListManager;
import pl.lordtricker.ltrynek.core.util.PriceFormatter;
import pl.lordtricker.ltrynek.core.util.SearchSummaryBuilder;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import static pl.lordtricker.ltrynek.client.config.ConfigLoader.saveAllConfigs;

import java.util.List;
import java.util.Map;

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
                        .executes(ctx -> {
                            String activeProfile = ClientPriceListManager.getActiveProfile();
                            String message = Messages.format("mod.info", Map.of("profile", activeProfile));
                            ctx.getSource().sendFeedback(CommandUi.colored(message));
                            return 1;
                        })
                        .then(ClientCommandManager.literal("scan")
                                .executes(ctx -> {
                                    ToggleScanner.scanningEnabled = !ToggleScanner.scanningEnabled;
                                    String msgKey = ToggleScanner.scanningEnabled
                                            ? "command.scanner.toggle.on"
                                            : "command.scanner.toggle.off";
                                    ctx.getSource().sendFeedback(CommandUi.colored(Messages.get(msgKey)));
                                    return 1;
                                })
                        )
                        .then(ClientCommandManager.literal("profiles")
                                .executes(ctx -> {
                                    String allProfiles = ClientPriceListManager.listProfiles();
                                    String[] profiles = allProfiles.split(",\\s*");
                                    MutableText finalText = CommandUi.colored(Messages.get("command.profiles.header"));
                                    finalText.append(Text.literal("\n"));

                                    String activeProfile = ClientPriceListManager.getActiveProfile();
                                    for (String profile : profiles) {
                                        String trimmedProfile = profile.trim();
                                        String lineTemplate;
                                        if (trimmedProfile.equals(activeProfile)) {
                                            lineTemplate = Messages.format("profile.picked.line", Map.of("profile", trimmedProfile));
                                            finalText.append(CommandUi.colored(lineTemplate));
                                        } else {
                                            lineTemplate = Messages.format("profile.available.line", Map.of("profile", trimmedProfile));
                                            MutableText lineText = CommandUi.clickable(
                                                    lineTemplate,
                                                    ClickEvent.Action.RUN_COMMAND,
                                                    "/ltr profile " + trimmedProfile,
                                                    "Kliknij, aby zmienic profil na " + trimmedProfile);
                                            finalText.append(lineText);
                                        }
                                        finalText.append(Text.literal("\n"));
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
                                            ctx.getSource().sendFeedback(CommandUi.colored(msg));
                                            return 1;
                                        })
                                )
                        )
                        .then(ClientCommandManager.literal("add")
                                .then(ClientCommandManager.argument("maxPrice", StringArgumentType.word())
                                        .then(ClientCommandManager.argument("itemName", StringArgumentType.greedyString())
                                                .suggests((context, builder) -> CommandUi.suggestItemIds(builder))
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

                                                    ctx.getSource().sendFeedback(CommandUi.colored(msg));
                                                    syncMemoryToConfig();
                                                    return 1;
                                                })
                                        )
                                )
                        )
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
                                            ctx.getSource().sendFeedback(CommandUi.colored(msg));
                                            syncMemoryToConfig();
                                            return 1;
                                        })
                                )
                        )
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

                                            String editIconStr = Messages.get("pricelist.icon.edit");
                                            MutableText editIcon = CommandUi.clickable(
                                                    editIconStr,
                                                    ClickEvent.Action.SUGGEST_COMMAND,
                                                    "/ltr add " + priceStr + " " + friendlyName,
                                                    "Kliknij aby zedytowac " + friendlyName);

                                            String removeIconStr = Messages.get("pricelist.icon.remove");
                                            MutableText removeIcon = CommandUi.clickable(
                                                    removeIconStr,
                                                    ClickEvent.Action.RUN_COMMAND,
                                                    "/ltr remove " + friendlyName,
                                                    "Kliknij aby usunac " + friendlyName);

                                            String itemLineStr = Messages.format("pricelist.item_line",
                                                    Map.of("item", friendlyName, "price", priceStr));
                                            MutableText itemLine = CommandUi.colored(itemLineStr);
                                            MutableText lineText = Text.empty()
                                                    .append(editIcon).append(Text.literal(" "))
                                                    .append(removeIcon).append(Text.literal(" "))
                                                    .append(itemLine).append(Text.literal("\n"));
                                            finalText.append(lineText);
                                        }
                                    }

                                    String msgHeader = Messages.format("command.list", Map.of("profile", activeProfile, "list", ""));
                                    ctx.getSource().sendFeedback(CommandUi.colored(msgHeader));
                                    ctx.getSource().sendFeedback(finalText);
                                    return 1;
                                })
                        )
                        .then(ClientCommandManager.literal("pomoc")
                                .executes(ctx -> {
                                    ctx.getSource().sendFeedback(CommandUi.colored(Messages.get("command.help")));
                                    return 1;
                                })
                        )
                        .then(ClientCommandManager.literal("config")
                                .then(ClientCommandManager.literal("save")
                                        .executes(ctx -> {
                                            syncMemoryToConfig();
                                            saveAllConfigs(LtrynekClient.serversConfig);
                                            ctx.getSource().sendFeedback(CommandUi.colored(Messages.get("command.config.save.success")));
                                            return 1;
                                        })
                                )
                                .then(ClientCommandManager.literal("reload")
                                        .executes(ctx -> {
                                            LtrynekClient.serversConfig = ConfigLoader.loadConfig();
                                            ClientPriceListManager.clearAllProfiles();
                                            reinitProfilesFromConfig();
                                            ctx.getSource().sendFeedback(CommandUi.colored(Messages.get("command.config.reload.success")));
                                            return 1;
                                        })
                                )
                        )
                        .then(ClientCommandManager.literal("sounds")
                                .executes(ctx -> {
                                    boolean current = LtrynekClient.serversConfig.soundsEnabled;
                                    String msg = current
                                            ? Messages.get("command.sounds.current_on")
                                            : Messages.get("command.sounds.current_off");
                                    ctx.getSource().sendFeedback(CommandUi.colored(msg));
                                    return 1;
                                })
                                .then(ClientCommandManager.literal("on")
                                        .executes(ctx -> {
                                            LtrynekClient.serversConfig.soundsEnabled = true;
                                            ctx.getSource().sendFeedback(CommandUi.colored(Messages.get("command.sounds.enabled")));
                                            saveAllConfigs(LtrynekClient.serversConfig);
                                            return 1;
                                        })
                                )
                                .then(ClientCommandManager.literal("off")
                                        .executes(ctx -> {
                                            LtrynekClient.serversConfig.soundsEnabled = false;
                                            ctx.getSource().sendFeedback(CommandUi.colored(Messages.get("command.sounds.disabled")));
                                            saveAllConfigs(LtrynekClient.serversConfig);
                                            return 1;
                                        })
                                )
                        )
                        .then(ClientCommandManager.literal("search")
                                .then(ClientCommandManager.literal("add")
                                        .then(ClientCommandManager.argument("item", StringArgumentType.greedyString())
                                                .suggests((context, builder) -> CommandUi.suggestItemIds(builder))
                                                .executes(ctx -> {
                                                    String rawItem = StringArgumentType.getString(ctx, "item");
                                                    ClientSearchListManager.addItem(rawItem);
                                                    String msg = Messages.format("command.searchlist.add", Map.of("item", rawItem));
                                                    ctx.getSource().sendFeedback(CommandUi.colored(msg));
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
                                                    ctx.getSource().sendFeedback(CommandUi.colored(msg));
                                                    return 1;
                                                })
                                        )
                                )
                                .then(ClientCommandManager.literal("start")
                                        .executes(ctx -> {
                                            SearchAutomationController.start(-1);
                                            ctx.getSource().sendFeedback(CommandUi.colored(Messages.get("command.searchlist.start")));
                                            return 1;
                                        })
                                        .then(ClientCommandManager.argument("pages", IntegerArgumentType.integer(1))
                                                .executes(ctx -> {
                                                    int pages = IntegerArgumentType.getInteger(ctx, "pages");
                                                    SearchAutomationController.start(pages);
                                                    ctx.getSource().sendFeedback(CommandUi.colored(Messages.get("command.searchlist.start")));
                                                    return 1;
                                                })
                                        )
                                )
                                .then(ClientCommandManager.literal("stop")
                                        .executes(ctx -> {
                                            SearchAutomationController.cancel();
                                            var reason = SearchAutomationController.consumeLastFinishReason();
                                            int pages = SearchAutomationController.consumeLastPagesScanned();
                                            String reasonLine = null;
                                            if (reason != null) {
                                                switch (reason) {
                                                    case MANUAL -> reasonLine = Messages.get("command.searchlist.stop.reason.manual");
                                                    case LIMIT_REACHED -> reasonLine = Messages.get("command.searchlist.stop.reason.limit");
                                                    case NO_NEXT_PAGE -> reasonLine = Messages.get("command.searchlist.stop.reason.no_next");
                                                    case NO_GUI -> reasonLine = Messages.get("command.searchlist.stop.reason.no_gui");
                                                }
                                            }
                                            List<String> lines = SearchSummaryBuilder.buildStopSummary(reasonLine, pages);
                                            MutableText finalText = Text.empty();
                                            for (int i = 0; i < lines.size(); i++) {
                                                finalText.append(CommandUi.colored(lines.get(i)));
                                                if (i < lines.size() - 1) {
                                                    finalText.append(Text.literal("\n"));
                                                }
                                            }
                                            ctx.getSource().sendFeedback(finalText);
                                            return 1;
                                        })
                                )
                                .then(ClientCommandManager.literal("list")
                                        .executes(ctx -> {
                                            List<String> searchItems = ClientSearchListManager.getSearchList();
                                            if (searchItems.isEmpty()) {
                                                ctx.getSource().sendFeedback(CommandUi.colored(Messages.get("command.searchlist.list.empty")));
                                                return 1;
                                            }
                                            MutableText finalText = CommandUi.colored(Messages.get("command.searchlist.list.header"));
                                            finalText.append(Text.literal("\n"));
                                            for (String compositeKey : searchItems) {
                                                String friendly = CompositeKeyUtil.getFriendlyName(compositeKey);
                                                String removeIconStr = Messages.get("pricelist.icon.remove");
                                                MutableText removeIcon = CommandUi.clickable(
                                                        removeIconStr,
                                                        ClickEvent.Action.RUN_COMMAND,
                                                        "/ltr search remove " + friendly,
                                                        Messages.get("command.searchlist.list.remove.hover"));

                                                MutableText lineText = Text.empty()
                                                        .append(removeIcon)
                                                        .append(Text.literal(" "))
                                                        .append(Text.literal(friendly));
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
        if (LtrynekClient.serversConfig == null || LtrynekClient.serversConfig.servers == null) {
            return;
        }
        Map<String, List<PriceEntry>> allProfiles = ClientPriceListManager.getAllProfiles();

        for (ServerEntry se : LtrynekClient.serversConfig.servers) {
            List<PriceEntry> memList = allProfiles.getOrDefault(se.profileName, List.of());

            se.prices.clear();
            for (PriceEntry src : memList) {
                PriceEntry pe = new PriceEntry();
                pe.name = src.name;
                pe.maxPrice = src.maxPrice;
                pe.lore = src.lore;
                pe.material = src.material;
                pe.enchants = src.enchants;
                se.prices.add(pe);
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

}
