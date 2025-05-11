package com.virtha;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class VirthaLootPlugin extends JavaPlugin implements Listener {

    private File lootChestsFile;
    private FileConfiguration lootChestsConfig;
    private File cooldownsFile;
    private FileConfiguration cooldownsConfig;
    private LootManager lootManager;
    
    // Mapa para almacenar los cooldowns de los jugadores para cada cofre
    private final Map<String, Map<UUID, Long>> chestCooldowns = new ConcurrentHashMap<>();
    
    /**
     * Obtiene el mapa de cooldowns de los cofres
     * @return Mapa con los cooldowns de los cofres
     */
    public Map<String, Map<UUID, Long>> getChestCooldowns() {
        return chestCooldowns;
    }
    
    /**
     * Obtiene la configuración de los cofres de loot
     * @return Configuración de los cofres de loot
     */
    public FileConfiguration getLootChestsConfig() {
        return lootChestsConfig;
    }
    
    /**
     * Obtiene el gestor de loot del plugin
     * @return Instancia del LootManager
     */
    public LootManager getLootManager() {
        return lootManager;
    }
    
    @Override
    public void onEnable() {
        // Registrar eventos
        getServer().getPluginManager().registerEvents(this, this);
        
        // Crear configuración por defecto
        saveDefaultConfig();
        
        // Inicializar archivos de configuración personalizados
        setupCustomConfigs();
        
        // Cargar cooldowns existentes
        loadCooldowns();
        
        // Inicializar el gestor de loot
        lootManager = new LootManager(this);
        
        // Verificar si PlaceholderAPI está presente
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            getLogger().info("PlaceholderAPI encontrado! Integración activada.");
            // Registrar la expansión de PlaceholderAPI
            new VirthaLootExpansion(this).register();
        } else {
            getLogger().warning("PlaceholderAPI no encontrado. Los placeholders no funcionarán.");
        }
        
        getLogger().info("VirthaLoot ha sido habilitado correctamente!");
    }
    
    @Override
    public void onDisable() {
        // Guardar datos antes de desactivar el plugin
        saveCooldowns();
        getLogger().info("VirthaLoot ha sido deshabilitado correctamente!");
    }
    
    private void setupCustomConfigs() {
        // Configuración para los cofres de loot
        lootChestsFile = new File(getDataFolder(), "lootchests.yml");
        if (!lootChestsFile.exists()) {
            lootChestsFile.getParentFile().mkdirs();
            saveResource("lootchests.yml", false);
        }
        lootChestsConfig = YamlConfiguration.loadConfiguration(lootChestsFile);
        
        // Configuración para los cooldowns
        cooldownsFile = new File(getDataFolder(), "cooldowns.yml");
        if (!cooldownsFile.exists()) {
            cooldownsFile.getParentFile().mkdirs();
            try {
                cooldownsFile.createNewFile();
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "No se pudo crear el archivo cooldowns.yml", e);
            }
        }
        cooldownsConfig = YamlConfiguration.loadConfiguration(cooldownsFile);
    }
    
    private void saveLootChestsConfig() {
        try {
            lootChestsConfig.save(lootChestsFile);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "No se pudo guardar el archivo lootchests.yml", e);
        }
    }
    
    private void saveCooldowns() {
        // Convertir el mapa de cooldowns a formato que se puede guardar en YAML
        for (Map.Entry<String, Map<UUID, Long>> entry : chestCooldowns.entrySet()) {
            String chestId = entry.getKey();
            Map<UUID, Long> playerCooldowns = entry.getValue();
            
            for (Map.Entry<UUID, Long> cooldownEntry : playerCooldowns.entrySet()) {
                UUID playerId = cooldownEntry.getKey();
                Long cooldownTime = cooldownEntry.getValue();
                
                cooldownsConfig.set("cooldowns." + chestId + "." + playerId.toString(), cooldownTime);
            }
        }
        
        try {
            cooldownsConfig.save(cooldownsFile);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "No se pudo guardar el archivo cooldowns.yml", e);
        }
    }
    
    private void loadCooldowns() {
        ConfigurationSection cooldownsSection = cooldownsConfig.getConfigurationSection("cooldowns");
        if (cooldownsSection != null) {
            for (String chestId : cooldownsSection.getKeys(false)) {
                ConfigurationSection chestSection = cooldownsSection.getConfigurationSection(chestId);
                if (chestSection != null) {
                    Map<UUID, Long> playerCooldowns = new HashMap<>();
                    
                    for (String playerIdStr : chestSection.getKeys(false)) {
                        UUID playerId = UUID.fromString(playerIdStr);
                        long cooldownTime = chestSection.getLong(playerIdStr);
                        playerCooldowns.put(playerId, cooldownTime);
                    }
                    
                    chestCooldowns.put(chestId, playerCooldowns);
                }
            }
        }
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cEste comando solo puede ser usado por jugadores.");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            sendHelpMessage(player);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "create":
                if (!player.hasPermission("virthaloot.create")) {
                    player.sendMessage("§cNo tienes permiso para usar este comando.");
                    return true;
                }
                if (args.length < 3) {
                    player.sendMessage("§cUso: /vloot create <nombre> <cooldown_segundos>");
                    return true;
                }
                createLootChest(player, args[1], Integer.parseInt(args[2]));
                return true;
                
            case "edit":
                if (!player.hasPermission("virthaloot.edit")) {
                    player.sendMessage("§cNo tienes permiso para usar este comando.");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage("§cUso: /vloot edit <nombre>");
                    return true;
                }
                editLootChest(player, args[1]);
                return true;
                
            case "info":
                if (args.length < 2) {
                    player.sendMessage("§cUso: /vloot info <nombre>");
                    return true;
                }
                showChestInfo(player, args[1]);
                return true;
                
            case "list":
                listLootChests(player);
                return true;
                
            case "delete":
                if (!player.hasPermission("virthaloot.admin")) {
                    player.sendMessage("§cNo tienes permiso para usar este comando.");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage("§cUso: /vloot delete <nombre>");
                    return true;
                }
                deleteLootChest(player, args[1]);
                return true;
                
            default:
                sendHelpMessage(player);
                return true;
        }
    }
    
    private void sendHelpMessage(Player player) {
        player.sendMessage("§6=== VirthaLoot - Comandos ===§r");
        player.sendMessage("§e/vloot create <nombre> <cooldown> §7- Crea un nuevo cofre de loot");
        player.sendMessage("§e/vloot edit <nombre> §7- Edita el contenido de un cofre de loot");
        player.sendMessage("§e/vloot info <nombre> §7- Muestra información sobre un cofre de loot");
        player.sendMessage("§e/vloot list §7- Lista todos los cofres de loot disponibles");
        player.sendMessage("§e/vloot delete <nombre> §7- Elimina un cofre de loot");
    }
    
    private void createLootChest(Player player, String name, int cooldownSeconds) {
        Block targetBlock = player.getTargetBlock(null, 5);
        if (targetBlock.getType() != Material.CHEST) {
            player.sendMessage("§cDebes estar mirando a un cofre para crear un cofre de loot.");
            return;
        }
        
        // Verificar si ya existe un cofre con ese nombre
        if (lootChestsConfig.contains("chests." + name)) {
            player.sendMessage("§cYa existe un cofre de loot con ese nombre.");
            return;
        }
        
        // Guardar la ubicación del cofre
        Location loc = targetBlock.getLocation();
        String locString = loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
        
        lootChestsConfig.set("chests." + name + ".location", locString);
        lootChestsConfig.set("chests." + name + ".cooldown", cooldownSeconds);
        lootChestsConfig.set("chests." + name + ".items", new ArrayList<>()); // Lista vacía para los items
        
        saveLootChestsConfig();
        player.sendMessage("§aCofre de loot '" + name + "' creado correctamente con un cooldown de " + cooldownSeconds + " segundos.");
    }
    
    private void editLootChest(Player player, String name) {
        // Verificar si existe el cofre
        if (!lootChestsConfig.contains("chests." + name)) {
            player.sendMessage("§cNo existe ningún cofre de loot con ese nombre.");
            return;
        }
        
        // Aquí se implementaría la lógica para editar el contenido del cofre
        // Por simplicidad, solo mostraremos un mensaje
        player.sendMessage("§aAbriendo editor de cofre de loot '" + name + "'...");
        player.sendMessage("§7(Esta funcionalidad requeriría una implementación de GUI más compleja)");
    }
    
    private void showChestInfo(Player player, String name) {
        // Verificar si existe el cofre
        if (!lootChestsConfig.contains("chests." + name)) {
            player.sendMessage("§cNo existe ningún cofre de loot con ese nombre.");
            return;
        }
        
        String locationStr = lootChestsConfig.getString("chests." + name + ".location");
        int cooldown = lootChestsConfig.getInt("chests." + name + ".cooldown");
        
        player.sendMessage("§6=== Información del cofre '" + name + "' ===§r");
        player.sendMessage("§eUbicación: §7" + locationStr);
        player.sendMessage("§eCooldown: §7" + cooldown + " segundos");
        
        // Si el jugador tiene un cooldown activo para este cofre, mostrar tiempo restante
        Map<UUID, Long> playerCooldowns = chestCooldowns.get(name);
        if (playerCooldowns != null && playerCooldowns.containsKey(player.getUniqueId())) {
            long cooldownTime = playerCooldowns.get(player.getUniqueId());
            long currentTime = System.currentTimeMillis();
            long timeLeft = (cooldownTime - currentTime) / 1000; // Convertir a segundos
            
            if (timeLeft > 0) {
                player.sendMessage("§eTiempo restante: §c" + timeLeft + " segundos");
            } else {
                player.sendMessage("§eTiempo restante: §aDisponible ahora");
            }
        } else {
            player.sendMessage("§eTiempo restante: §aDisponible ahora");
        }
    }
    
    private void listLootChests(Player player) {
        ConfigurationSection chestsSection = lootChestsConfig.getConfigurationSection("chests");
        if (chestsSection == null || chestsSection.getKeys(false).isEmpty()) {
            player.sendMessage("§cNo hay cofres de loot registrados.");
            return;
        }
        
        player.sendMessage("§6=== Cofres de Loot Disponibles ===§r");
        for (String chestName : chestsSection.getKeys(false)) {
            String locationStr = lootChestsConfig.getString("chests." + chestName + ".location");
            int cooldown = lootChestsConfig.getInt("chests." + chestName + ".cooldown");
            player.sendMessage("§e" + chestName + " §7- Cooldown: " + cooldown + "s - Ubicación: " + locationStr);
        }
    }
    
    private void deleteLootChest(Player player, String name) {
        // Verificar si existe el cofre
        if (!lootChestsConfig.contains("chests." + name)) {
            player.sendMessage("§cNo existe ningún cofre de loot con ese nombre.");
            return;
        }
        
        lootChestsConfig.set("chests." + name, null);
        saveLootChestsConfig();
        
        // Eliminar también los cooldowns asociados
        chestCooldowns.remove(name);
        cooldownsConfig.set("cooldowns." + name, null);
        try {
            cooldownsConfig.save(cooldownsFile);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "No se pudo guardar el archivo cooldowns.yml", e);
        }
        
        player.sendMessage("§aCofre de loot '" + name + "' eliminado correctamente.");
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null || clickedBlock.getType() != Material.CHEST) {
            return;
        }
        
        Player player = event.getPlayer();
        Location loc = clickedBlock.getLocation();
        String locString = loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
        
        // Buscar si este cofre es un cofre de loot
        ConfigurationSection chestsSection = lootChestsConfig.getConfigurationSection("chests");
        if (chestsSection == null) {
            return;
        }
        
        for (String chestName : chestsSection.getKeys(false)) {
            String chestLoc = lootChestsConfig.getString("chests." + chestName + ".location");
            if (chestLoc != null && chestLoc.equals(locString)) {
                // Es un cofre de loot, verificar cooldown
                if (!player.hasPermission("virthaloot.use")) {
                    player.sendMessage("§cNo tienes permiso para usar cofres de loot.");
                    event.setCancelled(true);
                    return;
                }
                
                // Verificar cooldown
                Map<UUID, Long> playerCooldowns = chestCooldowns.computeIfAbsent(chestName, k -> new ConcurrentHashMap<>());
                long currentTime = System.currentTimeMillis();
                long cooldownTime = playerCooldowns.getOrDefault(player.getUniqueId(), 0L);
                
                if (cooldownTime > currentTime) {
                    // El jugador está en cooldown
                    long timeLeft = (cooldownTime - currentTime) / 1000; // Convertir a segundos
                    String message = "§cDebes esperar §e" + timeLeft + " segundos §cpara volver a abrir este cofre.";
                    
                    // Aplicar placeholders si PlaceholderAPI está disponible
                    if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                        message = PlaceholderAPI.setPlaceholders(player, message);
                    }
                    
                    player.sendMessage(message);
                    event.setCancelled(true);
                    return;
                }
                
                // Establecer nuevo cooldown
                int cooldownSeconds = lootChestsConfig.getInt("chests." + chestName + ".cooldown");
                playerCooldowns.put(player.getUniqueId(), currentTime + (cooldownSeconds * 1000L));
                
                // Generar y entregar loot personalizado al jugador
                List<ItemStack> lootItems = lootManager.generateLoot(player, chestName);
                lootManager.giveLoot(player, lootItems);
                
                // Cancelar el evento para que no se abra el cofre normal
                event.setCancelled(true);
                return;
            }
        }
    }
    
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.CHEST) {
            return;
        }
        
        Player player = event.getPlayer();
        Location loc = block.getLocation();
        String locString = loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
        
        // Verificar si es un cofre de loot
        ConfigurationSection chestsSection = lootChestsConfig.getConfigurationSection("chests");
        if (chestsSection == null) {
            return;
        }
        
        for (String chestName : chestsSection.getKeys(false)) {
            String chestLoc = lootChestsConfig.getString("chests." + chestName + ".location");
            if (chestLoc != null && chestLoc.equals(locString)) {
                // Es un cofre de loot
                if (!player.hasPermission("virthaloot.admin")) {
                    player.sendMessage("§cNo tienes permiso para destruir cofres de loot.");
                    event.setCancelled(true);
                    return;
                }
                
                player.sendMessage("§aHas destruido el cofre de loot '" + chestName + "'. Usa §e/vloot delete " + chestName + " §apara eliminarlo completamente.");
                return;
            }
        }
    }
}