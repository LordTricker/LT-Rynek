package pl.lordtricker.ltrynek.client.mixin;

import net.minecraft.client.item.TooltipContext;
import pl.lordtricker.ltrynek.client.LtrynekClient;
import pl.lordtricker.ltrynek.client.config.PriceEntry;
import pl.lordtricker.ltrynek.client.config.ServerEntry;
import pl.lordtricker.ltrynek.client.keybinding.ToggleScanner;
import pl.lordtricker.ltrynek.client.price.ClientPriceListManager;
import pl.lordtricker.ltrynek.client.search.ClientSearchListManager;
import pl.lordtricker.ltrynek.client.util.ColorStripUtils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.ScreenHandlerProvider;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {

	@Shadow
	protected int x;
	@Shadow
	protected int y;

	private int lastMatchedCount = 0;

	/**
	 * Metoda wywoływana po wyrenderowaniu ekranu. Tu wykonujemy skanowanie slotów
	 * w ekwipunku/GUI i ewentualne podświetlanie.
	 */
	@Inject(method = "render", at = @At("TAIL"))
	private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		if (!ToggleScanner.scanningEnabled) {
			return;
		}
		ScreenHandler handler = ((ScreenHandlerProvider<?>) this).getScreenHandler();
		// Potrzebny dostęp do listy slotów - pamiętaj o Accessor (ScreenHandlerAccessor).
		List<Slot> slots = ((ScreenHandlerAccessor) handler).getSlots();

		int matchedCount = 0;
		for (Slot slot : slots) {
			if (processSlot(context, slot)) {
				matchedCount++;
			}
		}

		// Jeśli włączone są dźwięki w konfiguracji i liczba dopasowań wzrosła (> 0)
		if (LtrynekClient.serversConfig != null && LtrynekClient.serversConfig.soundsEnabled) {
			if (matchedCount != lastMatchedCount && matchedCount > 0) {
				playAlarmSound(matchedCount);
			}
		}
		lastMatchedCount = matchedCount;
	}

	/**
	 * Główna metoda przetwarzająca pojedynczy slot, w tym:
	 * - pobranie ceny z tooltipu (za pomocą regex),
	 * - obsługa searchList,
	 * - wyszukiwanie PriceEntry,
	 * - podświetlanie slotu w zależności od maxPrice.
	 *
	 * @return true jeśli slot został podświetlony (dopasowany), false w przeciwnym wypadku.
	 */
	private boolean processSlot(DrawContext context, Slot slot) {
		ItemStack stack = slot.getStack();
		if (stack.isEmpty()) {
			return false;
		}

		// W starszych wersjach jest getTooltip(PlayerEntity, boolean).
		// Drugi parametr (advanced = false) -> zwykły tooltip, bez zaawansowanych informacji.
		PlayerEntity player = MinecraftClient.getInstance().player;
		List<Text> tooltip = stack.getTooltip(player, TooltipContext.BASIC);

		// Usuwamy kody kolorów z tooltipu
		List<String> loreLines = new ArrayList<>();
		for (Text line : tooltip) {
			String plain = line.getString();
			String noColorLine = ColorStripUtils.stripAllColorsAndFormats(plain);
			loreLines.add(noColorLine);
		}

		// Szukamy w configu aktualnie aktywnego profilu
		String activeProfile = ClientPriceListManager.getActiveProfile();
		ServerEntry entry = findServerEntryByProfile(activeProfile);
		if (entry == null) {
			return false;
		}

		// Kolory podświetlenia z configu
		String colorStr = entry.highlightColor;
		String colorStackStr = (entry.highlightColorStack == null || entry.highlightColorStack.isEmpty())
				? colorStr
				: entry.highlightColorStack;
		int highlightColor = parseColor(colorStr);
		int highlightColorStack = parseColor(colorStackStr);

		// Regex do wyłuskania ceny (np. "Cena: 12k")
		String loreRegex = entry.loreRegex;
		double foundPrice = -1;
		Pattern pattern = Pattern.compile(loreRegex);
		for (String plain : loreLines) {
			Matcher m = pattern.matcher(plain);
			if (m.find()) {
				String priceGroup = m.group(1); // zakładamy, że w regex jest (group 1)
				double parsedPrice = parsePriceWithSuffix(priceGroup);
				if (parsedPrice >= 0) {
					foundPrice = parsedPrice;
					break;
				}
			}
		}
		if (foundPrice < 0) {
			return false;
		}

		// Informacje o stacku
		Identifier id = Registries.ITEM.getId(stack.getItem());
		String materialId = id.toString(); // np. "minecraft:diamond_sword"
		String displayName = stack.getName().getString();
		String noColorName = ColorStripUtils.stripAllColorsAndFormats(displayName);

		int stackSize = stack.getCount();
		boolean isStack = (stackSize > 1);
		double finalPrice = isStack ? (foundPrice / stackSize) : foundPrice;

		// Obsługa searchList (jeśli aktywna)
		if (ClientSearchListManager.isSearchActive()) {
			String uniqueKey = slot.id + "|" + noColorName + "|" + finalPrice + "|" + stackSize;
			if (!ClientSearchListManager.isAlreadyCounted(uniqueKey)) {
				ClientSearchListManager.markAsCounted(uniqueKey);

				// Dopasowanie do każdego compositeKey w searchList
				for (String compositeKey : ClientSearchListManager.getSearchList()) {
					if (ClientSearchListManager.matchesSearchTerm(compositeKey, noColorName, loreLines, materialId)) {
						ClientSearchListManager.updateStats(compositeKey, finalPrice, stackSize);
					}
				}
			}
		}

		// Obsługa PriceListManager – sprawdzamy, czy dany item pasuje do PriceEntry
		PriceEntry matchedEntry = ClientPriceListManager.findMatchingPriceEntry(noColorName, loreLines, materialId);
		if (matchedEntry == null) {
			// Brak wpisu w priceList -> nie podświetlamy
			return false;
		}

		double maxPrice = matchedEntry.maxPrice;
		// Podświetlamy tylko gdy finalPrice <= maxPrice
		if (finalPrice <= maxPrice) {
			double ratio = finalPrice / maxPrice;
			if (ratio > 1.0) ratio = 1.0;

			// Im bliżej maxPrice, tym mniejsza przezroczystość
			double alphaF = 1.0 - 0.75 * ratio;
			if (alphaF < 0.30) {
				alphaF = 0.30;
			}
			int computedAlpha = (int) (alphaF * 255.0) & 0xFF;

			// Kolor stack vs. pojedynczy item
			int baseRGB = isStack
					? (highlightColorStack & 0x00FFFFFF)
					: (highlightColor & 0x00FFFFFF);

			int dynamicColor = (computedAlpha << 24) | baseRGB;

			// Rysujemy overlay w miejscu slota
			int realX = this.x + slot.x;
			int realY = this.y + slot.y;
			context.fill(realX, realY, realX + 16, realY + 16, dynamicColor);

			return true;
		}
		return false;
	}

	/**
	 * Dźwięk alarmowy – odtwarzany w zależności od liczby trafień:
	 * - <= 9 -> miniAlarmSound (N razy)
	 * - > 9 -> miniAlarmSoundStack (1 raz)
	 */
	private void playAlarmSound(int matchedCount) {
		String activeProfile = ClientPriceListManager.getActiveProfile();
		ServerEntry entry = findServerEntryByProfile(activeProfile);
		if (entry == null) return;

		String miniSound = entry.miniAlarmSound;
		String stackSound = entry.miniAlarmSoundStack;
		if (miniSound == null) miniSound = "";
		if (stackSound == null) stackSound = "";

		if (matchedCount <= 9) {
			playSoundNTimes(miniSound, matchedCount);
		} else {
			playSoundNTimes(stackSound, 1);
		}
	}

	/**
	 * Odtwarza dany dźwięk N razy w krótkich odstępach (0.3s start, potem co 0.15s).
	 */
	private void playSoundNTimes(String soundId, int times) {
		if (soundId.isEmpty() || times <= 0) return;
		Identifier id = Identifier.tryParse(soundId);
		if (id == null) return;
		SoundEvent soundEvent = Registries.SOUND_EVENT.get(id);
		if (soundEvent == null) return;

		Timer timer = new Timer();
		long initialDelay = 300; // 0.3 sekundy
		long interval = 150;     // 0.15 sekundy odstęp

		for (int i = 0; i < times; i++) {
			long delay = initialDelay + i * interval;
			timer.schedule(new TimerTask() {
				@Override
				public void run() {
					// Odtwarzanie dźwięku w wątku głównym MC
					MinecraftClient.getInstance().execute(() -> {
						MinecraftClient.getInstance().getSoundManager().play(
								PositionedSoundInstance.master(soundEvent, 1.0F, 1.0F)
						);
					});
				}
			}, delay);
		}
		// Po ostatnim odtworzeniu anulujemy timer
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				timer.cancel();
			}
		}, initialDelay + times * interval + 50);
	}

	/**
	 * Parsuje cenę z ewentualnym sufiksem (k, m, mld).
	 */
	private double parsePriceWithSuffix(String raw) {
		raw = raw.trim().replace(',', '.');
		String lower = raw.toLowerCase();
		double multiplier = 1.0;

		// "mld"
		if (lower.endsWith("mld")) {
			multiplier = 1_000_000_000.0;
			raw = raw.substring(0, raw.length() - 3);
		}
		// "m"
		else if (lower.endsWith("m")) {
			multiplier = 1_000_000.0;
			raw = raw.substring(0, raw.length() - 1);
		}
		// "k"
		else if (lower.endsWith("k")) {
			multiplier = 1000.0;
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
	 * Wyszukuje w configu (serversConfig) wpis ServerEntry pasujący do nazwy profilu.
	 */
	private ServerEntry findServerEntryByProfile(String profileName) {
		if (LtrynekClient.serversConfig == null || LtrynekClient.serversConfig.servers == null) {
			return null;
		}
		for (ServerEntry se : LtrynekClient.serversConfig.servers) {
			if (se.profileName.equals(profileName)) {
				return se;
			}
		}
		return null;
	}

	/**
	 * Konwersja z "#RRGGBB" czy "RRGGBB" na int ARGB (z domyślną alfą "FF" jeśli brak).
	 */
	private int parseColor(String colorStr) {
		if (colorStr.startsWith("#")) {
			colorStr = colorStr.substring(1);
		}
		if (colorStr.length() == 6) {
			colorStr = "FF" + colorStr;
		}
		long argb = Long.parseLong(colorStr, 16);
		return (int) (argb & 0xFFFFFFFF);
	}
}
