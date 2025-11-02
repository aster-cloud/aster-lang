package aster.truffle.nodes;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class Env {
  private final Map<String,Object> vars = new HashMap<>();
  public Object get(String name) { return vars.get(name); }
  public void set(String name, Object v) { vars.put(name, v); }
  public Set<String> getAllKeys() { return vars.keySet(); }
}

