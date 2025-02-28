package pl.lordtricker.ltrynek.client.price;

import pl.lordtricker.ltrynek.client.config.PriceEntry;
import pl.lordtricker.ltrynek.client.util.ColorStripUtils;
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
     * Metoda przyjmuje rawItem w formacie:
     *    BazowaNazwa("Lore tekst")["material"]
     * Jeśli wpis zawiera dodatkowe informacje, zostaną one wyekstrahowane – w przeciwnym razie pola lore i materiał pozostaną puste.
     */
    public static void addPriceEntry(String rawItem, double maxPrice) {
        String compositeKey = CompositeKeyUtil.createCompositeKey(rawItem);
        String[] parts = compositeKey.split("\\|", -1);
        PriceEntry newEntry = new PriceEntry();
        newEntry.name = parts[0];
        newEntry.lore = parts[1];
        newEntry.material = parts[2];
        newEntry.maxPrice = maxPrice;

        List<PriceEntry> entries = priceLists.computeIfAbsent(activeProfile, k -> new ArrayList<>());
        // Usuwamy istniejący wpis o tym samym composite key (czyli te same baseName, lore i material)
        entries.removeIf(pe -> {
            String keyFromEntry = (pe.name + "|" + (pe.lore == null ? "" : pe.lore) + "|" + (pe.material == null ? "" : pe.material)).toLowerCase();
            return keyFromEntry.equals(compositeKey);
        });
        entries.add(newEntry);
    }

    /**
     * Usuwa wpis z aktywnego profilu na podstawie rawItem.
     */
    public static void removePriceEntry(String rawItem) {
        String compositeKey = CompositeKeyUtil.createCompositeKey(rawItem);
        List<PriceEntry> entries = priceLists.get(activeProfile);
        if (entries != null) {
            entries.removeIf(pe -> {
                String keyFromEntry = (pe.name + "|" + (pe.lore == null ? "" : pe.lore) + "|" + (pe.material == null ? "" : pe.material)).toLowerCase();
                return keyFromEntry.equals(compositeKey);
            });
        }
    }

    /**
     * Zwraca sformatowaną listę przedmiotów (name i maxPrice) z aktywnego profilu.
     * Format: "maxPrice name", każdy wpis w osobnej linii.
     */
    public static String getPriceListAsString() {
        List<PriceEntry> entries = priceLists.get(activeProfile);
        if (entries == null || entries.isEmpty()) {
            return "No items in profile " + activeProfile;
        }
        StringBuilder sb = new StringBuilder();
        for (PriceEntry pe : entries) {
            sb.append(pe.maxPrice)
                    .append(" ")
                    .append(pe.name);
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
     * Znajduje alias w aktywnym profilu na podstawie materialu i noColorName (jeśli wciąż potrzebne).
     */
    public static String findAlias(String material, String noColorName) {
        Map<String, String> lookup = customLookup.get(activeProfile);
        if (lookup == null) return null;
        return lookup.get(material + "|" + noColorName);
    }

    /**
     * Rejestruje custom item w aktywnym profilu – buduje klucz (material + "|" + noColorName) -> alias.
     */
    public static void registerCustomItem(String profile, String material, String noColorName, String alias) {
        customLookup.computeIfAbsent(profile, k -> new HashMap<>())
                .put(material + "|" + noColorName, alias);
    }

    /**
     * Wyszukuje wpis PriceEntry, który pasuje do przekazanych parametrów (nazwa, lore, materiał).
     * Zwraca pierwszy pasujący wpis lub null, jeśli żaden nie pasuje.
     */
    public static PriceEntry findMatchingPriceEntry(String noColorName, List<String> loreLines, String materialId) {
        List<PriceEntry> entries = priceLists.get(activeProfile);
        if (entries == null) return null;

        for (PriceEntry pe : entries) {
            // 1) Dopasowanie materiału (jeśli pe.material nie jest puste)
            if (pe.material != null && !pe.material.isEmpty()) {
                if (!materialId.equalsIgnoreCase(pe.material)) {
                    continue;
                }
            }
            // 2) Dopasowanie nazwy – zakładamy, że noColorName zawiera pe.name
            if (!noColorName.toLowerCase().contains(pe.name.toLowerCase())) {
                continue;
            }
            // 3) Dopasowanie lore (jeśli pe.lore nie jest puste)
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
     * Usuwa kody kolorów i formatowanie z podanego ciągu, delegując do ColorStripUtils.
     */
    public static String stripColorsAdvanced(String input) {
        return ColorStripUtils.stripAllColorsAndFormats(input);
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
