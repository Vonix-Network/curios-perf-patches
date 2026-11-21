package network.vonix.curiosperf.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import network.vonix.curiosperf.internal.CuriosPerfSlotless;

/**
 * Gives every living entity one bit of state: "a real capability walk already established
 * that nothing supplies a Curios inventory for this entity".
 *
 * <p>Deliberately a field on the entity rather than a static map, so it cannot leak: the
 * flag dies with the entity it describes.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements CuriosPerfSlotless {

  @Unique
  private boolean curiosperf$knownSlotless;

  @Override
  @Unique
  public boolean curiosperf$isKnownSlotless() {
    return this.curiosperf$knownSlotless;
  }

  @Override
  @Unique
  public void curiosperf$setKnownSlotless(boolean slotless) {
    this.curiosperf$knownSlotless = slotless;
  }
}
