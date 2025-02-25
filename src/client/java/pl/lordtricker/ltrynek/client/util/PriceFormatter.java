package pl.lordtricker.ltrynek.client.util;

public class PriceFormatter {
    public static double parsePrice(String raw) {
        if (raw == null || raw.isEmpty()) {
            return -1;
        }
        raw = raw.trim().replace(',', '.');
        double multiplier = 1.0;
        String lower = raw.toLowerCase();
        if (lower.endsWith("k")) {
            multiplier = 1000.0;
            raw = raw.substring(0, raw.length() - 1);
        } else if (lower.endsWith("mld")) {
            multiplier = 1_000_000_000.0;
            raw = raw.substring(0, raw.length() - 3);
        } else if (lower.endsWith("m")) {
            multiplier = 1_000_000.0;
            raw = raw.substring(0, raw.length() - 1);
        }
        try {
            double base = Double.parseDouble(raw);
            return base * multiplier;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Formatuje liczbę w "krótkim" formacie z przyrostkami k/m/mld:
     * - >= 1_000_000_000 -> "xx.xxmld"
     * - >= 1_000_000 -> "xx.xxm"
     * - >= 1_000 -> "xx.xxk"
     * - < 1_000 -> zwykły format
     *
     * Zwraca np. "1.59k", "12.50m", "1.20mld", "999.00" (zaokrąglone do 2 miejsc).
     * Zmodyfikuj w razie potrzeby (np. usuń .00, itp.).
     */
    public static String formatPrice(double value) {
        double absVal = Math.abs(value);
        String suffix = "";
        if (absVal >= 1_000_000_000) {
            value /= 1_000_000_000;
            suffix = "mld";
        } else if (absVal >= 1_000_000) {
            value /= 1_000_000;
            suffix = "m";
        } else if (absVal >= 1_000) {
            value /= 1_000;
            suffix = "k";
        }

        String formatted = String.format("%.2f", value);
        if (formatted.contains(".")) {
            formatted = formatted.replaceAll("0+$", "").replaceAll("\\.$", "");
        }

        return formatted + suffix;
    }
}
