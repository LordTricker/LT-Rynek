package pl.lordtricker.ltrynek.client.mixin;

import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.lordtricker.ltrynek.client.util.ServerListRefreshHelper;

@Mixin(MultiplayerScreen.class)
public class MultiplayerScreenMixin {

    @Inject(method = "init", at = @At("TAIL"))
    private void ltbpvp$afterInit(CallbackInfo ci) {
        ServerListRefreshHelper.onInit(this);
    }
}
