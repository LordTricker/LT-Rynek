package pl.lordtricker.ltrynek.client.mixin;

import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.lordtricker.ltrynek.client.LtrynekClient;
import pl.lordtricker.ltrynek.client.config.PriceEntry;
import pl.lordtricker.ltrynek.client.config.ServerEntry;
import pl.lordtricker.ltrynek.client.keybinding.ToggleScanner;
import pl.lordtricker.ltrynek.client.price.ClientPriceListManager;
import pl.lordtricker.ltrynek.client.search.ClientSearchListManager;
import pl.lordtricker.ltrynek.client.util.ColorStripUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin extends DrawableHelper {

	// Statyczne liczniki trafień – resetowane co klatkę (stary sposób)
	private static int matchedCount = 0;
	private static int lastMatchedCount = 0;

	/**
	 * Iniekcja do metody drawSlot – rysowanie odbywa się w stylu 1.19.4,
	 * a pobieranie lore odbywa się z NBT.
	 * Reszta logiki (maxPrice, searchList) działa wg. nowej wersji.
	 */
	@Inject(method = "drawSlot", at = @At("HEAD"))
	private void onDrawSlot(MatrixStack matrices, Slot slot, CallbackInfo ci) {
		if (!ToggleScanner.scanningEnabled) return;

		String activeProfile = ClientPriceListManager.getActiveProfile();
		ServerEntry entry = findServerEntryByProfile(activeProfile);
		if (entry == null) return;

		// Konfiguracja z profilu
		String loreRegex = entry.loreRegex;
		String colorStr = entry.highlightColor;
		String colorStackStr = (entry.highlightColorStack == null || entry.highlightColorStack.isEmpty())
				? colorStr
				: entry.highlightColorStack;
		int highlightColor = parseColor(colorStr);
		int highlightColorStack = parseColor(colorStackStr);

		ItemStack stack = slot.getStack();
		if (stack.isEmpty()) return;

		// Pobieramy lore z NBT – jak w 1.19.4
		if (!stack.hasNbt()) return;
		NbtCompound display = stack.getSubNbt("display");
		if (display == null || !display.contains("Lore", 9)) return;
		NbtList loreList = display.getList("Lore", 8);

		// Przygotowujemy listę linii lore (bez kodów kolorów)
		List<String> loreLines = new ArrayList<>();
		double foundPrice = -1;
		Pattern pattern = Pattern.compile(loreRegex);
		for (int i = 0; i < loreList.size(); i++) {
			String loreJson = loreList.getString(i);
			Text textLine = Text.Serializer.fromJson(loreJson);
			if (textLine != null) {
				String plain = textLine.getString();
				String noColorLine = ColorStripUtils.stripAllColorsAndFormats(plain);
				loreLines.add(noColorLine);
				// Szukamy ceny przy pomocy regexa
				Matcher m = pattern.matcher(noColorLine);
				if (m.find()) {
					String priceGroup = m.group(1);
					double parsedPrice = parsePriceWithSuffix(priceGroup);
					if (parsedPrice >= 0) {
						foundPrice = parsedPrice;
						// Nie przerywamy pętli, by zebrać pełną listę loreLines do searchList
					}
				}
			}
		}
		if (foundPrice < 0) return;

		// Pobieramy dane przedmiotu
		Identifier id = Registry.ITEM.getId(stack.getItem());
		String materialId = id.toString();
		String displayName = stack.getName().getString();
		String noColorName = ColorStripUtils.stripAllColorsAndFormats(displayName);

		int stackSize = stack.getCount();
		boolean isStack = stackSize > 1;
		double finalPrice = isStack ? (foundPrice / stackSize) : foundPrice;

		// Aktualizacja searchList – logika wg. nowej wersji
		if (ClientSearchListManager.isSearchActive()) {
			String uniqueKey = slot.id + "|" + noColorName + "|" + finalPrice + "|" + stackSize;
			if (!ClientSearchListManager.isAlreadyCounted(uniqueKey)) {
				ClientSearchListManager.markAsCounted(uniqueKey);
				for (String compositeKey : ClientSearchListManager.getSearchList()) {
					if (ClientSearchListManager.matchesSearchTerm(compositeKey, noColorName, loreLines, materialId)) {
						ClientSearchListManager.updateStats(compositeKey, finalPrice, stackSize);
					}
				}
			}
		}

		// Pobieramy maxPrice wg. nowej logiki
		PriceEntry matchedEntry = ClientPriceListManager.findMatchingPriceEntry(noColorName, loreLines, materialId);
		if (matchedEntry == null) return;
		double maxPrice = matchedEntry.maxPrice;

		// Stary sposób rysowania – podświetlenie slotu
		if (finalPrice <= maxPrice) {
			double ratio = finalPrice / maxPrice;
			if (ratio > 1.0) ratio = 1.0;
			double alphaF = 1.0 - 0.75 * ratio;
			if (alphaF < 0.50) alphaF = 0.50;
			int computedAlpha = (int) (alphaF * 255.0) & 0xFF;
			int baseRGB = isStack ? (highlightColorStack & 0x00FFFFFF) : (highlightColor & 0x00FFFFFF);
			int dynamicColor = (computedAlpha << 24) | baseRGB;
			fill(matrices, slot.x, slot.y, slot.x + 16, slot.y + 16, dynamicColor);
			matchedCount++; // zwiększamy licznik trafień (używany do alarmu)
		}
	}

	/**
	 * Iniekcja do metody render – wywołanie alarmu dźwiękowego.
	 */
	@Inject(method = "render", at = @At("TAIL"))
	private void onRender(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		if (!ToggleScanner.scanningEnabled) return;
		if (LtrynekClient.serversConfig != null && LtrynekClient.serversConfig.soundsEnabled) {
			if (matchedCount != lastMatchedCount && matchedCount > 0) {
				playAlarmSound(matchedCount);
			}
		}
		lastMatchedCount = matchedCount;
		matchedCount = 0; // reset licznika po renderze
	}

	private void playAlarmSound(int count) {
		String activeProfile = ClientPriceListManager.getActiveProfile();
		ServerEntry entry = findServerEntryByProfile(activeProfile);
		if (entry == null) return;
		String miniSound = entry.miniAlarmSound;
		String stackSound = entry.miniAlarmSoundStack;
		if (miniSound == null) miniSound = "";
		if (stackSound == null) stackSound = "";
		if (count <= 9) {
			playSoundNTimes(miniSound, count);
		} else {
			playSoundNTimes(stackSound, 1);
		}
	}

	private void playSoundNTimes(String soundId, int times) {
		if (soundId.isEmpty() || times <= 0) return;
		Identifier id = Identifier.tryParse(soundId);
		if (id == null) return;
		SoundEvent soundEvent = Registry.SOUND_EVENT.get(id);
		if (soundEvent == null) return;
		Timer timer = new Timer();
		long initialDelay = 300; // 0.3 sekundy
		long interval = 150;     // 0.15 sekundy odstęp
		for (int i = 0; i < times; i++) {
			long delay = initialDelay + i * interval;
			timer.schedule(new TimerTask() {
				@Override
				public void run() {
					MinecraftClient.getInstance().execute(() -> {
						MinecraftClient.getInstance().getSoundManager().play(
								PositionedSoundInstance.master(soundEvent, 1.0F, 1.0F)
						);
					});
				}
			}, delay);
		}
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				timer.cancel();
			}
		}, initialDelay + times * interval + 50);
	}

	/**
	 * Parsowanie ceny z sufiksami (np. k, m, mld).
	 */
	private double parsePriceWithSuffix(String raw) {
		raw = raw.trim().replace(" ", "");
		String lower = raw.toLowerCase();
		double multiplier = 1.0;
		// Najpierw sprawdzamy "mld"
		if (lower.endsWith("mld")) {
			multiplier = 1_000_000_000.0;
			raw = raw.substring(0, raw.length() - 3);
		}
		// Potem "m"
		else if (lower.endsWith("m")) {
			multiplier = 1_000_000.0;
			raw = raw.substring(0, raw.length() - 1);
		}
		// Następnie "k"
		else if (lower.endsWith("k")) {
			multiplier = 1000.0;
			raw = raw.substring(0, raw.length() - 1);
		}

		if (!raw.contains(".")) {
			int i = raw.indexOf(',', raw.length() - 3);
			if (i != -1) raw = raw.substring(0, i) + "." + raw.substring(i + 1);
		}
		raw = raw.replace(",", "");

		try {
			double base = Double.parseDouble(raw);
			return base * multiplier;
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	/**
	 * Wyszukiwanie konfiguracji serwera na podstawie profilu.
	 */
	private ServerEntry findServerEntryByProfile(String profileName) {
		if (LtrynekClient.serversConfig == null || LtrynekClient.serversConfig.servers == null)
			return null;
		for (ServerEntry se : LtrynekClient.serversConfig.servers) {
			if (se.profileName.equals(profileName)) {
				return se;
			}
		}
		return null;
	}

	/**
	 * Parsowanie koloru z opcjonalnym "#" i dodaniem alfa, jeśli potrzeba.
	 */
	private int parseColor(String colorStr) {
		if (colorStr.startsWith("#")) {
			colorStr = colorStr.substring(1);
		}
		if (colorStr.length() == 6) {
			colorStr = "FF" + colorStr;
		}
		long argb = Long.parseLong(colorStr, 16);
		return (int)(argb & 0xFFFFFFFF);
	}
}