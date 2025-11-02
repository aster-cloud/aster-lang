package aster.truffle;

import aster.truffle.runtime.AsterConfig;
import aster.truffle.runtime.Builtins;
import com.oracle.truffle.api.TruffleLanguage;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Aster 语言运行时上下文。
 *
 * 负责封装 Truffle 环境、内建函数注册表与配置快照，提供线程安全的访问接口。
 */
public final class AsterContext {
  private final TruffleLanguage.Env env;
  private final AtomicReference<Builtins> builtinsRef = new AtomicReference<>();
  private final ConfigView configView;
  private final Class<AsterConfig> configClass;

  public AsterContext(TruffleLanguage.Env env) {
    this.env = Objects.requireNonNull(env, "env");
    // 预先捕捉静态配置，避免执行过程中反复读取环境变量
    this.configView = new ConfigView(AsterConfig.DEBUG, AsterConfig.PROFILE, AsterConfig.DEFAULT_FUNCTION);
    this.configClass = AsterConfig.class;
  }

  public TruffleLanguage.Env getEnv() {
    return env;
  }

  /**
   * 延迟初始化内建函数注册表。
   * 目前注册表为静态实现，但仍通过原子引用保证未来扩展时的线程安全。
   */
  public Builtins getBuiltins() {
    Builtins current = builtinsRef.get();
    if (current != null) {
      return current;
    }
    Builtins created = new Builtins();
    return builtinsRef.compareAndSet(null, created) ? created : builtinsRef.get();
  }

  /**
   * 返回配置快照，便于在节点中读取调试/性能等开关。
   */
  public ConfigView getConfig() {
    return configView;
  }

  /**
   * 暴露原始配置类引用，方便后续迁移至非静态配置时保留向后兼容。
   */
  public Class<AsterConfig> getConfigClass() {
    return configClass;
  }

  /**
   * 配置快照，仅包含运行时常用的几个开关。
   */
  public static final class ConfigView {
    private final boolean debugEnabled;
    private final boolean profileEnabled;
    private final String defaultFunction;

    private ConfigView(boolean debugEnabled, boolean profileEnabled, String defaultFunction) {
      this.debugEnabled = debugEnabled;
      this.profileEnabled = profileEnabled;
      this.defaultFunction = defaultFunction;
    }

    public boolean isDebugEnabled() {
      return debugEnabled;
    }

    public boolean isProfileEnabled() {
      return profileEnabled;
    }

    public String getDefaultFunction() {
      return defaultFunction;
    }
  }
}
