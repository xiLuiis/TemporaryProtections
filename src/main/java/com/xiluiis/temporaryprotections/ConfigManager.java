package com.xiluiis.temporaryprotections;

//import org.bukkit.configuration.file.FileConfiguration;
import java.util.List;

public class ConfigManager {
    private final TemporaryProtections plugin;

    public ConfigManager(TemporaryProtections plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
    }

    public int getTemporaryProtectionSeconds() {
        return plugin.getConfig().getInt("temporary-protection-seconds", 60);
    }

    public boolean isDebugEnabled() {
        return plugin.getConfig().getBoolean("debug-messages", false);
    }

    public List<String> getAllowedProtectionBlocks() {
        return plugin.getConfig().getStringList("allowed-protection-blocks");
    }
}
