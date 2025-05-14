package com.virtha;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
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
    private static final String ENCHANT_MENU_TITLE = "§5Añadir Encantamientos";
    private static final String PREVIEW_MENU_TITLE = "§bVista Previa del Loot";
    private static final String CHANCE_MENU_TITLE = "§eConfigurar Probabilidad";
    
    // Variable para almacenar el slot seleccionado
    private final Map<UUID, Integer> selectedSlot;
    
    // Modos de edición
    public enum EditorMode {
        MAIN_MENU,
        ITEMS_EDIT,
        CONFIG_EDIT,
        ENCHANT_EDIT,
        PREVIEW_LOOT,
        CHANCE_EDIT
    }
    
    /**
     * Constructor del editor de cofres de loot
     * @param plugin Instancia del plugin principal
     */
    public LootChestEditor(VirthaLootPlugin plugin) {
        this.plugin = plugin;
        this.playerEditing = new HashMap<>();
        this.playerMode = new HashMap<>();
        this.selectedSlot = new HashMap<>();
        
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
        
        Inventory inventory = Bukkit.createInventory(null, 36, MAIN_MENU_TITLE + ": " + chestName);
        
        // Botón para editar items
        ItemStack editItemsButton = createGuiItem(Material.CHEST, "§e§lEditar Items", 
                "§7Haz clic para configurar los items", "§7que aparecerán en el cofre");
        inventory.setItem(10, editItemsButton);
        
        // Botón para configuración general
        ItemStack configButton = createGuiItem(Material.REDSTONE, "§c§lConfiguración", 
                "§7Haz clic para modificar la configuración", "§7general del cofre");
        inventory.setItem(12, configButton);
        
        // Botón para previsualizar loot
        ItemStack previewButton = createGuiItem(Material.ENDER_EYE, "§b§lPrevisualizar Loot", 
                "§7Haz clic para ver una muestra", "§7del loot que generará este cofre");
        inventory.setItem(14, previewButton);
        
        // Botón para ordenar items por probabilidad
        ItemStack sortButton = createGuiItem(Material.HOPPER, "§d§lOrdenar Items", 
                "§7Haz clic para ordenar los items", "§7por probabilidad (mayor a menor)");
        inventory.setItem(16, sortButton);
        
        // Botón para guardar y salir
        ItemStack saveButton = createGuiItem(Material.EMERALD_BLOCK, "§a§lGuardar y Salir", 
                "§7Guarda los cambios y cierra el editor");
        inventory.setItem(31, saveButton);
        
        // Decoración
        ItemStack decorItem = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i : new int[]{0,1,2,3,4,5,6,7,8,9,11,13,15,17,18,19,20,21,22,23,24,25,26,27,28,29,30,32,33,34,35}) {
            inventory.setItem(i, decorItem);
        }
        
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
        inventory.setItem(52, chanceButton);
        
        // Botón para añadir encantamientos
        ItemStack enchantButton = createGuiItem(Material.ENCHANTED_BOOK, "§5§lAñadir Encantamientos", 
                "§7Haz clic para añadir encantamientos", "§7al item seleccionado");
        inventory.setItem(53, enchantButton);
        
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
    /**
     * Actualiza la visualización de la probabilidad en el menú
     * @param inventory Inventario a actualizar
     * @param chance Probabilidad a mostrar
     */
    private void updateChanceDisplay(Inventory inventory, double chance) {
        // Actualizar el item central que muestra la probabilidad actual
        ItemStack chanceItem = createGuiItem(Material.PAPER, "§e§lProbabilidad: §f" + chance + "%", 
                "§7Configura la probabilidad de aparición", "§7del item en el cofre");
        inventory.setItem(4, chanceItem);
    }
    
    /**
     * Abre el menú para configurar la probabilidad de un item
     * @param player Jugador que verá el menú
     * @param item Item al que se configurará la probabilidad
     */
    private void openChanceMenu(Player player, ItemStack item) {
        playerMode.put(player.getUniqueId(), EditorMode.CHANCE_EDIT);
        String chestName = playerEditing.get(player.getUniqueId());
        
        Inventory inventory = Bukkit.createInventory(null, 27, CHANCE_MENU_TITLE + ": " + chestName);
        
        // Mostrar el item seleccionado
        inventory.setItem(4, item.clone());
        
        // Botones para ajustar la probabilidad
        ItemStack minus10 = createGuiItem(Material.RED_WOOL, "§c§l-10%", "§7Reduce la probabilidad en 10%");
        inventory.setItem(11, minus10);
        
        ItemStack minus5 = createGuiItem(Material.RED_WOOL, "§c§l-5%", "§7Reduce la probabilidad en 5%");
        inventory.setItem(12, minus5);
        
        ItemStack minus1 = createGuiItem(Material.RED_WOOL, "§c§l-1%", "§7Reduce la probabilidad en 1%");
        inventory.setItem(13, minus1);
        
        ItemStack plus1 = createGuiItem(Material.GREEN_WOOL, "§a§l+1%", "§7Aumenta la probabilidad en 1%");
        inventory.setItem(15, plus1);
        
        ItemStack plus5 = createGuiItem(Material.GREEN_WOOL, "§a§l+5%", "§7Aumenta la probabilidad en 5%");
        inventory.setItem(16, plus5);
        
        ItemStack plus10 = createGuiItem(Material.GREEN_WOOL, "§a§l+10%", "§7Aumenta la probabilidad en 10%");
        inventory.setItem(17, plus10);
        
        // Botón para confirmar
        ItemStack confirmButton = createGuiItem(Material.EMERALD, "§a§lConfirmar", "§7Guarda la probabilidad configurada");
        inventory.setItem(22, confirmButton);
        
        // Botón para cancelar
        ItemStack cancelButton = createGuiItem(Material.BARRIER, "§c§lCancelar", "§7Vuelve sin guardar cambios");
        inventory.setItem(26, cancelButton);
        
        // Extraer probabilidad actual del lore
        double currentChance = 100.0;
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasLore() && meta.getLore() != null) {
            List<String> lore = meta.getLore();
            if (!lore.isEmpty() && lore.get(0).startsWith("§eProbabilidad: §f")) {
                String chanceStr = lore.get(0).replace("§eProbabilidad: §f", "").replace("%", "");
                try {
                    currentChance = Double.parseDouble(chanceStr);
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Error al parsear probabilidad: " + chanceStr);
                }
            }
        }
        
        // Actualizar la visualización de la probabilidad
        updateChanceDisplay(inventory, currentChance);
        
        player.openInventory(inventory);
    }
    
    /**
     * Abre el menú para añadir encantamientos a un item
     * @param player Jugador que verá el menú
     * @param item Item al que se añadirán encantamientos
     */
    @SuppressWarnings("deprecation")
    private void openEnchantMenu(Player player, ItemStack item) {
        playerMode.put(player.getUniqueId(), EditorMode.ENCHANT_EDIT);
        String chestName = playerEditing.get(player.getUniqueId());
        
        Inventory inventory = Bukkit.createInventory(null, 54, ENCHANT_MENU_TITLE + ": " + chestName);
        
        // Mostrar el item seleccionado
        inventory.setItem(4, item.clone());
        
        // Añadir libros de encantamiento disponibles
        int slot = 19;
        for (Enchantment enchantment : Enchantment.values()) {
            if (slot >= 35) break; // Máximo 16 encantamientos mostrados
            
            ItemStack enchantBook = new ItemStack(Material.ENCHANTED_BOOK);
            ItemMeta meta = enchantBook.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§5§l" + enchantment.getName());
                
                List<String> lore = new ArrayList<>();
                lore.add("§7Encantamiento: §f" + enchantment.getName());
                lore.add("§7Nivel máximo: §f" + enchantment.getMaxLevel());
                lore.add("§7Haz clic para añadir nivel 1");
                lore.add("§7Shift+clic para añadir nivel máximo");
                
                meta.setLore(lore);
                enchantBook.setItemMeta(meta);
            }
            
            inventory.setItem(slot, enchantBook);
            slot++;
        }
        
        // Botón para volver
        ItemStack backButton = createGuiItem(Material.ARROW, "§7§lVolver", 
                "§7Volver al menú de edición de items");
        inventory.setItem(45, backButton);
        
        // Botón para limpiar encantamientos
        ItemStack clearButton = createGuiItem(Material.BARRIER, "§c§lLimpiar Encantamientos", 
                "§7Elimina todos los encantamientos", "§7del item seleccionado");
        inventory.setItem(53, clearButton);
        
        player.openInventory(inventory);
    }
    
    
    /**
     * Ordena los items del cofre por probabilidad (de mayor a menor)
     * @param player Jugador que está editando el cofre
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
            
            if (event.getRawSlot() == 10) { // Botón de editar items
                openItemsMenu(player);
            } else if (event.getRawSlot() == 12) { // Botón de configuración
                openConfigMenu(player);
            } else if (event.getRawSlot() == 14) { // Botón de previsualizar loot
                openPreviewMenu(player);
            } else if (event.getRawSlot() == 16) { // Botón de ordenar items
                sortItemsByChance(player);
            } else if (event.getRawSlot() == 31) { // Botón de guardar y salir
                // Mostrar mensaje de confirmación
                player.sendMessage("§e¿Estás seguro de que deseas guardar y salir? Escribe §a/vloot confirm §epara confirmar.");
                
                // Programar tarea para cancelar la confirmación después de 10 segundos
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (playerEditing.containsKey(playerId)) {
                        player.sendMessage("§cLa confirmación ha expirado. Puedes intentarlo de nuevo.");
                    }
                }, 200L); // 10 segundos (20 ticks = 1 segundo)
            }
        }
        // Menú de edición de items
        else if (title.startsWith(ITEMS_MENU_TITLE)) {
            // Verificar si el clic fue en la sección de botones (slots 45-53)
            if (event.getRawSlot() >= 45 && event.getRawSlot() <= 53) {
                event.setCancelled(true); // Cancelar para los botones
                
                if (event.getRawSlot() == 45) { // Botón de volver
                    openMainMenu(player);
                } else if (event.getRawSlot() == 49) { // Botón de guardar
                    saveItemsFromInventory(player, event.getInventory());
                } else if (event.getRawSlot() == 52) { // Botón de configurar probabilidad
                    // Verificar si hay algún item seleccionado
                    if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
                        // Buscar el primer item no vacío en el inventario
                        boolean foundItem = false;
                        for (int i = 0; i < 45; i++) {
                            ItemStack item = event.getInventory().getItem(i);
                            if (item != null && item.getType() != Material.AIR) {
                                selectedSlot.put(player.getUniqueId(), i);
                                openChanceMenu(player, item);
                                foundItem = true;
                                break;
                            }
                        }
                        
                        if (!foundItem) {
                            player.sendMessage("§cNo hay items para configurar la probabilidad.");
                        }
                    } else {
                        player.sendMessage("§cPrimero debes añadir un item al cofre.");
                    }
                } else if (event.getRawSlot() == 53) { // Botón de añadir encantamientos
                    // Verificar si hay algún item seleccionado
                    if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
                        // Buscar el primer item no vacío en el inventario
                        boolean foundItem = false;
                        for (int i = 0; i < 45; i++) {
                            ItemStack item = event.getInventory().getItem(i);
                            if (item != null && item.getType() != Material.AIR) {
                                selectedSlot.put(player.getUniqueId(), i);
                                openEnchantMenu(player, item);
                                foundItem = true;
                                break;
                            }
                        }
                        
                        if (!foundItem) {
                            player.sendMessage("§cNo hay items para añadir encantamientos.");
                        }
                    } else {
                        player.sendMessage("§cPrimero debes añadir un item al cofre.");
                    }
                }
            }
            // Permitir interacción con el inventario del jugador (slots > 53)
            else if (event.getRawSlot() > 53) {
                // No cancelar el evento para permitir mover items desde el inventario del jugador
            }
            // Permitir interacción con los slots de items (0-44)
            else {
                // No cancelar el evento para permitir colocar/mover items en estos slots
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
        // Menú de configuración de probabilidad
        else if (title.startsWith(CHANCE_MENU_TITLE)) {
            event.setCancelled(true); // Cancelar el evento para evitar mover items
            
            // Obtener el item seleccionado y su probabilidad actual
            int slot = selectedSlot.getOrDefault(playerId, 0);
            ItemStack selectedItem = null;
            double currentChance = 100.0;
            
            // Obtener el inventario de edición
            Inventory itemsInventory = null;
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getUniqueId().equals(playerId) && p.getOpenInventory() != null) {
                    Inventory inv = p.getOpenInventory().getTopInventory();
                    String invTitle = p.getOpenInventory().getTitle();
                    if (invTitle != null && invTitle.startsWith(ITEMS_MENU_TITLE)) {
                        itemsInventory = inv;
                        break;
                    }
                }
            }
            
            if (itemsInventory != null) {
                selectedItem = itemsInventory.getItem(slot);
            } else {
                // Si no se encuentra el inventario, usar el item mostrado en el menú
                selectedItem = event.getInventory().getItem(4);
            }
            
            if (selectedItem == null) {
                player.closeInventory();
                player.sendMessage("§cError: No se pudo encontrar el item seleccionado.");
                return;
            }
            
            // Extraer probabilidad actual del lore
            ItemMeta meta = selectedItem.getItemMeta();
            if (meta != null && meta.hasLore() && meta.getLore() != null) {
                List<String> lore = meta.getLore();
                if (!lore.isEmpty() && lore.get(0).startsWith("§eProbabilidad: §f")) {
                    String chanceStr = lore.get(0).replace("§eProbabilidad: §f", "").replace("%", "");
                    try {
                        currentChance = Double.parseDouble(chanceStr);
                    } catch (NumberFormatException e) {
                        plugin.getLogger().warning("Error al parsear probabilidad: " + chanceStr);
                    }
                }
            }
            
            // Manejar clics en los botones
            if (event.getRawSlot() == 11) { // -10%
                currentChance = Math.max(0.1, currentChance - 10);
                updateChanceDisplay(event.getInventory(), currentChance);
            } else if (event.getRawSlot() == 12) { // -5%
                currentChance = Math.max(0.1, currentChance - 5);
                updateChanceDisplay(event.getInventory(), currentChance);
            } else if (event.getRawSlot() == 13) { // -1%
                currentChance = Math.max(0.1, currentChance - 1);
                updateChanceDisplay(event.getInventory(), currentChance);
            } else if (event.getRawSlot() == 15) { // +1%
                currentChance = Math.min(100, currentChance + 1);
                updateChanceDisplay(event.getInventory(), currentChance);
            } else if (event.getRawSlot() == 16) { // +5%
                currentChance = Math.min(100, currentChance + 5);
                updateChanceDisplay(event.getInventory(), currentChance);
            } else if (event.getRawSlot() == 17) { // +10%
                currentChance = Math.min(100, currentChance + 10);
                updateChanceDisplay(event.getInventory(), currentChance);
            } else if (event.getRawSlot() == 22) { // Confirmar
                // Actualizar la probabilidad en el item
                if (selectedItem != null && meta != null) {
                    List<String> lore = meta.hasLore() && meta.getLore() != null ? meta.getLore() : new ArrayList<>();
                    
                    if (lore.isEmpty()) {
                        lore.add("§eProbabilidad: §f" + currentChance + "%");
                    } else if (lore.get(0).startsWith("§eProbabilidad: §f")) {
                        lore.set(0, "§eProbabilidad: §f" + currentChance + "%");
                    } else {
                        lore.add(0, "§eProbabilidad: §f" + currentChance + "%");
                    }
                    
                    meta.setLore(lore);
                    selectedItem.setItemMeta(meta);
                    
                    // Actualizar el item en el inventario de edición
                    if (itemsInventory != null) {
                        itemsInventory.setItem(slot, selectedItem);
                    }
                    
                    player.sendMessage("§aProbabilidad actualizada a " + currentChance + "%");
                }
                
                // Volver al menú de edición de items
                openItemsMenu(player);
            } else if (event.getRawSlot() == 26) { // Cancelar
                openItemsMenu(player);
            }
        }
        // Menú de encantamientos
        else if (title.startsWith(ENCHANT_MENU_TITLE)) {
            event.setCancelled(true); // Cancelar el evento para evitar mover items
            
            // Obtener el item seleccionado
            int slot = selectedSlot.getOrDefault(playerId, 0);
            ItemStack selectedItem = null;
            
            // Obtener el inventario de edición
            Inventory itemsInventory = null;
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getUniqueId().equals(playerId) && p.getOpenInventory() != null) {
                    Inventory inv = p.getOpenInventory().getTopInventory();
                    String invTitle = p.getOpenInventory().getTitle();
                    if (invTitle != null && invTitle.startsWith(ITEMS_MENU_TITLE)) {
                        itemsInventory = inv;
                        break;
                    }
                }
            }
            
            if (itemsInventory != null) {
                selectedItem = itemsInventory.getItem(slot);
            } else {
                // Si no se encuentra el inventario, usar el item mostrado en el menú
                selectedItem = event.getInventory().getItem(4);
            }
            
            if (selectedItem == null) {
                player.closeInventory();
                player.sendMessage("§cError: No se pudo encontrar el item seleccionado.");
                return;
            }
            
            // Manejar clics en los botones de encantamientos
            if (event.getRawSlot() >= 19 && event.getRawSlot() <= 34) {
                ItemStack enchantBook = event.getCurrentItem();
                if (enchantBook != null && enchantBook.getType() == Material.ENCHANTED_BOOK) {
                    ItemMeta bookMeta = enchantBook.getItemMeta();
                    if (bookMeta != null && bookMeta.hasLore() && bookMeta.getLore() != null) {
                        List<String> lore = bookMeta.getLore();
                        if (lore.size() >= 2 && lore.get(0).startsWith("§7Encantamiento: §f")) {
                            String enchantName = lore.get(0).replace("§7Encantamiento: §f", "");
                            String maxLevelStr = lore.get(1).replace("§7Nivel máximo: §f", "");
                            
                            try {
                                int maxLevel = Integer.parseInt(maxLevelStr);
                                int level = event.isShiftClick() ? maxLevel : 1;
                                
                                // Añadir encantamiento al item
                                @SuppressWarnings("deprecation")
                                Enchantment enchantment = Enchantment.getByName(enchantName);
                                if (enchantment != null) {
                                    selectedItem.addUnsafeEnchantment(enchantment, level);
                                    player.sendMessage("§aEncantamiento " + enchantName + " nivel " + level + " añadido.");
                                    
                                    // Actualizar el item en el inventario de edición
                                    if (itemsInventory != null) {
                                        itemsInventory.setItem(slot, selectedItem);
                                    }
                                    
                                    // Actualizar el item mostrado en el menú
                                    event.getInventory().setItem(4, selectedItem.clone());
                                } else {
                                    player.sendMessage("§cError: Encantamiento no encontrado.");
                                }
                            } catch (NumberFormatException e) {
                                plugin.getLogger().warning("Error al parsear nivel máximo: " + maxLevelStr);
                            }
                        }
                    }
                }
            } else if (event.getRawSlot() == 45) { // Botón de volver
                openItemsMenu(player);
            } else if (event.getRawSlot() == 53) { // Botón para limpiar encantamientos
                if (selectedItem != null) {
                    // Eliminar todos los encantamientos
                    for (Enchantment enchantment : selectedItem.getEnchantments().keySet()) {
                        selectedItem.removeEnchantment(enchantment);
                    }
                    
                    // Actualizar el item en el inventario de edición
                    if (itemsInventory != null) {
                        itemsInventory.setItem(slot, selectedItem);
                    }
                    
                    // Actualizar el item mostrado en el menú
                    event.getInventory().setItem(4, selectedItem.clone());
                    
                    player.sendMessage("§aEncantamientos eliminados correctamente.");
                }
            }
        }
        // Menú de previsualización de loot
        else if (title.startsWith(PREVIEW_MENU_TITLE)) {
            event.setCancelled(true); // Cancelar el evento para evitar mover items
            
            if (event.getRawSlot() == 49) { // Regenerar loot
                openPreviewMenu(player);
            } else if (event.getRawSlot() == 53) { // Volver
                openMainMenu(player);
            }
        }
    }
    
    /**
     * Abre el menú para configurar la probabilidad de un item
     * @param player Jugador que verá el menú
     * @param item Item al que se configurará la probabilidad
     */

    /**
     * Crea un libro encantado para el menú de encantamientos
     * @param enchantName Nombre del encantamiento
     * @param displayName Nombre a mostrar
     * @param maxLevel Nivel máximo del encantamiento
     * @return ItemStack configurado
     */
    @SuppressWarnings("unused")
    private ItemStack createEnchantmentBook(String enchantName, String displayName, int maxLevel) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = book.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            
            List<String> lore = new ArrayList<>();
            lore.add("§7Encantamiento: §f" + enchantName);
            lore.add("§7Nivel máximo: §f" + maxLevel);
            lore.add("");
            lore.add("§eHaz clic para añadir nivel 1");
            lore.add("§eShift + Clic para nivel máximo");
            
            meta.setLore(lore);
            book.setItemMeta(meta);
        }
        return book;
    }
    
    /**
     * Abre el menú de previsualización de loot
     * @param player Jugador que verá el menú
     */
    private void openPreviewMenu(Player player) {
        playerMode.put(player.getUniqueId(), EditorMode.PREVIEW_LOOT);
        String chestName = playerEditing.get(player.getUniqueId());
        
        Inventory inventory = Bukkit.createInventory(null, 54, PREVIEW_MENU_TITLE + ": " + chestName);
        
        // Generar una muestra de loot
        List<ItemStack> lootItems = plugin.getLootManager().generateLoot(player, chestName);
        
        // Mostrar los items generados
        int slot = 10;
        for (ItemStack item : lootItems) {
            if (slot >= 44) break; // Evitar sobrepasar el inventario
            
            inventory.setItem(slot, item);
            
            // Avanzar al siguiente slot, saltando los bordes
            slot++;
            if (slot % 9 == 8) slot += 2;
        }
        
        // Botón para regenerar loot
        ItemStack regenerateButton = createGuiItem(Material.ENDER_PEARL, "§b§lRegenerar Loot", 
                "§7Haz clic para generar una nueva", "§7muestra aleatoria de loot");
        inventory.setItem(49, regenerateButton);
        
        // Botón para volver
        ItemStack backButton = createGuiItem(Material.ARROW, "§7§lVolver", 
                "§7Volver al menú principal");
        inventory.setItem(53, backButton);
        
        // Decoración
        ItemStack decorItem = createGuiItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, decorItem);
        }
        for (int i = 45; i < 54; i++) {
            if (i != 49 && i != 53) {
                inventory.setItem(i, decorItem);
            }
        }
        for (int i = 0; i < 6; i++) {
            inventory.setItem(i * 9, decorItem);
            inventory.setItem(i * 9 + 8, decorItem);
        }
        
        player.openInventory(inventory);
    }
    
    /**
     * Ordena los items del cofre por probabilidad (de mayor a menor)
     * @param player Jugador que está editando
     */
    private void sortItemsByChance(Player player) {
        String chestName = playerEditing.get(player.getUniqueId());
        
        // Obtener la lista de items
        List<Map<?, ?>> itemsList = plugin.getLootChestsConfig().getMapList("chests." + chestName + ".items");
        
        // Convertir a una lista que podamos ordenar
        List<Map<String, Object>> sortableList = new ArrayList<>();
        for (Map<?, ?> map : itemsList) {
            @SuppressWarnings("unchecked")
            Map<String, Object> itemMap = (Map<String, Object>) map;
            sortableList.add(itemMap);
        }
        
        // Ordenar por probabilidad (de mayor a menor)
        sortableList.sort((map1, map2) -> {
            double chance1 = map1.containsKey("chance") ? ((Number) map1.get("chance")).doubleValue() : 100.0;
            double chance2 = map2.containsKey("chance") ? ((Number) map2.get("chance")).doubleValue() : 100.0;
            return Double.compare(chance2, chance1); // Orden descendente
        });
        
        // Guardar la lista ordenada
        plugin.getLootChestsConfig().set("chests." + chestName + ".items", sortableList);
        try {
            plugin.getLootChestsConfig().save(plugin.getDataFolder() + "/lootchests.yml");
            player.sendMessage("§aItems ordenados por probabilidad correctamente.");
            
            // Reabrir el menú de edición de items para mostrar los cambios
            openItemsMenu(player);
        } catch (Exception e) {
            player.sendMessage("§cError al ordenar los items del cofre.");
            plugin.getLogger().severe("Error al guardar lootchests.yml: " + e.getMessage());
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
        
        // Verificar si el jugador estaba editando un cofre
        if (playerEditing.containsKey(playerId)) {
            EditorMode mode = playerMode.getOrDefault(playerId, EditorMode.MAIN_MENU);
            String title = event.getView().getTitle();
            
            // Evitar procesamiento si el jugador está cambiando entre menús del editor
            try {
                if (player.getOpenInventory() != null && player.getOpenInventory().getTitle() != null && 
                    (player.getOpenInventory().getTitle().startsWith(MAIN_MENU_TITLE) ||
                     player.getOpenInventory().getTitle().startsWith(ITEMS_MENU_TITLE) ||
                     player.getOpenInventory().getTitle().startsWith(CONFIG_MENU_TITLE) ||
                     player.getOpenInventory().getTitle().startsWith(ENCHANT_MENU_TITLE) ||
                     player.getOpenInventory().getTitle().startsWith(PREVIEW_MENU_TITLE) ||
                     player.getOpenInventory().getTitle().startsWith(CHANCE_MENU_TITLE))) {
                    return;
                }
            } catch (Exception e) {
                // Ignorar errores al verificar el título del inventario
                plugin.getLogger().warning("Error al verificar el título del inventario: " + e.getMessage());
            }
            
            if (mode == EditorMode.ITEMS_EDIT && title.startsWith(ITEMS_MENU_TITLE)) {
                // Guardar cambios al cerrar el menú de edición de items
                saveItemsFromInventory(player, event.getInventory());
                
                // Programar tarea para abrir el menú principal después de un tick
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (playerEditing.containsKey(playerId)) {
                        openMainMenu(player);
                    }
                }, 1L);
            } else if ((mode == EditorMode.MAIN_MENU && title.startsWith(MAIN_MENU_TITLE)) ||
                      (mode == EditorMode.CONFIG_EDIT && title.startsWith(CONFIG_MENU_TITLE))) {
                // Eliminar al jugador de la lista de editores al cerrar estos menús
                playerEditing.remove(playerId);
                playerMode.remove(playerId);
                selectedSlot.remove(playerId);
                player.sendMessage("§aHas salido del editor de cofres de loot.");
            } else if ((mode == EditorMode.ENCHANT_EDIT && title.startsWith(ENCHANT_MENU_TITLE)) ||
                      (mode == EditorMode.CHANCE_EDIT && title.startsWith(CHANCE_MENU_TITLE)) ||
                      (mode == EditorMode.PREVIEW_LOOT && title.startsWith(PREVIEW_MENU_TITLE))) {
                // Volver al menú de edición de items al cerrar estos menús
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (playerEditing.containsKey(playerId)) {
                        openItemsMenu(player);
                        }
                    }, 1L);
                }
            }
        }
    }
