package io.quarkus.runtime.graal;

import java.net.Inet6Address;

/**
 * 处理 {@code Inet.INET6_ANY_CIDR} 静态字段。
 */
public final class Inet6AnyCidrAccessor {
  private static volatile Inet6Address value = InetAccessorUtils.resolveV6("::");

  private Inet6AnyCidrAccessor() {
  }

  public static Inet6Address get() {
    return value;
  }

  public static void set(Inet6Address newValue) {
    value = newValue;
  }
}
