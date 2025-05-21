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
import org.bukkit.inventory.meta.ItemMeta;
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
    private LootChestEditor lootChestEditor;
    
    // Mapa para almacenar los cooldowns de los jugadores para cada contenedor
    private final Map<String, Map<UUID, Long>> chestCooldowns = new ConcurrentHashMap<>();
    
    /**
     * Obtiene el mapa de cooldowns de los contenedores
     * @return Mapa con los cooldowns de los contenedores
     */
    public Map<String, Map<UUID, Long>> getChestCooldowns() {
        return chestCooldowns;
    }
    
    /**
     * Obtiene la configuración de los contenedores de loot
     * @return Configuración de los contenedores de loot
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
        
        // Inicializar el editor de cofres de loot
        lootChestEditor = new LootChestEditor(this);
        
        // Registrar el TabCompleter para los comandos
        VirthaLootTabCompleter tabCompleter = new VirthaLootTabCompleter(this);
        getCommand("virthaloot").setTabCompleter(tabCompleter);
        
        // Verificar si PlaceholderAPI está presente
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            getLogger().info("PlaceholderAPI encontrado! Integración activada.");
            // Registrar la expansión de PlaceholderAPI
            new VirthaLootExpansion(this).register();
        } else {
            getLogger().warning("PlaceholderAPI no encontrado. Los placeholders no funcionarán.");
        }
        
        getLogger().info("VirthaLoot ha sido habilitado correctamente!");
        getLogger().info("TabCompleter registrado para autocompletado de comandos.");
    }
    
    @Override
    public void onDisable() {
        // Guardar datos antes de desactivar el plugin
        saveCooldowns();
        getLogger().info("VirthaLoot ha sido deshabilitado correctamente!");
    }
    
    private void setupCustomConfigs() {
        // Configuración para los contenedores de loot
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
                
            case "chance":
                if (!player.hasPermission("virthaloot.edit")) {
                    player.sendMessage("§cNo tienes permiso para usar este comando.");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage("§cUso: /vloot chance <probabilidad>");
                    return true;
                }
                try {
                    double chance = Double.parseDouble(args[1]);
                    if (chance < 0 || chance > 100) {
                        player.sendMessage("§cLa probabilidad debe estar entre 0 y 100.");
                        return true;
                    }
                    setItemChance(player, chance);
                } catch (NumberFormatException e) {
                    player.sendMessage("§cLa probabilidad debe ser un número válido.");
                }
                return true;
                
            case "cooldown":
                if (!player.hasPermission("virthaloot.edit")) {
                    player.sendMessage("§cNo tienes permiso para usar este comando.");
                    return true;
                }
                if (args.length < 3) {
                    player.sendMessage("§cUso: /vloot cooldown <nombre> <segundos>");
                    return true;
                }
                try {
                    String chestName = args[1];
                    int cooldown = Integer.parseInt(args[2]);
                    if (cooldown < 0) {
                        player.sendMessage("§cEl cooldown no puede ser negativo.");
                        return true;
                    }
                    setChestCooldown(player, chestName, cooldown);
                } catch (NumberFormatException e) {
                    player.sendMessage("§cEl cooldown debe ser un número entero válido.");
                }
                return true;
                
            default:
                sendHelpMessage(player);
                return true;
        }
    }
    
    private void sendHelpMessage(Player player) {
        player.sendMessage("§6=== VirthaLoot - Comandos ===§r");
        player.sendMessage("§e/vloot create <nombre> <cooldown> §7- Crea un nuevo contenedor de loot en el bloque seleccionado");
        player.sendMessage("§e/vloot edit <nombre> §7- Edita el contenido de un contenedor de loot");
        player.sendMessage("§e/vloot info <nombre> §7- Muestra información sobre un contenedor de loot");
        player.sendMessage("§e/vloot list §7- Lista todos los contenedores de loot disponibles");
        player.sendMessage("§e/vloot delete <nombre> §7- Elimina un contenedor de loot");
        player.sendMessage("§e/vloot chance <probabilidad> §7- Configura la probabilidad del item seleccionado");
        player.sendMessage("§e/vloot cooldown <nombre> <segundos> §7- Modifica el cooldown de un contenedor");
    }
    
    /**
     * Configura la probabilidad de aparición del item que el jugador tiene en la mano
     * @param player Jugador que está configurando la probabilidad
     * @param chance Probabilidad de aparición (0-100)
     */
    private void setItemChance(Player player, double chance) {
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage("§cDebes tener un item en la mano para configurar su probabilidad.");
            return;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            player.sendMessage("§cEste item no puede tener metadatos.");
            return;
        }
        
        // Crear o actualizar el lore con la información de probabilidad
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        
        // Si ya existe una línea de probabilidad, actualizarla
        boolean foundChanceLine = false;
        if (lore != null) {
            for (int i = 0; i < lore.size(); i++) {
                if (lore.get(i).startsWith("§eProbabilidad: §f")) {
                    lore.set(i, "§eProbabilidad: §f" + chance + "%");
                    foundChanceLine = true;
                    break;
                }
            }
        } else {
            lore = new ArrayList<>();
        }
        
        // Si no existe, añadir al principio
        if (!foundChanceLine) {
            lore.add(0, "§eProbabilidad: §f" + chance + "%");
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        player.sendMessage("§aProbabilidad del item configurada a §f" + chance + "%§a.");
    }
    
    /**
     * Configura el cooldown de un contenedor de loot
     * @param player Jugador que está configurando el cooldown
     * @param chestName Nombre del contenedor
     * @param cooldown Tiempo de cooldown en segundos
     */
    private void setChestCooldown(Player player, String chestName, int cooldown) {
        // Verificar si existe el contenedor
        if (!lootChestsConfig.contains("chests." + chestName)) {
            player.sendMessage("§cNo existe ningún contenedor de loot con ese nombre.");
            return;
        }
        
        // Actualizar el cooldown
        lootChestsConfig.set("chests." + chestName + ".cooldown", cooldown);
        saveLootChestsConfig();
        
        player.sendMessage("§aCooldown del contenedor '" + chestName + "' actualizado a §f" + cooldown + " segundos§a.");
    }
    
    private void createLootChest(Player player, String name, int cooldownSeconds) {
        Block targetBlock = player.getTargetBlock(null, 5);
        if (targetBlock.getType() == Material.AIR) {
            player.sendMessage("§cDebes estar mirando a un bloque sólido para crear un contenedor de loot.");
            return;
        }
        
        // Verificar si ya existe un contenedor con ese nombre
        if (lootChestsConfig.contains("chests." + name)) {
            player.sendMessage("§cYa existe un contenedor de loot con ese nombre.");
            return;
        }
        
        // Guardar la ubicación y tipo del bloque
        Location loc = targetBlock.getLocation();
        String locString = loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
        String blockType = targetBlock.getType().toString();
        
        lootChestsConfig.set("chests." + name + ".location", locString);
        lootChestsConfig.set("chests." + name + ".blockType", blockType);
        lootChestsConfig.set("chests." + name + ".cooldown", cooldownSeconds);
        lootChestsConfig.set("chests." + name + ".items", new ArrayList<>()); // Lista vacía para los items
        
        saveLootChestsConfig();
        player.sendMessage("§aContenedor de loot '" + name + "' creado correctamente con un cooldown de " + cooldownSeconds + " segundos.");
    }
    
    
    private void editLootChest(Player player, String name) {
        // Verificar si existe el cofre
        if (!lootChestsConfig.contains("chests." + name)) {
            player.sendMessage("§cNo existe ningún cofre de loot con ese nombre.");
            return;
        }
        
        // Abrir el editor de cofres de loot
        if (lootChestEditor == null) {
            lootChestEditor = new LootChestEditor(this);
        }
        
        lootChestEditor.openEditor(player, name);
        player.sendMessage("§aAbriendo editor de cofre de loot '" + name + "'...");
    }
    
    private void showChestInfo(Player player, String name) {
        // Verificar si existe el contenedor
        if (!lootChestsConfig.contains("chests." + name)) {
            player.sendMessage("§cNo existe ningún contenedor de loot con ese nombre.");
            return;
        }
        
        String locationStr = lootChestsConfig.getString("chests." + name + ".location");
        int cooldown = lootChestsConfig.getInt("chests." + name + ".cooldown");
        String blockType = lootChestsConfig.getString("chests." + name + ".blockType", "CHEST"); // Por defecto CHEST para compatibilidad
        
        player.sendMessage("§6=== Información del contenedor '" + name + "' ===§r");
        player.sendMessage("§eUbicación: §7" + locationStr);
        player.sendMessage("§eTipo de bloque: §7" + blockType);
        player.sendMessage("§eCooldown: §7" + cooldown + " segundos");
        
        // Si el jugador tiene un cooldown activo para este contenedor, mostrar tiempo restante
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
            player.sendMessage("§cNo hay contenedores de loot registrados.");
            return;
        }
        
        player.sendMessage("§6=== Contenedores de Loot Disponibles ===§r");
        for (String chestName : chestsSection.getKeys(false)) {
            String locationStr = lootChestsConfig.getString("chests." + chestName + ".location");
            int cooldown = lootChestsConfig.getInt("chests." + chestName + ".cooldown");
            String blockType = lootChestsConfig.getString("chests." + chestName + ".blockType", "CHEST"); // Por defecto CHEST para compatibilidad
            player.sendMessage("§e" + chestName + " §7- Tipo: " + blockType + " - Cooldown: " + cooldown + "s - Ubicación: " + locationStr);
        }
    }
    
    private void deleteLootChest(Player player, String name) {
        // Verificar si existe el contenedor
        if (!lootChestsConfig.contains("chests." + name)) {
            player.sendMessage("§cNo existe ningún contenedor de loot con ese nombre.");
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
        
        player.sendMessage("§aContenedor de loot '" + name + "' eliminado correctamente.");
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) {
            return;
        }
        
        Player player = event.getPlayer();
        Location loc = clickedBlock.getLocation();
        String locString = loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
        
        // Buscar si este bloque es un contenedor de loot
        ConfigurationSection chestsSection = lootChestsConfig.getConfigurationSection("chests");
        if (chestsSection == null) {
            return;
        }
        
        for (String chestName : chestsSection.getKeys(false)) {
            String chestLoc = lootChestsConfig.getString("chests." + chestName + ".location");
            if (chestLoc != null && chestLoc.equals(locString)) {
                // Verificar si el tipo de bloque coincide (para compatibilidad con versiones anteriores)
                String configBlockType = lootChestsConfig.getString("chests." + chestName + ".blockType");
                if (configBlockType != null && !configBlockType.equals(clickedBlock.getType().toString())) {
                    // El bloque ha sido reemplazado por otro tipo
                    continue;
                }
                
                // Es un contenedor de loot, verificar cooldown
                if (!player.hasPermission("virthaloot.use")) {
                    player.sendMessage("§cNo tienes permiso para usar contenedores de loot.");
                    event.setCancelled(true);
                    return;
                }
                
                // Verificar si el contenedor tiene items configurados
                List<Map<?, ?>> itemsList = lootChestsConfig.getMapList("chests." + chestName + ".items");
                if (itemsList == null || itemsList.isEmpty()) {
                    player.sendMessage("§cEste contenedor no tiene recompensas disponibles.");
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
                    String message = "§cDebes esperar §e" + timeLeft + " segundos §cpara volver a usar este contenedor.";
                    
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
                
                // Ejecutar comando personalizado si está configurado (compatibilidad con versiones anteriores)
                String command = lootChestsConfig.getString("chests." + chestName + ".command");
                if (command != null && !command.isEmpty()) {
                    // Reemplazar placeholder del nombre del jugador
                    command = command.replace("%player_name%", player.getName());
                    
                    // Ejecutar el comando como consola
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                }
                
                // Ejecutar comandos configurados como items con probabilidades
                List<Map<?, ?>> commandsList = lootChestsConfig.getMapList("chests." + chestName + ".commands");
                if (commandsList != null && !commandsList.isEmpty()) {
                    Random random = new Random();
                    
                    for (Map<?, ?> commandMap : commandsList) {
                        String cmdStr = (String) commandMap.get("command");
                        double chance = commandMap.containsKey("chance") ? ((Number) commandMap.get("chance")).doubleValue() : 100.0;
                        
                        // Verificar si el comando debe ejecutarse según su probabilidad
                        if (random.nextDouble() * 100 <= chance) {
                            // Reemplazar placeholder del nombre del jugador
                            cmdStr = cmdStr.replace("%player_name%", player.getName());
                            
                            // Ejecutar el comando como consola
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmdStr);
                        }
                    }
                }
                
                // Cancelar el evento para que no se abra el contenedor normal (si es un cofre)
                event.setCancelled(true);
                return;
            }
        }
    }
    
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        
        Player player = event.getPlayer();
        Location loc = block.getLocation();
        String locString = loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
        
        // Verificar si es un contenedor de loot
        ConfigurationSection chestsSection = lootChestsConfig.getConfigurationSection("chests");
        if (chestsSection == null) {
            return;
        }
        
        for (String chestName : chestsSection.getKeys(false)) {
            String chestLoc = lootChestsConfig.getString("chests." + chestName + ".location");
            if (chestLoc != null && chestLoc.equals(locString)) {
                // Verificar si el tipo de bloque coincide (para compatibilidad con versiones anteriores)
                String configBlockType = lootChestsConfig.getString("chests." + chestName + ".blockType");
                if (configBlockType != null && !configBlockType.equals(block.getType().toString())) {
                    // El bloque ha sido reemplazado por otro tipo
                    continue;
                }
                
                // Es un contenedor de loot
                if (!player.hasPermission("virthaloot.admin")) {
                    player.sendMessage("§cNo tienes permiso para destruir contenedores de loot.");
                    event.setCancelled(true);
                    return;
                }
                
                player.sendMessage("§aHas destruido el contenedor de loot '" + chestName + "'. Usa §e/vloot delete " + chestName + " §apara eliminarlo completamente.");
                return;
            }
        }
    }
}