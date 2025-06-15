package com.xiluiis.temporaryprotections;

//import java.io.ObjectInputFilter.Config;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import dev.espi.protectionstones.PSProtectBlock;
import dev.espi.protectionstones.PSRegion;
import dev.espi.protectionstones.ProtectionStones;
import dev.espi.protectionstones.event.PSRemoveEvent;

public class ProtectionListener implements Listener {
    
    private final TemporaryProtections plugin;
    private final TemporaryRegionManager regionManager;
    private final Set<UUID> enRegionTemporal = new HashSet<>();
    private final ConfigManager configManager;

    public ProtectionListener(TemporaryProtections plugin,TemporaryRegionManager regionManager, ConfigManager configManager) {
        this.regionManager = regionManager;
        this.plugin = plugin;
        this.configManager = configManager;
    }

@EventHandler
    public void onBlockBreak(org.bukkit.event.block.BlockBreakEvent e) {
        Player player = e.getPlayer();
        Location loc = e.getBlock().getLocation();

        RegionManager regionManager = WorldGuard.getInstance()
            .getPlatform()
            .getRegionContainer()
            .get(BukkitAdapter.adapt(loc.getWorld()));
        if (regionManager == null) return;

        Set<ProtectedRegion> regions = regionManager.getApplicableRegions(BukkitAdapter.asBlockVector(loc)).getRegions();
        for (ProtectedRegion region : regions) {
            if (region.getId().startsWith("temp_")) {
                e.setCancelled(true);
                player.sendMessage(ChatColor.RED + "No puedes romper bloques en una región temporal.");
                return;
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location loc = player.getLocation();
        World world = loc.getWorld();

        RegionManager wgRegionManager = WorldGuard.getInstance()
            .getPlatform()
            .getRegionContainer()
            .get(BukkitAdapter.adapt(world));
        if (wgRegionManager == null) return;

        Set<ProtectedRegion> regions = wgRegionManager.getApplicableRegions(BukkitAdapter.asBlockVector(loc)).getRegions();
        boolean estaEnTemporal = false;
        String tempRegionId = null;

        for (ProtectedRegion region : regions) {
            if (region.getId().startsWith("temp_")) {
                estaEnTemporal = true;
                tempRegionId = region.getId();
                break;
            }
        }

        UUID uuid = player.getUniqueId();

        if (estaEnTemporal && !enRegionTemporal.contains(uuid)) {
            enRegionTemporal.add(uuid);
            UUID owner = regionManager.getTempRegionOwner(tempRegionId);
            String ownerName = owner != null ? Bukkit.getOfflinePlayer(owner).getName() : "desconocido";
            player.sendMessage(ChatColor.GOLD + "¡Entraste en una región temporal de " + ownerName + "!");
        } else if (!estaEnTemporal && enRegionTemporal.contains(uuid)) {
            enRegionTemporal.remove(uuid);
            player.sendMessage(ChatColor.YELLOW + "Saliste de una región temporal.");
        }
    }

    @EventHandler
    public void onPSRemove(PSRemoveEvent event) {
        Player player = event.getPlayer();
        PSRegion oldRegion = event.getRegion();

        if (player != null && oldRegion != null) {
            // Obtener el tipo de bloque de la región eliminada
            String blockType = oldRegion.getType(); 
            PSProtectBlock psBlock = ProtectionStones.getBlockOptions(blockType);
            if (psBlock == null) return;

            String blockAlias = psBlock.alias;
            player.sendMessage(ChatColor.GRAY + "El alias de la Protection Stone eliminada es: " + blockAlias);

            if (!configManager.getAllowedProtectionBlocks().contains(blockAlias)) {
                // No está en la lista, no crees región temporal
                return;
            }

            player.sendMessage(ChatColor.RED + "¡Has eliminado una protección! Se creará una protección temporal por 60 segundos.");
            plugin.getLogger().info("Se ejecutó onPSRemove para " + player.getName());

            // Obtener el WorldGuard RegionManager
            World world = oldRegion.getWorld();
            com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);
            RegionManager wgRegionManager = WorldGuard.getInstance()
                .getPlatform()
                .getRegionContainer()
                .get(weWorld);
            if (wgRegionManager == null) {
                player.sendMessage(ChatColor.RED + "No se pudo acceder a WorldGuard.");
                return;
            }

            ProtectedRegion original = oldRegion.getWGRegion();
            regionManager.crearRegionTemporal(player, wgRegionManager, original, 60);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        Player player = e.getPlayer();
        Location loc = e.getBlock().getLocation();

        RegionManager wgRegionManager = WorldGuard.getInstance()
            .getPlatform()
            .getRegionContainer()
            .get(BukkitAdapter.adapt(loc.getWorld()));
        if (wgRegionManager == null) return;

        Set<ProtectedRegion> regions = wgRegionManager.getApplicableRegions(BukkitAdapter.asBlockVector(loc)).getRegions();
        boolean enTemporal = false;
        for (ProtectedRegion region : regions) {
            if (region.getId().startsWith("temp_")) {
                enTemporal = true;
                break;
            }
        }

        String blockType = e.getBlockPlaced().getType().name();
        PSProtectBlock psBlock = ProtectionStones.getBlockOptions(blockType);
       
        String blockAlias = (psBlock != null) ? psBlock.alias: null;
        // Si está en región temporal y NO es piedra de protección permitida, cancela
        if (enTemporal && (psBlock == null || !ProtectionStones.isProtectBlockType(blockType) ||!configManager.getAllowedProtectionBlocks().contains(blockAlias))) {
            e.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Solo puedes colocar piedras de protección en una región temporal.");
            return;
        }
        
        
        // Calcula los límites de la nueva protección de PS usando los radios reales
        BlockVector3 placed = BlockVector3.at(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

        player.sendMessage(ChatColor.GRAY + "El alias de este bloque de protección es: " + blockAlias);

        if (!configManager.getAllowedProtectionBlocks().contains(blockAlias)) {
            // No está en la lista, no crees/elimines regiones temporales, pero tampoco bloquees la acción
            return;
        }
        if(psBlock == null) {
            
            return;
        }

        int xRadius = psBlock.xRadius;
        int yRadius = psBlock.yRadius;
        int zRadius = psBlock.zRadius;

        int minY, maxY;
        if (yRadius == -1) {
            minY = 0;
            maxY = loc.getWorld().getMaxHeight();
        } else {
            minY = placed.y() - yRadius;
            maxY = placed.y() + yRadius;
        }

        BlockVector3 min = BlockVector3.at(
            placed.x() - xRadius,
            minY,
            placed.z() - zRadius
        );
        BlockVector3 max = BlockVector3.at(
            placed.x() + xRadius,
            maxY,
            placed.z() + zRadius
        );

        // Elimina regiones temporales solapadas en X y Z
        List<String> regionesAEliminar = new ArrayList<>();
        for (String tempRegionId : regionManager.getAllTempRegionIds()) {
            UUID tempOwner = regionManager.getTempRegionOwner(tempRegionId);
            if (!player.getUniqueId().equals(tempOwner)) continue;
            ProtectedRegion tempRegion = wgRegionManager.getRegion(tempRegionId);
            if (tempRegion == null) continue;

            // Mensajes de depuración en el chat del jugador
            player.sendMessage(ChatColor.GRAY + "=== DEPURACIÓN DE SOLAPAMIENTO ===");
            player.sendMessage(ChatColor.GRAY + "Revisando temporal: " + tempRegionId + " dueño: " + tempOwner);
            player.sendMessage(ChatColor.GRAY + "Nueva protección min: " + min + " max: " + max);
            player.sendMessage(ChatColor.GRAY + "Temporal min: " + tempRegion.getMinimumPoint() + " max: " + tempRegion.getMaximumPoint());

            if (regionManager.regionsOverlap(tempRegion.getMinimumPoint(), tempRegion.getMaximumPoint(), min, max)) {
                regionesAEliminar.add(tempRegionId);
            }
        }
        for (String tempRegionId : regionesAEliminar) {
            wgRegionManager.removeRegion(tempRegionId);
            regionManager.removeTempRegion(tempRegionId);
            player.sendMessage(ChatColor.YELLOW + "Se eliminó una protección temporal solapada: " + tempRegionId);
        }
    }
}