package pl.lordtricker.ltrynek.client;

import pl.lordtricker.ltrynek.client.command.ClientCommandRegistration;
import pl.lordtricker.ltrynek.client.config.ConfigLoader;
import pl.lordtricker.ltrynek.client.config.PriceEntry;
import pl.lordtricker.ltrynek.client.config.ServerEntry;
import pl.lordtricker.ltrynek.client.config.ServersConfig;
import pl.lordtricker.ltrynek.client.price.ClientPriceListManager;
import pl.lordtricker.ltrynek.client.keybinding.ToggleScanner;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;

import java.util.Map;

public class LtrynekClient implements ClientModInitializer {
	public static ServersConfig serversConfig;

	@Override
	public void onInitializeClient() {
		ToggleScanner.init();

		serversConfig = ConfigLoader.loadConfig();

		for (ServerEntry entry : serversConfig.servers) {
			ClientPriceListManager.setActiveProfile(entry.profileName);
			for (PriceEntry pe : entry.prices) {
				ClientPriceListManager.addPriceEntry(pe.name, pe.maxPrice);
			}
		}
		ClientPriceListManager.setActiveProfile(serversConfig.defaultProfile);

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			String address = getServerAddress();
			ServerEntry entry = findServerEntry(address);
			if (entry != null) {
				ClientPriceListManager.setActiveProfile(entry.profileName);
				if (client.player != null) {
					String welcomeMsg = Messages.format("player.join", Map.of("profile", entry.profileName));
					client.player.sendMessage(ColorUtils.translateColorCodes(welcomeMsg), false);
				}
			} else {
				String def = serversConfig.defaultProfile;
				ClientPriceListManager.setActiveProfile(def);
				if (client.player != null) {
					String welcomeMsg = Messages.format("player.join", Map.of("profile", def));
					client.player.sendMessage(ColorUtils.translateColorCodes(welcomeMsg), false);
				}
			}
		});

		ClientCommandRegistration.registerCommands();
	}

	private String getServerAddress() {
		if (MinecraftClient.getInstance().getCurrentServerEntry() != null) {
			return MinecraftClient.getInstance().getCurrentServerEntry().address;
		}
		return "singleplayer";
	}

	private ServerEntry findServerEntry(String address) {
		for (ServerEntry entry : serversConfig.servers) {
			for (String domain : entry.domains) {
				if (address.contains(domain)) {
					return entry;
				}
			}
		}
		return null;
	}

	/**
	 * Zwraca wpis konfiguracyjny dla aktywnego profilu.
	 */
	public static ServerEntry getServerEntryForActiveProfile() {
		String activeProfile = ClientPriceListManager.getActiveProfile();
		if (serversConfig != null && serversConfig.servers != null) {
			for (ServerEntry se : serversConfig.servers) {
				if (se.profileName.equals(activeProfile)) {
					return se;
				}
			}
		}
		return null;
	}
}
