# Changelog

All notable changes to Curios Performance Patches. Format follows Keep a Changelog;
versioning is SemVer.

## [1.20.1-1.0.0] — 2026-11-21

First release, for **Curios 5.14.1+1.20.1 on Minecraft 1.20.1 / Forge 47.x**.

### Added

- **Slot-less inventory lookup elision** (`patches.slotlessInventoryLookupElision`,
  default on). Entities whose type has no Curios slots no longer pay a full capability
  provider walk on every `getCuriosInventory` call. The first lookup still runs the real
  walk and returns its answer unchanged; the empty answer is then remembered per entity
  and reused. The flag is cleared on `CapabilityProvider.invalidateCaps()` and
  `reviveCaps()`, so a capability rebuild — including a Curios inventory attached later by
  another mod — is always seen.

### Safety

- **Exact version pin.** The Curios mixin applies only on `5.14.1+1.20.1`. On any other
  Curios build it refuses, logs one WARN, and leaves Curios untouched. It never applies on
  a version guess.
- **Self-verification.** At startup the mod reflects on the loaded
  `CuriosImplMixinHooks` class and asserts the injected handler is present. If Curios
  matched the pin but the patch is somehow absent, the mod refuses to run rather than
  advertise a patch that is not there.
- **Config-driven A/B.** Every patch has its own switch.

### Deliberately excluded

- Item-tick cadence (staggering `curioTick`): changes behaviour of items that count ticks.
- Per-slot curio capability caching: cannot be made non-leaky without weak references that
  defeat the cache.
- `getRenders()` hoisting: breaks when another mod injects a lambda into the same class.

See `../docs/MEASUREMENTS.md` for the profile breakdown that drove these decisions.
