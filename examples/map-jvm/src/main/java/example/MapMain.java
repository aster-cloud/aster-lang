package example;

import java.util.*;

public final class MapMain {
  public static void main(String[] args) {
    var m = new HashMap<String,String>();
    m.put("a", "1"); m.put("b", "2");
    System.out.println("val(a) => " + demo.map.val_fn.val(m, "a"));
    System.out.println("val(c) => " + demo.map.val_fn.val(m, "c"));
    System.out.println("valStrict(b) => " + demo.map.valStrict_fn.valStrict(m, "b"));
  }
}

