package pl.lordtricker.ltrynek.client.price;

import pl.lordtricker.ltrynek.client.util.ColorStripUtils;

import java.util.HashMap;
import java.util.Map;

public class ClientPriceListManager {

    /**
     * Struktura: profile -> (nazwaItemu -> maxPrice).
     * nazwaItemu to może być alias (np. "magic_pickaxe") albo standardowy ID (np. "minecraft:gunpowder").
     */
    private static final Map<String, Map<String, Double>> priceLists = new HashMap<>();

    /**
     * Struktura do rozpoznawania aliasów dla custom items:
     * Dla każdego profilu mamy mapę: (material + "|" + noColorName) -> alias.
     * Możesz rejestrować te aliasy dynamicznie (np. przez komendy), jeśli chcesz.
     */
    private static final Map<String, Map<String, String>> customLookup = new HashMap<>();

    private static String activeProfile = "default";

    /**
     * Ustawia aktywny profil – jeśli nie istnieje, tworzy nowy wpis w priceLists i customLookup.
     */
    public static void setActiveProfile(String profile) {
        activeProfile = profile;
        priceLists.computeIfAbsent(profile, k -> new HashMap<>());
        customLookup.computeIfAbsent(profile, k -> new HashMap<>());
    }

    public static String getActiveProfile() {
        return activeProfile;
    }

    /**
     * Zwraca listę wszystkich profili, jakie mamy w priceLists.
     */
    public static String listProfiles() {
        if (priceLists.isEmpty()) {
            return "No profiles defined.";
        }
        return String.join(", ", priceLists.keySet());
    }

    /**
     * Dodaje/ustawia cenę (maxPrice) dla danego itemName (alias lub standardowy ID) w aktywnym profilu.
     */
    public static void addPriceEntry(String itemName, double maxPrice) {
        priceLists.computeIfAbsent(activeProfile, k -> new HashMap<>()).put(itemName, maxPrice);
    }

    /**
     * Usuwa wpis z aktywnego profilu.
     */
    public static void removePriceEntry(String itemName) {
        Map<String, Double> items = priceLists.get(activeProfile);
        if (items != null) {
            items.remove(itemName);
        }
    }

    /**
     * Zwraca sformatowaną listę przedmiotów (itemName i maxPrice) z aktywnego profilu.
     * Format: "itemName maxPrice", każdy wpis w osobnej linii.
     */
    public static String getPriceListAsString() {
        Map<String, Double> items = priceLists.get(activeProfile);
        if (items == null || items.isEmpty()) {
            return "No items in profile " + activeProfile;
        }
        StringBuilder sb = new StringBuilder();
        items.forEach((k, v) -> {
            sb.append(v).append(" ").append(k).append("\n");
        });
        return sb.toString().trim();
    }

    /**
     * Rejestruje custom item w aktywnym profilu – buduje klucz (material + "|" + noColorName) -> alias.
     * Ta metoda może być wywoływana przez komendy, jeśli gracz chce dodać customowy alias.
     */
    public static void registerCustomItem(String profile, String material, String noColorName, String alias) {
        customLookup.computeIfAbsent(profile, k -> new HashMap<>())
                .put(material + "|" + noColorName, alias);
    }

    /**
     * Znajduje alias w aktywnym profilu na podstawie materialu i noColorName.
     * Zwraca alias lub null, jeśli nie znaleziono.
     */
    public static String findAlias(String material, String noColorName) {
        Map<String, String> lookup = customLookup.get(activeProfile);
        if (lookup == null) return null;
        return lookup.get(material + "|" + noColorName);
    }

    /**
     * Pobiera cenę dla danego itemName (alias lub standardowy ID) w aktywnym profilu.
     * Zwraca -1, jeśli brak wpisu.
     */
    public static double getMaxPrice(String itemName) {
        Map<String, Double> items = priceLists.get(activeProfile);
        if (items == null) return -1;
        return items.getOrDefault(itemName, -1.0);
    }

    /**
     * Usuwa kody kolorów i formatowanie z podanego ciągu, delegując do ColorStripUtils.
     */
    public static String stripColorsAdvanced(String input) {
        return ColorStripUtils.stripAllColorsAndFormats(input);
    }

    public static Map<String, Map<String, Double>> getAllProfiles() {
        return priceLists;
    }

    public static void clearAllProfiles() {
        priceLists.clear();
        customLookup.clear();
        activeProfile = "default";
    }

    public static double getMatchingMaxPrice(String cleanedItemName, String materialId) {
        Map<String, Double> items = priceLists.get(activeProfile);
        if (items == null) return -1;

        if (items.containsKey(materialId)) {
            return items.get(materialId);
        }

        String lowerName = cleanedItemName.toLowerCase();
        for (Map.Entry<String, Double> entry : items.entrySet()) {
            String key = entry.getKey().toLowerCase();
            if (lowerName.contains(key)) {
                return entry.getValue();
            }
        }
        return -1;
    }
}
