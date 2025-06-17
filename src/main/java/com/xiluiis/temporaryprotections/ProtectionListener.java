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
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
//import org.bukkit.event.entity.EntityDamageByEntityEvent;
//import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

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
    private final DebugManager debugManager;

    public ProtectionListener(TemporaryProtections plugin, TemporaryRegionManager regionManager, ConfigManager configManager, DebugManager debugManager) {
        this.regionManager = regionManager;
        this.plugin = plugin;
        this.configManager = configManager;
        this.debugManager = debugManager;
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
            if (region.getId().startsWith("temp_")&& !player.hasPermission("temporaryprotections.admin.bypass")) {
                e.setCancelled(true);
                player.sendMessage(ChatColor.GOLD + "No puedes romper bloques en una " + ChatColor.AQUA + "región temporal" + ChatColor.GOLD + ".");
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
            String ownerName = owner != null ? Bukkit.getOfflinePlayer(owner).getName() : ChatColor.RED + "desconocido";
            player.sendMessage(ChatColor.AQUA + "Entraste a una región temporal de " + ChatColor.YELLOW + ownerName + ChatColor.AQUA + ".");
        } else if (!estaEnTemporal && enRegionTemporal.contains(uuid)) {
            enRegionTemporal.remove(uuid);
            player.sendMessage(ChatColor.GRAY + "Saliste de una región temporal.");
        }
    }

    @EventHandler
    public void onPSRemove(PSRemoveEvent event) {
        Player player = event.getPlayer();
        PSRegion oldRegion = event.getRegion();

        if (player != null && oldRegion != null) {
            String blockType = oldRegion.getType(); 
            PSProtectBlock psBlock = ProtectionStones.getBlockOptions(blockType);
            if (psBlock == null) return;

            String blockAlias = psBlock.alias;
            if (debugManager.isDebugOptionEnabled(player, "regioncreate")) {
                player.sendMessage(ChatColor.GRAY + "Alias de Protection Stone eliminada: " + ChatColor.AQUA + blockAlias);
            }

            if (!configManager.getAllowedProtectionBlocks().contains(blockAlias)) {
                return;
            }

            if (debugManager.isDebugOptionEnabled(player, "regioncreate")) {
                player.sendMessage(ChatColor.YELLOW + "Se creará una protección temporal por 60 segundos.");
            }

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
            player.sendMessage(ChatColor.LIGHT_PURPLE + "¡§lProtección temporal creada!§r " + ChatColor.YELLOW + "Tienes " + ChatColor.GOLD + "§l60 segundos§r" + ChatColor.YELLOW + " para colocar otra piedra de protección.");
            // Ya no mostrar mensaje de WorldGuard creada
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
        Material placedType = e.getBlockPlaced().getType();
        // Bloquear colocación de agua, lava y End Crystal en regiones temporales
        if (enTemporal && (placedType == Material.WATER || placedType == Material.LAVA || placedType == Material.END_CRYSTAL)) {
            e.setCancelled(true);
            player.sendMessage(ChatColor.RED + "No puedes colocar agua, lava o End Crystals en una región temporal.");
            return;
        }

        String blockType = e.getBlockPlaced().getType().name();
        PSProtectBlock psBlock = ProtectionStones.getBlockOptions(blockType);
       
        String blockAlias = (psBlock != null) ? psBlock.alias: null;
        if (enTemporal && ( (psBlock == null || !ProtectionStones.isProtectBlockType(blockType) || !configManager.getAllowedProtectionBlocks().contains(blockAlias)) && !player.hasPermission("temporaryprotections.admin.bypass") )) {
            e.setCancelled(true);
            player.sendMessage(ChatColor.GOLD + "Solo puedes colocar piedras de protección válidas en una " + ChatColor.AQUA + "región temporal" + ChatColor.GOLD + ".");
            return;
        }
        
        // Calcula los límites de la nueva protección de PS usando los radios reales
        BlockVector3 placed = BlockVector3.at(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

        // Mensaje de alias de bloque de protección (debug regioncreate)
        if (debugManager.isDebugOptionEnabled(player, "regioncreate")) {
            player.sendMessage(ChatColor.GRAY + "El alias de este bloque de protección es: " + blockAlias);
        }

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

            // Mensajes de depuración en el chat del jugador SOLO si tiene debug overlap activado
            if (debugManager.isDebugOptionEnabled(player, "overlap")) {
                player.sendMessage(ChatColor.GRAY + "=== DEPURACIÓN DE SOLAPAMIENTO ===");
                player.sendMessage(ChatColor.GRAY + "Revisando temporal: " + tempRegionId + " dueño: " + tempOwner);
                player.sendMessage(ChatColor.GRAY + "Nueva protección min: " + min + " max: " + max);
                player.sendMessage(ChatColor.GRAY + "Temporal min: " + tempRegion.getMinimumPoint() + " max: " + tempRegion.getMaximumPoint());
            }

            if (regionManager.regionsOverlap(tempRegion.getMinimumPoint(), tempRegion.getMaximumPoint(), min, max)) {
                regionesAEliminar.add(tempRegionId);
            }
        }
        // Mensaje al eliminar protección temporal solapada
        for (String tempRegionId : regionesAEliminar) {
            wgRegionManager.removeRegion(tempRegionId);
            UUID tempOwner = regionManager.getTempRegionOwner(tempRegionId);
            String ownerName = tempOwner != null ? Bukkit.getOfflinePlayer(tempOwner).getName() : ChatColor.RED + "desconocido";
            regionManager.removeTempRegion(tempRegionId);
            player.sendMessage(ChatColor.LIGHT_PURPLE + "Has eliminado una protección temporal por colapsamiento de " + ChatColor.AQUA + ownerName + ChatColor.LIGHT_PURPLE + ".");
            if (debugManager.isDebugOptionEnabled(player, "overlap")) {
                player.sendMessage(ChatColor.YELLOW + "Se eliminó una protección temporal solapada de: " + ChatColor.AQUA + ownerName);
            }
        }
        // Mensaje de debug al crear región temporal (debug regioncreate)
        if (debugManager.isDebugOptionEnabled(player, "regioncreate")) {
            player.sendMessage(ChatColor.GREEN + "Protección temporal creada para este bloque de protección.");
        }
        // Solo loguea si la opción 'listenerload' está activa en DebugManager para algún admin
        if (debugManager.isGlobalDebugOptionEnabled("listenerload")) {
            plugin.getLogger().fine("ProtectionListener cargado correctamente.");
        }
    }

    @EventHandler
    public void onEntityDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Location loc = player.getLocation();
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
        if (!enTemporal) return;

        // Permitir daño de mobs (PvE), bloquear solo PvP y daño ambiental
        if (event instanceof org.bukkit.event.entity.EntityDamageByEntityEvent edbe) {
            if (edbe.getDamager() instanceof Player) {
                // PvP: bloquear
                event.setCancelled(true);
                return;
            }
            // Si el daño es causado por un mob, permitir
            return;
        }
        // Daño ambiental (caída, fuego, etc.): bloquear
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("temporaryprotections.admin.bypass")) return;
        Location loc = event.getBlock().getLocation();
        RegionManager wgRegionManager = WorldGuard.getInstance()
            .getPlatform()
            .getRegionContainer()
            .get(BukkitAdapter.adapt(loc.getWorld()));
        if (wgRegionManager == null) return;
        Set<ProtectedRegion> regions = wgRegionManager.getApplicableRegions(BukkitAdapter.asBlockVector(loc)).getRegions();
        for (ProtectedRegion region : regions) {
            if (region.getId().startsWith("temp_")) {
                Material bucket = event.getBucket();
                // Bloquear cualquier cubo excepto milk_bucket, pero permitir a admins
                if (bucket.name().contains("BUCKET") && !bucket.name().contains("MILK")) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "No puedes vaciar cubos en una región temporal.");
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return; // Solo main hand
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) return;
        Material mat = item.getType();
        // Bloquear huevos de mobs hostiles y warden
        if (mat.name().endsWith("_SPAWN_EGG")) {
            // Lista de mobs hostiles y warden (puedes expandirla)
            String[] hostiles = {"WARDEN", "CREEPER", "ZOMBIE", "SKELETON", "SPIDER", "ENDERMAN", "BLAZE", "WITHER", "PILLAGER", "VINDICATOR", "EVOKER", "RAVAGER", "SHULKER", "GUARDIAN", "ELDER_GUARDIAN", "DROWNED", "PHANTOM", "PIGLIN", "HOGLIN", "MAGMA_CUBE", "SLIME", "WITCH", "VEX", "VINDICATOR", "ILLUSIONER", "HUSK", "STRAY", "ZOMBIFIED_PIGLIN", "ZOMBIE_VILLAGER", "ZOMBIE_HORSE", "SILVERFISH", "GHAST", "ENDERMITE", "PILLAGER", "RAVAGER", "SHULKER", "VEX", "VINDICATOR", "WITHER_SKELETON", "ZOGLIN"};
            String mob = mat.name().replace("_SPAWN_EGG", "");
            for (String hostile : hostiles) {
                if (mob.equalsIgnoreCase(hostile)) {
                    Location loc = player.getLocation();
                    RegionManager wgRegionManager = WorldGuard.getInstance()
                        .getPlatform()
                        .getRegionContainer()
                        .get(BukkitAdapter.adapt(loc.getWorld()));
                    if (wgRegionManager == null) return;
                    Set<ProtectedRegion> regions = wgRegionManager.getApplicableRegions(BukkitAdapter.asBlockVector(loc)).getRegions();
                    for (ProtectedRegion region : regions) {
                        if (region.getId().startsWith("temp_")) {
                            event.setCancelled(true);
                            player.sendMessage(ChatColor.RED + "No puedes colocar huevos de mobs hostiles en una región temporal.");
                            return;
                        }
                    }
                }
            }
        }
        // Bloquear uso de camas en Nether/End en regiones temporales
        if (mat.name().contains("BED") || mat.name().contains("bed")) {
            Location loc = event.getClickedBlock() != null ? event.getClickedBlock().getLocation() : player.getLocation();
            String worldName = loc.getWorld().getName().toLowerCase();
            if (worldName.contains("nether") || worldName.contains("end")) {
                RegionManager wgRegionManager = WorldGuard.getInstance()
                    .getPlatform()
                    .getRegionContainer()
                    .get(BukkitAdapter.adapt(loc.getWorld()));
                if (wgRegionManager == null) return;
                Set<ProtectedRegion> regions = wgRegionManager.getApplicableRegions(BukkitAdapter.asBlockVector(loc)).getRegions();
                for (ProtectedRegion region : regions) {
                    if (region.getId().startsWith("temp_")) {
                        event.setCancelled(true);
                        player.sendMessage(ChatColor.RED + "No puedes usar camas para explotar en una región temporal.");
                        return;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        Location loc = event.getLocation();
        RegionManager wgRegionManager = WorldGuard.getInstance()
            .getPlatform()
            .getRegionContainer()
            .get(BukkitAdapter.adapt(loc.getWorld()));
        if (wgRegionManager == null) return;
        Set<ProtectedRegion> regions = wgRegionManager.getApplicableRegions(BukkitAdapter.asBlockVector(loc)).getRegions();
        for (ProtectedRegion region : regions) {
            if (region.getId().startsWith("temp_")) {
                event.setCancelled(true);
                // Debug: mostrar mundo y tipo de explosión
                for (Player p : loc.getWorld().getPlayers()) {
                    if (debugManager.isDebugOptionEnabled(p, "explosion")) {
                        p.sendMessage(ChatColor.GRAY + "[DEBUG] Explosión cancelada en región temporal. Mundo: " + loc.getWorld().getName() + ", Tipo: EntityExplodeEvent");
                    }
                }
                return;
            }
        }
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        Location loc = event.getBlock().getLocation();
        RegionManager wgRegionManager = WorldGuard.getInstance()
            .getPlatform()
            .getRegionContainer()
            .get(BukkitAdapter.adapt(loc.getWorld()));
        if (wgRegionManager == null) return;
        Set<ProtectedRegion> regions = wgRegionManager.getApplicableRegions(BukkitAdapter.asBlockVector(loc)).getRegions();
        for (ProtectedRegion region : regions) {
            if (region.getId().startsWith("temp_")) {
                event.setCancelled(true);
                // Debug: mostrar mundo y tipo de explosión
                for (Player p : loc.getWorld().getPlayers()) {
                    if (debugManager.isDebugOptionEnabled(p, "explosion")) {
                        p.sendMessage(ChatColor.GRAY + "[DEBUG] Explosión cancelada en región temporal. Mundo: " + loc.getWorld().getName() + ", Tipo: BlockExplodeEvent");
                    }
                }
                return;
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        // Bloquear explosión de End Crystals por daño
        if (event.getEntity() instanceof EnderCrystal) {
            Location loc = event.getEntity().getLocation();
            RegionManager wgRegionManager = WorldGuard.getInstance()
                .getPlatform()
                .getRegionContainer()
                .get(BukkitAdapter.adapt(loc.getWorld()));
            if (wgRegionManager == null) return;
            Set<ProtectedRegion> regions = wgRegionManager.getApplicableRegions(BukkitAdapter.asBlockVector(loc)).getRegions();
            for (ProtectedRegion region : regions) {
                if (region.getId().startsWith("temp_")) {
                    event.setCancelled(true);
                    // Debug: mostrar mundo y tipo de interacción
                    if (event.getDamager() instanceof Player p && debugManager.isDebugOptionEnabled(p, "explosion")) {
                        p.sendMessage(ChatColor.GRAY + "[DEBUG] Interacción con End Crystal bloqueada en región temporal. Mundo: " + loc.getWorld().getName());
                    }
                    return;
                }
            }
        }
    }
}