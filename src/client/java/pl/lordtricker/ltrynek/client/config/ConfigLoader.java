package pl.lordtricker.ltrynek.client.config;

import net.fabricmc.loader.api.FabricLoader;
import pl.lordtricker.ltrynek.core.config.CoreConfigLoader;
import pl.lordtricker.ltrynek.core.config.ServersConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigLoader {
    private static final Path MOD_CONFIG_DIR;

    static {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        MOD_CONFIG_DIR = configDir.resolve("LT-Mods").resolve("LT-Rynek");
        try {
            if (!Files.exists(MOD_CONFIG_DIR)) {
                Files.createDirectories(MOD_CONFIG_DIR);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ServersConfig loadConfig() {
        return CoreConfigLoader.loadConfig(MOD_CONFIG_DIR);
    }

    public static void saveAllConfigs(ServersConfig config) {
        CoreConfigLoader.saveAllConfigs(config, MOD_CONFIG_DIR);
    }
}
