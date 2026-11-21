package network.vonix.curiosperf;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Every patch is individually switchable so a pack owner can A/B test, and so a patch can
 * be turned off alone if a specific mod combination misbehaves.
 */
public final class CuriosPerfConfig {

  public static final ForgeConfigSpec SPEC;
  public static final Common COMMON;

  static {
    Pair<Common, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(Common::new);
    COMMON = pair.getLeft();
    SPEC = pair.getRight();
  }

  private CuriosPerfConfig() {
  }

  public static final class Common {

    /**
     * Skips the Curios inventory capability walk for entities whose type has no curio
     * slots, once that has been established for the entity.
     */
    public final ForgeConfigSpec.BooleanValue slotlessInventoryLookupElision;

    Common(ForgeConfigSpec.Builder builder) {
      builder.comment("Curios Performance Patches").push("patches");

      this.slotlessInventoryLookupElision = builder
          .comment(
              "Curios resolves an entity's inventory by walking every capability provider",
              "attached to it. For entity types that no datapack gives curio slots to",
              "(animals, most monsters, most modded mobs) that walk always comes back empty.",
              "This remembers the empty answer per entity and reuses it while it stays true.",
              "Turns itself off automatically for any entity that is given slots or that",
              "another mod supplies an inventory for.")
          .define("slotlessInventoryLookupElision", true);

      builder.pop();
    }
  }
}
