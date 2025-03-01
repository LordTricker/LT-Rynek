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
    public static void addPriceEntry(String rawItem, double maxPrice) {
        // Tworzymy klucz kompozytowy z rawItem (np. "minecraft:netherite_sword")
        String compositeKey = CompositeKeyUtil.createCompositeKey(rawItem);

        // Rozbijamy na części: name|lore|material
        String[] parts = compositeKey.split("\\|", -1);
        if (parts.length < 3) {
            // Jeśli coś jest nie tak z parsowaniem, wyjdź
            return;
        }

        // Jeśli wszystko puste (np. "|||"), nie dodajemy
        if (parts[0].isEmpty() && parts[1].isEmpty() && parts[2].isEmpty()) {
            return;
        }

        PriceEntry newEntry = new PriceEntry();
        newEntry.name = parts[0];
        newEntry.lore = parts[1];
        newEntry.material = parts[2];
        newEntry.maxPrice = maxPrice;

        // Pobieramy listę wpisów dla aktywnego profilu
        List<PriceEntry> entries = priceLists.computeIfAbsent(activeProfile, k -> new ArrayList<>());

        // Usuwamy istniejący wpis o tym samym kluczu kompozytowym (baseName|lore|material)
        entries.removeIf(pe -> {
            String keyFromEntry = (pe.name + "|" +
                    (pe.lore == null ? "" : pe.lore) + "|" +
                    (pe.material == null ? "" : pe.material)).toLowerCase();
            return keyFromEntry.equals(compositeKey);
        });

        // Dodajemy nowy wpis
        entries.add(newEntry);

        // DEBUG: wypisujemy do konsoli, co mamy w pamięci
        System.out.println("[ClientPriceListManager] After addPriceEntry('" + rawItem + "', " + maxPrice + ")");
        for (PriceEntry pe : entries) {
            System.out.println(" -> name='" + pe.name + "', lore='" + pe.lore + "', material='" + pe.material + "', maxPrice=" + pe.maxPrice);
        }
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

            // DEBUG: wypisujemy do konsoli, co mamy w pamięci
            System.out.println("[ClientPriceListManager] After removePriceEntry('" + rawItem + "')");
            for (PriceEntry pe : entries) {
                System.out.println(" -> name='" + pe.name + "', lore='" + pe.lore + "', material='" + pe.material + "', maxPrice=" + pe.maxPrice);
            }
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
            // np. "500000.0 " + "" + "(lore)" + "[minecraft:netherite_sword]"
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
            // 1) Jeśli w PriceEntry jest materiał, sprawdzamy czy pasuje do materialId
            if (pe.material != null && !pe.material.isEmpty()) {
                if (!materialId.equalsIgnoreCase(pe.material)) {
                    continue;
                }
            }
            // 2) Nazwa: sprawdzamy, czy noColorName zawiera pe.name (o ile name nie jest puste)
            if (!pe.name.isEmpty()) {
                if (!noColorName.toLowerCase().contains(pe.name.toLowerCase())) {
                    continue;
                }
            }
            // 3) Lore: jeśli pe.lore nie jest puste, sprawdzamy, czy przynajmniej jedna linia je zawiera
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
            // Jeśli wszystkie warunki spełnione, zwracamy ten wpis
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