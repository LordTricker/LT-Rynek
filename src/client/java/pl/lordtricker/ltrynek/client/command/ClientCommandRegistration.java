package pl.lordtricker.ltrynek.client.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import pl.lordtricker.ltrynek.client.ColorUtils;
import pl.lordtricker.ltrynek.client.LtrynekClient;
import pl.lordtricker.ltrynek.client.Messages;
import pl.lordtricker.ltrynek.client.config.ConfigLoader;
import pl.lordtricker.ltrynek.client.config.PriceEntry;
import pl.lordtricker.ltrynek.client.config.ServerEntry;
import pl.lordtricker.ltrynek.client.price.ClientPriceListManager;

import java.util.Map;

public class ClientCommandRegistration {

    public static void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
                ClientCommandManager.literal("ltr")
                        // /ltr
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

                        // /ltr profiles
                        .then(ClientCommandManager.literal("profiles")
                                .executes(ctx -> {
                                    String allProfiles = ClientPriceListManager.listProfiles();
                                    String[] profiles = allProfiles.split(",\\s*");
                                    String headerStr = Messages.get("command.profiles.header");
                                    MutableText finalText = (MutableText) ColorUtils.translateColorCodes(headerStr);
                                    finalText.append(Text.of("\n"));
                                    for (String profile : profiles) {
                                        String lineTemplate = Messages.format("profile.available.line", Map.of("profile", profile.trim()));
                                        MutableText lineText = (MutableText) ColorUtils.translateColorCodes(lineTemplate);
                                        Style clickableStyle = Style.EMPTY
                                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ltr profile " + profile))
                                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                                        Text.of("Kliknij aby zmienić profil " + profile)));
                                        lineText.setStyle(clickableStyle);
                                        finalText.append(lineText).append(Text.of("\n"));
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
                                                    String itemName = StringArgumentType.getString(ctx, "itemName");
                                                    try {
                                                        double maxPrice = Double.parseDouble(maxPriceStr);
                                                        String activeProfile = ClientPriceListManager.getActiveProfile();
                                                        ClientPriceListManager.addPriceEntry(itemName, maxPrice);
                                                        String msg = Messages.format("command.add.success", Map.of(
                                                                "item", itemName,
                                                                "price", String.valueOf(maxPrice),
                                                                "profile", activeProfile
                                                        ));
                                                        ctx.getSource().sendFeedback(ColorUtils.translateColorCodes(msg));
                                                    } catch (NumberFormatException e) {
                                                        ctx.getSource().sendError(Text.of("Invalid price format: " + maxPriceStr));
                                                    }
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
                                    MutableText finalText = (MutableText) Text.of("");
                                    for (String line : lines) {
                                        String[] parts = line.split("\\s+", 2);
                                        if (parts.length < 2) continue;
                                        String priceStr = parts[0];
                                        String itemName = parts[1];

                                        // Ikona edycji
                                        String editIconStr = Messages.get("pricelist.icon.edit");
                                        MutableText editIcon = (MutableText) ColorUtils.translateColorCodes(editIconStr);
                                        editIcon.setStyle(
                                                Style.EMPTY
                                                        .withClickEvent(new ClickEvent(
                                                                ClickEvent.Action.SUGGEST_COMMAND,
                                                                "/ltr add " + priceStr + " " + itemName
                                                        ))
                                                        .withHoverEvent(new HoverEvent(
                                                                HoverEvent.Action.SHOW_TEXT,
                                                                Text.of("Kliknij aby zedytować " + itemName)
                                                        ))
                                        );

                                        // Ikona usuwania
                                        String removeIconStr = Messages.get("pricelist.icon.remove");
                                        MutableText removeIcon = (MutableText) ColorUtils.translateColorCodes(removeIconStr);
                                        removeIcon.setStyle(
                                                Style.EMPTY
                                                        .withClickEvent(new ClickEvent(
                                                                ClickEvent.Action.RUN_COMMAND,
                                                                "/ltr remove " + itemName
                                                        ))
                                                        .withHoverEvent(new HoverEvent(
                                                                HoverEvent.Action.SHOW_TEXT,
                                                                Text.of("Kliknij aby usunąć " + itemName)
                                                        ))
                                        );

                                        String itemLineStr = Messages.format("pricelist.item_line",
                                                Map.of("item", itemName, "price", priceStr));
                                        MutableText itemLine = (MutableText) ColorUtils.translateColorCodes(itemLineStr);

                                        MutableText lineText = ((MutableText) Text.of(""))
                                                .append(editIcon).append(Text.of(" "))
                                                .append(removeIcon).append(Text.of(" "))
                                                .append(itemLine)
                                                .append(Text.of("\n"));

                                        finalText.append(lineText);
                                    }
                                    // Nagłówek
                                    String msgHeader = Messages.format("command.list",
                                            Map.of("profile", activeProfile, "list", "")
                                    );
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
                                            String message = Messages.get("command.config.save.success");
                                            ctx.getSource().sendFeedback(ColorUtils.translateColorCodes(message));
                                            return 1;
                                        })
                                )
                                .then(ClientCommandManager.literal("reload")
                                        .executes(ctx -> {
                                            LtrynekClient.serversConfig = ConfigLoader.loadConfig();
                                            ClientPriceListManager.clearAllProfiles();
                                            reinitProfilesFromConfig();
                                            String message = Messages.get("command.config.reload.success");
                                            ctx.getSource().sendFeedback(ColorUtils.translateColorCodes(message));
                                            return 1;
                                        })
                                )
                        )
        );
    }

    private static void syncMemoryToConfig() {
        for (ServerEntry entry : LtrynekClient.serversConfig.servers) {
            entry.prices.clear();
        }
        Map<String, Map<String, Double>> allProfiles = ClientPriceListManager.getAllProfiles();
        for (Map.Entry<String, Map<String, Double>> profEntry : allProfiles.entrySet()) {
            String profileName = profEntry.getKey();
            Map<String, Double> items = profEntry.getValue();
            ServerEntry se = findServerEntryByProfile(profileName);
            if (se == null) continue;
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

    private static void reinitProfilesFromConfig() {
        for (ServerEntry entry : LtrynekClient.serversConfig.servers) {
            ClientPriceListManager.setActiveProfile(entry.profileName);
            for (PriceEntry pe : entry.prices) {
                ClientPriceListManager.addPriceEntry(pe.name, pe.maxPrice);
            }
        }
        ClientPriceListManager.setActiveProfile(LtrynekClient.serversConfig.defaultProfile);
    }

    private static ServerEntry findServerEntryByProfile(String profileName) {
        for (ServerEntry entry : LtrynekClient.serversConfig.servers) {
            if (entry.profileName.equals(profileName)) {
                return entry;
            }
        }
        return null;
    }
}
