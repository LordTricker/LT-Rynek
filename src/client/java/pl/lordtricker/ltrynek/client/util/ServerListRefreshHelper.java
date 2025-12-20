package pl.lordtricker.ltrynek.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.ServerList;
import pl.lordtricker.ltrynek.core.config.ServersConfig;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public final class ServerListRefreshHelper {
    private static final Map<Object, Boolean> refreshedOnce =
            Collections.synchronizedMap(new WeakHashMap<>());

    private ServerListRefreshHelper() {}

    public static void onInit(Object screen) {
        if (!ServersConfig.adsEnabled) return;
        // First immediate try
        ServerList sl = findServerList(screen);
        if (sl != null) {
            ServerListPatcher.injectOrMove(sl);
        }
        // Schedule one delayed refresh to catch late remote-config fetch
        if (refreshedOnce.putIfAbsent(screen, Boolean.TRUE) == null) {
            try {
                Thread t = new Thread(() -> {
                    try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (mc != null) {
                        mc.execute(() -> {
                            ServerList s2 = findServerList(screen);
                            if (s2 != null) {
                                ServerListPatcher.injectOrMove(s2);
                            }
                        });
                    }
                }, "ltbpvp-servers-refresh");
                t.setDaemon(true);
                t.start();
            } catch (Throwable ignored) {}
        }
    }

    private static ServerList findServerList(Object screen) {
        try {
            for (java.lang.reflect.Field f : screen.getClass().getDeclaredFields()) {
                if (ServerList.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    Object v = f.get(screen);
                    if (v instanceof ServerList sl) {
                        return sl;
                    }
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }
}
