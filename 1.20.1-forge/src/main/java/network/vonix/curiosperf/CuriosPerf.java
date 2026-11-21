package network.vonix.curiosperf;

import java.lang.reflect.Method;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import network.vonix.curiosperf.internal.CuriosPerfMixinPlugin;
import network.vonix.curiosperf.mixin.CuriosPerfLog;

@Mod(CuriosPerf.MODID)
public final class CuriosPerf {

  public static final String MODID = "curiosperf";

  /**
   * Name fragment of the handler method our mixin injects into the Curios lookup. Mixin
   * derives this name from the config name {@code curiosperf} and the patch method name.
   */
  private static final String INJECTED_METHOD_MARKER = "curiosperf$skipSlotlessLookup";

  public CuriosPerf() {
    ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CuriosPerfConfig.SPEC,
        "curiosperf-common.toml");
    FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
  }

  private void commonSetup(final FMLCommonSetupEvent event) {
    event.enqueueWork(() -> {
      // Load the patched class now and prove the patch is really there. A mixin that
      // silently failed to apply is worse than one that refuses to start: it would look
      // installed while doing nothing.
      verifyPatchApplied();

      MinecraftForge.EVENT_BUS.register(this);
      CuriosPerfLog.info("Curios Performance Patches active; patches are individually "
          + "switchable in config/curiosperf-common.toml");
    });
  }

  private void verifyPatchApplied() {
    final String target = "top.theillusivec4.curios.mixin.CuriosImplMixinHooks";

    // A deliberate refusal (unpinned Curios) is a supported state, not corruption: the mod
    // stays loaded, reports itself inactive, and Curios runs untouched.
    if (CuriosPerfMixinPlugin.refusedCuriosPatches()) {
      CuriosPerfLog.warn("INACTIVE: Curios " + CuriosPerfMixinPlugin.detectedCuriosVersion()
          + " does not match the pinned build (5.14.1+1.20.1). No Curios code was changed. "
          + "Install the pinned Curios build, or use a build of these patches for your version.");
      return;
    }

    try {
      Class<?> hooks = Class.forName(target);
      for (Method m : hooks.getDeclaredMethods()) {
        if (m.getName().contains(INJECTED_METHOD_MARKER)) {
          CuriosPerfLog.info("verified: slotless inventory lookup patch present in " + target);
          return;
        }
      }
      throw new IllegalStateException(
          "[curiosperf] the Curios inventory lookup patch is NOT present in " + target
              + " even though Curios matched the pin. Refusing to run rather than pretend "
              + "to be installed. Report this with your Curios and Forge versions.");
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException(
          "[curiosperf] could not load " + target + "; the Curios build does not match the "
              + "pinned version. Refusing to run.", e);
    }
  }

  /**
   * Logs the effective config once per world load rather than per patch application, so an
   * operator reading the log can see what is actually running.
   */
  @SubscribeEvent
  public void onEntityJoin(EntityJoinLevelEvent event) {
    if (!event.getLevel().isClientSide() && event.getEntity().tickCount == 0) {
      CuriosPerfLog.infoOnce("patches.slotlessInventoryLookupElision="
          + CuriosPerfConfig.COMMON.slotlessInventoryLookupElision.get()
          + " (server side)");
    }
  }

  @SubscribeEvent
  public void onServerStopped(ServerStoppedEvent event) {
    CuriosPerfLog.info("server stopped; slot-less flags are per-entity and not persisted");
  }
}
