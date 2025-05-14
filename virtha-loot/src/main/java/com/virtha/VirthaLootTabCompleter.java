package com.virtha;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TabCompleter para los comandos del plugin VirthaLoot
 * Proporciona sugerencias automáticas al escribir comandos
 */
public class VirthaLootTabCompleter implements TabCompleter {

    private final VirthaLootPlugin plugin;
    @SuppressWarnings("unused")
    private final List<String> subCommands = Arrays.asList(
            "create", "edit", "info", "list", "delete", "chance", "cooldown"
    );

    /**
     * Constructor del TabCompleter
     * @param plugin Instancia del plugin principal
     */
    public VirthaLootTabCompleter(VirthaLootPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        // Si no es un jugador, no ofrecer sugerencias
        if (!(sender instanceof Player)) {
            return completions;
        }

        Player player = (Player) sender;

        // Primer argumento: subcomandos
        if (args.length == 1) {
            // Filtrar subcomandos según permisos
            List<String> availableCommands = new ArrayList<>();
            
            // Comandos que requieren permiso específico
            if (player.hasPermission("virthaloot.create")) {
                availableCommands.add("create");
            }
            
            if (player.hasPermission("virthaloot.edit")) {
                availableCommands.add("edit");
                availableCommands.add("chance");
                availableCommands.add("cooldown");
            }
            
            if (player.hasPermission("virthaloot.admin")) {
                availableCommands.add("delete");
            }
            
            // Comandos disponibles para todos
            availableCommands.add("info");
            availableCommands.add("list");
            
            // Filtrar según lo que ya ha escrito el jugador
            return filterCompletions(availableCommands, args[0]);
        }

        // Segundo argumento: depende del subcomando
        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "edit":
                case "info":
                case "delete":
                case "cooldown":
                    // Sugerir nombres de cofres existentes
                    return filterCompletions(getChestNames(), args[1]);
                case "chance":
                    // Sugerir valores de probabilidad
                    return filterCompletions(Arrays.asList("10", "25", "50", "75", "100"), args[1]);
                case "create":
                    // No sugerir nada para el nombre del nuevo cofre
                    return completions;
            }
        }

        // Tercer argumento: depende del subcomando
        if (args.length == 3) {
            switch (args[0].toLowerCase()) {
                case "create":
                case "cooldown":
                    // Sugerir valores de cooldown en segundos
                    return filterCompletions(Arrays.asList("60", "300", "600", "1800", "3600"), args[2]);
            }
        }

        return completions;
    }

    /**
     * Obtiene la lista de nombres de cofres existentes
     * @return Lista de nombres de cofres
     */
    private List<String> getChestNames() {
        List<String> chestNames = new ArrayList<>();
        ConfigurationSection chestsSection = plugin.getLootChestsConfig().getConfigurationSection("chests");
        
        if (chestsSection != null) {
            chestNames.addAll(chestsSection.getKeys(false));
        }
        
        return chestNames;
    }

    /**
     * Filtra las sugerencias según lo que ya ha escrito el jugador
     * @param options Lista de opciones disponibles
     * @param current Texto actual que ha escrito el jugador
     * @return Lista filtrada de sugerencias
     */
    private List<String> filterCompletions(List<String> options, String current) {
        return options.stream()
                .filter(option -> option.toLowerCase().startsWith(current.toLowerCase()))
                .collect(Collectors.toList());
    }
}