package pl.lordtricker.ltrynek.client.search;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import pl.lordtricker.ltrynek.client.util.Messages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class ClientSearchListManager {
    private static final List<String> searchList = new ArrayList<>();

    private static final Map<String, Stats> statsMap = new HashMap<>();
    private static boolean searchActive = false;
    private static Timer searchTimer = null;

    private static final Set<String> alreadyCountedSession = new HashSet<>();

    public static void addItem(String itemName) {
        String key = itemName.toLowerCase();
        if (!searchList.contains(key)) {
            searchList.add(key);
            statsMap.put(key, new Stats());
        }
    }

    public static void removeItem(String itemName) {
        String key = itemName.toLowerCase();
        searchList.remove(key);
        statsMap.remove(key);
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
        }, 300000); // 5 minut = 300000 ms
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
     * Sprawdza, czy dany unikalny klucz już został zliczony w tej sesji.
     */
    public static boolean isAlreadyCounted(String key) {
        return alreadyCountedSession.contains(key);
    }

    /**
     * Oznacza dany klucz jako już zliczony.
     */
    public static void markAsCounted(String key) {
        alreadyCountedSession.add(key);
    }

    /**
     * Aktualizuje statystyki dla danego terminu.
     */
    public static void updateStats(String searchTerm, double unitPrice, int quantity) {
        String key = searchTerm.toLowerCase();
        Stats s = statsMap.get(key);
        if (s == null) {
            s = new Stats();
            statsMap.put(key, s);
        }
        s.update(unitPrice, quantity);
    }

    public static Stats getStats(String searchTerm) {
        return statsMap.get(searchTerm.toLowerCase());
    }

    public static Map<String, Stats> getAllStats() {
        return statsMap;
    }

    public static class Stats {
        private int count;
        private double sum;
        private double min;
        private double max;

        public Stats() {
            this.count = 0;
            this.sum = 0.0;
            this.min = Double.MAX_VALUE;
            this.max = Double.MIN_VALUE;
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
    }
}