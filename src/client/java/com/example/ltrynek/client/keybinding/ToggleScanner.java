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
                "key.ltxb.toggle_scanning",
                GLFW.GLFW_KEY_R,
                "category.ltxb"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleScanningKey.wasPressed()) {
                long window = client.getWindow().getHandle();
                boolean ctrlPressed = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS ||
                        GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
                if (!ctrlPressed) {
                    continue;
                }
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
