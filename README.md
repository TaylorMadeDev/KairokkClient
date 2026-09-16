# Kairokk

Kairokk is a client-side Minecraft client project built on Fabric for Minecraft 26.1.2.

The current UI ports the Shulk-inspired main menu and options flow through ModernUI, with Kairokk’s blue accent palette and animated blue orb background.

The options flow includes the hub, Video Settings, Sounds, Controls, Language, and the linked vanilla Minecraft categories. Add-ons is currently a visual entry point reserved for the next screen import.

## Development

- Requires Java 25.
- Run `./gradlew.bat runClient` to launch the development client.
- Run `./gradlew.bat build` to create the distributable JAR in `build/libs`.

The development client uses the local ModernUI 3.13.0.5 universal runtime in `libs/` plus Forge Config API Port 26.1.3.

## Setup

For setup instructions, please see the [Fabric Documentation page](https://docs.fabricmc.net/develop/getting-started/creating-a-project#setting-up) related to the IDE that you are using.

## License

This template is available under the CC0 license. Feel free to learn from it and incorporate it in your own projects.
