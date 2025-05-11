package com.virtha;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

/**
 * Clase que maneja la integración con PlaceholderAPI para VirthaLoot
 */
public class VirthaLootExpansion extends PlaceholderExpansion {

    private final VirthaLootPlugin plugin;

    /**
     * Constructor para la expansión de PlaceholderAPI
     * @param plugin Instancia del plugin principal
     */
    public VirthaLootExpansion(VirthaLootPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Identificador de la expansión para PlaceholderAPI
     * @return El identificador de la expansión
     */
    @Override
    public String getIdentifier() {
        return "virthaloot";
    }

    /**
     * Autor de la expansión
     * @return El autor de la expansión
     */
    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    /**
     * Versión de la expansión
     * @return La versión de la expansión
     */
    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    /**
     * Indica si la expansión permanece activa mientras el servidor está en ejecución
     * @return true para mantener la expansión activa
     */
    @Override
    public boolean persist() {
        return true;
    }

    /**
     * Procesa los placeholders solicitados
     * @param player El jugador para el que se solicita el placeholder
     * @param identifier El identificador del placeholder
     * @return El valor del placeholder o null si no se encuentra
     */
    @Override
    public String onRequest(OfflinePlayer player, String identifier) {
        if (player == null || !player.isOnline()) {
            return "";
        }

        Player onlinePlayer = player.getPlayer();
        if (onlinePlayer == null) {
            return "";
        }

        // Placeholder para verificar si un cofre específico está disponible
        // Formato: %virthaloot_available_[nombre_cofre]%
        if (identifier.startsWith("available_")) {
            String chestName = identifier.substring("available_".length());
            return isChestAvailable(onlinePlayer.getUniqueId(), chestName) ? 
                   plugin.getConfig().getString("placeholders.available-text", "&aDisponible") : 
                   plugin.getConfig().getString("placeholders.cooldown-text", "&c%time_left%s")
                         .replace("%time_left%", String.valueOf(getChestCooldownTime(onlinePlayer.getUniqueId(), chestName)));
        }

        // Placeholder para obtener el tiempo restante de cooldown de un cofre
        // Formato: %virthaloot_cooldown_[nombre_cofre]%
        if (identifier.startsWith("cooldown_")) {
            String chestName = identifier.substring("cooldown_".length());
            return String.valueOf(getChestCooldownTime(onlinePlayer.getUniqueId(), chestName));
        }

        // Placeholder para obtener el número total de cofres disponibles para el jugador
        // Formato: %virthaloot_available_count%
        if (identifier.equals("available_count")) {
            return String.valueOf(getAvailableChestsCount(onlinePlayer.getUniqueId()));
        }

        return null;
    }

    /**
     * Verifica si un cofre está disponible para un jugador
     * @param playerId UUID del jugador
     * @param chestName Nombre del cofre
     * @return true si el cofre está disponible, false si está en cooldown
     */
    private boolean isChestAvailable(UUID playerId, String chestName) {
        Map<UUID, Long> playerCooldowns = plugin.getChestCooldowns().get(chestName);
        if (playerCooldowns == null || !playerCooldowns.containsKey(playerId)) {
            return true;
        }

        long cooldownTime = playerCooldowns.get(playerId);
        return System.currentTimeMillis() >= cooldownTime;
    }

    /**
     * Obtiene el tiempo restante de cooldown para un cofre
     * @param playerId UUID del jugador
     * @param chestName Nombre del cofre
     * @return Tiempo restante en segundos, 0 si está disponible
     */
    private long getChestCooldownTime(UUID playerId, String chestName) {
        Map<UUID, Long> playerCooldowns = plugin.getChestCooldowns().get(chestName);
        if (playerCooldowns == null || !playerCooldowns.containsKey(playerId)) {
            return 0;
        }

        long cooldownTime = playerCooldowns.get(playerId);
        long currentTime = System.currentTimeMillis();

        if (currentTime >= cooldownTime) {
            return 0;
        }

        return (cooldownTime - currentTime) / 1000; // Convertir a segundos
    }

    /**
     * Obtiene el número de cofres disponibles para un jugador
     * @param playerId UUID del jugador
     * @return Número de cofres disponibles
     */
    private int getAvailableChestsCount(UUID playerId) {
        int count = 0;
        for (String chestName : plugin.getLootChestsConfig().getConfigurationSection("chests").getKeys(false)) {
            if (isChestAvailable(playerId, chestName)) {
                count++;
            }
        }
        return count;
    }
}