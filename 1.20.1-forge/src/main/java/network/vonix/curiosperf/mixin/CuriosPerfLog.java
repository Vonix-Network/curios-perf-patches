package network.vonix.curiosperf.mixin;

import com.mojang.logging.LogUtils;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;

/** Keeps startup noise to one line per distinct message. */
public final class CuriosPerfLog {

  private static final Logger LOGGER = LogUtils.getLogger();
  private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();
  private static final Set<String> INFORMED = ConcurrentHashMap.newKeySet();

  private CuriosPerfLog() {
  }

  public static void warnOnce(String message) {
    if (WARNED.add(message)) {
      LOGGER.warn("[curiosperf] {}", message);
    }
  }

  public static void infoOnce(String message) {
    if (INFORMED.add(message)) {
      LOGGER.info("[curiosperf] {}", message);
    }
  }

  public static void warn(String message) {
    LOGGER.warn("[curiosperf] {}", message);
  }

  public static void info(String message) {
    LOGGER.info("[curiosperf] {}", message);
  }
}
