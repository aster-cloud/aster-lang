package aster.truffle;

import aster.truffle.nodes.AsterRootNode;
import aster.truffle.runtime.AsterConfig;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.source.Source;

@TruffleLanguage.Registration(id = "aster", name = "Aster", version = "0.1")
public final class AsterLanguage extends TruffleLanguage<AsterContext> {

  @Override
  protected AsterContext createContext(Env env) {
    return new AsterContext(env);
  }

  @Override
  protected CallTarget parse(ParsingRequest request) throws Exception {
    Source source = request.getSource();
    String jsonContent = source.getCharacters().toString();

    Loader loader = new Loader(this);
    String funcName = AsterConfig.DEFAULT_FUNCTION;

    Loader.Program program = loader.buildProgram(jsonContent, funcName, null);
    AsterRootNode rootNode = new AsterRootNode(this, program.root, program.env, program.params);
    // 当前阶段直接暴露 Loader 生成的节点树；后续会逐步替换为 Truffle DSL
    return rootNode.getCallTarget();
  }
}
