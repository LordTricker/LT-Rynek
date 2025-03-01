package pl.lordtricker.ltrynek.client.config;

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
    private static final String CONFIG_FILE_NAME = "ltrynek-config.json";

    public static ServersConfig loadConfig() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        Path configFile = configDir.resolve(CONFIG_FILE_NAME);

        if (!Files.exists(configFile)) {
            ServersConfig defaultConfig = createDefaultConfig();
            saveConfig(defaultConfig);
            return defaultConfig;
        }

        try (Reader reader = Files.newBufferedReader(configFile)) {
            ServersConfig config = GSON.fromJson(reader, ServersConfig.class);
            if (config != null && config.servers != null) {
                for (ServerEntry server : config.servers) {
                    if (server.prices != null) {
                        for (PriceEntry pe : server.prices) {
                            if (pe.lore == null) {
                                pe.lore = "";
                            }
                            if (pe.material == null) {
                                pe.material = "";
                            }
                        }
                    }
                }
            }
            return config;
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
     * - loreRegex, highlightColor, highlightColorStack,
     * - miniAlarmSound oraz miniAlarmSoundStack.
     */
    private static ServersConfig createDefaultConfig() {
        ServersConfig cfg = new ServersConfig();
        cfg.defaultProfile = "default";

        ServerEntry server1 = new ServerEntry();
        server1.domains = List.of("minestar.pl");
        server1.profileName = "minestar_boxpvp";
        server1.loreRegex = "(?i).*Cena.*?\\$?([\\d.,]+(?:mld|[km])?).*";
        server1.highlightColor = "#00FF33";
        server1.highlightColorStack = "#FFAB00";
        server1.miniAlarmSound = "minecraft:ui.button.click";
        server1.miniAlarmSoundStack = "minecraft:ui.toast.challenge_complete";

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

        PriceEntry pe2 = new PriceEntry();
        pe2.name = "minecraft:emerald";
        pe2.maxPrice = 200.0;
        server2.prices.add(pe2);

        cfg.servers.add(server2);

        ServerEntry server3 = new ServerEntry();
        server3.domains = List.of("rapy.pl");
        server3.profileName = "rapy";
        server3.loreRegex = "(?i).*Cena.*?\\$?([\\d.,]+(?:mld|m|k)?).*";
        server3.highlightColor = "#00FF33";
        server3.highlightColorStack = "#FFAB00";
        server3.miniAlarmSound = "minecraft:ui.button.click";
        server3.miniAlarmSoundStack = "minecraft:ui.toast.challenge_complete";

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

        PriceEntry pe4 = new PriceEntry();
        pe4.name = "minecraft:emerald";
        pe4.maxPrice = 200.0;
        server4.prices.add(pe4);

        cfg.servers.add(server4);

        ServerEntry server5 = new ServerEntry();
        server5.domains = List.of("n1mc.pl");
        server5.profileName = "n1mc";
        server5.loreRegex = "(?i).*Cena.*?([\\d.,]+(?:mld|m|k)?)\\$.*";
        server5.highlightColor = "#00FF33";
        server5.highlightColorStack = "#FFAB00";
        server5.miniAlarmSound = "minecraft:ui.button.click";
        server5.miniAlarmSoundStack = "minecraft:ui.toast.challenge_complete";

        PriceEntry pe5 = new PriceEntry();
        pe5.name = "minecraft:emerald";
        pe5.maxPrice = 200.0;
        server5.prices.add(pe5);

        cfg.servers.add(server5);

        return cfg;
    }

}