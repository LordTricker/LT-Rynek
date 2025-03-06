<<<<<<<< HEAD:src/main/java/pl/lordtricker/ltrynek/core/manager/ClientPriceListManager.java
package pl.lordtricker.ltrynek.core.manager;
========
package pl.lordtricker.ltrynek.client.manager;
>>>>>>>> 86d6843 (Big update to version 1.4.0):src/client/java/pl/lordtricker/ltrynek/client/manager/ClientPriceListManager.java

import pl.lordtricker.ltrynek.core.config.PriceEntry;
import pl.lordtricker.ltrynek.core.util.CompositeKeyUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientPriceListManager {

<<<<<<<< HEAD:src/main/java/pl/lordtricker/ltrynek/core/manager/ClientPriceListManager.java
========
    /**
     * Struktura: profile -> lista wpisów typu PriceEntry.
     * Każdy wpis zawiera: name, lore, material, enchants, maxPrice.
     */
>>>>>>>> 86d6843 (Big update to version 1.4.0):src/client/java/pl/lordtricker/ltrynek/client/manager/ClientPriceListManager.java
    private static final Map<String, List<PriceEntry>> priceLists = new HashMap<>();

    private static final Map<String, Map<String, String>> customLookup = new HashMap<>();

    private static String activeProfile = "default";

    public static void setActiveProfile(String profile) {
        activeProfile = profile;
        priceLists.computeIfAbsent(profile, k -> new ArrayList<>());
        customLookup.computeIfAbsent(profile, k -> new HashMap<>());
    }

    public static String getActiveProfile() {
        return activeProfile;
    }

    public static String listProfiles() {
        if (priceLists.isEmpty()) {
            return "No profiles defined.";
        }
        return String.join(", ", priceLists.keySet());
    }

<<<<<<<< HEAD:src/main/java/pl/lordtricker/ltrynek/core/manager/ClientPriceListManager.java
========
    /**
     * Dodaje lub ustawia wpis (name, lore, material, enchants, maxPrice) w aktywnym profilu.
     */
>>>>>>>> 86d6843 (Big update to version 1.4.0):src/client/java/pl/lordtricker/ltrynek/client/manager/ClientPriceListManager.java
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

<<<<<<<< HEAD:src/main/java/pl/lordtricker/ltrynek/core/manager/ClientPriceListManager.java
    public static PriceEntry findMatchingPriceEntry(String noColorName, List<String> loreLines, String materialId, String enchantments) {
        List<PriceEntry> entries = priceLists.get(activeProfile);
        if (entries == null) return null;

        PriceEntry best = null;
        int bestScore = -1;

        String lowerName = noColorName.toLowerCase();
        String lowerMaterialId = materialId.toLowerCase();
        String lowerEnchantments = enchantments == null ? "" : enchantments.toLowerCase();

========
    /**
     * Wyszukuje wpis PriceEntry, który pasuje do przekazanych parametrów (name, lore, material).
     * Jeśli chcesz uwzględnić enchanty, zmodyfikuj logikę porównania.
     */
    public static PriceEntry findMatchingPriceEntry(String noColorName, List<String> loreLines, String materialId, String enchantments) {
        List<PriceEntry> entries = priceLists.get(activeProfile);
        if (entries == null) return null;
>>>>>>>> 86d6843 (Big update to version 1.4.0):src/client/java/pl/lordtricker/ltrynek/client/manager/ClientPriceListManager.java
        for (PriceEntry pe : entries) {
            int score = 0;

            if (pe.material != null && !pe.material.isEmpty()) {
                if (!materialId.equalsIgnoreCase(pe.material)) {
                    continue;
                }
                score += 1000;
            }

            if (!pe.name.isEmpty()) {
<<<<<<<< HEAD:src/main/java/pl/lordtricker/ltrynek/core/manager/ClientPriceListManager.java
                String lowerEntryName = pe.name.toLowerCase();
                boolean nameMatches = lowerName.contains(lowerEntryName) || lowerMaterialId.contains(lowerEntryName);
                if (!nameMatches) {
========
                String lowerName = noColorName.toLowerCase();
                String lowerMaterial = materialId.toLowerCase();
                String lowerEntryName = pe.name.toLowerCase();
                if (!lowerName.contains(lowerEntryName) && !lowerMaterial.contains(lowerEntryName)) {
>>>>>>>> 86d6843 (Big update to version 1.4.0):src/client/java/pl/lordtricker/ltrynek/client/manager/ClientPriceListManager.java
                    continue;
                }
                score += Math.min(500, lowerEntryName.length());
                if (lowerName.equals(lowerEntryName)) {
                    score += 200;
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
                score += 50;
            }

            if (pe.enchants != null && !pe.enchants.isEmpty()) {
                if (lowerEnchantments.isEmpty() || !lowerEnchantments.contains(pe.enchants.toLowerCase())) {
                    continue;
                }
                score += 25;
            }

            if (score > bestScore) {
                bestScore = score;
                best = pe;
            }
<<<<<<<< HEAD:src/main/java/pl/lordtricker/ltrynek/core/manager/ClientPriceListManager.java
========
            if (pe.enchants != null && !pe.enchants.isEmpty()) {
                if (enchantments == null || enchantments.isEmpty() ||
                        !enchantments.toLowerCase().contains(pe.enchants.toLowerCase())) {
                    continue;
                }
            }
            return pe;
>>>>>>>> 86d6843 (Big update to version 1.4.0):src/client/java/pl/lordtricker/ltrynek/client/manager/ClientPriceListManager.java
        }
        return best;
    }

<<<<<<<< HEAD:src/main/java/pl/lordtricker/ltrynek/core/manager/ClientPriceListManager.java
========


    /**
     * Daje dostęp do wszystkich profili (przydatne np. do zapisywania w configu).
     */
>>>>>>>> 86d6843 (Big update to version 1.4.0):src/client/java/pl/lordtricker/ltrynek/client/manager/ClientPriceListManager.java
    public static Map<String, List<PriceEntry>> getAllProfiles() {
        return priceLists;
    }

    public static void clearAllProfiles() {
        priceLists.clear();
        customLookup.clear();
        activeProfile = "default";
    }
}
