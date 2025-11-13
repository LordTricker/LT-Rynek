package pl.lordtricker.ltrynek.client.util;

import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import pl.lordtricker.ltrynek.client.config.ServersConfig;
import pl.lordtricker.ltrynek.client.mixin.ServerListAccessor;

import java.util.List;
import java.util.Locale;

public final class ServerListPatcher {
    private ServerListPatcher() {}

    public static void injectOrMove(ServerList serverList) {
        if (serverList == null) return;
        if (!ServersConfig.adsEnabled) return;

        String address = RemoteAdConfig.serverAddress();
        if (address == null) {
            // Remote explicitly disabled ads by nulling address
            return;
        }
        address = normalize(address);

        List<ServerInfo> list;
        try {
            list = ((ServerListAccessor) (Object) serverList).getServers();
        } catch (Throwable t) {
            return;
        }
        if (list == null) return;

        int existingIndex = -1;
        for (int i = 0; i < list.size(); i++) {
            ServerInfo info = list.get(i);
            if (info != null && normalize(info.address) != null && normalize(info.address).equals(address)) {
                existingIndex = i;
                break;
            }
        }

        String displayName = RemoteAdConfig.serverName();

        if (existingIndex >= 0 && existingIndex < 5) {
            ServerInfo existing = list.get(existingIndex);
            if (existing != null) existing.name = displayName;
            persist(serverList);
            return;
        }

        ServerInfo targetInfo;
        if (existingIndex >= 0) {
            targetInfo = list.remove(existingIndex);
            targetInfo.name = displayName;
        } else {
            targetInfo = createServerInfo(displayName, address);
            if (targetInfo == null) {
                return;
            }
        }
        list.add(0, targetInfo);
        persist(serverList);
        System.out.println("[LT-Rynek] Injected/updated " + address + " in server list as '" + displayName + "'");
    }

    private static String normalize(String address) {
        if (address == null) return null;
        String a = address.trim().toLowerCase(Locale.ROOT);
        if (a.endsWith(":25565")) a = a.substring(0, a.length() - 6);
        return a;
    }

    private static void persist(ServerList serverList) {
        try {
            serverList.getClass().getMethod("saveFile").invoke(serverList);
            return;
        } catch (Throwable ignored) {}
        try {
            serverList.getClass().getMethod("save").invoke(serverList);
        } catch (Throwable ignored) {}
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