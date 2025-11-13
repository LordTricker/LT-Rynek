package pl.lordtricker.ltrynek.client.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RemoteAdConfig {
    private static final String CONFIG_URL = "https://raw.githubusercontent.com/LordTricker/LT-Rynek/refs/heads/ad/config.json";
    private static final String DEFAULT_NAME = "LT-Rynek";
    private static final String DEFAULT_ADDRESS = "pvpstar.pl";

    private static volatile String cachedName = DEFAULT_NAME;
    private static volatile String cachedAddress = DEFAULT_ADDRESS;
    private static volatile boolean fetched = false;
    private static final AtomicBoolean started = new AtomicBoolean(false);

    private static final Gson GSON = new Gson();

    private RemoteAdConfig() {}

    public static void preloadAsync() {
        if (!started.compareAndSet(false, true)) return;
        Thread t = new Thread(RemoteAdConfig::fetchOnce, "ltrynek-remote-ad-fetch");
        t.setDaemon(true);
        t.start();
    }

    public static String serverName() {
        if (!fetched) return DEFAULT_NAME;
        return (cachedName != null && !cachedName.isBlank()) ? cachedName : DEFAULT_NAME;
    }

    public static String serverAddress() {
        // Before first successful fetch: do not inject anything
        if (!fetched) return null;
        // After fetch: explicit null disables injection; blank treated as null
        return (cachedAddress != null && !cachedAddress.isBlank()) ? cachedAddress : null;
    }

    private static void fetchOnce() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(2))
                    .build();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(CONFIG_URL))
                    .header("Accept", "application/json")
                    .header("User-Agent", "LT-Rynek/1 JavaHttpClient")
                    .timeout(Duration.ofSeconds(4))
                    .GET()
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                String body = resp.body();
                JsonObject obj = GSON.fromJson(body, JsonObject.class);
                if (obj != null) {
                    boolean hasName = obj.has("serverName");
                    boolean nameIsNull = hasName && obj.get("serverName").isJsonNull();
                    boolean hasAddr = obj.has("serverAddress");
                    boolean addrIsNull = hasAddr && obj.get("serverAddress").isJsonNull();

                    if (hasName) {
                        if (nameIsNull) {
                            cachedName = null;
                        } else {
                            String name = obj.get("serverName").getAsString();
                            if (name != null && !name.isBlank()) cachedName = name;
                        }
                    }

                    if (hasAddr) {
                        if (addrIsNull) {
                            cachedAddress = null; // disable injection
                        } else {
                            String addr = obj.get("serverAddress").getAsString();
                            if (addr != null && !addr.isBlank()) cachedAddress = normalizeAddress(addr);
                        }
                    }
                    fetched = true;
                    System.out.println("[LT-Rynek] Remote config loaded: name='" + cachedName + "', address='" + cachedAddress + "'");
                }
            } else {
                System.out.println("[LT-Rynek] Remote config HTTP status: " + resp.statusCode());
            }
        } catch (Throwable t) {
            System.out.println("[LT-Rynek] Remote config fetch failed: " + t.getMessage());
        }
    }

    private static String normalizeAddress(String address) {
        if (address == null) return null;
        String a = address.trim().toLowerCase(Locale.ROOT);
        if (a.endsWith(":25565")) a = a.substring(0, a.length() - 6);
        return a;
    }
}