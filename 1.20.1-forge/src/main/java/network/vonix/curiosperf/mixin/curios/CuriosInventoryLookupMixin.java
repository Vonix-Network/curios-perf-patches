package network.vonix.curiosperf.mixin.curios;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.util.LazyOptional;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.mixin.CuriosImplMixinHooks;

import network.vonix.curiosperf.CuriosPerfConfig;
import network.vonix.curiosperf.internal.CuriosPerfSlotless;
import network.vonix.curiosperf.mixin.CuriosPerfLog;

/**
 * The single point every Curios inventory lookup funnels through.
 *
 * <p>{@code CuriosApi.getCuriosInventory} is a stub that delegates here, so patching this
 * one method speeds up every caller at once: Curios' own tick handler, every accessory
 * mod's "is this item worn?" check, render layers, and third-party integrations.
 *
 * <p>Forge's {@code CapabilityDispatcher.getCapability} walks every provider attached to
 * the entity and returns the first non-empty answer. Curios attaches a provider to every
 * living entity, and that provider returns {@code LazyOptional.empty()} immediately when
 * the entity's type has no curio slots. For such an entity the walk therefore cannot
 * return an inventory from Curios — it only burns time crossing the remaining providers.
 *
 * <p>This patch does not assume that. The first time an entity of a slot-less type is
 * asked, the real walk still runs and its answer is returned unchanged; only when that
 * walk comes back empty is the entity remembered as slot-less. From then on the walk is
 * skipped. The memory is cleared whenever the entity's capabilities are invalidated or
 * rebuilt, so an inventory supplied by another mod is always seen on the next lookup.
 */
@Mixin(value = CuriosImplMixinHooks.class, remap = false)
public abstract class CuriosInventoryLookupMixin {

  @Inject(method = "getCuriosInventory", at = @At("HEAD"), cancellable = true, remap = false)
  private static void curiosperf$skipSlotlessLookup(LivingEntity livingEntity,
                                                    CallbackInfoReturnable<LazyOptional<ICuriosItemHandler>> cir) {
    // A null entity is handled by the original method; leave it alone.
    if (livingEntity == null || !CuriosPerfConfig.COMMON.slotlessInventoryLookupElision.get()) {
      return;
    }

    CuriosPerfSlotless state = (CuriosPerfSlotless) livingEntity;

    if (state.curiosperf$isKnownSlotless()) {
      cir.setReturnValue(LazyOptional.empty());
      return;
    }

    // Cheap type-level gate: only entities whose type has no slots at all are candidates.
    if (!CuriosApi.getEntitySlots(livingEntity).isEmpty()) {
      return;
    }

    // Run the genuine lookup once, and return exactly what it returned.
    LazyOptional<ICuriosItemHandler> actual =
        livingEntity.getCapability(top.theillusivec4.curios.api.CuriosCapability.INVENTORY);

    if (!actual.isPresent()) {
      // Confirmed: no provider (Curios' own or anyone else's) answers for this entity.
      state.curiosperf$setKnownSlotless(true);
      CuriosPerfLog.infoOnce(
          "slotless inventory lookup elision active; entities with no curio slots now skip "
              + "the capability walk (config: patches.slotlessInventoryLookupElision)");
    }

    cir.setReturnValue(actual);
  }
}
