package network.vonix.curiosperf.internal;

import java.util.List;
import java.util.Set;
import net.minecraftforge.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import network.vonix.curiosperf.mixin.CuriosPerfLog;

/**
 * Applies the Curios mixins only when the installed Curios build is the exact one they
 * were written against.
 *
 * <p>This is the "refuse, never misapply" rule: if a pack ships a different Curios, the
 * patch is skipped with one WARN line and Curios is left exactly as it was. It never
 * applies on a guess. Mixins that target Minecraft itself (which every 1.20.1 pack
 * shares) are always applied.
 */
public final class CuriosPerfMixinPlugin implements IMixinConfigPlugin {

  /** The only Curios build these patches are valid for. Must match gradle.properties. */
  public static final String PINNED_CURIOS_VERSION = "5.14.1+1.20.1";

  private static final String CURIOS_MIXIN_PACKAGE = "network.vonix.curiosperf.mixin.curios";

  /**
   * True when the Curios patches were deliberately skipped because the installed build is
   * not the pinned one. The mod reads this so it can report "installed but inactive"
   * instead of treating the absence of the patch as corruption.
   */
  private static volatile boolean refusedCuriosPatches = false;

  private static volatile String detectedCuriosVersion = "<unresolved>";

  public static boolean refusedCuriosPatches() {
    return refusedCuriosPatches;
  }

  public static String detectedCuriosVersion() {
    return detectedCuriosVersion;
  }

  private boolean checked = false;
  private boolean curiosVersionOk = false;
  private String detectedVersion = "<unresolved>";

  @Override
  public void onLoad(String mixinPackage) {
    this.detectCuriosVersion();
  }

  @Override
  public String getRefMapperConfig() {
    return null;
  }

  @Override
  public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
    if (!mixinClassName.startsWith(CURIOS_MIXIN_PACKAGE)) {
      return true;
    }
    if (!this.checked) {
      this.detectCuriosVersion();
    }
    if (!this.curiosVersionOk) {
      CuriosPerfMixinPlugin.detectedCuriosVersion = this.detectedVersion;
      CuriosPerfMixinPlugin.refusedCuriosPatches = true;
      CuriosPerfLog.warnOnce(
          "Curios " + this.detectedVersion + " is not the build these patches were written for ("
              + PINNED_CURIOS_VERSION + "); "
              + mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1)
              + " is skipped and Curios is left unchanged.");
      return false;
    }
    return true;
  }

  private void detectCuriosVersion() {
    if (this.checked) {
      return;
    }
    this.checked = true;
    try {
      var modFile = FMLLoader.getLoadingModList().getModFileById("curios");
      if (modFile == null) {
        this.detectedVersion = "<curios not installed>";
        this.curiosVersionOk = false;
        return;
      }
      this.detectedVersion = modFile.versionString();
      this.curiosVersionOk = PINNED_CURIOS_VERSION.equals(this.detectedVersion);
    } catch (Throwable t) {
      // Loading list not ready yet at this point in startup. Do not guess: refuse.
      this.detectedVersion = "<not resolvable this early>";
      this.curiosVersionOk = false;
    }
    if (this.curiosVersionOk) {
      CuriosPerfMixinPlugin.detectedCuriosVersion = this.detectedVersion;
      CuriosPerfMixinPlugin.refusedCuriosPatches = false;
      CuriosPerfLog.infoOnce("Curios " + this.detectedVersion + " matches the pin; patches enabled.");
    } else {
      CuriosPerfMixinPlugin.detectedCuriosVersion = this.detectedVersion;
    }
  }

  @Override
  public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
  }

  @Override
  public List<String> getMixins() {
    return null;
  }

  @Override
  public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName,
                       IMixinInfo mixinInfo) {
  }

  @Override
  public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName,
                        IMixinInfo mixinInfo) {
  }
}
