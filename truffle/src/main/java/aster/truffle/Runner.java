package aster.truffle;

import aster.truffle.nodes.*;
import com.oracle.truffle.api.frame.FrameDescriptor;

public final class Runner {
  public static void main(String[] args) {
    // Tiny demo: if (true) return "Hi" else return "Bye"
    var fd = new FrameDescriptor();
    var n = new IfNode(new LiteralNode(true), new ReturnNode(new LiteralNode("Hi")), new ReturnNode(new LiteralNode("Bye")));
    try {
      n.execute(null);
    } catch (ReturnNode.ReturnException rex) {
      System.out.println(rex.value);
    }
  }
}

