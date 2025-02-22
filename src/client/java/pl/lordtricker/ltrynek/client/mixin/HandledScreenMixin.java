package pl.lordtricker.ltrynek.client.mixin;

import pl.lordtricker.ltrynek.client.LtrynekClient;
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
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
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

	@Inject(method = "render", at = @At("TAIL"))
	private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		if (!ToggleScanner.scanningEnabled) {
			return;
		}
		ScreenHandler handler = ((ScreenHandlerProvider<?>) this).getScreenHandler();
		List<Slot> slots = ((ScreenHandlerAccessor) handler).getSlots();

		int matchedCount = 0;
		for (Slot slot : slots) {
			if (processSlot(context, slot)) {
				matchedCount++;
			}
		}

		if (LtrynekClient.serversConfig != null && LtrynekClient.serversConfig.soundsEnabled) {
			if (matchedCount != lastMatchedCount && matchedCount > 0) {
				playAlarmSound(matchedCount);
			}
		}
		lastMatchedCount = matchedCount;
	}

	private boolean processSlot(DrawContext context, Slot slot) {
		ItemStack stack = slot.getStack();
		if (stack.isEmpty()) return false;

		List<Text> tooltip = stack.getTooltip(Item.TooltipContext.DEFAULT, null, TooltipType.BASIC);
		String activeProfile = ClientPriceListManager.getActiveProfile();
		ServerEntry entry = findServerEntryByProfile(activeProfile);
		if (entry == null) return false;

		String loreRegex = entry.loreRegex;
		String colorStr = entry.highlightColor;
		String colorStackStr = (entry.highlightColorStack == null || entry.highlightColorStack.isEmpty())
				? colorStr
				: entry.highlightColorStack;
		int highlightColor = parseColor(colorStr);
		int highlightColorStack = parseColor(colorStackStr);

		double foundPrice = -1;
		Pattern pattern = Pattern.compile(loreRegex);
		for (Text textLine : tooltip) {
			String plain = textLine.getString();
			Matcher m = pattern.matcher(plain);
			if (m.find()) {
				String priceGroup = m.group(1);
				double parsedPrice = parsePriceWithSuffix(priceGroup);
				if (parsedPrice >= 0) {
					foundPrice = parsedPrice;
					break;
				}
			}
		}
		if (foundPrice < 0) return false;

		Identifier id = Registries.ITEM.getId(stack.getItem());
		String materialId = id.toString();
		String displayName = stack.getName().getString();
		String noColorName = ColorStripUtils.stripAllColorsAndFormats(displayName);

		int stackSize = stack.getCount();
		boolean isStack = stackSize > 1;
		double finalPrice = isStack ? (foundPrice / stackSize) : foundPrice;

		if (ClientSearchListManager.isSearchActive()) {
			String uniqueKey = slot.id + "|" + noColorName + "|" + finalPrice + "|" + stackSize;
			if (!ClientSearchListManager.isAlreadyCounted(uniqueKey)) {
				ClientSearchListManager.markAsCounted(uniqueKey);
				String lowerName = noColorName.toLowerCase();
				for (String searchTerm : ClientSearchListManager.getSearchList()) {
					if (lowerName.contains(searchTerm.toLowerCase())) {
						ClientSearchListManager.updateStats(searchTerm, finalPrice, stackSize);
					}
				}
			}
		}

		double maxPrice = ClientPriceListManager.getMatchingMaxPrice(noColorName, materialId);
		if (maxPrice < 0) return false;

		if (finalPrice <= maxPrice) {
			double ratio = finalPrice / maxPrice;
			if (ratio > 1.0) ratio = 1.0;
			double alphaF = 1.0 - 0.75 * ratio;
			if (alphaF < 0.30) alphaF = 0.30;
			int computedAlpha = (int) (alphaF * 255.0) & 0xFF;
			int baseRGB = isStack ? (highlightColorStack & 0x00FFFFFF) : (highlightColor & 0x00FFFFFF);
			int dynamicColor = (computedAlpha << 24) | baseRGB;
			int realX = this.x + slot.x;
			int realY = this.y + slot.y;
			context.fill(realX, realY, realX + 16, realY + 16, dynamicColor);
			return true;
		}
		return false;
	}

	/**
	 * Wybiera odpowiedni dźwięk w zależności od liczby trafień:
	 * - dla <= 9: miniAlarmSound, odtwarzany określoną liczbę razy
	 * - dla > 9: miniAlarmSoundStack, odtwarzany raz
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
	 * Odtwarza dany dźwięk N razy z opóźnieniem:
	 * - Pierwszy dźwięk po 0.3 s
	 * - Kolejne w odstępach 0.15 s
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

	private double parsePriceWithSuffix(String raw) {
		raw = raw.trim().replace(',', '.');
		double multiplier = 1.0;
		if (raw.endsWith("k") || raw.endsWith("K")) {
			multiplier = 1000.0;
			raw = raw.substring(0, raw.length() - 1);
		} else if (raw.endsWith("m") || raw.endsWith("M")) {
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
