package io.quarkus.runtime.graal;

import io.smallrye.common.net.CidrAddress;

/**
 * 处理 {@code Inet.INET4_ANY_CIDR} 静态字段。
 */
public final class Inet4AnyCidrAccessor {
  private static volatile CidrAddress value = CidrAddress.INET4_ANY_CIDR;

  private Inet4AnyCidrAccessor() {
  }

  public static CidrAddress get() {
    return value;
  }

  public static void set(CidrAddress newValue) {
    value = newValue;
  }
}
