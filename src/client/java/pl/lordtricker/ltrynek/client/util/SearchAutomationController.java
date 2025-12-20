package pl.lordtricker.ltrynek.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import pl.lordtricker.ltrynek.client.LtrynekClient;
import pl.lordtricker.ltrynek.client.command.CommandUi;
import pl.lordtricker.ltrynek.client.keybinding.ToggleScanner;
import pl.lordtricker.ltrynek.core.config.ServerEntry;
import pl.lordtricker.ltrynek.core.config.ServersConfig;
import pl.lordtricker.ltrynek.core.manager.ClientSearchListManager;
import pl.lordtricker.ltrynek.core.util.ColorStripUtils;
import pl.lordtricker.ltrynek.core.util.SearchSummaryBuilder;
import pl.lordtricker.ltrynek.core.util.Messages;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SearchAutomationController {
    private static final int DEFAULT_OPEN_DELAY_MS = 1500;
    private static final int DEFAULT_NEXT_DELAY_MS = 800;
    private static final int DEFAULT_CLOSE_DELAY_MS = 500;

    private static final State state = new State();
    private static FinishReason lastFinishReason = null;
    private static int lastPagesScanned = -1;

    private SearchAutomationController() {}

    public static void start(int pagesRequested) {
        if (pagesRequested <= 0) {
            pagesRequested = -1;
        }
        reset();
        state.active = true;
        state.pagesRequested = pagesRequested;
        if (!ToggleScanner.scanningEnabled) {
            ToggleScanner.scanningEnabled = true;
            state.scanEnabledByAutomation = true;
        }
        ClientSearchListManager.startSearch();
        sendNextCommand();
    }

    public static void cancel() {
        lastFinishReason = FinishReason.MANUAL;
        lastPagesScanned = state.pagesScanned;
        if (state.scanEnabledByAutomation) {
            ToggleScanner.scanningEnabled = false;
        }
        reset();
        ClientSearchListManager.stopSearch();
    }

    public static FinishReason consumeLastFinishReason() {
        FinishReason r = lastFinishReason;
        lastFinishReason = null;
        return r;
    }

    public static int consumeLastPagesScanned() {
        int p = lastPagesScanned;
        lastPagesScanned = -1;
        return p;
    }

    public static void onScreenRender(Text title, ScreenHandler handler, List<Slot> slots) {
        if (!state.active) return;
        ServersConfig config = LtrynekClient.serversConfig;
        ServerEntry entry = findServerEntry(config);
        if (entry == null) {
            finish(true, FinishReason.NO_GUI, true);
            return;
        }
        long now = System.currentTimeMillis();
        int pageNumber = extractPageNumber(title);

        if (state.awaitingReopen) {
            state.awaitingReopen = false;
            state.screenClosedAt = 0;
        }

        if (!state.marketOpen) {
            if (titleMatches(entry.marketGuiTitle, title)) {
                state.marketOpen = true;
                if (pageNumber > 0) {
                    updatePageNumbers(pageNumber);
                } else if (state.startPageNumber <= 0) {
                    state.startPageNumber = 1;
                    state.lastPageNumber = -1;
                    state.pagesScanned = Math.max(1, state.pagesScanned);
                }
                if (state.pagesScanned <= 0) {
                    state.pagesScanned = 1;
                }
                if (state.waitingPage) {
                    state.waitingPage = false;
                    state.nextClickAttempts = 0;
                }
                state.lastActionMs = now;
                state.pageSignature = buildPageSignature(slots);
                return;
            }
            int openDelay = getDelay(entry.marketOpenDelayMs, DEFAULT_OPEN_DELAY_MS);
            if (now - state.lastCommandMs >= openDelay) {
                if (!sendNextCommand()) {
                    finish(true, FinishReason.NO_GUI, true);
                }
            }
            return;
        }

        if (state.finishing) {
            int closeDelay = getDelay(entry.marketCloseDelayMs, DEFAULT_CLOSE_DELAY_MS);
            if (now - state.finishStartMs >= closeDelay) {
                finish(true, state.finishReason, true);
            }
            return;
        }

        if (state.waitingPage) {
            int nextDelay = getDelay(entry.marketNextDelayMs, DEFAULT_NEXT_DELAY_MS);
            String signature = buildPageSignature(slots);
            if (updatePageNumbers(pageNumber)) {
                state.pageSignature = signature;
                state.waitingPage = false;
                state.lastActionMs = now;
                state.nextClickAttempts = 0;
                return;
            }
            if (signature != null && !signature.equals(state.pageSignature)) {
                state.pageSignature = signature;
                state.waitingPage = false;
                if (pageNumber <= 0 && !state.pageNumberKnown) {
                    state.pagesScanned++;
                }
                state.lastActionMs = now;
                state.nextClickAttempts = 0;
                return;
            }
            if (now - state.lastActionMs >= nextDelay) {
                if (state.nextClickAttempts >= 3) {
                    state.finishing = true;
                    state.finishReason = FinishReason.NO_NEXT_PAGE;
                    state.finishStartMs = now;
                    return;
                }
                Slot nextSlot = findNextPageSlot(entry, slots);
                if (nextSlot == null) {
                    state.finishing = true;
                    state.finishReason = FinishReason.NO_NEXT_PAGE;
                    state.finishStartMs = now;
                    return;
                }
                clickSlot(handler, nextSlot);
                state.nextClickAttempts++;
                state.lastActionMs = now;
            }
            return;
        }

        if (!state.waitingPage && pageNumber > 0) {
            updatePageNumbers(pageNumber);
        }

        if (state.pagesRequested > 0 && state.pagesScanned >= state.pagesRequested) {
            state.finishing = true;
            state.finishReason = FinishReason.LIMIT_REACHED;
            state.finishStartMs = now;
            return;
        }

        Slot nextSlot = findNextPageSlot(entry, slots);
        if (nextSlot == null) {
            state.finishing = true;
            state.finishReason = FinishReason.NO_NEXT_PAGE;
            state.finishStartMs = now;
            return;
        }

        int nextDelay = getDelay(entry.marketNextDelayMs, DEFAULT_NEXT_DELAY_MS);
        if (now - state.lastActionMs >= nextDelay) {
            clickSlot(handler, nextSlot);
            state.waitingPage = true;
            state.nextClickAttempts = 1;
            state.lastActionMs = now;
        }
    }

    public static void onClientTick() {
        if (!state.active || state.marketOpen) return;
        ServersConfig config = LtrynekClient.serversConfig;
        ServerEntry entry = findServerEntry(config);
        if (entry == null) {
            finish(true, FinishReason.NO_GUI, true);
            return;
        }
        if (state.awaitingReopen) {
            int reopenDelay = getDelay(entry.marketNextDelayMs, DEFAULT_NEXT_DELAY_MS) * 5;
            long now = System.currentTimeMillis();
            if (now - state.screenClosedAt >= reopenDelay) {
                finish(true, FinishReason.MANUAL, true);
                return;
            }
            return;
        }
        int openDelay = getDelay(entry.marketOpenDelayMs, DEFAULT_OPEN_DELAY_MS);
        long now = System.currentTimeMillis();
        if (now - state.lastCommandMs >= openDelay) {
            if (!sendNextCommand()) {
                finish(true, FinishReason.NO_GUI, true);
            }
        }
    }

    private static void finish(boolean sendSummary, FinishReason reason, boolean closeScreen) {
        lastFinishReason = reason;
        lastPagesScanned = state.pagesScanned;
        if (sendSummary) {
            sendSummary(reason);
        }
        state.active = false;
        if (closeScreen) {
            closeScreen();
        }
        ClientSearchListManager.stopSearch();
        if (state.scanEnabledByAutomation) {
            ToggleScanner.scanningEnabled = false;
        }
        reset();
    }

    public static void onScreenClosed() {
        if (!state.active) return;
        state.awaitingReopen = true;
        state.screenClosedAt = System.currentTimeMillis();
        state.marketOpen = false;
    }

    private static void sendSummary(FinishReason reason) {
        var client = MinecraftClient.getInstance();
        if (client.player == null) return;
        String reasonMessage = reasonToMessage(reason);
        List<String> lines = SearchSummaryBuilder.buildStopSummary(reasonMessage, state.pagesScanned);
        for (String line : lines) {
            client.player.sendMessage(CommandUi.colored(line), false);
        }
    }

    private static void closeScreen() {
        var client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.closeHandledScreen();
        }
    }

    private static boolean sendNextCommand() {
        ServersConfig config = LtrynekClient.serversConfig;
        ServerEntry entry = findServerEntry(config);
        if (entry == null || entry.marketCommands == null || entry.marketCommands.isEmpty()) {
            return false;
        }
        if (state.commandIndex >= entry.marketCommands.size()) {
            return false;
        }
        String command = entry.marketCommands.get(state.commandIndex++);
        sendCommand(command);
        state.lastCommandMs = System.currentTimeMillis();
        return true;
    }

    private static void sendCommand(String command) {
        if (command == null || command.isBlank()) return;
        String trimmed = command.trim();
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        var client = MinecraftClient.getInstance();
        if (client.player != null && client.player.networkHandler != null) {
            client.player.networkHandler.sendChatCommand(trimmed);
        }
    }

    private static boolean titleMatches(String expectedTitle, Text title) {
        if (expectedTitle == null || expectedTitle.isEmpty() || title == null) {
            return false;
        }
        String plain = ColorStripUtils.stripAllColorsAndFormats(title.getString());
        if (plain.equalsIgnoreCase(expectedTitle)) {
            return true;
        }
        return plain.toLowerCase().contains(expectedTitle.toLowerCase());
    }

    private static int extractPageNumber(Text title) {
        if (title == null) return -1;
        String plain = ColorStripUtils.stripAllColorsAndFormats(title.getString());
        Matcher m = PAGE_PATTERN.matcher(plain);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {
                return -1;
            }
        }
        return -1;
    }

    private static boolean updatePageNumbers(int pageNumber) {
        if (pageNumber <= 0) return false;
        if (!state.pageNumberKnown) {
            state.pageNumberKnown = true;
            state.startPageNumber = pageNumber;
            state.lastPageNumber = pageNumber;
            state.pagesScanned = Math.max(1, state.pagesScanned);
            return true;
        }
        if (state.startPageNumber <= 0) {
            state.startPageNumber = pageNumber;
        }
        if (pageNumber != state.lastPageNumber) {
            state.lastPageNumber = pageNumber;
            int computed = pageNumber - state.startPageNumber + 1;
            if (computed > state.pagesScanned) {
                state.pagesScanned = computed;
            }
            return true;
        }
        return false;
    }

    private static Slot findNextPageSlot(ServerEntry entry, List<Slot> slots) {
        if (entry == null || slots == null) return null;
        if (entry.marketNextPageSlot != null) {
            int targetId = entry.marketNextPageSlot;
            for (Slot slot : slots) {
                if (slot != null && slot.id == targetId) {
                    return slot;
                }
            }
        }
        String expectedName = entry.marketNextPageName;
        String expectedMaterial = entry.marketNextPageMaterial;
        if ((expectedName == null || expectedName.isEmpty()) &&
                (expectedMaterial == null || expectedMaterial.isEmpty())) {
            return null;
        }
        for (Slot slot : slots) {
            if (slot == null || slot.getStack().isEmpty()) continue;
            String material = net.minecraft.registry.Registries.ITEM.getId(slot.getStack().getItem()).toString();
            String displayName = ColorStripUtils.stripAllColorsAndFormats(slot.getStack().getName().getString());
            if (!matchesExpected(expectedName, displayName)) continue;
            if (!matchesExpected(expectedMaterial, material)) continue;
            return slot;
        }
        return null;
    }

    private static boolean matchesExpected(String expected, String actual) {
        if (expected == null || expected.isEmpty()) return true;
        if (actual == null) return false;
        return actual.equalsIgnoreCase(expected);
    }

    private static void clickSlot(ScreenHandler handler, Slot slot) {
        var client = MinecraftClient.getInstance();
        if (client.player == null || client.interactionManager == null) return;
        client.interactionManager.clickSlot(
                handler.syncId,
                slot.id,
                0,
                SlotActionType.PICKUP,
                client.player
        );
    }

    private static int getDelay(Integer configured, int fallback) {
        if (configured == null || configured < 0) return fallback;
        return configured;
    }

    private static String buildPageSignature(List<Slot> slots) {
        if (slots == null) return null;
        StringBuilder sb = new StringBuilder();
        for (Slot slot : slots) {
            if (slot == null || slot.getStack().isEmpty()) {
                sb.append("|");
                continue;
            }
            String material = net.minecraft.registry.Registries.ITEM.getId(slot.getStack().getItem()).toString();
            String displayName = ColorStripUtils.stripAllColorsAndFormats(slot.getStack().getName().getString());
            sb.append(material).append('#').append(displayName).append('|');
        }
        return sb.toString();
    }

    private static ServerEntry findServerEntry(ServersConfig config) {
        if (config == null || config.servers == null) return null;
        String activeProfile = pl.lordtricker.ltrynek.core.manager.ClientPriceListManager.getActiveProfile();
        for (ServerEntry entry : config.servers) {
            if (entry.profileName.equals(activeProfile)) {
                return entry;
            }
        }
        return null;
    }

    private static void reset() {
        state.active = false;
        state.marketOpen = false;
        state.waitingPage = false;
        state.finishing = false;
        state.pagesRequested = 0;
        state.pagesScanned = 0;
        state.commandIndex = 0;
        state.pageSignature = null;
        state.finishReason = null;
        state.nextClickAttempts = 0;
        state.scanEnabledByAutomation = false;
        state.awaitingReopen = false;
        state.screenClosedAt = 0;
        state.lastActionMs = 0;
        state.lastCommandMs = 0;
        state.finishStartMs = 0;
        state.startPageNumber = -1;
        state.lastPageNumber = -1;
        state.pageNumberKnown = false;
    }

    private static class State {
        boolean active;
        boolean marketOpen;
        boolean waitingPage;
        boolean finishing;
        boolean scanEnabledByAutomation;
        boolean awaitingReopen;
        int pagesRequested;
        int pagesScanned;
        int commandIndex;
        int nextClickAttempts;
        int startPageNumber;
        int lastPageNumber;
        boolean pageNumberKnown;
        long lastActionMs;
        long lastCommandMs;
        long finishStartMs;
        long screenClosedAt;
        String pageSignature;
        FinishReason finishReason;
    }

    private static final Pattern PAGE_PATTERN = Pattern.compile("\\((\\d+)\\s*/\\s*\\d+\\)");

    public enum FinishReason {
        LIMIT_REACHED,
        NO_NEXT_PAGE,
        NO_GUI,
        MANUAL
    }

    private static String reasonToMessage(FinishReason reason) {
        if (reason == null) return null;
        switch (reason) {
            case LIMIT_REACHED:
                return Messages.get("command.searchlist.stop.reason.limit");
            case NO_NEXT_PAGE:
                return Messages.get("command.searchlist.stop.reason.no_next");
            case NO_GUI:
                return Messages.get("command.searchlist.stop.reason.no_gui");
            case MANUAL:
                return Messages.get("command.searchlist.stop.reason.manual");
            default:
                return null;
        }
    }
}
