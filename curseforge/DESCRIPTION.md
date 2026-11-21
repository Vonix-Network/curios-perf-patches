**Behaviour-preserving performance patches for Curios API.**

Curios resolves an entity's accessory inventory by walking every capability provider
attached to that entity. For entity types that no datapack gives curio slots to — animals,
fish, villagers, most modded mobs — that walk always comes back empty, and it runs for every
such entity, every tick.

This mod remembers the empty answer per entity and reuses it. It never changes how often an
accessory ticks, so **no item behaves differently**.

### What it does not do

It does not touch your items' tick rate and it does not rewrite Curios' tick handler. If a
pack already runs another Curios optimiser, this mod becomes redundant rather than
conflicting — both patches are correct, and you can switch this one off in config.

### Safety

- **Exact version pin.** The patch applies only on Curios 5.14.1+1.20.1. On any other Curios
  build it refuses, logs one WARN, and leaves Curios exactly as it was. It never applies on a
  version guess.
- **Self-verification at startup.** The mod confirms the patch is actually present in the
  loaded Curios classes. If it is not, it says so instead of pretending to be installed.
- **The cache cannot go stale.** The remembered answer is cleared whenever an entity's
  capabilities are rebuilt, so an inventory attached later by another mod is always seen.
- **Every patch has its own config switch** in `config/curiosperf-common.toml`.

### Honest expectations

The gain is real but not dramatic. On packs already running a Curios-aware optimiser expect
a percent or two of tick time; on unpatched packs with many entities and dozens of
capability providers, the saved provider walk is worth considerably more per entity. If you
have seen "Curios costs 10% of the tick" claims — most of that belongs to the accessory mods'
own item code and to pack-specific mixins, not to Curios itself. The full profile breakdown
is in the repository under `docs/MEASUREMENTS.md`.

### Requirements

Minecraft 1.20.1 · Forge 47.x · Curios 5.14.1+1.20.1 · server-side (works on single player too)
