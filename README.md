# TemporaryProtections

## Descripción
TemporaryProtections es un plugin para servidores Minecraft (Spigot/Paper) que permite gestionar regiones de protección temporal utilizando ProtectionStones. Permite proteger zonas por tiempo limitado, con mensajes personalizables (en desarrollo) y limpieza automática de regiones que ya no tienen dueño.

## Características
- Protección temporal de regiones basada en ProtectionStones.
- Duración configurable para cada protección.
- Protección total contra cualquier tipo de daño dentro de regiones temporales.
- Limpieza automática de regiones temporales huérfanas.
- Mensajes personalizables desde `config.yml` (en desarrollo).
- Comandos y permisos diferenciados para administración y usuarios.

## Dependencias
- ProtectionStones (requerido)
- Servidor Spigot o Paper compatible

## Instalación
1. Instala ProtectionStones en tu servidor.
2. Descarga la última versión de TemporaryProtections desde la sección de releases.
3. Coloca el archivo `.jar` en la carpeta `plugins` de tu servidor.
4. Reinicia el servidor.

## Uso
- Utiliza `/tmpp` y sus subcomandos para gestionar protecciones temporales.
- Configura los parámetros en `plugins/TemporaryProtections/config.yml`:
  - allowed-protection-blocks: Lista los alias de los bloques de ProtectionStones que activarán la protección temporal (por ejemplo, emerald_block, 64, 20).
  - temporary-protection-seconds: Define la duración (en segundos) de la protección temporal (por ejemplo, 60).
  - debug-messages: Ponlo en true para activar mensajes de depuración en consola y facilitar el diagnóstico del plugin.

## Comandos principales
- `/tmpp help` — Muestra la ayuda general del plugin.
- `/tmpp info` — Muestra tus opciones de debug activas.
- `/tmpp enable <opcion>` — Activa una opción de debug (requiere permiso de admin).
- `/tmpp disable <opcion>` — Desactiva una opción de debug (requiere permiso de admin).
- `/tmpp debug` — Lista las opciones de debug disponibles (requiere permiso de admin).
- `/tmpp reload` — Recarga la configuración del plugin (requiere permiso de admin).

## Permisos
- `temporaryprotections.admin` — Requerido para acceder a los comandos de administración y debug.
- `temporaryprotections.admin.bypass` — Permite saltarse restricciones en regiones temporales.

## Notas
- Actualmente solo funciona con ProtectionStones.
- Se planean mejoras para hacerlo más flexible y compatible en el futuro.
- Puedes descargarlo también en: https://www.spigotmc.org/resources/temporaryprotections.126123/.
- Compatible con Minecraft 1.21.5 (Spigot/Paper)
