package com.example.ltrynek.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ConfigLoader {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE_NAME = "ltrynek-profiles.json";

    public static ServersConfig loadConfig() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        Path configFile = configDir.resolve(CONFIG_FILE_NAME);

        if (!Files.exists(configFile)) {
            ServersConfig defaultConfig = createDefaultConfig();
            saveConfig(defaultConfig);
            return defaultConfig;
        }

        try (Reader reader = Files.newBufferedReader(configFile)) {
            return GSON.fromJson(reader, ServersConfig.class);
        } catch (IOException e) {
            e.printStackTrace();
            return new ServersConfig();
        }
    }

    public static void saveConfig(ServersConfig config) {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        Path configFile = configDir.resolve(CONFIG_FILE_NAME);

        try (Writer writer = Files.newBufferedWriter(configFile)) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Tworzy domyślny config z przykładowymi wartościami, uwzględniając nowe pola:
     * loreRegex, highlightColor oraz highlightColorStack.
     */
    private static ServersConfig createDefaultConfig() {
        ServersConfig cfg = new ServersConfig();
        cfg.defaultProfile = "default";

        ServerEntry server1 = new ServerEntry();
        server1.domains = List.of("minestar.pl");
        server1.profileName = "minestar_boxpvp";
        server1.loreRegex = "(?i).*Cena.*?\\$?([\\d.,]+[km]?).*";
        server1.highlightColor = "#00FF33";
        server1.highlightColorStack = "#FFAB00";

        PriceEntry pe1 = new PriceEntry();
        pe1.name = "minecraft:gunpowder";
        pe1.maxPrice = 100.0;
        server1.prices.add(pe1);

        cfg.servers.add(server1);

        ServerEntry server2 = new ServerEntry();
        server2.domains = List.of("anarchia.gg");
        server2.profileName = "anarchia_smp";
        server2.loreRegex = "(?i).*Koszt.*?\\$([\\d.,kmKM]+).*";
        server2.highlightColor = "#00FF33";
        server2.highlightColorStack = "#FFAB00";

        PriceEntry pe2 = new PriceEntry();
        pe2.name = "minecraft:emerald";
        pe2.maxPrice = 200.0;
        server2.prices.add(pe2);

        cfg.servers.add(server2);

        return cfg;
    }
}
