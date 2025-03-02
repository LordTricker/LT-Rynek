package pl.lordtricker.ltrynek.client.price;

import pl.lordtricker.ltrynek.client.config.PriceEntry;
import pl.lordtricker.ltrynek.client.util.CompositeKeyUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientPriceListManager {

    /**
     * Struktura: profile -> lista wpisów typu PriceEntry.
     * Każdy wpis zawiera: name, lore, material, maxPrice.
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
     * Dodaje lub ustawia wpis (nazwa, lore, materiał, maxPrice) w aktywnym profilu.
     */

    public static void addPriceEntry(PriceEntry entry) {
        String compositeKey = (entry.name + "|" +
                (entry.lore == null ? "" : entry.lore) + "|" +
                (entry.material == null ? "" : entry.material)).toLowerCase();

        List<PriceEntry> entries = priceLists.computeIfAbsent(activeProfile, k -> new ArrayList<>());

        entries.removeIf(pe -> {
            String keyFromEntry = (pe.name + "|" +
                    (pe.lore == null ? "" : pe.lore) + "|" +
                    (pe.material == null ? "" : pe.material)).toLowerCase();
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

        if (parts[0].isEmpty() && parts[1].isEmpty() && parts[2].isEmpty()) {
            return;
        }

        PriceEntry newEntry = new PriceEntry();
        newEntry.name = parts[0];
        newEntry.lore = parts[1];
        newEntry.material = parts[2];
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
                        (pe.material == null ? "" : pe.material)).toLowerCase();
                return keyFromEntry.equals(compositeKey);
            });
        }
    }

    /**
     * Zwraca sformatowaną listę przedmiotów (maxPrice + nazwa) z aktywnego profilu.
     * Format: "maxPrice name(lore)[material]" w każdej linii.
     */
    public static String getPriceListAsString() {
        List<PriceEntry> entries = priceLists.get(activeProfile);
        if (entries == null || entries.isEmpty()) {
            return "No items in profile " + activeProfile;
        }
        StringBuilder sb = new StringBuilder();
        for (PriceEntry pe : entries) {
            sb.append(pe.maxPrice).append(" ").append(pe.name);
            if (pe.lore != null && !pe.lore.isEmpty()) {
                sb.append("(").append(pe.lore).append(")");
            }
            if (pe.material != null && !pe.material.isEmpty()) {
                sb.append("[").append(pe.material).append("]");
            }
            sb.append("\n");
        }
        return sb.toString().trim();
    }

    /**
     * Wyszukuje wpis PriceEntry, który pasuje do przekazanych parametrów (nazwa, lore, materiał).
     * Zwraca pierwszy pasujący wpis lub null, jeśli żaden nie pasuje.
     */
    public static PriceEntry findMatchingPriceEntry(String noColorName, List<String> loreLines, String materialId) {
        List<PriceEntry> entries = priceLists.get(activeProfile);
        if (entries == null) return null;

        for (PriceEntry pe : entries) {
            if (pe.material != null && !pe.material.isEmpty()) {
                if (!materialId.equalsIgnoreCase(pe.material)) {
                    continue;
                }
            }
            if (!pe.name.isEmpty()) {
                if (!noColorName.toLowerCase().contains(pe.name.toLowerCase())) {
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
