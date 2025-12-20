package pl.lordtricker.ltrynek.client.mixin;

import net.minecraft.client.option.ServerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.lordtricker.ltrynek.core.config.ServersConfig;
import pl.lordtricker.ltrynek.client.util.ServerListPatcher;

@Mixin(ServerList.class)
public class ServerListMixin {

    @Inject(method = "loadFile", at = @At("TAIL"), require = 0)
    private void ltbpvp$afterLoadFile(CallbackInfo ci) {
        ltbpvp$injectOrMove();
    }

    // Fallback for name variations in different mappings
    @Inject(method = "load", at = @At("TAIL"), cancellable = false, require = 0)
    private void ltbpvp$afterLoad(CallbackInfo ci) {
        ltbpvp$injectOrMove();
    }

    @Unique
    private void ltbpvp$injectOrMove() {
        if (!ServersConfig.adsEnabled) return;
        ServerListPatcher.injectOrMove((ServerList)(Object)this);
    }
}
