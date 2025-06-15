// TemporaryProtections.java
// Este plugin es un ejemplo básico de cómo crear un plugin de Minecraft con Bukkit/Spigot.
// Permite a los jugadores activar o desactivar eventos específicos relacionados con ProtectionStones.
// Asegúrate de tener las dependencias necesarias en tu proyecto, como Bukkit, Spigot y ProtectionStones.
// También incluye un comando de depuración para verificar el estado de ProtectionStones.

// Hecho por xiLuiis. 
// https://www.youtube.com/@xshadowsystem

package com.xiluiis.temporaryprotections;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;

import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import dev.espi.protectionstones.PSRegion;
import dev.espi.protectionstones.ProtectionStones;
import dev.espi.protectionstones.event.PSRemoveEvent;
import dev.espi.protectionstones.PSProtectBlock;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldedit.math.BlockVector3;

import java.util.*;

public class TemporaryProtections extends JavaPlugin implements Listener {

    // Guarda los jugadores actualmente dentro de una región temporal
    private final Set<UUID> enRegionTemporal = new HashSet<>();
    // Guarda el dueño de cada región temporal
    private final Map<String, UUID> tempRegionOwners = new HashMap<>();
    // Guarda el tiempo restante de cada región temporal
    private final Map<String, Integer> tempRegionTimers = new HashMap<>();

    private final Map<UUID, Set<String>> playerEvents = new HashMap<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
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
        player.sendMessage(ChatColor.AQUA + "=== tp Help ===");
        player.sendMessage(ChatColor.YELLOW + "/tmpp help" + ChatColor.WHITE + " - Muestra este mensaje.");
        player.sendMessage(ChatColor.YELLOW + "/tmpp enable <evento> yes|no" + ChatColor.WHITE + " - Activa o desactiva un evento.");
        player.sendMessage(ChatColor.GRAY + "Ejemplo: /tmpp enable blockcoords yes");
        player.sendMessage(ChatColor.YELLOW + "Eventos disponibles: blockcoords");
        player.sendMessage(ChatColor.YELLOW + "Usa /tmpp debug para ver el estado de ProtectionStones.");
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

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        Player player = e.getPlayer();
        Location loc = e.getBlock().getLocation();

        RegionManager regionManager = WorldGuard.getInstance()
            .getPlatform()
            .getRegionContainer()
            .get(BukkitAdapter.adapt(loc.getWorld()));
        if (regionManager == null) return;

        Set<ProtectedRegion> regions = regionManager.getApplicableRegions(BukkitAdapter.asBlockVector(loc)).getRegions();
        boolean enTemporal = false;
        for (ProtectedRegion region : regions) {
            if (region.getId().startsWith("temp_")) {
                enTemporal = true;
                break;
            }
        }

        String blockType = e.getBlockPlaced().getType().name();
        PSProtectBlock psBlock = ProtectionStones.getBlockOptions(blockType);

        // Si está en región temporal y NO es piedra de protección, cancela
        if (enTemporal && (psBlock == null || !ProtectionStones.isProtectBlockType(blockType))) {
            e.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Solo puedes colocar piedras de protección en una región temporal.");
            return;
        }

        // Si el bloque NO es de Protection Stones, no sigas (evita el NullPointerException)
        if (psBlock == null) {
            return;
        }

        // Calcula los límites de la nueva protección de PS usando los radios reales
        BlockVector3 placed = BlockVector3.at(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
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
        for (Map.Entry<String, UUID> entry : tempRegionOwners.entrySet()) {
            String tempRegionId = entry.getKey();
            UUID tempOwner = entry.getValue();
            if (!player.getUniqueId().equals(tempOwner)) continue;
            ProtectedRegion tempRegion = regionManager.getRegion(tempRegionId);
            if (tempRegion == null) continue;

            // Mensajes de depuración en el chat del jugador
            player.sendMessage(ChatColor.GRAY + "=== DEPURACIÓN DE SOLAPAMIENTO ===");
            player.sendMessage(ChatColor.GRAY + "Revisando temporal: " + tempRegionId + " dueño: " + tempOwner);
            player.sendMessage(ChatColor.GRAY + "Nueva protección min: " + min + " max: " + max);
            player.sendMessage(ChatColor.GRAY + "Temporal min: " + tempRegion.getMinimumPoint() + " max: " + tempRegion.getMaximumPoint());

            if (regionsOverlap(tempRegion.getMinimumPoint(), tempRegion.getMaximumPoint(), min, max)) {
                regionesAEliminar.add(tempRegionId);
            }
        }
        for (String tempRegionId : regionesAEliminar) {
            regionManager.removeRegion(tempRegionId);
            tempRegionOwners.remove(tempRegionId);
            tempRegionTimers.remove(tempRegionId);
            player.sendMessage(ChatColor.YELLOW + "Se eliminó una protección temporal solapada: " + tempRegionId);
        }
    }

    // Método auxiliar para detectar solapamiento de regiones cúbicas
    private boolean regionsOverlap(BlockVector3 minA, BlockVector3 maxA, BlockVector3 minB, BlockVector3 maxB) {
        boolean xOverlap = (minA.x() <= maxB.x() && maxA.x() >= minB.x());
        boolean zOverlap = (minA.z() <= maxB.z() && maxA.z() >= minB.z());
        // Solo elimina si colapsan en X y Z
        return xOverlap && zOverlap;
    }

    @EventHandler
    public void onPSRemove(PSRemoveEvent event) {
        Player player = event.getPlayer();
        PSRegion oldRegion = event.getRegion();

        if (player != null && oldRegion != null) {
            player.sendMessage(ChatColor.RED + "¡Has eliminado una protección! Se creará una protección temporal por 60 segundos.");
            getLogger().info("Se ejecutó onPSRemove para " + player.getName());

            // Obtener el WorldGuard RegionManager
            World world = oldRegion.getWorld();
            com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);
            RegionManager regionManager = WorldGuard.getInstance()
                .getPlatform()
                .getRegionContainer()
                .get(weWorld);
            if (regionManager == null) {
                player.sendMessage(ChatColor.RED + "No se pudo acceder a WorldGuard.");
                return;
            }

            ProtectedRegion original = oldRegion.getWGRegion();
            crearRegionTemporal(player, regionManager, original, 60);
        }
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
        }.runTaskTimer(this, 20, 20); // Cada segundo
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        // Removido el bloqueo de /ps merge
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location loc = player.getLocation();
        World world = loc.getWorld();

        RegionManager regionManager = WorldGuard.getInstance()
            .getPlatform()
            .getRegionContainer()
            .get(BukkitAdapter.adapt(world));
        if (regionManager == null) return;

        Set<ProtectedRegion> regions = regionManager.getApplicableRegions(BukkitAdapter.asBlockVector(loc)).getRegions();
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
            UUID owner = tempRegionOwners.get(tempRegionId);
            String ownerName = owner != null ? Bukkit.getOfflinePlayer(owner).getName() : "desconocido";
            player.sendMessage(ChatColor.GOLD + "¡Entraste en una región temporal de " + ownerName + "!");
        } else if (!estaEnTemporal && enRegionTemporal.contains(uuid)) {
            enRegionTemporal.remove(uuid);
            player.sendMessage(ChatColor.YELLOW + "Saliste de una región temporal.");
        }
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
}