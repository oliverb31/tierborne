# Project Instructions

This is a Minecraft Forge mod targeting:

- Minecraft: 1.19.2
- Minecraft Forge: 43.5.0
- Java: 17
- Mod loader: Forge

Do not:
- Upgrade Minecraft.
- Upgrade Forge unless explicitly requested.
- Migrate to NeoForge or Fabric.
- Use APIs from newer Minecraft/Forge versions.
- Change the Java version.

For Forge API questions, prefer the official Forge 1.19.2 documentation:
https://docs.minecraftforge.net/en/1.19.2/

When uncertain whether an API exists in Forge 1.19.2, verify it against
the 1.19.2 documentation or the dependencies/source available in this
Gradle project rather than assuming a modern Forge API is available.

## Validation

After making code changes, run:

Windows:
./gradlew.bat build

The build should complete successfully before considering a task finished.

For changes that need runtime testing, use:

./gradlew.bat runClient