# Curios Performance Patches

Behaviour-preserving performance patches for **Curios API**, applied as a standalone
companion mod. One mixin per target class, replacement logic in an `internal/` package,
and **exact version pins** — a patch refuses to apply if the installed Curios build does
not match the one it was written against, rather than silently misapplying.

Curios is a shared library: every pack's accessories tick through `CuriosEventHandler`.
A patch here is paid once and collected by every mod that uses Curios.

## What this actually patches (and what it does not)

This mod only changes work that is provably redundant. It never changes *how often* your
items tick, so no accessory behaves differently.

**Included**

- **Slot-less entity tick elision.** Curios' `LivingTickEvent` handler runs for *every*
  living entity, every tick, and starts by resolving that entity's Curios inventory.
  For entities whose type has no Curios slots — animals, most monsters, most modded
  mobs — that resolution walks the entity's whole capability provider list to be told
  there is nothing there. The answer is now remembered per entity and reused while it
  stays true.

**Deliberately not included**

- **Item-tick cadence.** Running `curioTick` on a staggered interval would reduce cost,
  but items that count ticks would silently change behaviour. Rejected.
- **Per-slot curio capability caching.** Caching `CuriosApi.getCurio(stack)` per slot
  needs an identity-keyed map, and the cache value holds a strong reference back to the
  stack key, so the entries cannot be collected. Rejected rather than shipped leaky.
- **`getRenders()` hoisting.** The call sits inside a synthetic lambda whose index shifts
  if any other mod injects a lambda into the same class. Rejected as fragile.

The measurements behind those decisions, and the alternative implementations, are in
[`docs/MEASUREMENTS.md`](docs/MEASUREMENTS.md).

## Honest magnitude

On a 1.20.1 Forge pack already running a Curios-aware optimiser, this mod's remaining
headroom is small — a percent or two of tick time. Its value is on packs that are *not*
already patched, where the slot-less lookup runs for hundreds of entities per tick.
Do not install it expecting the double-digit gains that are sometimes attributed to
"the Curios tick handler"; those numbers belong to the accessory mods' own item code and
to pack-specific mixins, not to Curios. See `docs/MEASUREMENTS.md` for the full profile
breakdown.

## Compatibility

- Designed to coexist with other Curios mixins. Where another mod already optimises the
  same code, both patches are correct and this one becomes redundant, not conflicting.
- Every patch has its own switch in `config/curiosperf-common.toml`; `enabled = false`
  turns all of them off for A/B testing.

## Versions

| Lane | Loader | Curios build pinned | Status |
|---|---|---|---|
| 1.20.1 | Forge 47.x | `5.14.1+1.20.1` | **released** (`v1.20.1-1.0.0`) |
| 1.20.4 | NeoForge | `7.4.3+1.20.4` | planned |
| 1.20.6 | NeoForge | `8.1.0+1.20.6` | planned |
| 1.21.1 | NeoForge | `9.6.0+1.21.1` | planned |

Each lane is a separate Gradle build with its own exact pin, because Curios' lookup
signature and event type differ per line (`LivingTickEvent` and `LazyOptional` on 1.20.1 vs
`EntityTickEvent.Post` and `Optional` on 1.21.1). [`docs/VERSIONS.md`](docs/VERSIONS.md) has
the per-lane porting table and the gate list a lane must pass before release.

### Download

[`curiosperf-1.20.1-1.0.0.jar`](https://github.com/Vonix-Network/curios-perf-patches/releases/download/v1.20.1-1.0.0/curiosperf-1.20.1-1.0.0.jar)
— sha256 `d994d7d1afa3b70d5f910135bfc81edee813dcc503dacc85e6ed35edfcaa0580`

## Building

```bash
cd 1.20.1-forge
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew build
```

Output: `1.20.1-forge/build/libs/curiosperf-1.20.1-<version>.jar`

## Licence

LGPL-3.0-or-later. This mod patches Curios API, which is LGPL-3.0-or-later by C4; the
mixin logic that mirrors Curios' own control flow is derived from that source and carries
the same licence. Source is public here.
