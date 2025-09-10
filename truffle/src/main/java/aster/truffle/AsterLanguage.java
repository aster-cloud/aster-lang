package aster.truffle;

import com.oracle.truffle.api.TruffleLanguage;

@TruffleLanguage.Registration(id = "aster", name = "Aster", version = "0.1")
public final class AsterLanguage extends TruffleLanguage<Object> {
  @Override protected Object createContext(Env env) { return new Object(); }
}

