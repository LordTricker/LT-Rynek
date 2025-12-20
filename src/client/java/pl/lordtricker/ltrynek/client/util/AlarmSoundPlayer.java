package pl.lordtricker.ltrynek.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import pl.lordtricker.ltrynek.core.config.ServersConfig;

import java.util.Timer;
import java.util.TimerTask;

public final class AlarmSoundPlayer {
    private AlarmSoundPlayer() {}

    public static void playForMatchCount(ServersConfig config, String activeProfile, int matchedCount) {
        if (config == null || config.servers == null) return;
        String miniSound = "";
        String stackSound = "";
        for (var entry : config.servers) {
            if (entry.profileName.equals(activeProfile)) {
                if (entry.miniAlarmSound != null) miniSound = entry.miniAlarmSound;
                if (entry.miniAlarmSoundStack != null) stackSound = entry.miniAlarmSoundStack;
                break;
            }
        }
        if (matchedCount <= 9) {
            playSoundNTimes(miniSound, matchedCount);
        } else {
            playSoundNTimes(stackSound, 1);
        }
    }

    private static void playSoundNTimes(String soundId, int times) {
        if (soundId.isEmpty() || times <= 0) return;
        Identifier id = Identifier.tryParse(soundId);
        if (id == null) return;
        SoundEvent soundEvent = Registries.SOUND_EVENT.get(id);
        if (soundEvent == null) return;
        Timer timer = new Timer();
        long initialDelay = 300;
        long interval = 150;
        for (int i = 0; i < times; i++) {
            long delay = initialDelay + i * interval;
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    MinecraftClient.getInstance().execute(() -> {
                        MinecraftClient.getInstance().getSoundManager().play(
                                PositionedSoundInstance.master(soundEvent, 1.0F, 1.0F)
                        );
                    });
                }
            }, delay);
        }
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                timer.cancel();
            }
        }, initialDelay + times * interval + 50);
    }
}
