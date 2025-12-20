package pl.lordtricker.ltrynek.core.config;

import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;

public class ServerEntry {
    public List<String> domains = new ArrayList<>();
    public String profileName;

    public String loreRegex = "Cena: (\\d+)";
    public String highlightColor = "#80FF00";
    public String highlightColorStack = "#FF8000";
    public String miniAlarmSound = "minecraft:ui.button.click";
    public String miniAlarmSoundStack = "minecraft:entity.player.levelup";

    public List<String> marketCommands = new ArrayList<>();
    public String marketGuiTitle;
    public String marketNextPageName;
    public String marketNextPageMaterial;
    public Integer marketNextPageSlot;
    public Integer marketOpenDelayMs;
    public Integer marketNextDelayMs;
    public Integer marketCloseDelayMs;

    public List<PriceEntry> prices = new ArrayList<>();

    public transient Path sourceFile;
}
