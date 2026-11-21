# Multi-version plan

Owner direction: **a Curios performance patch for all affected versions, 1.20.1 Forge
first.** 1.20.1 Forge is done and released. This file records what the other lanes need, so
the work is a port rather than a re-investigation.

## Why Curios lines, not Minecraft versions

The patch targets Curios, so the lane boundaries are **Curios releases**, and the Minecraft
version follows from them. Curios has one release per Minecraft line:

| Curios | MC | Loader | Released | Patch relevance |
|---|---|---|---|---|
| `5.14.1+1.20.1` | 1.20.1 | Forge (+ NeoForge) | 2025-04-13 | **done** — newest 1.20.1 build, branch frozen |
| `7.4.3+1.20.4` | 1.20.4 | NeoForge | 2024-05-31 | candidate |
| `8.1.0+1.20.6` | 1.20.6 | NeoForge (+ Forge) | 2024-10-25 | candidate |
| `9.6.0+1.21.1` | 1.21.1 | NeoForge | 2026-11-19 | candidate, actively maintained |

1.20.1 is the only frozen line. Everything from 1.21.1 onward is still receiving upstream
releases (1.21.1 shipped `9.6.0` on 2026-11-19), so the *need* is lower there — check whether
upstream has already fixed the lookup before porting, and say so rather than shipping a
patch that duplicates it.

## What changes per lane

Verified by reading the decompiled sources, not assumed:

| Concern | 1.20.1 Forge | 1.21.1 NeoForge |
|---|---|---|
| Event type | `LivingEvent.LivingTickEvent` | `EntityTickEvent.Post` |
| Lookup hook | `CuriosImplMixinHooks.getCuriosInventory` (static, `LazyOptional`) | same class, returns `Optional` |
| Capability API | `net.minecraftforge.common.util.LazyOptional` | NeoForge `net.neoforged.neoforge.common.util.LazyOptional` |
| Capability invalidation hook | `CapabilityProvider.invalidateCaps/reviveCaps` | verify the NeoForge equivalent before porting |
| Mixin config | `curiosperf.mixins.json`, `JAVA_17` | `JAVA_21` |
| Gradle | ForgeGradle `[6.0,6.2)`, `net.minecraftforge:forge:1.20.1-47.4.20` | NeoGradle / ModDevGradle with `neoforge:21.1.x` |

The patch *mechanism* is identical across lanes: run the real provider walk once, remember a
negative answer per entity, clear it on capability invalidation. Only the API surface moves.

## Procedure per lane

1. Fetch the pinned Curios **sources** jar for that line (Modrinth has them for every
   release). Read `CuriosEventHandler#tick` and the lookup hook for that version; do not
   assume the 1.20.1 shape.
2. Check whether another mod already patches the same method in the wild — the 1.20.1
   finding was that two mods already inject into `CuriosEventHandler#tick`. If the ecosystem
   has covered it, report that instead of shipping.
3. New Gradle lane directory (`<mc>-<loader>/`), its own pin in `gradle.properties`, its own
   `verifyCuriosPin` task.
4. Port the three classes: entity flag, capability-invalidation hook, lookup mixin.
5. Gates, all of which must pass before release:
   - build with the pin verified
   - production-jar boot on a real dedicated server, with the reflective
     "patch present" line in the log
   - version-mismatch refusal test (change the Curios `mods.toml` version and confirm it
     refuses and still boots)
   - fresh-clone build reproduces the artifact
6. Add a CHANGELOG section and a README row; publish a release tagged for that lane.

## Two things not to repeat

- **Do not rewrite `CuriosEventHandler#tick`'s body.** On 1.20.1 two third-party mods inject
  into that exact method. Patching the shared lookup callee gets the same win and composes.
- **Do not stagger item ticks.** It is the only change that reaches double digits, and it
  changes the behaviour of any item that counts ticks. That is a gameplay change, not a
  performance patch.
