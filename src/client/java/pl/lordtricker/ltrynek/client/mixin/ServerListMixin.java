package pl.lordtricker.ltrynek.client.mixin;

import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.lordtricker.ltrynek.client.config.ServersConfig;
import pl.lordtricker.ltrynek.client.util.ServerListPatcher;

import java.util.List;

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

    @Unique
    private static String normalizeAddress(String address) {
        if (address == null) return null;
        String a = address.trim().toLowerCase(java.util.Locale.ROOT);
        if (a.endsWith(":25565")) {
            a = a.substring(0, a.length() - 6);
        }
        return a;
    }

    private static ServerInfo createServerInfo(String name, String address) {
        try {
            java.lang.reflect.Constructor<?>[] ctors = ServerInfo.class.getConstructors();
            for (java.lang.reflect.Constructor<?> c : ctors) {
                Class<?>[] pts = c.getParameterTypes();
                Object[] args = new Object[pts.length];
                int stringCount = 0;
                boolean unknown = false;
                for (int i = 0; i < pts.length; i++) {
                    Class<?> t = pts[i];
                    if (t == String.class) {
                        args[i] = (stringCount == 0) ? name : address;
                        stringCount++;
                    } else if (t == boolean.class || t == Boolean.class) {
                        args[i] = Boolean.FALSE;
                    } else if (t.isEnum()) {
                        Object[] constants = t.getEnumConstants();
                        if (constants != null && constants.length > 0) {
                            args[i] = constants[0];
                        } else {
                            unknown = true; break;
                        }
                    } else {
                        // Try Text.of(String)
                        try {
                            if ("net.minecraft.text.Text".equals(t.getName())) {
                                Class<?> textClass = Class.forName("net.minecraft.text.Text");
                                java.lang.reflect.Method ofMethod = textClass.getMethod("of", String.class);
                                args[i] = ofMethod.invoke(null, name);
                            } else {
                                unknown = true; break;
                            }
                        } catch (Throwable ex) {
                            unknown = true; break;
                        }
                    }
                }
                if (unknown) continue;
                try {
                    Object inst = c.newInstance(args);
                    return (ServerInfo) inst;
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
        return null;
    }

    @Unique
    private void ltbpvp$persist() {
        // Try to persist via saveFile()/save() without hard remap dependency
        Object self = this;
        Class<?> cls = self.getClass();
        try {
            java.lang.reflect.Method m = cls.getMethod("saveFile");
            m.invoke(self);
            return;
        } catch (Throwable ignored) {}
        try {
            java.lang.reflect.Method m = cls.getMethod("save");
            m.invoke(self);
        } catch (Throwable ignored) {}
    }
}