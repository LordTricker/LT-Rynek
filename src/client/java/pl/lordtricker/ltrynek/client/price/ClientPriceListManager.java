package pl.lordtricker.ltrynek.client.price;

import pl.lordtricker.ltrynek.client.config.PriceEntry;
import pl.lordtricker.ltrynek.client.util.ColorStripUtils;
import pl.lordtricker.ltrynek.client.util.CompositeKeyUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Klasa zarządzająca listami cen w oparciu o profile i obiekty typu PriceEntry.
 * Każdy profil przechowuje listę PriceEntry (nazwa, lore, materiał, maxPrice).
 */
public class ClientPriceListManager {

    /**
     * Struktura: nazwa_profilu -> lista wpisów typu PriceEntry.
     * Każdy PriceEntry zawiera: name, lore, material, maxPrice.
     */
    private static final Map<String, List<PriceEntry>> priceLists = new HashMap<>();

    /**
     * Struktura do rozpoznawania aliasów dla custom items:
     * Dla każdego profilu mamy mapę: (material + "|" + noColorName) -> alias.
     */
    private static final Map<String, Map<String, String>> customLookup = new HashMap<>();

    /**
     * Aktualnie aktywny profil (domyślnie "default").
     */
    private static String activeProfile = "default";

    /**
     * Ustawia aktywny profil – jeśli nie istnieje, tworzy nowy wpis na liście (Map).
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
     * Zwraca listę wszystkich dostępnych profili.
     */
    public static String listProfiles() {
        if (priceLists.isEmpty()) {
            return "No profiles defined.";
        }
        return String.join(", ", priceLists.keySet());
    }

    /**
     * Dodaje lub ustawia wpis (nazwa, lore, materiał, maxPrice) w aktywnym profilu.
     * Wewnątrz wykorzystuje klucz kompozytowy, aby uniknąć duplikatów.
     *
     * @param rawItem  Surowy opis przedmiotu, np. "Diamentowy Miecz|Miecz Boga|DIAMOND_SWORD".
     * @param maxPrice Maksymalna cena do przypisania w PriceEntry.
     */
    public static void addPriceEntry(String rawItem, double maxPrice) {
        // Tworzymy klucz kompozytowy np. "baseName|lore|material"
        String compositeKey = CompositeKeyUtil.createCompositeKey(rawItem).toLowerCase();
        String[] parts = compositeKey.split("\\|", -1);

        // Zapełniamy obiekt PriceEntry danymi
        PriceEntry newEntry = new PriceEntry();
        newEntry.name = parts[0];       // baseName
        newEntry.lore = parts[1];       // lore
        newEntry.material = parts[2];   // material
        newEntry.maxPrice = maxPrice;

        // Pobieramy (lub tworzymy) listę wpisów dla aktywnego profilu
        List<PriceEntry> entries = priceLists.computeIfAbsent(activeProfile, k -> new ArrayList<>());

        // Usuwamy istniejący wpis o tym samym composite key, by nie duplikować
        entries.removeIf(pe -> {
            String existingKey = (
                    (pe.name == null ? "" : pe.name) + "|" +
                            (pe.lore == null ? "" : pe.lore) + "|" +
                            (pe.material == null ? "" : pe.material)
            ).toLowerCase();
            return existingKey.equals(compositeKey);
        });

        // Dodajemy nowy wpis
        entries.add(newEntry);
    }

    /**
     * Usuwa wpis z aktywnego profilu na podstawie surowego opisu (rawItem).
     */
    public static void removePriceEntry(String rawItem) {
        String compositeKey = CompositeKeyUtil.createCompositeKey(rawItem).toLowerCase();
        List<PriceEntry> entries = priceLists.get(activeProfile);
        if (entries != null) {
            entries.removeIf(pe -> {
                String existingKey = (
                        (pe.name == null ? "" : pe.name) + "|" +
                                (pe.lore == null ? "" : pe.lore) + "|" +
                                (pe.material == null ? "" : pe.material)
                ).toLowerCase();
                return existingKey.equals(compositeKey);
            });
        }
    }

    /**
     * Zwraca sformatowaną listę wszystkich wpisów (PriceEntry) z aktywnego profilu.
     * Format każdej linii: "maxPrice name(lore)[material]"
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
     * Rejestruje custom item w danym profilu (lub aktywnym), przypisując alias
     * do klucza zbudowanego na bazie (material + "|" + noColorName).
     */
    public static void registerCustomItem(String profile, String material, String noColorName, String alias) {
        customLookup.computeIfAbsent(profile, k -> new HashMap<>())
                .put(material + "|" + noColorName, alias);
    }

    /**
     * Znajduje alias w aktywnym profilu na podstawie (material + "|" + noColorName).
     * Zwraca alias lub null, jeśli nie znaleziono.
     */
    public static String findAlias(String material, String noColorName) {
        Map<String, String> lookup = customLookup.get(activeProfile);
        if (lookup == null) return null;
        return lookup.get(material + "|" + noColorName);
    }

    /**
     * Szuka pasującego wpisu (PriceEntry) w aktywnym profilu na podstawie:
     * - nazwy bez kolorów (noColorName)
     * - listy linii lore (loreLines)
     * - identyfikatora materiału (materialId)
     * Zwraca pierwszy pasujący wpis lub null, jeśli nie znaleziono.
     */
    public static PriceEntry findMatchingPriceEntry(String noColorName, List<String> loreLines, String materialId) {
        List<PriceEntry> entries = priceLists.get(activeProfile);
        if (entries == null) {
            return null;
        }
        String lowerNoColorName = noColorName.toLowerCase();

        for (PriceEntry pe : entries) {
            // Materiał (jeśli w PriceEntry jest ustawiony)
            if (pe.material != null && !pe.material.isEmpty()) {
                if (!materialId.equalsIgnoreCase(pe.material)) {
                    continue;
                }
            }

            // Nazwa
            if (!lowerNoColorName.contains((pe.name == null ? "" : pe.name).toLowerCase())) {
                continue;
            }

            // Lore (jeśli w PriceEntry jest ustawiony)
            if (pe.lore != null && !pe.lore.isEmpty()) {
                boolean foundLore = false;
                for (String loreLine : loreLines) {
                    if (loreLine.toLowerCase().contains(pe.lore.toLowerCase())) {
                        foundLore = true;
                        break;
                    }
                }
                if (!foundLore) {
                    continue;
                }
            }
            // Jeśli wszystko pasuje, zwracamy ten wpis
            return pe;
        }
        return null;
    }

    /**
     * Pobiera cenę (maxPrice) dla danego itemName (alias lub ID), o ile
     * przechowujesz jeszcze w starej formie. Jeśli wolisz, możesz zaadaptować
     * do nowego systemu (z PriceEntry). Możesz tę metodę zostawić
     * lub usunąć, w zależności od potrzeb.
     *
     * Metoda jest zachowana z poprzedniego kodu; teraz bardziej sensowne jest
     * korzystanie z findMatchingPriceEntry, ale jeśli masz dużo istniejących
     * odwołań do getMaxPrice, możesz ją zaadaptować do PriceEntry.
     *
     * @param itemName alias lub ID (ze starej logiki).
     * @return -1 jeśli brak wpisu; w innym wypadku zwróci zdefiniowaną cenę.
     */
    public static double getMaxPrice(String itemName) {
        // Możesz spróbować wyszukać wpis w priceLists na podstawie nazwy
        List<PriceEntry> entries = priceLists.get(activeProfile);
        if (entries == null) return -1;

        String lowerItem = itemName.toLowerCase();

        for (PriceEntry pe : entries) {
            // Tworzymy coś w rodzaju "composite key" z aktualnego wpisu
            String fullKey = (pe.name + "|" +
                    (pe.lore == null ? "" : pe.lore) + "|" +
                    (pe.material == null ? "" : pe.material)).toLowerCase();

            // Jeśli "itemName" odpowiada temu fullKey
            // lub np. nazwa (pe.name) jest tożsama – zależnie od tego,
            // jak chcesz mapować stare nazwy na nowe PriceEntry.
            // Przykładowo: wystarczy, że lowerItem zawiera pe.name:
            if (lowerItem.contains(pe.name.toLowerCase())) {
                return pe.maxPrice;
            }
            // Ewentualnie sprawdzaj inaczej, np. equalsIgnoreCase
            if (fullKey.equals(lowerItem)) {
                return pe.maxPrice;
            }
        }
        return -1;
    }

    /**
     * Usuwa kody kolorów i formatowanie z podanego ciągu, delegując do ColorStripUtils.
     */
    public static String stripColorsAdvanced(String input) {
        return ColorStripUtils.stripAllColorsAndFormats(input);
    }

    /**
     * Zwraca całą strukturę profilów z listami PriceEntry (przydatne np. do zapisu w configu).
     */
    public static Map<String, List<PriceEntry>> getAllProfiles() {
        return priceLists;
    }

    /**
     * Czyści wszystkie profile (łącznie z aliasami) i resetuje aktywny profil do "default".
     */
    public static void clearAllProfiles() {
        priceLists.clear();
        customLookup.clear();
        activeProfile = "default";
    }
}