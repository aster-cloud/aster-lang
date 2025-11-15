package io.quarkus.runtime.graal;

import java.net.Inet4Address;

/**
 * 处理 {@code Inet.INET4_ANY_CIDR} 静态字段。
 */
public final class Inet4AnyCidrAccessor {
  private static volatile Inet4Address value = InetAccessorUtils.resolveV4("0.0.0.0");

  private Inet4AnyCidrAccessor() {
  }

  public static Inet4Address get() {
    return value;
  }

  public static void set(Inet4Address newValue) {
    value = newValue;
  }
}
