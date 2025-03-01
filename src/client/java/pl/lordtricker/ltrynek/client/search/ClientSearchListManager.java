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

    /**
     * Lista wyszukiwanych przedmiotów przechowująca *kompozytowe klucze*:
     * w formacie "baseName|lore|material".
     */
    private static final List<String> searchList = new ArrayList<>();

    /**
     * Mapa statystyk (cena minimalna, maksymalna, średnia, mediana, itp.) przypisanych do
     * poszczególnych kluczy kompozytowych.
     */
    private static final Map<String, Stats> statsMap = new HashMap<>();

    /**
     * Flaga informująca, czy wyszukiwanie (skan) jest aktywne.
     */
    private static boolean searchActive = false;

    /**
     * Timer, który zatrzymuje wyszukiwanie po określonym czasie (np. 5 minut).
     */
    private static Timer searchTimer = null;

    /**
     * Zbiór kluczy (np. unikalnych ID aukcji), które zostały już zliczone w danej sesji,
     * aby nie zliczać ich ponownie.
     */
    private static final Set<String> alreadyCountedSession = new HashSet<>();

    /**
     * Dodaje nowy przedmiot do listy wyszukiwania. Parametr {@code rawItem} jest
     * przetwarzany na klucz kompozytowy (np. "bazowaNazwa|lore|material").
     *
     * @param rawItem surowy opis przedmiotu (nazwa, lore, materiał) do przetworzenia.
     */
    public static void addItem(String rawItem) {
        String compositeKey = createCompositeKey(rawItem);
        if (!searchList.contains(compositeKey)) {
            searchList.add(compositeKey);
            statsMap.put(compositeKey, new Stats());
        }
    }

    /**
     * Usuwa przedmiot z listy wyszukiwania na podstawie klucza kompozytowego.
     *
     * @param rawItem surowy opis przedmiotu, który zostanie przetworzony na klucz kompozytowy.
     */
    public static void removeItem(String rawItem) {
        String compositeKey = createCompositeKey(rawItem);
        searchList.remove(compositeKey);
        statsMap.remove(compositeKey);
    }

    /**
     * Zwraca listę wszystkich kluczy kompozytowych, które obecnie są wyszukiwane.
     */
    public static List<String> getSearchList() {
        return searchList;
    }

    /**
     * Uruchamia tryb wyszukiwania (skanowania). Resetuje statystyki wszystkich
     * przedmiotów i ustawia timer, który wyłączy skanowanie po 5 minutach.
     */
    public static void startSearch() {
        searchActive = true;

        // Resetujemy statystyki dla wszystkich przedmiotów
        for (String key : searchList) {
            statsMap.put(key, new Stats());
        }

        // Czyścimy zbiór już zliczonych kluczy aukcji
        alreadyCountedSession.clear();

        // Jeżeli timer już istnieje, anulujemy go
        if (searchTimer != null) {
            searchTimer.cancel();
        }

        // Ustawiamy nowy timer na 5 minut
        searchTimer = new Timer();
        searchTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                stopSearch();
                // Komunikat o wygaśnięciu wyszukiwania
                MinecraftClient.getInstance().execute(() -> {
                    if (MinecraftClient.getInstance().player != null) {
                        MinecraftClient.getInstance().player.sendMessage(
                                Text.of(Messages.get("command.searchlist.expired")),
                                false
                        );
                    }
                });
            }
        }, 300_000); // 5 minut = 300000 ms
    }

    /**
     * Zatrzymuje aktywne wyszukiwanie (skanowanie) i czyści timer.
     */
    public static void stopSearch() {
        searchActive = false;
        if (searchTimer != null) {
            searchTimer.cancel();
            searchTimer = null;
        }
    }

    /**
     * Sprawdza, czy skanowanie jest aktywne.
     */
    public static boolean isSearchActive() {
        return searchActive;
    }

    /**
     * Sprawdza, czy dany unikalny klucz (np. ID aukcji) został już zliczony w tej sesji
     * i nie powinien być przetwarzany ponownie.
     *
     * @param key unikalny klucz aukcji
     * @return true, jeśli klucz był już zliczony
     */
    public static boolean isAlreadyCounted(String key) {
        return alreadyCountedSession.contains(key);
    }

    /**
     * Oznacza klucz (np. ID aukcji) jako zliczony w tej sesji.
     *
     * @param key unikalny klucz aukcji
     */
    public static void markAsCounted(String key) {
        alreadyCountedSession.add(key);
    }

    /**
     * Aktualizuje statystyki (cena/sztuka, ilość) dla podanego przedmiotu.
     * Zakładamy, że parametrem jest surowy opis, który sam w sobie stanowi
     * *już wygenerowany* klucz kompozytowy lub wymaga konwersji.
     *
     * @param rawItem   opis przedmiotu (lub klucz kompozytowy)
     * @param unitPrice cena za sztukę
     * @param quantity  liczba sztuk
     */
    public static void updateStats(String rawItem, double unitPrice, int quantity) {
        // Jeżeli rawItem to np. "baseName|lore|material", można go użyć bezpośrednio;
        // w razie potrzeby można też wywołać createCompositeKey(rawItem).
        String compositeKey = rawItem.toLowerCase();
        Stats s = statsMap.get(compositeKey);
        if (s == null) {
            s = new Stats();
            statsMap.put(compositeKey, s);
        }
        s.update(unitPrice, quantity);
    }

    /**
     * Zwraca obiekt statystyk powiązany z danym przedmiotem (kluczem kompozytowym).
     *
     * @param rawItem klucz kompozytowy (lub surowy opis, jeśli w `updateStats` robimy toLowerCase)
     * @return obiekt {@link Stats}, lub {@code null} jeśli nie istnieje w mapie
     */
    public static Stats getStats(String rawItem) {
        return statsMap.get(rawItem.toLowerCase());
    }

    /**
     * Zwraca mapę wszystkich statystyk.
     */
    public static Map<String, Stats> getAllStats() {
        return statsMap;
    }

    /**
     * Metoda pomocnicza sprawdzająca, czy zeskanowany item pasuje
     * do danego klucza kompozytowego z listy.
     *
     * @param compositeKey  klucz kompozytowy w formacie "baseName|lore|material"
     * @param noColorName   nazwa przedmiotu (bez znaków kolorów), np. "Diamentowy Miecz"
     * @param loreLines     linie lore (bez kolorów) w liście
     * @param materialId    identyfikator materiału (np. "DIAMOND_SWORD")
     * @return true, jeśli wszystkie części klucza (baseName, lore, material) pasują
     */
    public static boolean matchesSearchTerm(
            String compositeKey,
            String noColorName,
            List<String> loreLines,
            String materialId
    ) {
        String[] parts = compositeKey.split("\\|", -1);
        String baseName = parts.length > 0 ? parts[0] : "";
        String lore     = parts.length > 1 ? parts[1] : "";
        String material = parts.length > 2 ? parts[2] : "";

        boolean nameMatches = noColorName.toLowerCase().contains(baseName.toLowerCase());

        // Jeśli lore w kluczu jest puste, dopasowujemy wszystko.
        // Jeśli nie jest puste, sprawdzamy czy istnieje w którejś z linii lore.
        boolean loreMatches = true;
        if (!lore.isEmpty()) {
            loreMatches = false;
            for (String line : loreLines) {
                if (line.toLowerCase().contains(lore.toLowerCase())) {
                    loreMatches = true;
                    break;
                }
            }
        }

        // Jeśli material w kluczu jest pusty, dopasowujemy wszystko.
        // Jeśli nie jest, sprawdzamy czy nazwa materiału się zgadza.
        boolean materialMatches = true;
        if (!material.isEmpty()) {
            materialMatches = materialId.equalsIgnoreCase(material);
        }

        return nameMatches && loreMatches && materialMatches;
    }

    /**
     * Klasa przechowuje statystyki dotyczące cen: ilość przedmiotów, suma cen,
     * minimalna i maksymalna cena, a także listę wszystkich wartości potrzebnych
     * do obliczenia mediany i kwartylów.
     */
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

        /**
         * Aktualizuje statystyki o kolejną ofertę (cena za sztukę + liczba sztuk).
         */
        public void update(double unitPrice, int quantity) {
            count += quantity;
            sum += unitPrice * quantity;

            // Aktualizacja minimum i maksimum
            if (unitPrice < min) {
                min = unitPrice;
            }
            if (unitPrice > max) {
                max = unitPrice;
            }

            // Dodajemy każdą sztukę, by móc potem liczyć medianę, kwartyle itp.
            for (int i = 0; i < quantity; i++) {
                values.add(unitPrice);
            }
        }

        /**
         * Zwraca łączną liczbę "sztuk" (zsumowana ilość).
         */
        public int getCount() {
            return count;
        }

        /**
         * Zwraca średnią cenę.
         */
        public double getAverage() {
            return (count == 0) ? 0 : sum / count;
        }

        /**
         * Zwraca minimalną cenę (lub 0, jeśli nie dodano żadnej oferty).
         */
        public double getMin() {
            return (count == 0) ? 0 : min;
        }

        /**
         * Zwraca maksymalną cenę (lub 0, jeśli nie dodano żadnej oferty).
         */
        public double getMax() {
            return (count == 0) ? 0 : max;
        }

        /**
         * Zwraca medianę (wartość środkową) wszystkich dodanych cen.
         */
        public double getMedian() {
            if (values.isEmpty()) return 0;
            List<Double> sorted = new ArrayList<>(values);
            Collections.sort(sorted);
            int n = sorted.size();
            if (n % 2 == 1) {
                // liczba nieparzysta - wartość środkowa
                return sorted.get(n / 2);
            } else {
                // liczba parzysta - średnia z dwóch środkowych wartości
                return (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
            }
        }

        /**
         * Zwraca dolny kwartyl (Q1).
         */
        public double getQuartile1() {
            if (values.isEmpty()) return 0;
            List<Double> sorted = new ArrayList<>(values);
            Collections.sort(sorted);
            int n = sorted.size();

            // dolna połowa: od 0 do n/2
            List<Double> lowerHalf = sorted.subList(0, n / 2);
            return median(lowerHalf);
        }

        /**
         * Zwraca górny kwartyl (Q3).
         */
        public double getQuartile3() {
            if (values.isEmpty()) return 0;
            List<Double> sorted = new ArrayList<>(values);
            Collections.sort(sorted);
            int n = sorted.size();

            // górna połowa: jeśli n jest parzyste, od n/2 do końca, jeśli nie - od (n/2 + 1)
            List<Double> upperHalf = (n % 2 == 0)
                    ? sorted.subList(n / 2, n)
                    : sorted.subList((n / 2) + 1, n);

            return median(upperHalf);
        }

        /**
         * Metoda pomocnicza do liczenia mediany z dowolnej listy double.
         */
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