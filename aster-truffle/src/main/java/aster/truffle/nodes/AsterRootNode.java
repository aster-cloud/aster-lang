package aster.truffle.nodes;

import aster.truffle.AsterLanguage;
import aster.truffle.core.CoreModel;
import aster.truffle.runtime.FrameSlotBuilder;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import java.util.List;
import java.util.Map;

public final class AsterRootNode extends RootNode {
  private final Node body;
  private final Env globalEnv;
  private final List<CoreModel.Param> params;
  private final FrameDescriptor frameDescriptor;
  private final Map<String, Integer> symbolTable;

  public AsterRootNode(AsterLanguage lang, Node body, Env globalEnv, List<CoreModel.Param> params) {
    this(lang, body, globalEnv, params, initFrame(params));
  }

  private AsterRootNode(
      AsterLanguage lang,
      Node body,
      Env globalEnv,
      List<CoreModel.Param> params,
      FrameInit frameInit) {
    super(lang, frameInit.descriptor);
    this.body = body;
    this.globalEnv = globalEnv;
    this.params = params;
    this.frameDescriptor = frameInit.descriptor;
    this.symbolTable = frameInit.symbolTable;
  }

  /**
   * 顶层程序入口，直接委托给 Loader 生成的节点树执行。
   * 捕获 ReturnException 以兼容旧运行时语义。
   */
  @Override
  public Object execute(VirtualFrame frame) {
    bindArgumentsToFrame(frame);
    bindArgumentsToEnv(frame);
    try {
      return Exec.exec(body, frame);
    } catch (ReturnNode.ReturnException rex) {
      return rex.value;
    }
  }

  public Env getGlobalEnv() {
    return globalEnv;
  }

  private void bindArgumentsToFrame(VirtualFrame frame) {
    if (params == null || params.isEmpty()) return;
    Object[] args = frame.getArguments();
    int count = Math.min(params.size(), args != null ? args.length : 0);
    for (int i = 0; i < count; i++) {
      frame.setObject(i, args[i]);
    }
  }

  private void bindArgumentsToEnv(VirtualFrame frame) {
    if (params == null || params.isEmpty()) return;
    Object[] args = frame.getArguments();
    int count = Math.min(params.size(), args != null ? args.length : 0);
    for (int i = 0; i < count; i++) {
      CoreModel.Param param = params.get(i);
      globalEnv.set(param.name, args[i]);
    }
  }

  public Map<String, Integer> getSymbolTable() {
    return symbolTable;
  }

  private static FrameInit initFrame(List<CoreModel.Param> params) {
    FrameSlotBuilder builder = new FrameSlotBuilder();
    if (params != null) {
      for (CoreModel.Param param : params) {
        builder.addParameter(param.name);
      }
    }
    FrameDescriptor descriptor = builder.build();
    return new FrameInit(descriptor, builder.getSymbolTable());
  }

  private record FrameInit(FrameDescriptor descriptor, Map<String, Integer> symbolTable) {}
}
