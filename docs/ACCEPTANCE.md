# Acceptance evidence — 1.20.1 Forge

Recorded Nov 21, 2026. Every line below is a real command result, not a description.

## Build

```
$ JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew build
Curios pin OK: maven.modrinth:curios:5.14.1+1.20.1_mapped_official_1.20.1 (...)
BUILD SUCCESSFUL in 56s

$ ls -l build/libs/
curiosperf-1.20.1-1.0.0.jar   14915 bytes
```

Upstream pin: `curios-forge-5.14.1+1.20.1.jar`
sha256 `1e817919a35b37cf30524aaec73f0ca5130452f23f168f844854df282eb8e51f`
(Modrinth version `IPQlZkz1`; `mods.toml` reports `5.14.1+1.20.1`).

The build has a `verifyCuriosPin` task that fails the build if the pinned artifact does not
resolve, so a jar can never be produced against a substitute Curios.

## Gate 1 — production jar, pinned Curios, real Forge server

Forge 1.20.1-47.4.20 installed via the official installer; `curios-forge-5.14.1+1.20.1.jar`
and `curiosperf-1.20.1-1.0.0.jar` in `mods/`.

```
[curiosperf] Curios 5.14.1+1.20.1 matches the pin; patches enabled.
[curiosperf] verified: slotless inventory lookup patch present in
             top.theillusivec4.curios.mixin.CuriosImplMixinHooks
[curiosperf] Curios Performance Patches active; patches are individually switchable
             in config/curiosperf-common.toml
[DedicatedServer]: Done (18.071s)! For help, type "help"
```

`verified: ... patch present` is a reflective check of the loaded Curios class for the
injected handler. It is the proof that the mixin applied — not the absence of an error.

## Gate 2 — version mismatch must refuse, not misapply

Same Curios bytecode with `mods.toml` version changed to `5.14.2+1.20.1`:

```
[curiosperf] Curios 5.14.2+1.20.1 is not the build these patches were written for
             (5.14.1+1.20.1); CuriosInventoryLookupMixin is skipped and Curios is
             left unchanged.
[curiosperf] INACTIVE: Curios 5.14.2+1.20.1 does not match the pinned build
             (5.14.1+1.20.1). No Curios code was changed. Install the pinned Curios
             build, or use a build of these patches for your version.
[DedicatedServer]: Done (7.539s)! For help, type "help"
```

Result: the patch refused, Curios was not touched, and the server still booted normally.
No mixin error was produced, because nothing was attempted.

## Gate 3 — development server, mixin application audit

`runServer` with Forge's `DEBUG` mixin logging:

```
Mixing CapabilityProviderMixin from curiosperf.mixins.json into
       net.minecraftforge.common.capabilities.CapabilityProvider
Mixing LivingEntityMixin from curiosperf.mixins.json into
       net.minecraft.world.entity.LivingEntity
```

No `MixinApplyError` and no `InvalidInjectionException` anywhere in either log.

> **Caught by this gate:** the first version injected `invalidateCaps` into
> `net.minecraft.world.entity.Entity`. That method actually lives on Forge's
> `CapabilityProvider`, so the injection was a critical failure and the server refused to
> start. The mixin was retargeted and re-verified. This is exactly the defect class that a
> "it compiles" check misses.

## Not yet done (requires server owner sign-off)

- A post-install spark profile on the live server, compared against the two baseline
  profiles, with the Curios handler share and MSPT median as the two numbers that must move.
- Nothing in this repo has been installed on any production server.
