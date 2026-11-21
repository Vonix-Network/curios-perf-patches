package network.vonix.curiosperf.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.capabilities.CapabilityProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import network.vonix.curiosperf.internal.CuriosPerfSlotless;

/**
 * Safety net for the slot-less flag.
 *
 * <p>{@code invalidateCaps()} and {@code reviveCaps()} live on Forge's
 * {@code CapabilityProvider}, which {@code Entity} extends. They are called whenever an
 * entity's capability providers are torn down or rebuilt — most importantly when an entity
 * moves between levels. A remembered "no inventory" answer must not survive that, or a mod
 * that attaches an inventory afterwards would be ignored. Clearing on both hooks means the
 * next lookup after a rebuild runs the real walk again.
 *
 * <p>This is what makes the cached answer safe: it is invalidated by the same event that
 * invalidates the capabilities it describes.
 */
@Mixin(value = CapabilityProvider.class, remap = false)
public abstract class CapabilityProviderMixin {

  @Inject(method = "invalidateCaps", at = @At("HEAD"), remap = false)
  private void curiosperf$clearSlotlessOnInvalidate(CallbackInfo ci) {
    if ((Object) this instanceof LivingEntity living) {
      ((CuriosPerfSlotless) living).curiosperf$setKnownSlotless(false);
    }
  }

  @Inject(method = "reviveCaps", at = @At("HEAD"), remap = false)
  private void curiosperf$clearSlotlessOnRevive(CallbackInfo ci) {
    if ((Object) this instanceof LivingEntity living) {
      ((CuriosPerfSlotless) living).curiosperf$setKnownSlotless(false);
    }
  }
}
