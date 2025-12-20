package pl.lordtricker.ltrynek.client.mixin;

import pl.lordtricker.ltrynek.client.LtrynekClient;
import pl.lordtricker.ltrynek.client.keybinding.ToggleScanner;
import pl.lordtricker.ltrynek.client.util.AlarmSoundPlayer;
import pl.lordtricker.ltrynek.core.manager.ClientPriceListManager;
import pl.lordtricker.ltrynek.core.util.ColorStripUtils;
import pl.lordtricker.ltrynek.core.util.EnchantStringParser;
import pl.lordtricker.ltrynek.core.scan.ScanEvaluator;
import pl.lordtricker.ltrynek.core.scan.ScanInput;
import pl.lordtricker.ltrynek.core.scan.ScanResult;
import pl.lordtricker.ltrynek.client.util.SearchAutomationController;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.ScreenHandlerProvider;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {

	@Shadow
	protected int x;
	@Shadow
	protected int y;

        private int lastMatchedCount = 0;
        private int currentMatchedCount = 0;

        @Inject(method = "render", at = @At("HEAD"))
        private void onRenderHead(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
                currentMatchedCount = 0;
        }

        @Inject(method = "render", at = @At("TAIL"))
        private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
                ScreenHandler handler = ((ScreenHandlerProvider<?>) this).getScreenHandler();
                List<Slot> slots = ((ScreenHandlerAccessor) handler).getSlots();
                Text title = ((HandledScreen)(Object)this).getTitle();
                SearchAutomationController.onScreenRender(title, handler, slots);

                if (!ToggleScanner.scanningEnabled) {
                        return;
                }

                if (LtrynekClient.serversConfig != null && LtrynekClient.serversConfig.soundsEnabled) {
                        if (currentMatchedCount != lastMatchedCount && currentMatchedCount > 0) {
                                AlarmSoundPlayer.playForMatchCount(
                                                LtrynekClient.serversConfig,
                                                ClientPriceListManager.getActiveProfile(),
                                                currentMatchedCount
                                );
                        }
                }
                lastMatchedCount = currentMatchedCount;

        }

        @Inject(
                method = "drawSlot",
                at = @At(
                        value = "INVOKE",
                        target = "Lnet/minecraft/client/gui/DrawContext;drawItem(Lnet/minecraft/item/ItemStack;III)V",
                        shift = At.Shift.BEFORE
                )
        )
        private void onDrawSlotBeforeItem(DrawContext context, Slot slot, CallbackInfo ci) {
                if (!ToggleScanner.scanningEnabled) {
                        return;
                }

                if (processSlot(context, slot)) {
                        currentMatchedCount++;
                }
        }

	@Inject(method = "removed", at = @At("TAIL"))
	private void onRemoved(CallbackInfo ci) {
		SearchAutomationController.onScreenClosed();
	}

	private boolean processSlot(DrawContext context, Slot slot) {
		ItemStack stack = slot.getStack();
		if (stack.isEmpty()) return false;

        String displayName = stack.getName().getString();
        String noColorName = ColorStripUtils.stripAllColorsAndFormats(displayName);

		List<Text> tooltip = stack.getTooltip(Item.TooltipContext.DEFAULT, null, TooltipType.BASIC);
		List<String> loreLines = new ArrayList<>();
        for (int i = 0; i < tooltip.size(); i++) {
            // Skip the first line which is the item name; we only want lore
            if (i == 0) continue;
            Text textLine = tooltip.get(i);
			String plain = textLine.getString();
			String noColor = ColorStripUtils.stripAllColorsAndFormats(plain);
			loreLines.add(noColor);
		}

		String rawEnchants = stack.getEnchantments().toString();
		String enchantmentsString = EnchantStringParser.parse(rawEnchants);
		if (!enchantmentsString.isEmpty()) {
			loreLines.add(enchantmentsString);
		}

		Identifier id = Registries.ITEM.getId(stack.getItem());
		String materialId = id.toString();

		int stackSize = stack.getCount();
		ScanInput input = new ScanInput(
				noColorName,
				loreLines,
				materialId,
				enchantmentsString,
				stackSize,
				slot.id
		);
		ScanResult result = ScanEvaluator.evaluate(input, LtrynekClient.serversConfig);
                if (result.highlight) {
                        int realX = slot.x;
                        int realY = slot.y;
                        context.fill(realX, realY, realX + 16, realY + 16, result.color);
                        return true;
                }

		return false;
	}

}
