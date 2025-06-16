// TemporaryProtections.java
// Este plugin es un ejemplo básico de cómo crear un plugin de Minecraft con Bukkit/Spigot.
// Permite a los jugadores activar o desactivar eventos específicos relacionados con ProtectionStones.
// Asegúrate de tener las dependencias necesarias en tu proyecto, como Bukkit, Spigot y ProtectionStones.
// También incluye un comando de depuración para verificar el estado de ProtectionStones.

// Hecho por xiLuiis. 
// https://www.youtube.com/@xshadowsystem

package com.xiluiis.temporaryprotections;

// import com.xiluiis.temporaryprotections.TemporaryRegionManager;
// import com.xiluiis.temporaryprotections.ProtectionListener;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
//import org.bukkit.entity.Player;
import org.bukkit.event.*;

import org.bukkit.plugin.java.JavaPlugin;

//import dev.espi.protectionstones.PSRegion;

//import java.util.*;

public class TemporaryProtections extends JavaPlugin implements Listener {

    private DebugManager debugManager;
    private ConfigManager configManager; // <--- referencia global
    private TemporaryRegionManager regionManager;

    @Override
    public void onEnable() {
        saveDefaultConfig(); // <--- Esto asegura que config.yml exista

        configManager = new ConfigManager(this);
        debugManager = new DebugManager();
        regionManager = new TemporaryRegionManager(this);
        Bukkit.getPluginManager().registerEvents(new ProtectionListener(this, regionManager, configManager, debugManager), this);

        this.getCommand("tmpp").setExecutor(this);
        getLogger().info("Plugin TemporaryProtections activado.");

        // --- Limpieza y temporización de regiones temp_ al iniciar el plugin ---
        int tempSeconds = configManager.getTemporaryProtectionSeconds();
        for (org.bukkit.World world : getServer().getWorlds()) {
            com.sk89q.worldguard.protection.managers.RegionManager wgRegionManager =
                com.sk89q.worldguard.WorldGuard.getInstance().getPlatform().getRegionContainer().get(
                    com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(world));
            if (wgRegionManager == null) continue;
            for (String regionId : wgRegionManager.getRegions().keySet()) {
                if (regionId.startsWith("temp_")) {
                    // Asigna temporizador para eliminar la región después de tempSeconds
                    regionManager.scheduleTempRegionRemoval(wgRegionManager, regionId, tempSeconds);
                    getLogger().info("[TemporaryProtections] Región temporal encontrada al iniciar: " + regionId + " en mundo " + world.getName() + ". Se eliminará en " + tempSeconds + " segundos.");
                }
            }
        }
    }

    @Override
    public void onDisable() {
        // Ya no se guarda/carga tempregions.yml
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            mostrarAyuda(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("info")) {
            debugManager.showInfo(sender);
            return true;
        }

        if ((args[0].equalsIgnoreCase("enable") || args[0].equalsIgnoreCase("disable")) && args.length > 1) {
            if (!sender.hasPermission("temporaryprotections.admin")) {
                sender.sendMessage(ChatColor.RED + "No tienes permiso para este comando.");
                return true;
            }
            boolean enable = args[0].equalsIgnoreCase("enable");
            debugManager.handleToggle(sender, args[1], enable);
            return true;
        }

        if (args[0].equalsIgnoreCase("debug")) {
            if (!sender.hasPermission("temporaryprotections.admin")) {
                sender.sendMessage(ChatColor.RED + "No tienes permiso para este comando.");
                return true;
            }
            debugManager.showDebugOptions(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("temporaryprotections.admin")) {
                sender.sendMessage(ChatColor.RED + "No tienes permiso para este comando.");
                return true;
            }
            reloadConfig();
            sender.sendMessage(ChatColor.GREEN + "Configuración recargada.");
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "Usa /tmpp help para ver los comandos.");
        return true;
    }

    private void mostrarAyuda(CommandSender sender) {
        sender.sendMessage(ChatColor.AQUA + "=== TemporaryProtections Help ===");
        sender.sendMessage(ChatColor.YELLOW + "/tmpp help" + ChatColor.WHITE + " - Muestra este mensaje.");
        sender.sendMessage(ChatColor.YELLOW + "/tmpp info" + ChatColor.WHITE + " - Muestra tus opciones de debug activas.");
        if (sender.hasPermission("temporaryprotections.admin")) {
            sender.sendMessage(ChatColor.YELLOW + "/tmpp enable <opcion>" + ChatColor.WHITE + " - Activa una opción de debug (admin).");
            sender.sendMessage(ChatColor.YELLOW + "/tmpp disable <opcion>" + ChatColor.WHITE + " - Desactiva una opción de debug (admin).");
            sender.sendMessage(ChatColor.YELLOW + "/tmpp debug" + ChatColor.WHITE + " - Lista las opciones de debug disponibles (admin).");
            sender.sendMessage(ChatColor.YELLOW + "/tmpp reload" + ChatColor.WHITE + " - Recarga la configuración (admin).");
        }
    }

    // Método para exponer el DebugManager a los listeners
    public DebugManager getDebugManager() {
        return debugManager;
    }
    
    public TemporaryRegionManager getRegionManager() {
        return regionManager;
    }
}