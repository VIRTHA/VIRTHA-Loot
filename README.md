# VirthaLoot

Plugin de Minecraft para servidores Spigot que implementa un sistema de cofres con loot personalizable y cooldowns.

## Características

- Creación de cofres con loot personalizable directamente en el juego
- Sistema de cooldown configurable para cada cofre
- Integración con PlaceholderAPI para mostrar información dinámica
- Configuración completa de items, probabilidades y encantamientos
- Sistema de permisos para controlar el acceso a las funcionalidades

## Requisitos

- Servidor Spigot/Paper 1.19+
- PlaceholderAPI (opcional, para usar los placeholders)

## Instalación

1. Descarga el archivo .jar del plugin
2. Colócalo en la carpeta `plugins` de tu servidor
3. Reinicia el servidor
4. ¡Listo para usar!

## Comandos

- `/vloot create <nombre> <cooldown>` - Crea un nuevo cofre de loot
- `/vloot edit <nombre>` - Edita el contenido de un cofre de loot
- `/vloot info <nombre>` - Muestra información sobre un cofre de loot
- `/vloot list` - Lista todos los cofres de loot disponibles
- `/vloot delete <nombre>` - Elimina un cofre de loot

## Permisos

- `virthaloot.admin` - Acceso a todos los comandos administrativos
- `virthaloot.create` - Permite crear cofres de loot
- `virthaloot.edit` - Permite editar cofres de loot
- `virthaloot.use` - Permite usar cofres de loot (por defecto: true)

## Placeholders

El plugin proporciona los siguientes placeholders para usar con PlaceholderAPI:

- `%virthaloot_available_[nombre_cofre]%` - Muestra si un cofre está disponible o el tiempo restante
- `%virthaloot_cooldown_[nombre_cofre]%` - Muestra el tiempo restante de cooldown en segundos
- `%virthaloot_available_count%` - Muestra el número de cofres disponibles para el jugador

## Configuración

El plugin genera varios archivos de configuración:

### config.yml
Contiene la configuración general del plugin, mensajes y sonidos.

### lootchests.yml
Contiene la configuración de los cofres de loot, incluyendo ubicaciones, cooldowns e items.

### cooldowns.yml
Almacena los cooldowns activos de los jugadores (no editar manualmente).

## Ejemplo de configuración de loot

```yaml
chests:
  ejemplo_comun:
    location: world,0,0,0
    cooldown: 300
    items:
      - material: IRON_INGOT
        amount: 3
        chance: 70
      - material: GOLD_INGOT
        amount: 2
        chance: 50
      - material: DIAMOND
        amount: 1
        chance: 20
```

## Soporte

Si encuentras algún problema o tienes sugerencias, por favor reporta los issues en el repositorio del proyecto.

## Licencia

Este proyecto está bajo la licencia MIT. Ver el archivo LICENSE para más detalles.