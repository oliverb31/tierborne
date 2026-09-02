---
name: forge-1-19-2
description: Create, modify, debug, and review Minecraft Forge 1.19.2 Java mods. Use for Tierborne gameplay, rendering, networking, registries, events, data, configuration, and compatibility work; do not use modern NeoForge, Fabric, or newer Forge APIs.
---

# Forge 1.19.2 Modding

Work against the version declared by the project, not current Minecraft examples.

## Version contract

- Minecraft 1.19.2
- Forge 43.5.0 / FML 43
- Java 17
- Mojang official mappings for 1.19.2
- `mods.toml` and `net.minecraftforge.*`; never migrate to NeoForge or Fabric

Treat the repository's `AGENTS.md` as authoritative. Do not upgrade Minecraft,
Forge, mappings, Gradle, or Java unless the user explicitly requests it.

## Workflow

1. Inspect `gradle.properties`, `build.gradle`, the relevant existing classes,
   and nearby patterns before designing a change.
2. Keep logical state and validation server-authoritative. Use client code only
   for input, screens, rendering, prediction, and presentation.
3. When an API, event, signature, or mapping is uncertain, verify it in this
   order:
   - the project's resolved Forge/Minecraft sources and dependencies;
   - existing compiling code in this repository;
   - the official [Forge 1.19.2 documentation](https://docs.minecraftforge.net/en/1.19.2/).
4. Do not copy examples from 1.20.x, 1.21.x, current Forge, or NeoForge without
   verifying every referenced type and signature locally.
5. Preserve dedicated-server safety: client-only Minecraft classes belong in
   client packages and must be guarded by the appropriate distribution/event
   subscriber boundaries.
6. For multiplayer gameplay, validate packet direction and payloads, enqueue
   handling on the correct thread, and derive the sending player from the
   network context rather than trusting a client-supplied identity.
7. Follow the registry and event-bus style already used by Tierborne. Avoid
   introducing a second framework for the same concern.
8. After code or resource changes, run `./gradlew.bat build`. Use
   `./gradlew.bat runClient` when the result requires visual or runtime testing.

## Tierborne-specific decisions

- Mod ID: `tierborne`; root package: `com.ollie.tierborne`.
- Prefer the existing `SimpleChannel`, saved-data, skill-tree, ability-runtime,
  configuration, and client-state structures over parallel replacements.
- Balance values that designers may tune belong in `RpgBalanceConfig`.
- New ability state must be cleared on disconnect/death where applicable and
  synchronized only when another side needs it.
- For first-person arm or held-item animation, begin with Forge 1.19.2 client
  render events and `PoseStack` transforms. Add a library only when the desired
  result requires model/keyframe or third-person animation that native hooks
  cannot reasonably provide.
- Do not modify or remove unrelated user changes in this dirty worktree.

## Completion standard

The change is not complete until the Forge build succeeds, unless a concrete
environmental blocker is reported. For rendering or input changes, distinguish
successful compilation from visual verification and state whether `runClient`
was performed.
