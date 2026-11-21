# Measurements and patch decisions

Data comes from two live spark profiles on a **1.20.1 Forge 47.4.20 server**, Java 21,
Ryzen 9 7950X, 9–10 players, running a large modpack (~418 mods). Profiles were parsed from
the raw sampler protobuf (`bytebin.lucko.me/<key>` → `spark.proto` / `spark_sampler.proto`)
rather than read off the web UI, so the numbers below are reproducible.

Percentages are share of server-thread time, per 30-second window, summed across windows.

## Headline numbers

| | Profile A | Profile B |
|---|---|---|
| TPS (1m/5m/15m) | 16.61 / 15.83 / 16.75 | 17.80 / 16.61 / 16.68 |
| MSPT median / 95th / max (5m) | 57.5 / 80.4 / 283 | 55.9 / 67.7 / 1133 |
| `CuriosEventHandler#tick` | **9.96%** | **10.50%** |
| Player tick | 41.03% | 46.40% |
| Entity tick | 38.47% | 31.97% |
| Chunk tick | 6.01% | 6.92% |

Garbage collection is not a factor: G1 Young only, **zero** old-generation collections on
both profiles, no GC warnings. Process CPU was 8–9% of a 32-thread machine, so the server
is main-thread bound. Raising the heap or tuning GC would not help.

## The Curios tick handler does not break down the way it looks

`CuriosEventHandler#tick` measured 9.96% / 10.50%. It is tempting to read that as "Curios
wastes 10% of the tick". Decomposed by owning code, it is not:

| Component | Share (A) | Whose code |
|---|---|---|
| Equipped items' `curioTick` work (Artifacts 1.09, one MCreator item 0.71, another 0.51, 0.31, 0.31, 0.30, long tail …) | **~4.6%** | **the accessory mods' own item code** |
| A third-party mixin re-applying attribute modifiers for every slot every tick | **2.26%** | **not Curios** |
| `CurioChangeEvent` post + listeners | **1.06%** | change-detection path |
| `syncCurios` → `SPacketSyncStack` | **0.55%** | change-detection path |
| `CuriosApi#getCurio` capability lookup per slot | 0.37% | Curios |
| `ItemStack` copy + `matches` for previous-stack comparison | 0.61% | Curios |
| Slot-loop overhead (`SlotContext` alloc, map walk, `getRenders`) | ~0.5% | Curios |
| `clearCachedSlotModifiers` | 0.05% | Curios, already optimised in the wild |

Curios' own attributable waste inside the handler is **≈1.0–1.5%**, not ~10%.

Reaching a double-digit number requires one of two things, and neither belongs in a
performance patch mod:

1. **Stop ticking equipped items every tick** (staggered cadence). Any item that counts
   ticks changes behaviour. That is a gameplay change.
2. **Fix the per-tick churn loop in the change-detection path.** `CurioChangeEvent` and a
   sync packet fire far more often than equip events justify — the signature of an equipped
   item whose NBT mutates every tick, so `ItemStack.matches(stack, prevStack)` never
   matches again. Real, worth more than Curios' own waste, and *not* safe as a blanket
   patch: those items depend on the sync to reach clients. It is a pack-level finding.

## What this mod patches instead

`CuriosApi.getCuriosInventory` → `CuriosImplMixinHooks.getCuriosInventory` →
`entity.getCapability(CAPABILITY)`. Forge's capability dispatcher walks **every** provider
attached to the entity and returns the first non-empty answer.

Curios attaches a provider to every living entity, and that provider returns
`LazyOptional.empty()` immediately when the entity's type has no curio slots
(`CurioInventoryCapability.Provider#getCapability`). So for every animal, fish, villager and
most modded mobs, every tick pays a full provider walk in order to be told there is nothing
there — and this runs not just from Curios' own tick handler but from every accessory mod's
"is this item worn?" check on every entity.

The patch:

1. First lookup for an entity of a slot-less type: run the **real** walk, return its answer
   unchanged.
2. Only if that walk came back empty, set one boolean on the entity.
3. Subsequent lookups return `LazyOptional.empty()` without the walk.
4. Clear the flag on `CapabilityProvider.invalidateCaps()` / `reviveCaps()`, so any
   capability rebuild — including an inventory attached later by another mod — is seen on
   the next lookup.

Independent reporting of the same technique on the same Curios build puts a tick handler
call at ~409 ns → ~9 ns with 25 capability providers attached, and ~16 ns → ~9 ns with
Curios' provider alone. Those are third-party figures; re-measure on your own pack.

## Designs considered and rejected

- **Per-slot curio capability cache.** Needs an identity-keyed `ItemStack → ICurio` map. The
  cached value holds the stack that is the key, so entries are unreachable to the collector
  unless weak references are used — and a `WeakHashMap` whose value references its own key
  collects nothing. A leak is worse than the 0.37% it would save. Rejected.
- **Hoisting `getRenders()` out of the slot loop.** It sits inside a synthetic lambda; any
  other mod injecting a lambda into that class shifts the index and the patch silently stops
  finding its target. Rejected as fragile across packs.
- **Staggering `curioTick` / `inventoryTick`.** Behaviour change. Rejected.
- **Gating `clearCachedSlotModifiers` / `handleInvalidStacks`.** Already ~0.05%, and already
  optimised by mods in the wild. Not worth a patch.

## Reproducing

```bash
# raw profile data (protobuf) — no web UI or account needed
curl -s "https://bytebin.lucko.me/<profile-key>" -o profile.bin
```

Decode with `spark_sampler.proto` from the spark repository. The percentages above are
per-window shares of the `Server thread` root node, summed across windows.
