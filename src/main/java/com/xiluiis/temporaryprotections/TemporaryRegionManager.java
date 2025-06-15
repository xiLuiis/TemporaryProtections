package com.xiluiis.temporaryprotections;
import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class TemporaryRegionManager {

    private final TemporaryProtections plugin;
    private final Map<String, UUID> tempRegionOwners = new HashMap<>();
    private final Map<String, Integer> tempRegionTimers = new HashMap<>();

    public TemporaryRegionManager(TemporaryProtections plugin) {
        this.plugin = plugin;
    }   
    
    public void crearRegionTemporal(Player player, RegionManager regionManager, ProtectedRegion original, int segundos) {
        // Crear un ID único para la región temporal
        String tempRegionId = "temp_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        // Obtener los límites de la región original
        BlockVector3 min = original.getMinimumPoint();
        BlockVector3 max = original.getMaximumPoint();

        // Crear la nueva región cúbica temporal
        ProtectedRegion tempRegion = new com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion(
            tempRegionId, min, max
        );

        // Asignar el dueño original
        tempRegion.getOwners().addPlayer(player.getUniqueId());

        // Configurar flags (puedes personalizar)
        tempRegion.setFlag(Flags.PVP, StateFlag.State.DENY);
        tempRegion.setFlag(Flags.FIRE_SPREAD, StateFlag.State.DENY);
        tempRegion.setFlag(Flags.TNT, StateFlag.State.DENY);
        tempRegion.setFlag(Flags.CREEPER_EXPLOSION, StateFlag.State.DENY);

        // Registrar la región temporal en WorldGuard
        regionManager.addRegion(tempRegion);

        // Guarda el dueño y el tiempo restante
        tempRegionOwners.put(tempRegionId, player.getUniqueId());
        tempRegionTimers.put(tempRegionId, segundos);

        player.sendMessage(ChatColor.GREEN + "¡Protección temporal de WorldGuard creada!");

        // Cuenta regresiva y eliminación automática
        new BukkitRunnable() {
            int tiempo = segundos;
            @Override
            public void run() {
                if (!tempRegionTimers.containsKey(tempRegionId)) {
                    cancel();
                    return;
                }
                tiempo--;
                tempRegionTimers.put(tempRegionId, tiempo);

                if (tiempo == 30 || tiempo == 10 || (tiempo <= 5 && tiempo > 0)) {
                    // Envía mensaje a todos los jugadores dentro de la región
                    UUID owner = tempRegionOwners.get(tempRegionId);
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        Location loc = p.getLocation();
                        Set<ProtectedRegion> regions = regionManager.getApplicableRegions(BukkitAdapter.asBlockVector(loc)).getRegions();
                        boolean isInRegion = false;
                        for (ProtectedRegion region : regions) {
                            if (region.getId().equals(tempRegionId)) {
                                isInRegion = true;
                                break;
                            }
                        }
                        // Envía el mensaje si el jugador está en la región o es el dueño
                        if (isInRegion || (owner != null && p.getUniqueId().equals(owner))) {
                            p.sendMessage(ChatColor.RED + "¡La protección temporal se eliminará en " + tiempo + " segundos!");
                        }
                    }
                }
                if (tiempo <= 0) {
                    regionManager.removeRegion(tempRegionId);
                    tempRegionOwners.remove(tempRegionId);
                    tempRegionTimers.remove(tempRegionId);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 20, 20); // Cada segundo
    }

    public boolean regionsOverlap(BlockVector3 minA, BlockVector3 maxA, BlockVector3 minB, BlockVector3 maxB) {
        boolean xOverlap = (minA.x() <= maxB.x() && maxA.x() >= minB.x());
        boolean zOverlap = (minA.z() <= maxB.z() && maxA.z() >= minB.z());
        // Solo elimina si colapsan en X y Z
        return xOverlap && zOverlap;
    }

    public UUID getTempRegionOwner(String regionId) {
        return tempRegionOwners.get(regionId);
    }

    public Integer getTempRegionTimer(String regionId) {
        return tempRegionTimers.get(regionId);
    }

    public Set<String> getAllTempRegionIds() {
        return tempRegionOwners.keySet();
    }

    public void removeTempRegion(String regionId) {
        tempRegionOwners.remove(regionId);
        tempRegionTimers.remove(regionId);
    }

    public boolean hasTempRegion(String regionId) {
        return tempRegionOwners.containsKey(regionId);
    }
}
