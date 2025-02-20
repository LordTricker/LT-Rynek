package com.example.ltrynek.client.keybinding;

import com.example.ltrynek.client.ColorUtils;
import com.example.ltrynek.client.Messages;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;

public class ToggleScanner {
    public static KeyBinding toggleScanningKey;
    public static boolean scanningEnabled = false;

    public static void init() {
        toggleScanningKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ltrynek.toggle_scanning",
                GLFW.GLFW_KEY_R,
                "category.ltrynek"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleScanningKey.wasPressed()) {
                scanningEnabled = !scanningEnabled;
                String msgKey = scanningEnabled ? "command.scanner.toggle.on" : "command.scanner.toggle.off";
                String msg = Messages.get(msgKey);
                if (client.player != null) {
                    client.player.sendMessage(ColorUtils.translateColorCodes(msg), false);
                }
            }
        });
    }
}
