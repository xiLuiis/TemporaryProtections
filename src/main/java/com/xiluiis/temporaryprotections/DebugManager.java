package com.xiluiis.temporaryprotections;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class DebugManager {
    private final Map<UUID, Set<String>> debugOptions = new HashMap<>();
    private final List<String> opcionesDebug = Arrays.asList("overlap", "regioncreate", "broadcast", "explosion");

    // Opción global de debug para logs internos del plugin
    private final Set<String> globalDebugOptions = new HashSet<>();

    public void handleToggle(CommandSender sender, String opcion, boolean enable) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Este comando solo puede usarse en el juego.");
            return;
        }
        String opt = opcion.toLowerCase();
        if (!opcionesDebug.contains(opt)) {
            player.sendMessage(ChatColor.RED + "Opción de debug desconocida. Usa /tmpp debug para ver las opciones.");
            return;
        }
        debugOptions.putIfAbsent(player.getUniqueId(), new HashSet<>());
        Set<String> enabled = debugOptions.get(player.getUniqueId());
        if (enable) {
            enabled.add(opt);
            player.sendMessage(ChatColor.GREEN + "Opción de debug '" + opt + "' activada.");
        } else {
            enabled.remove(opt);
            player.sendMessage(ChatColor.RED + "Opción de debug '" + opt + "' desactivada.");
        }
    }

    public boolean isDebugOptionEnabled(Player player, String option) {
        Set<String> enabled = debugOptions.getOrDefault(player.getUniqueId(), new HashSet<>());
        return enabled.contains(option.toLowerCase());
    }

    public void showDebugOptions(CommandSender sender) {
        sender.sendMessage(ChatColor.AQUA + "=== Opciones de debug disponibles ===");
        sender.sendMessage(ChatColor.YELLOW + "overlap" + ChatColor.WHITE + " - Mensajes de solapamiento de regiones temporales.");
        sender.sendMessage(ChatColor.YELLOW + "regioncreate" + ChatColor.WHITE + " - Mensajes al crear regiones temporales.");
        sender.sendMessage(ChatColor.YELLOW + "broadcast" + ChatColor.WHITE + " - Mensajes globales de debug.");
        sender.sendMessage(ChatColor.YELLOW + "explosion" + ChatColor.WHITE + " - Mensajes sobre eventos de explosión.");
    }

    public void showInfo(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Este comando solo puede usarse en el juego.");
            return;
        }
        Set<String> enabled = debugOptions.getOrDefault(player.getUniqueId(), new HashSet<>());
        player.sendMessage(ChatColor.AQUA + "=== Opciones de debug activas ===");
        if (enabled.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "Ninguna opción de debug activada.");
        } else {
            for (String opt : enabled) {
                player.sendMessage(ChatColor.YELLOW + "- " + opt);
            }
        }
    }

    // Log a consola del servidor
    public void logToConsole(String msg) {
        org.bukkit.Bukkit.getLogger().info(msg);
    }

    public void enableGlobalDebugOption(String option) {
        globalDebugOptions.add(option.toLowerCase());
    }

    public void disableGlobalDebugOption(String option) {
        globalDebugOptions.remove(option.toLowerCase());
    }

    public boolean isGlobalDebugOptionEnabled(String option) {
        return globalDebugOptions.contains(option.toLowerCase());
    }
}
