package example;

import java.util.*;

public final class ListMain {
  public static void main(String[] args) {
    var xs = Arrays.asList("a", "b", "c", "d");
    System.out.println("length => " + demo.list.length_fn.length(xs));
    System.out.println("third => " + demo.list.third_fn.third(xs));
    System.out.println("isEmpty => " + demo.list.isEmpty_fn.isEmpty(xs));
  }
}
