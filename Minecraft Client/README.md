# Kairokk

Kairokk is a client-side Minecraft client project built on Fabric for Minecraft 26.1.2.

The current UI ports the Shulk-inspired main menu and options flow through ModernUI, with Kairokk’s blue accent palette and animated blue orb background. The Add-ons screen includes a verified Modrinth downloader for PackDisabler for Hypixel SkyBlock.

The options flow includes the hub, Video Settings, Sounds, Controls, Language, and the linked vanilla Minecraft categories.

## Development

- Requires Java 25.
- Run `./gradlew.bat runClient` to launch the development client.
- Run `./gradlew.bat runAuthenticatedClient` to launch with your Microsoft Minecraft account. The first run opens a device-code login; later runs refresh the cached session automatically.
- Run `./gradlew.bat build` to create the distributable JAR in `build/libs`.

### Link the website dashboard

Start the local server, then set `KAIROKK_DASHBOARD_EMAIL` and `KAIROKK_DASHBOARD_PASSWORD` before the first `runAuthenticatedClient` launch. `KAIROKK_BACKEND` is optional and defaults to `http://127.0.0.1:3000`. The password is never saved; later launches rotate the saved refresh session automatically.

The link can also be prepared separately with `./gradlew.bat linkKairokkDashboard`. Its renewable session and random device fingerprint live outside the repository at `%LOCALAPPDATA%\Kairokk\dashboard-link.json`.

The live bridge currently supports website-driven HUD toggling, Item ESP toggling, chat messages, and pathfinder start/stop commands. Commands are validated and executed on Minecraft's main thread.

World View is subscription-driven and runs through the same authenticated socket. Its independent `WorldViewService` captures a bounded nearby chunk window, prioritizes nearby chunks, sends interpolatable player state and Pathfinder routes, and stops immediately on unsubscribe or backend disconnect. See [`../WORLD_VIEW_PROTOCOL.md`](../WORLD_VIEW_PROTOCOL.md) for the exact v1 lifecycle and packet shapes.

The development client uses the local ModernUI 3.13.0.5 universal runtime in `libs/` plus Forge Config API Port 26.1.3.
The authenticated launcher stores its refresh session outside the repository at `%LOCALAPPDATA%\Kairokk\minecraft-auth.json`.

## Setup

For setup instructions, please see the [Fabric Documentation page](https://docs.fabricmc.net/develop/getting-started/creating-a-project#setting-up) related to the IDE that you are using.

## License

This template is available under the CC0 license. Feel free to learn from it and incorporate it in your own projects.
