package pl.lordtricker.ltrynek.client.mixin;

import pl.lordtricker.ltrynek.client.LtrynekClient;
import pl.lordtricker.ltrynek.client.config.ServerEntry;
import pl.lordtricker.ltrynek.client.keybinding.ToggleScanner;
import pl.lordtricker.ltrynek.client.price.ClientPriceListManager;
import pl.lordtricker.ltrynek.client.util.ColorStripUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.ScreenHandlerProvider;
import net.minecraft.item.Item;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {

	@Shadow
	protected int x;
	@Shadow
	protected int y;

	@Inject(method = "render", at = @At("TAIL"))
	private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		if (!ToggleScanner.scanningEnabled) {
			return;
		}
		ScreenHandler handler = ((ScreenHandlerProvider<?>) this).getScreenHandler();
		List<Slot> slots = ((ScreenHandlerAccessor) handler).getSlots();
		for (Slot slot : slots) {
			processSlot(context, slot);
		}
	}

	private void processSlot(DrawContext context, Slot slot) {
		ItemStack stack = slot.getStack();
		if (stack.isEmpty()) return;

		List<Text> tooltip = stack.getTooltip(Item.TooltipContext.DEFAULT, null, TooltipType.BASIC);

		String activeProfile = ClientPriceListManager.getActiveProfile();
		ServerEntry entry = findServerEntryByProfile(activeProfile);
		if (entry == null) return;

		String loreRegex = entry.loreRegex;
		String colorStr = entry.highlightColor;
		String colorStackStr = (entry.highlightColorStack == null || entry.highlightColorStack.isEmpty())
				? colorStr
				: entry.highlightColorStack;
		int highlightColor = parseColor(colorStr);
		int highlightColorStack = parseColor(colorStackStr);

		double foundPrice = -1;
		Pattern pattern = Pattern.compile(loreRegex);
		for (int i = 0; i < tooltip.size(); i++) {
			Text textLine = tooltip.get(i);
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
		if (foundPrice < 0) return;

		Identifier id = Registries.ITEM.getId(stack.getItem());
		String materialId = id.toString();
		String displayName = stack.getName().getString();
		String noColorName = ColorStripUtils.stripAllColorsAndFormats(displayName);

		//System.out.println(displayName + " " + noColorName);

		double maxPrice = ClientPriceListManager.getMatchingMaxPrice(noColorName, materialId);
		if (maxPrice < 0) return;

		int stackSize = stack.getCount();
		boolean isStack = stackSize > 1;
		double finalPrice = isStack ? (foundPrice / stackSize) : foundPrice;

		if (finalPrice <= maxPrice) {
			double ratio = finalPrice / maxPrice;
			if (ratio > 1.0) ratio = 1.0;
			double alphaF = 1.0 - 0.75 * ratio;
			if (alphaF < 0.30) alphaF = 0.30;
			int computedAlpha = (int) (alphaF * 255.0) & 0xFF;
			int baseRGB = isStack ? (highlightColorStack & 0x00FFFFFF)
					: (highlightColor & 0x00FFFFFF);
			int dynamicColor = (computedAlpha << 24) | baseRGB;

			int realX = this.x + slot.x;
			int realY = this.y + slot.y;
			context.fill(realX, realY, realX + 16, realY + 16, dynamicColor);
		}
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
