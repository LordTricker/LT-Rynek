package pl.lordtricker.ltrynek.client.config;

import pl.lordtricker.ltrynek.client.LtrynekClient;
import pl.lordtricker.ltrynek.client.price.ClientPriceListManager;

import java.util.Map;

/**
 * Utility class that synchronizes in-memory price list data with the config file
 * and persists it to disk.
 */
public class ConfigSaver {
    /**
     * Synchronizes data from ClientPriceListManager to LtrynekClient.serversConfig
     * and writes the result to the config file.
     */
    public static void save() {
        syncMemoryToConfig();
        ConfigLoader.saveConfig(LtrynekClient.serversConfig);
    }

    /**
     * Synchronizes data from memory (ClientPriceListManager) to the serversConfig object,
     * so that it can be saved to the config file.
     */
    public static void syncMemoryToConfig() {
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
     * Finds the ServerEntry in serversConfig for the given profile name.
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

