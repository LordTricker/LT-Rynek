package pl.lordtricker.ltrynek.client.mixin;

import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.lordtricker.ltrynek.client.LtrynekClient;

import java.util.List;

@Mixin(ServerList.class)
public class ServerListMixin {

    @Inject(method = "loadFile", at = @At("TAIL"))
    private void ltrynek$afterLoadFile(CallbackInfo ci) {
        ltrynek$injectOrMove();
    }

    // Fallback for name variations in different mappings
    @Inject(method = "load", at = @At("TAIL"), cancellable = false, require = 0)
    private void ltrynek$afterLoad(CallbackInfo ci) {
        ltrynek$injectOrMove();
    }

    @Unique
    private void ltrynek$injectOrMove() {
        if (LtrynekClient.serversConfig == null) return;
        Boolean enabled = LtrynekClient.serversConfig.adsEnabled;
        if (enabled != null && !enabled) return;

        List<ServerInfo> list = ((ServerListAccessor) (Object) this).getServers();
        if (list == null) return;

        final String targetAddress = "pvpstar.pl";

        int existingIndex = -1;
        for (int i = 0; i < list.size(); i++) {
            ServerInfo info = list.get(i);
            if (info != null && info.address != null && info.address.equalsIgnoreCase(targetAddress)) {
                existingIndex = i;
                break;
            }
        }

        if (existingIndex >= 0 && existingIndex < 5) {
            // Already within top 5; ensure display name is correct, keep position
            ServerInfo existing = list.get(existingIndex);
            if (existing != null) existing.name = "Serwer LT-Mods";
            return;
        }

        ServerInfo targetInfo;
        if (existingIndex >= 0) {
            // Move existing entry to the desired position and ensure display name
            targetInfo = list.remove(existingIndex);
            targetInfo.name = "Serwer LT-Mods";
        } else {
            // Create a new entry (handle signature differences across MC versions)
            targetInfo = createServerInfo("Serwer LT-Mods", targetAddress);
            if (targetInfo == null) {
                return;
            }
        }
        // Always place at the very top (index 0)
        list.add(0, targetInfo);
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
}