package pl.lordtricker.ltrynek.client.manager;

import pl.lordtricker.ltrynek.client.config.PriceEntry;
import pl.lordtricker.ltrynek.client.util.CompositeKeyUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientPriceListManager {

    /**
     * Struktura: profile -> lista wpisów typu PriceEntry.
     * Każdy wpis zawiera: name, lore, material, enchants, maxPrice.
     */
    private static final Map<String, List<PriceEntry>> priceLists = new HashMap<>();

    /**
     * CustomLookup – w razie potrzeby, choć dane można przenieść do PriceEntry.
     */
    private static final Map<String, Map<String, String>> customLookup = new HashMap<>();

    private static String activeProfile = "default";

    /**
     * Ustawia aktywny profil – jeśli nie istnieje, tworzy nową listę wpisów.
     */
    public static void setActiveProfile(String profile) {
        activeProfile = profile;
        priceLists.computeIfAbsent(profile, k -> new ArrayList<>());
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
     * Dodaje lub ustawia wpis (name, lore, material, enchants, maxPrice) w aktywnym profilu.
     */
    public static void addPriceEntry(PriceEntry entry) {
        String compositeKey = CompositeKeyUtil.getCompositeKeyFromEntry(entry);

        List<PriceEntry> entries = priceLists.computeIfAbsent(activeProfile, k -> new ArrayList<>());

        entries.removeIf(pe -> {
            String keyFromEntry = CompositeKeyUtil.getCompositeKeyFromEntry(pe);
            return keyFromEntry.equals(compositeKey);
        });

        entries.add(entry);
    }

    public static void addPriceEntry(String rawItem, double maxPrice) {
        String compositeKey = CompositeKeyUtil.createCompositeKey(rawItem);
        String[] parts = compositeKey.split("\\|", -1);
        if (parts.length < 3) {
            return;
        }
        PriceEntry newEntry = new PriceEntry();
        newEntry.name = parts[0];
        newEntry.lore = parts[1];
        newEntry.material = parts[2];
        newEntry.enchants = parts.length > 3 ? parts[3] : "";
        newEntry.maxPrice = maxPrice;
        addPriceEntry(newEntry);
    }

    /**
     * Usuwa wpis z aktywnego profilu na podstawie rawItem.
     */
    public static void removePriceEntry(String rawItem) {
        String compositeKey = CompositeKeyUtil.createCompositeKey(rawItem);
        List<PriceEntry> entries = priceLists.get(activeProfile);
        if (entries != null) {
            entries.removeIf(pe -> {
                String keyFromEntry = (pe.name + "|" +
                        (pe.lore == null ? "" : pe.lore) + "|" +
                        (pe.material == null ? "" : pe.material) + "|" +
                        (pe.enchants == null ? "" : pe.enchants)).toLowerCase();
                return keyFromEntry.equals(compositeKey);
            });
        }
    }

    /**
     * Wyszukuje wpis PriceEntry, który pasuje do przekazanych parametrów (name, lore, material).
     * Jeśli chcesz uwzględnić enchanty, zmodyfikuj logikę porównania.
     */
    public static PriceEntry findMatchingPriceEntry(String noColorName, List<String> loreLines, String materialId, String enchantments) {
        List<PriceEntry> entries = priceLists.get(activeProfile);
        if (entries == null) return null;
        for (PriceEntry pe : entries) {
            if (pe.material != null && !pe.material.isEmpty()) {
                if (!materialId.equalsIgnoreCase(pe.material)) {
                    continue;
                }
            }
            if (!pe.name.isEmpty()) {
                String lowerName = noColorName.toLowerCase();
                String lowerMaterial = materialId.toLowerCase();
                String lowerEntryName = pe.name.toLowerCase();
                if (!lowerName.contains(lowerEntryName) && !lowerMaterial.contains(lowerEntryName)) {
                    continue;
                }
            }
            if (pe.lore != null && !pe.lore.isEmpty()) {
                boolean foundLore = false;
                for (String line : loreLines) {
                    if (line.toLowerCase().contains(pe.lore.toLowerCase())) {
                        foundLore = true;
                        break;
                    }
                }
                if (!foundLore) {
                    continue;
                }
            }
            if (pe.enchants != null && !pe.enchants.isEmpty()) {
                if (enchantments == null || enchantments.isEmpty() ||
                        !enchantments.toLowerCase().contains(pe.enchants.toLowerCase())) {
                    continue;
                }
            }
            return pe;
        }
        return null;
    }



    /**
     * Daje dostęp do wszystkich profili (przydatne np. do zapisywania w configu).
     */
    public static Map<String, List<PriceEntry>> getAllProfiles() {
        return priceLists;
    }

    /**
     * Czyści wszystko i ustawia domyślny profil.
     */
    public static void clearAllProfiles() {
        priceLists.clear();
        customLookup.clear();
        activeProfile = "default";
    }
}
