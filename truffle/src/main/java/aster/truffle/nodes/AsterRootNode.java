package aster.truffle.nodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import aster.truffle.AsterLanguage;

public final class AsterRootNode extends RootNode {
  public AsterRootNode(AsterLanguage lang) { super(lang, new FrameDescriptor()); }
  @Override public Object execute(VirtualFrame frame) { return null; }
}

