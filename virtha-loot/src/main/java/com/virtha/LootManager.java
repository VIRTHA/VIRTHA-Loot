package com.virtha;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Clase que maneja la generación de loot para los cofres personalizados
 */
public class LootManager {

    private final VirthaLootPlugin plugin;
    private final Random random;

    /**
     * Constructor del LootManager
     * @param plugin Instancia del plugin principal
     */
    public LootManager(VirthaLootPlugin plugin) {
        this.plugin = plugin;
        this.random = new Random();
    }

    /**
     * Genera items de loot para un jugador basado en la configuración del cofre
     * @param player Jugador que recibirá el loot
     * @param chestName Nombre del cofre de loot
     * @return Lista de items generados
     */
    public List<ItemStack> generateLoot(Player player, String chestName) {
        List<ItemStack> lootItems = new ArrayList<>();
        
        ConfigurationSection chestSection = plugin.getLootChestsConfig().getConfigurationSection("chests." + chestName);
        if (chestSection == null) {
            return lootItems;
        }
        
        List<Map<?, ?>> itemsList = chestSection.getMapList("items");
        if (itemsList == null || itemsList.isEmpty()) {
            return lootItems;
        }
        
        // Obtener configuración global
        int maxItemsPerChest = plugin.getLootChestsConfig().getInt("settings.max-items-per-chest", 5);
        double baseChanceMultiplier = plugin.getLootChestsConfig().getDouble("settings.base-chance-multiplier", 1.0);
        
        // Generar loot aleatorio
        for (Map<?, ?> itemMap : itemsList) {
            // Verificar si se alcanzó el máximo de items
            if (lootItems.size() >= maxItemsPerChest) {
                break;
            }
            
            // Obtener propiedades del item
            String materialName = (String) itemMap.get("material");
            int amount = itemMap.containsKey("amount") && itemMap.get("amount") instanceof Integer ? (int) itemMap.get("amount") : 1;
            double chance = itemMap.containsKey("chance") && itemMap.get("chance") instanceof Number ? ((Number) itemMap.get("chance")).doubleValue() : 100.0;
            
            // Aplicar multiplicador de probabilidad
            chance *= baseChanceMultiplier;
            
            // Verificar si el item debe ser generado según su probabilidad
            if (random.nextDouble() * 100 <= chance) {
                try {
                    Material material = Material.valueOf(materialName);
                    ItemStack item = new ItemStack(material, amount);
                    
                    // Aplicar encantamientos si existen
                    if (itemMap.containsKey("enchantments") && itemMap.get("enchantments") instanceof List) {
                        List<?> enchantmentsList = (List<?>) itemMap.get("enchantments");
                        for (Object enchObj : enchantmentsList) {
                            if (enchObj instanceof Map) {
                                Map<?, ?> enchMap = (Map<?, ?>) enchObj;
                                String enchType = (String) enchMap.get("type");
                                int level = enchMap.containsKey("level") && enchMap.get("level") instanceof Integer ? (int) enchMap.get("level") : 1;
                                
                                @SuppressWarnings("deprecation")
                                Enchantment enchantment = Enchantment.getByName(enchType);
                                if (enchantment != null) {
                                    item.addUnsafeEnchantment(enchantment, level);
                                }
                            }
                        }
                    }
                    
                    // Aplicar nombre personalizado si existe
                    if (itemMap.containsKey("name") && itemMap.get("name") instanceof String) {
                        ItemMeta meta = item.getItemMeta();
                        if (meta != null) {
                            meta.setDisplayName((String) itemMap.get("name"));
                            item.setItemMeta(meta);
                        }
                    }
                    
                    // Aplicar lore personalizado si existe
                    if (itemMap.containsKey("lore") && itemMap.get("lore") instanceof List) {
                        List<?> loreList = (List<?>) itemMap.get("lore");
                        if (!loreList.isEmpty()) {
                            List<String> lore = new ArrayList<>();
                            for (Object line : loreList) {
                                if (line instanceof String) {
                                    lore.add((String) line);
                                }
                            }
                            
                            ItemMeta meta = item.getItemMeta();
                            if (meta != null) {
                                meta.setLore(lore);
                                item.setItemMeta(meta);
                            }
                        }
                    }
                    
                    lootItems.add(item);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Material inválido en la configuración del cofre " + chestName + ": " + materialName);
                }
            }
        }
        
        return lootItems;
    }
    
    /**
     * Entrega items de loot a un jugador
     * @param player Jugador que recibirá el loot
     * @param items Lista de items para entregar
     */
    public void giveLoot(Player player, List<ItemStack> items) {
        if (items.isEmpty()) {
            player.sendMessage("§cEste cofre no contiene ningún item.");
            return;
        }
        
        // Entregar items al jugador
        for (ItemStack item : items) {
            // Si el inventario está lleno, soltar el item en el suelo
            if (player.getInventory().firstEmpty() == -1) {
                player.getWorld().dropItem(player.getLocation(), item);
                player.sendMessage("§eTu inventario está lleno. El item ha sido soltado en el suelo.");
            } else {
                player.getInventory().addItem(item);
            }
        }
        
        // Reproducir sonido si está configurado
        String soundName = plugin.getConfig().getString("settings.loot-sound", "ENTITY_PLAYER_LEVELUP");
        float volume = (float) plugin.getConfig().getDouble("settings.sound-volume", 1.0);
        float pitch = (float) plugin.getConfig().getDouble("settings.sound-pitch", 1.0);
        
        try {
            player.playSound(player.getLocation(), soundName, volume, pitch);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Sonido inválido en la configuración: " + soundName);
        }
        
        // Mostrar mensaje de loot
        String message = plugin.getConfig().getString("settings.loot-message", "&a¡Has abierto un cofre de loot!");
        message = message.replace("&", "§");
        player.sendMessage(message);
    }
}