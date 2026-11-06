# Yogurines Core

Mod de estilo de **Los Yogurines VI: Overclock** para Minecraft 1.20.1 (Forge). Es solo de cliente: va en el modpack de cada jugador y el servidor no lo necesita.

## Qué hace

- **Menú principal propio**: fondo animado en pixel art con las estatuas de titanio de los tres fundadores (MrYotu04, Samugodin y MettaYT). Cada vez que se abre el juego toca uno de los tres fondos al azar: la forja, el núcleo hackeado o la sala de servidores.
- **Botones**: Jugar (entra directo al server), Salir, engranaje (ajustes de Minecraft) y + (mundos en solitario).
- **Modo desarrollador escondido**: en el menú, `Ctrl + Shift + D`. Pide contraseña (la de fábrica es `overclock`, cámbiala desde el propio editor). Desde ahí se cambia la IP, los textos de los botones y si se ve el +.
- **Pausa y ajustes**: los botones normales pasan a estilo "vanilla oscuro" con borde verde al pasar el ratón, y el fondo de los ajustes es una rejilla verde.

## Cómo llega la IP a todos

Al abrir el juego, el mod lee [`server.json`](server.json) de este repositorio. En el modo desarrollador, "Publicar para todos" copia la configuración y abre este archivo en GitHub: pegas, pulsas **Commit changes** y la próxima vez que cualquiera abra el juego le llega. Si no hay internet, se usa la última que se descargó (`config/yogurines_core.json`).

Si la IP está vacía, Jugar abre la lista de servidores normal.

## Descargar

Cada cambio en `main` publica un `.jar` en [Releases](../../releases).

## Propuesta visual

En `tools/propuesta/` está la página de la propuesta y los scripts que generan las estatuas a partir de las skins (`statues.py`) y las capas del fondo (`export.js`).
