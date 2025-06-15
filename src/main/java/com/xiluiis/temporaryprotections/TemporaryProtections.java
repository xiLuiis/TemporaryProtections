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
import org.bukkit.entity.Player;
import org.bukkit.event.*;

import org.bukkit.plugin.java.JavaPlugin;

import dev.espi.protectionstones.PSRegion;

import java.util.*;

public class TemporaryProtections extends JavaPlugin implements Listener {

    private final Map<UUID, Set<String>> playerEvents = new HashMap<>();
    private ConfigManager configManager; // <--- referencia global

    @Override
    public void onEnable() {
        saveDefaultConfig(); // <--- Esto asegura que config.yml exista

        configManager = new ConfigManager(this);
        TemporaryRegionManager regionManager = new TemporaryRegionManager(this);
        Bukkit.getPluginManager().registerEvents(new ProtectionListener(this, regionManager, configManager), this);

        this.getCommand("tmpp").setExecutor(this);
        getLogger().info("Plugin TemporaryProtections activado.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Este comando solo puede usarse en el juego.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            mostrarAyuda(player);
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("debug")) {
            ejecutarDebug(player);
            return true;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("enable")) {
            manejarEnable(player, args[1], args[2]);
            return true;
        }

        player.sendMessage(ChatColor.YELLOW + "Usa /tmpp help para ver los comandos.");
        return true;
    }

    private void mostrarAyuda(Player player) {
        player.sendMessage(ChatColor.AQUA + "=== TemporaryProtections Help ===");
        player.sendMessage(ChatColor.YELLOW + "/tmpp help" + ChatColor.WHITE + " - Muestra este mensaje.");
        player.sendMessage(ChatColor.YELLOW + "/tmpp enable <evento> yes | no" + ChatColor.WHITE + " - Activa o desactiva un evento.");
        player.sendMessage(ChatColor.GRAY + "Ejemplo: /tmpp enable blockcoords yes");
        player.sendMessage(ChatColor.YELLOW + "Eventos disponibles: blockcoords");
        player.sendMessage(ChatColor.YELLOW + "Usa /tmpp debug para ver el estado de ProtectionStones y más datos.");
    }

    private void ejecutarDebug(Player player) {
        org.bukkit.plugin.PluginManager pm = Bukkit.getPluginManager();
        boolean psActivo = pm.isPluginEnabled("ProtectionStones");

        if (psActivo) {
            player.sendMessage(ChatColor.GREEN + "ProtectionStones está ACTIVADO en el servidor.");

            PSRegion region = PSRegion.fromLocation(player.getLocation());
            if (region != null) {
                player.sendMessage(ChatColor.AQUA + "¡Estás dentro de una zona de ProtectionStones!");
                List<UUID> owners = region.getOwners();
                if (!owners.isEmpty()) {
                    player.sendMessage(ChatColor.GRAY + "Dueño: " + owners.get(0));
                } else {
                    player.sendMessage(ChatColor.GRAY + "Sin dueño principal.");
                }
            } else {
                player.sendMessage(ChatColor.YELLOW + "No estás en una zona de ProtectionStones.");
            }
        } else {
            player.sendMessage(ChatColor.RED + "ProtectionStones NO está activo en el servidor.");
        }
    }

    private void manejarEnable(Player player, String evento, String opcion) {
        String eventName = evento.toLowerCase();
        String option = opcion.toLowerCase();

        if (!eventName.equals("blockcoords")) {
            player.sendMessage(ChatColor.RED + "Evento desconocido. Usa /tmpp help para ver los eventos.");
            return;
        }

        playerEvents.putIfAbsent(player.getUniqueId(), new HashSet<>());
        Set<String> enabled = playerEvents.get(player.getUniqueId());

        if (option.equals("yes")) {
            enabled.add(eventName);
            player.sendMessage(ChatColor.GREEN + "Evento '" + eventName + "' activado.");
        } else if (option.equals("no")) {
            enabled.remove(eventName);
            player.sendMessage(ChatColor.RED + "Evento '" + eventName + "' desactivado.");
        } else {
            player.sendMessage(ChatColor.YELLOW + "Uso: /tmpp enable <evento> yes|no");
        }
    }
    
}