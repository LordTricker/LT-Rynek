package pl.lordtricker.ltrynek.core.util;

import pl.lordtricker.ltrynek.core.manager.ClientSearchListManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class SearchSummaryBuilder {
    private SearchSummaryBuilder() {}

    public static List<String> buildStopSummary(String reasonMessage, int pagesScanned) {
        List<String> searchItems = ClientSearchListManager.getSearchList();
        if (searchItems.isEmpty()) {
            if (reasonMessage != null && !reasonMessage.isEmpty()) {
                if (pagesScanned >= 0) {
                    String pagesLine = Messages.format("command.searchlist.stop.pages",
                            Map.of("pages", String.valueOf(pagesScanned)));
                    return List.of(reasonMessage, pagesLine, Messages.get("command.searchlist.list.empty"));
                }
                return List.of(reasonMessage, Messages.get("command.searchlist.list.empty"));
            }
            return List.of(Messages.get("command.searchlist.list.empty"));
        }
        List<String> lines = new ArrayList<>();
        if (reasonMessage != null && !reasonMessage.isEmpty()) {
            lines.add(reasonMessage);
        }
        if (pagesScanned >= 0) {
            lines.add(Messages.format("command.searchlist.stop.pages",
                    Map.of("pages", String.valueOf(pagesScanned))));
        }
        lines.add(Messages.get("command.searchlist.stop.header"));
        for (String compositeKey : searchItems) {
            ClientSearchListManager.Stats stats = ClientSearchListManager.getStats(compositeKey);
            if (stats == null || stats.getCount() == 0) continue;
            String lineRaw = Messages.format("command.searchlist.stop.line", Map.of(
                    "item", CompositeKeyUtil.getFriendlyName(compositeKey),
                    "count", String.valueOf(stats.getCount()),
                    "min", PriceFormatter.formatPrice(stats.getMin()),
                    "max", PriceFormatter.formatPrice(stats.getMax()),
                    "avg", PriceFormatter.formatPrice(stats.getAverage()),
                    "median", PriceFormatter.formatPrice(stats.getMedian()),
                    "quartile1", PriceFormatter.formatPrice(stats.getQuartile1()),
                    "quartile3", PriceFormatter.formatPrice(stats.getQuartile3())
            ));
            lines.add(lineRaw);
        }
        return lines;
    }

    public static List<String> buildStopSummary() {
        return buildStopSummary(null, -1);
    }

    public static List<String> buildStopSummary(String reasonMessage) {
        return buildStopSummary(reasonMessage, -1);
    }
}
