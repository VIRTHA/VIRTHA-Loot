package com.virtha;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Clase que maneja la interfaz gráfica para editar cofres de loot
 */
public class LootChestEditor implements Listener {

    private final VirthaLootPlugin plugin;
    private final Map<UUID, String> playerEditing; // Jugador -> Nombre del cofre que está editando
    private final Map<UUID, EditorMode> playerMode; // Jugador -> Modo de edición actual
    
    // Constantes para la GUI
    private static final String MAIN_MENU_TITLE = "§6Editor de Cofre de Loot";
    private static final String ITEMS_MENU_TITLE = "§6Editar Items del Cofre";
    private static final String CONFIG_MENU_TITLE = "§6Configuración del Cofre";
    
    // Modos de edición
    public enum EditorMode {
        MAIN_MENU,
        ITEMS_EDIT,
        CONFIG_EDIT
    }
    
    /**
     * Constructor del editor de cofres de loot
     * @param plugin Instancia del plugin principal
     */
    public LootChestEditor(VirthaLootPlugin plugin) {
        this.plugin = plugin;
        this.playerEditing = new HashMap<>();
        this.playerMode = new HashMap<>();
        
        // Registrar eventos
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Abre el editor de cofres de loot para un jugador
     * @param player Jugador que abrirá el editor
     * @param chestName Nombre del cofre a editar
     */
    public void openEditor(Player player, String chestName) {
        playerEditing.put(player.getUniqueId(), chestName);
        openMainMenu(player);
    }
    
    /**
     * Abre el menú principal del editor
     * @param player Jugador que verá el menú
     */
    private void openMainMenu(Player player) {
        playerMode.put(player.getUniqueId(), EditorMode.MAIN_MENU);
        String chestName = playerEditing.get(player.getUniqueId());
        
        Inventory inventory = Bukkit.createInventory(null, 27, MAIN_MENU_TITLE + ": " + chestName);
        
        // Botón para editar items
        ItemStack editItemsButton = createGuiItem(Material.CHEST, "§e§lEditar Items", 
                "§7Haz clic para configurar los items", "§7que aparecerán en el cofre");
        inventory.setItem(11, editItemsButton);
        
        // Botón para configuración general
        ItemStack configButton = createGuiItem(Material.REDSTONE, "§c§lConfiguración", 
                "§7Haz clic para modificar la configuración", "§7general del cofre");
        inventory.setItem(15, configButton);
        
        // Botón para guardar y salir
        ItemStack saveButton = createGuiItem(Material.EMERALD_BLOCK, "§a§lGuardar y Salir", 
                "§7Guarda los cambios y cierra el editor");
        inventory.setItem(26, saveButton);
        
        player.openInventory(inventory);
    }
    
    /**
     * Abre el menú de edición de items
     * @param player Jugador que verá el menú
     */
    private void openItemsMenu(Player player) {
        playerMode.put(player.getUniqueId(), EditorMode.ITEMS_EDIT);
        String chestName = playerEditing.get(player.getUniqueId());
        
        Inventory inventory = Bukkit.createInventory(null, 54, ITEMS_MENU_TITLE + ": " + chestName);
        
        // Cargar items existentes
        List<Map<?, ?>> itemsList = plugin.getLootChestsConfig().getMapList("chests." + chestName + ".items");
        int slot = 0;
        
        for (Map<?, ?> itemMap : itemsList) {
            if (slot >= 45) break; // Máximo 45 items (9 slots reservados para botones)
            
            String materialName = (String) itemMap.get("material");
            int amount = itemMap.containsKey("amount") ? (int) itemMap.get("amount") : 1;
            double chance = itemMap.containsKey("chance") ? ((Number) itemMap.get("chance")).doubleValue() : 100.0;
            
            try {
                Material material = Material.valueOf(materialName);
                ItemStack item = new ItemStack(material, amount);
                
                // Aplicar nombre personalizado si existe
                if (itemMap.containsKey("name")) {
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        meta.setDisplayName((String) itemMap.get("name"));
                        item.setItemMeta(meta);
                    }
                }
                
                // Añadir lore con información de probabilidad
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    List<String> lore = new ArrayList<>();
                    lore.add("§eProbabilidad: §f" + chance + "%");
                    
                    // Añadir lore personalizado si existe
                    if (itemMap.containsKey("lore") && itemMap.get("lore") instanceof List) {
                        List<?> customLore = (List<?>) itemMap.get("lore");
                        for (Object line : customLore) {
                            if (line instanceof String) {
                                lore.add((String) line);
                            }
                        }
                    }
                    
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }
                
                inventory.setItem(slot, item);
                slot++;
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Material inválido en la configuración: " + materialName);
            }
        }
        
        // Botón para volver al menú principal
        ItemStack backButton = createGuiItem(Material.ARROW, "§7§lVolver", 
                "§7Volver al menú principal");
        inventory.setItem(45, backButton);
        
        // Botón para guardar cambios
        ItemStack saveButton = createGuiItem(Material.EMERALD, "§a§lGuardar Cambios", 
                "§7Guarda los cambios realizados");
        inventory.setItem(49, saveButton);
        
        // Botón para configurar probabilidad del item seleccionado
        ItemStack chanceButton = createGuiItem(Material.PAPER, "§e§lConfigurar Probabilidad", 
                "§7Haz clic para configurar la probabilidad", "§7del item seleccionado");
        inventory.setItem(53, chanceButton);
        
        player.openInventory(inventory);
    }
    
    /**
     * Abre el menú de configuración general del cofre
     * @param player Jugador que verá el menú
     */
    private void openConfigMenu(Player player) {
        playerMode.put(player.getUniqueId(), EditorMode.CONFIG_EDIT);
        String chestName = playerEditing.get(player.getUniqueId());
        
        Inventory inventory = Bukkit.createInventory(null, 27, CONFIG_MENU_TITLE + ": " + chestName);
        
        // Obtener configuración actual
        int cooldown = plugin.getLootChestsConfig().getInt("chests." + chestName + ".cooldown");
        
        // Item para mostrar y editar el cooldown
        ItemStack cooldownItem = createGuiItem(Material.CLOCK, "§e§lCooldown: §f" + cooldown + " segundos", 
                "§7Haz clic para modificar el tiempo de espera", "§7entre aperturas del cofre");
        inventory.setItem(13, cooldownItem);
        
        // Botón para volver al menú principal
        ItemStack backButton = createGuiItem(Material.ARROW, "§7§lVolver", 
                "§7Volver al menú principal");
        inventory.setItem(18, backButton);
        
        // Botón para guardar cambios
        ItemStack saveButton = createGuiItem(Material.EMERALD, "§a§lGuardar Cambios", 
                "§7Guarda los cambios realizados");
        inventory.setItem(26, saveButton);
        
        player.openInventory(inventory);
    }
    
    /**
     * Guarda los items del cofre desde el inventario de edición
     * @param player Jugador que está editando
     * @param inventory Inventario de edición
     */
    private void saveItemsFromInventory(Player player, Inventory inventory) {
        String chestName = playerEditing.get(player.getUniqueId());
        List<Map<String, Object>> itemsList = new ArrayList<>();
        
        // Recorrer los primeros 45 slots (los últimos 9 son para botones)
        for (int i = 0; i < 45; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                Map<String, Object> itemMap = new HashMap<>();
                
                // Guardar propiedades básicas
                itemMap.put("material", item.getType().toString());
                itemMap.put("amount", item.getAmount());
                
                // Extraer probabilidad del lore
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    // Guardar nombre personalizado si existe
                    if (meta.hasDisplayName()) {
                        itemMap.put("name", meta.getDisplayName());
                    }
                    
                    // Procesar lore
                    if (meta.hasLore() && meta.getLore() != null) {
                        List<String> lore = meta.getLore();
                        double chance = 100.0; // Probabilidad por defecto
                        
                        // Extraer probabilidad del primer elemento del lore
                        if (!lore.isEmpty() && lore.get(0).startsWith("§eProbabilidad: §f")) {
                            String chanceStr = lore.get(0).replace("§eProbabilidad: §f", "").replace("%", "");
                            try {
                                chance = Double.parseDouble(chanceStr);
                            } catch (NumberFormatException e) {
                                plugin.getLogger().warning("Error al parsear probabilidad: " + chanceStr);
                            }
                            
                            // Crear nuevo lore sin la línea de probabilidad
                            List<String> customLore = new ArrayList<>();
                            for (int j = 1; j < lore.size(); j++) {
                                customLore.add(lore.get(j));
                            }
                            
                            if (!customLore.isEmpty()) {
                                itemMap.put("lore", customLore);
                            }
                        } else {
                            // Si no tiene formato de probabilidad, guardar todo el lore
                            itemMap.put("lore", lore);
                        }
                        
                        itemMap.put("chance", chance);
                    } else {
                        // Si no tiene lore, asignar probabilidad por defecto
                        itemMap.put("chance", 100.0);
                    }
                }
                
                itemsList.add(itemMap);
            }
        }
        
        // Guardar la lista de items en la configuración
        plugin.getLootChestsConfig().set("chests." + chestName + ".items", itemsList);
        try {
            plugin.getLootChestsConfig().save(plugin.getDataFolder() + "/lootchests.yml");
            player.sendMessage("§aItems del cofre guardados correctamente.");
        } catch (Exception e) {
            player.sendMessage("§cError al guardar los items del cofre.");
            plugin.getLogger().severe("Error al guardar lootchests.yml: " + e.getMessage());
        }
    }
    
    /**
     * Crea un item para la interfaz gráfica
     * @param material Material del item
     * @param name Nombre del item
     * @param lore Descripción del item
     * @return ItemStack configurado
     */
    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            
            if (lore != null && lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * Maneja los clics en el inventario
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        UUID playerId = player.getUniqueId();
        
        // Verificar si el jugador está editando un cofre
        if (!playerEditing.containsKey(playerId)) {
            return;
        }
        
        String title = event.getView().getTitle();
        
        // Menú principal
        if (title.startsWith(MAIN_MENU_TITLE)) {
            event.setCancelled(true); // Cancelar el evento para evitar mover items
            
            if (event.getRawSlot() == 11) { // Botón de editar items
                openItemsMenu(player);
            } else if (event.getRawSlot() == 15) { // Botón de configuración
                openConfigMenu(player);
            } else if (event.getRawSlot() == 26) { // Botón de guardar y salir
                playerEditing.remove(playerId);
                playerMode.remove(playerId);
                player.closeInventory();
                player.sendMessage("§aEditor de cofre cerrado. Cambios guardados.");
            }
        }
        // Menú de edición de items
        else if (title.startsWith(ITEMS_MENU_TITLE)) {
            // Permitir editar los items en los slots 0-44
            if (event.getRawSlot() >= 45) {
                event.setCancelled(true); // Cancelar para los botones
                
                if (event.getRawSlot() == 45) { // Botón de volver
                    openMainMenu(player);
                } else if (event.getRawSlot() == 49) { // Botón de guardar
                    saveItemsFromInventory(player, event.getInventory());
                } else if (event.getRawSlot() == 53) { // Botón de configurar probabilidad
                    // Aquí se implementaría la lógica para configurar la probabilidad
                    // Por simplicidad, solo mostraremos un mensaje
                    player.sendMessage("§ePara configurar la probabilidad, añade un item y luego usa el comando:");
                    player.sendMessage("§e/vloot chance <probabilidad>");
                }
            }
        }
        // Menú de configuración
        else if (title.startsWith(CONFIG_MENU_TITLE)) {
            event.setCancelled(true); // Cancelar el evento para evitar mover items
            
            if (event.getRawSlot() == 13) { // Item de cooldown
                // Aquí se implementaría la lógica para modificar el cooldown
                // Por simplicidad, solo mostraremos un mensaje
                player.sendMessage("§ePara cambiar el cooldown, usa el comando:");
                player.sendMessage("§e/vloot cooldown <nombre_cofre> <segundos>");
            } else if (event.getRawSlot() == 18) { // Botón de volver
                openMainMenu(player);
            } else if (event.getRawSlot() == 26) { // Botón de guardar
                player.sendMessage("§aConfiguración guardada.");
                openMainMenu(player);
            }
        }
    }
    
    /**
     * Maneja el cierre del inventario
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // Verificar si el jugador estaba editando items
        if (playerEditing.containsKey(playerId) && 
            playerMode.getOrDefault(playerId, EditorMode.MAIN_MENU) == EditorMode.ITEMS_EDIT) {
            
            // Si el inventario que se cerró es el de edición de items, guardar los cambios
            if (event.getView().getTitle().startsWith(ITEMS_MENU_TITLE)) {
                saveItemsFromInventory(player, event.getInventory());
                
                // Programar tarea para abrir el menú principal después de un tick
                // Esto evita problemas al cerrar un inventario y abrir otro inmediatamente
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    // Verificar si el jugador sigue editando (podría haber salido completamente)
                    if (playerEditing.containsKey(playerId)) {
                        openMainMenu(player);
                    }
                }, 1L);
            }
        }
        // Si se cierra cualquier otro menú del editor, eliminar al jugador de la lista de editores
        else if (playerEditing.containsKey(playerId) && 
                (event.getView().getTitle().startsWith(MAIN_MENU_TITLE) || 
                 event.getView().getTitle().startsWith(CONFIG_MENU_TITLE))) {
            
            playerEditing.remove(playerId);
            playerMode.remove(playerId);
        }
    }
}