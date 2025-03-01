package pl.lordtricker.ltrynek.client.search;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import pl.lordtricker.ltrynek.client.util.Messages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import static pl.lordtricker.ltrynek.client.util.CompositeKeyUtil.createCompositeKey;

public class ClientSearchListManager {
    private static final List<String> searchList = new ArrayList<>();
    private static final Map<String, Stats> statsMap = new HashMap<>();
    private static boolean searchActive = false;
    private static Timer searchTimer = null;
    private static final Set<String> alreadyCountedSession = new HashSet<>();

    /**
     * Dodaje nowy przedmiot do listy wyszukiwania (w formacie bazowaNazwa("Lore")["Material"]).
     */
    public static void addItem(String rawItem) {
        String compositeKey = createCompositeKey(rawItem);
        if (!searchList.contains(compositeKey)) {
            searchList.add(compositeKey);
            statsMap.put(compositeKey, new Stats());
        }
    }

    public static void removeItem(String rawItem) {
        String compositeKey = createCompositeKey(rawItem);
        searchList.remove(compositeKey);
        statsMap.remove(compositeKey);
    }

    public static List<String> getSearchList() {
        return searchList;
    }

    public static void startSearch() {
        searchActive = true;
        for (String key : searchList) {
            statsMap.put(key, new Stats());
        }
        alreadyCountedSession.clear();
        if (searchTimer != null) {
            searchTimer.cancel();
        }
        searchTimer = new Timer();
        searchTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                stopSearch();
                MinecraftClient.getInstance().execute(() -> {
                    if (MinecraftClient.getInstance().player != null) {
                        MinecraftClient.getInstance().player.sendMessage(Text.literal(Messages.get("command.searchlist.expired")), false);
                    }
                });
            }
        }, 300_000); // 5 minut = 300000 ms
    }

    public static void stopSearch() {
        searchActive = false;
        if (searchTimer != null) {
            searchTimer.cancel();
            searchTimer = null;
        }
    }

    public static boolean isSearchActive() {
        return searchActive;
    }

    /**
     * Sprawdza, czy dany unikalny klucz (np. aukcja) został już zliczony w tej sesji.
     */
    public static boolean isAlreadyCounted(String key) {
        return alreadyCountedSession.contains(key);
    }

    public static void markAsCounted(String key) {
        alreadyCountedSession.add(key);
    }

    /**
     * Aktualizuje statystyki (cena/sztuka, ilość) dla podanego compositeKey.
     */
    public static void updateStats(String compositeKey, double unitPrice, int quantity) {
        Stats s = statsMap.get(compositeKey);
        if (s == null) {
            s = new Stats();
            statsMap.put(compositeKey, s);
        }
        s.update(unitPrice, quantity);
    }

    public static Stats getStats(String rawItem) {
        return statsMap.get(rawItem.toLowerCase());
    }

    /**
     * Pomocnicza metoda do sprawdzania, czy zeskanowany item pasuje do compositeKey z listy.
     */
    public static boolean matchesSearchTerm(String compositeKey, String noColorName, List<String> loreLines, String materialId) {
        String[] parts = compositeKey.split("\\|", -1);
        String baseName = parts[0];
        String lore = parts[1];
        String material = parts[2];

        boolean nameMatches = noColorName.toLowerCase().contains(baseName);
        boolean loreMatches = true;
        if (!lore.isEmpty()) {
            loreMatches = false;
            for (String line : loreLines) {
                if (line.toLowerCase().contains(lore)) {
                    loreMatches = true;
                    break;
                }
            }
        }
        boolean materialMatches = true;
        if (!material.isEmpty()) {
            materialMatches = materialId.equalsIgnoreCase(material);
        }
        return nameMatches && loreMatches && materialMatches;
    }

    public static class Stats {
        private int count;
        private double sum;
        private double min;
        private double max;
        private final List<Double> values;

        public Stats() {
            this.count = 0;
            this.sum = 0.0;
            this.min = Double.MAX_VALUE;
            this.max = Double.MIN_VALUE;
            this.values = new ArrayList<>();
        }

        public void update(double unitPrice, int quantity) {
            count += quantity;
            sum += unitPrice * quantity;
            if (unitPrice < min) {
                min = unitPrice;
            }
            if (unitPrice > max) {
                max = unitPrice;
            }
            for (int i = 0; i < quantity; i++) {
                values.add(unitPrice);
            }
        }

        public int getCount() {
            return count;
        }

        public double getAverage() {
            return count == 0 ? 0 : sum / count;
        }

        public double getMin() {
            return count == 0 ? 0 : min;
        }

        public double getMax() {
            return count == 0 ? 0 : max;
        }

        public double getMedian() {
            if (values.isEmpty()) return 0;
            List<Double> sorted = new ArrayList<>(values);
            Collections.sort(sorted);
            int n = sorted.size();
            if (n % 2 == 1) {
                return sorted.get(n / 2);
            } else {
                return (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
            }
        }

        public double getQuartile1() {
            if (values.isEmpty()) return 0;
            List<Double> sorted = new ArrayList<>(values);
            Collections.sort(sorted);
            int n = sorted.size();
            List<Double> lowerHalf = sorted.subList(0, n / 2);
            return median(lowerHalf);
        }

        public double getQuartile3() {
            if (values.isEmpty()) return 0;
            List<Double> sorted = new ArrayList<>(values);
            Collections.sort(sorted);
            int n = sorted.size();
            List<Double> upperHalf = (n % 2 == 0)
                    ? sorted.subList(n / 2, n)
                    : sorted.subList(n / 2 + 1, n);
            return median(upperHalf);
        }

        private double median(List<Double> list) {
            int size = list.size();
            if (size == 0) return 0;
            if (size % 2 == 1) {
                return list.get(size / 2);
            } else {
                return (list.get(size / 2 - 1) + list.get(size / 2)) / 2.0;
            }
        }
    }
}
