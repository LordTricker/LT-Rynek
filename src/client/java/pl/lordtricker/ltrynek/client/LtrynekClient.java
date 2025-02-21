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
import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.MinecraftClient;
import pl.lordtricker.ltrynek.client.Messages;
import pl.lordtricker.ltrynek.client.ColorUtils;

import java.util.Map;

public class LtrynekClient implements ClientModInitializer {
	public static ServersConfig serversConfig;

	@Override
	public void onInitializeClient() {
		// Inicjalizacja keybindingów
		ToggleScanner.init();

		// Ładujemy konfigurację
		serversConfig = ConfigLoader.loadConfig();

		// Wczytujemy profile i dodajemy wpisy cenowe
		for (ServerEntry entry : serversConfig.servers) {
			ClientPriceListManager.setActiveProfile(entry.profileName);
			for (PriceEntry pe : entry.prices) {
				ClientPriceListManager.addPriceEntry(pe.name, pe.maxPrice);
			}
		}
		ClientPriceListManager.setActiveProfile(serversConfig.defaultProfile);

		// Rejestrujemy event do połączenia z serwerem
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

		// Rejestrujemy komendy klienta
		CommandDispatcher<FabricClientCommandSource> dispatcher = ClientCommandManager.DISPATCHER;
		ClientCommandRegistration.registerCommands(dispatcher);
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
