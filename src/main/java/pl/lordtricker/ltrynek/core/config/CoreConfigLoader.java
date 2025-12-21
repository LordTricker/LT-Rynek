package pl.lordtricker.ltrynek.core.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class CoreConfigLoader {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String MAIN_CONFIG_FILE_NAME = "ltrynek-config.json";

    private CoreConfigLoader() {}

    public static ServersConfig loadConfig(Path modConfigDir) {
        Path configDir = ensureConfigDir(modConfigDir);
        Path mainConfigFile = configDir.resolve(MAIN_CONFIG_FILE_NAME);
        ServersConfig config;
        if (!Files.exists(mainConfigFile)) {
            config = createDefaultConfig();
            saveAllConfigs(config, configDir);
        } else {
            try (Reader reader = Files.newBufferedReader(mainConfigFile)) {
                config = GSON.fromJson(reader, ServersConfig.class);
                if (config == null) {
                    config = createDefaultConfig();
                }
                if (config.adsEnabled == null) {
                    config.adsEnabled = true;
                    saveAllConfigs(config, configDir);
                }
            } catch (IOException e) {
                e.printStackTrace();
                config = createDefaultConfig();
            }
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(configDir, "*.json")) {
            for (Path entry : stream) {
                if (entry.getFileName().toString().equals(MAIN_CONFIG_FILE_NAME)) {
                    continue;
                }
                try (Reader miniReader = Files.newBufferedReader(entry)) {
                    ServerEntry miniServer = GSON.fromJson(miniReader, ServerEntry.class);
                    if (miniServer == null) {
                        System.err.println("Mini config " + entry.getFileName() + " jest niepoprawny - nie udalo sie sparsowac JSON.");
                        continue;
                    }
                    if (miniServer.domains == null || miniServer.domains.isEmpty()) {
                        System.err.println("Mini config " + entry.getFileName() + " jest niepoprawny - brak wymaganych domen.");
                        continue;
                    }
                    String fileName = entry.getFileName().toString();
                    String profileNameFromFile = fileName.substring(0, fileName.lastIndexOf('.'));
                    miniServer.profileName = profileNameFromFile;

                    applyDefaults(miniServer);

                    config.servers.removeIf(se -> se.profileName.equalsIgnoreCase(miniServer.profileName));

                    miniServer.sourceFile = entry;

                    config.servers.add(miniServer);
                } catch (Exception ex) {
                    System.err.println("Blad podczas ladowania mini configu " + entry.getFileName() + ": " + ex.getMessage());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (ServerEntry entry : config.servers) {
            applyDefaults(entry);
        }
        return config;
    }

    public static void saveAllConfigs(ServersConfig config, Path modConfigDir) {
        Path configDir = ensureConfigDir(modConfigDir);
        List<ServerEntry> mainServers = new ArrayList<>();
        for (ServerEntry entry : config.servers) {
            if (entry.sourceFile == null) {
                mainServers.add(entry);
            }
        }
        ServersConfig mainConfig = new ServersConfig();
        mainConfig.defaultProfile = config.defaultProfile;
        mainConfig.soundsEnabled = config.soundsEnabled;
        mainConfig.adsEnabled = (config.adsEnabled == null) ? Boolean.TRUE : config.adsEnabled;
        mainConfig.servers = mainServers;

        Path mainConfigFile = configDir.resolve(MAIN_CONFIG_FILE_NAME);
        try (Writer writer = Files.newBufferedWriter(mainConfigFile)) {
            GSON.toJson(mainConfig, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (ServerEntry entry : config.servers) {
            if (entry.sourceFile != null) {
                try (Writer writer = Files.newBufferedWriter(entry.sourceFile)) {
                    GSON.toJson(entry, writer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static Path ensureConfigDir(Path modConfigDir) {
        if (modConfigDir == null) {
            throw new IllegalArgumentException("modConfigDir is null");
        }
        try {
            if (!Files.exists(modConfigDir)) {
                Files.createDirectories(modConfigDir);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return modConfigDir;
    }

    private static ServersConfig createDefaultConfig() {
        ServersConfig cfg = new ServersConfig();
        cfg.defaultProfile = "default";
        cfg.adsEnabled = true;

        ServerEntry server1 = new ServerEntry();
        server1.domains = List.of("minestar.pl");
        server1.profileName = "minestar_boxpvp";
        server1.loreRegex = "(?i).*Cena.*?\\$?([\\d.,]+(?:mld|[km])?).*";
        server1.highlightColor = "#00FF33";
        server1.highlightColorStack = "#FFAB00";
        server1.miniAlarmSound = "minecraft:ui.button.click";
        server1.miniAlarmSoundStack = "minecraft:ui.toast.challenge_complete";
        server1.marketCommands = List.of("/ah otworz", "/rynek otworz");
        server1.marketGuiTitle = "Rynek";
        server1.marketNextPageName = "Nastepna strona";
        server1.marketNextPageMaterial = "minecraft:tipped_arrow";
        server1.marketNextPageSlot = 53;
        server1.marketOpenDelayMs = 500;
        server1.marketNextDelayMs = 40;
        server1.marketCloseDelayMs = 500;
        applyDefaults(server1);

        PriceEntry pe1 = new PriceEntry();
        pe1.name = "minecraft:gunpowder";
        pe1.maxPrice = 100.0;
        server1.prices.add(pe1);

        cfg.servers.add(server1);

        ServerEntry server2 = new ServerEntry();
        server2.domains = List.of("anarchia.gg");
        server2.profileName = "anarchia_smp";
        server2.loreRegex = "(?i).*Koszt.*?\\$([\\d.,]+(?:mld|[km])?).*";
        server2.highlightColor = "#00FF33";
        server2.highlightColorStack = "#FFAB00";
        server2.miniAlarmSound = "minecraft:ui.button.click";
        server2.miniAlarmSoundStack = "minecraft:ui.toast.challenge_complete";
        server2.marketCommands = List.of("/ah", "/rynek");
        server2.marketGuiTitle = "Rynek";
        server2.marketNextPageName = "Nastepna strona";
        server2.marketNextPageMaterial = "minecraft:lime_dye";
        server2.marketNextPageSlot = 50;
        server2.marketOpenDelayMs = 500;
        server2.marketNextDelayMs = 8;
        server2.marketCloseDelayMs = 500;
        applyDefaults(server2);

        PriceEntry pe2 = new PriceEntry();
        pe2.name = "minecraft:emerald";
        pe2.maxPrice = 200.0;
        server2.prices.add(pe2);

        cfg.servers.add(server2);

        ServerEntry server3 = new ServerEntry();
        server3.domains = List.of("rapy.pl", "rapy.gg", "rapysmp.pl", "jjsmp.pl");
        server3.profileName = "rapy";
        server3.loreRegex = "(?i).*Cena\\s*:?\\s*(?:\\$\\s*)?((?:\\d{1,3}(?:[\\s\\u00A0.,]\\d{3})*|\\d+)(?:mld|m|k)?)(?:\\s*\\$)?.*";
        server3.highlightColor = "#00FF33";
        server3.highlightColorStack = "#FFAB00";
        server3.miniAlarmSound = "minecraft:ui.button.click";
        server3.miniAlarmSoundStack = "minecraft:ui.toast.challenge_complete";
        server3.marketCommands = List.of("/ah", "/rynek");
        server3.marketGuiTitle = "Market";
        server3.marketNextPageName = "Nastepna strona";
        server3.marketNextPageMaterial = "minecraft:arrow";
        server3.marketNextPageSlot = 53;
        server3.marketOpenDelayMs = 500;
        server3.marketNextDelayMs = 8;
        server3.marketCloseDelayMs = 500;
        applyDefaults(server3);

        PriceEntry pe3 = new PriceEntry();
        pe3.name = "minecraft:emerald";
        pe3.maxPrice = 200.0;
        server3.prices.add(pe3);

        cfg.servers.add(server3);

        ServerEntry server4 = new ServerEntry();
        server4.domains = List.of("pykmc.pl");
        server4.profileName = "pykmc";
        server4.loreRegex = "(?i).*Kwota.*?\\$([\\d.,]+(?:mld|m|k)?).*";
        server4.highlightColor = "#00FF33";
        server4.highlightColorStack = "#FFAB00";
        server4.miniAlarmSound = "minecraft:ui.button.click";
        server4.miniAlarmSoundStack = "minecraft:ui.toast.challenge_complete";
        server4.marketCommands = List.of("/ah", "/rynek");
        server4.marketGuiTitle = "Rynek";
        server4.marketNextPageName = "Nastepna";
        server4.marketNextPageMaterial = "minecraft:green_terracotta";
        server4.marketNextPageSlot = 50;
        server4.marketOpenDelayMs = 500;
        server4.marketNextDelayMs = 8;
        server4.marketCloseDelayMs = 500;
        applyDefaults(server4);

        PriceEntry pe4 = new PriceEntry();
        pe4.name = "minecraft:emerald";
        pe4.maxPrice = 200.0;
        server4.prices.add(pe4);

        cfg.servers.add(server4);

        return cfg;
    }

    private static void applyDefaults(ServerEntry entry) {
        if (entry.prices == null) {
            entry.prices = new ArrayList<>();
        }
        if (entry.loreRegex == null) {
            entry.loreRegex = "Cena: (\\d+)";
        }
        if (entry.highlightColor == null) {
            entry.highlightColor = "#80FF00";
        }
        if (entry.highlightColorStack == null) {
            entry.highlightColorStack = "#FF8000";
        }
        if (entry.miniAlarmSound == null) {
            entry.miniAlarmSound = "minecraft:ui.button.click";
        }
        if (entry.miniAlarmSoundStack == null) {
            entry.miniAlarmSoundStack = "minecraft:entity.player.levelup";
        }
        if (entry.marketCommands == null) {
            entry.marketCommands = new ArrayList<>();
        }
        if (entry.marketOpenDelayMs == null) {
            entry.marketOpenDelayMs = 1500;
        }
        if (entry.marketNextDelayMs == null) {
            entry.marketNextDelayMs = 800;
        }
        if (entry.marketCloseDelayMs == null) {
            entry.marketCloseDelayMs = 500;
        }
    }
}
