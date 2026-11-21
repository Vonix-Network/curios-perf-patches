package network.vonix.curiosperf.internal;

/**
 * Duck interface added to {@code LivingEntity} by {@code LivingEntityMixin}.
 *
 * <p>Holds one bit per entity: "the full capability walk already told us this entity has
 * no Curios inventory". It is only ever consulted for entity types that the Curios slot
 * manager reports as having no slots, and it is cleared whenever a capability is attached
 * to the entity, so a mod that supplies an inventory later is seen immediately.
 */
public interface CuriosPerfSlotless {

  boolean curiosperf$isKnownSlotless();

  void curiosperf$setKnownSlotless(boolean slotless);
}
