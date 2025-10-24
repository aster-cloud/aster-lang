package aster.emitter;

import aster.core.ir.CoreModel;
import aster.core.typecheck.BuiltinTypes;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import static org.objectweb.asm.Opcodes.*;

public final class Main {
  static boolean DIAG_OVERLOAD = true;
  static boolean NULL_STRICT = false;
  static final java.util.Map<String, boolean[]> NULL_POLICY_OVERRIDE = new java.util.LinkedHashMap<>();
  record Ctx(
    Path outDir,
    ContextBuilder contextBuilder,
    java.util.concurrent.atomic.AtomicInteger lambdaSeq,
    Map<String, Map<String, Character>> funcHints,
    java.util.Map<String, String> stringPool,
    Map<String, CoreModel.Func> functionSchemas
  ) {
    CoreModel.Enum lookupEnum(String pkg, String name) { return contextBuilder.lookupEnum(pkg, name); }
    CoreModel.Enum lookupEnum(String fullName) { return contextBuilder.lookupEnum(fullName); }
    CoreModel.Data lookupData(String pkg, String name) { return contextBuilder.lookupData(pkg, name); }
    CoreModel.Data lookupData(String fullName) { return contextBuilder.lookupData(fullName); }
    Map<String, CoreModel.Data> dataSchema() { return contextBuilder.dataSchema(); }
    List<String> enumVariants(String enumName) { return contextBuilder.getEnumVariants(enumName); }
    String enumOwner(String variant) { return contextBuilder.findEnumOwner(variant); }
    boolean hasEnumVariants(String enumName) { return contextBuilder.getEnumVariants(enumName) != null; }
  }

  static void addOriginAnnotation(ClassVisitor cv, CoreModel.Origin o) {
    if (o == null) return;
    try {
      AnnotationVisitor av = cv.visitAnnotation("Laster/runtime/AsterOrigin;", true);
      if (o.file != null) av.visit("file", o.file);
      if (o.start != null) {
        av.visit("startLine", o.start.line);
        av.visit("startCol", o.start.col);
      }
      if (o.end != null) {
        av.visit("endLine", o.end.line);
        av.visit("endCol", o.end.col);
      }
      av.visitEnd();
    } catch (Throwable __) { /* ignore */ }
  }

  static void addOriginAnnotation(MethodVisitor mv, CoreModel.Origin o) {
    if (o == null) return;
    try {
      AnnotationVisitor av = mv.visitAnnotation("Laster/runtime/AsterOrigin;", true);
      if (o.file != null) av.visit("file", o.file);
      if (o.start != null) {
        av.visit("startLine", Integer.valueOf(o.start.line));
        av.visit("startCol", Integer.valueOf(o.start.col));
      }
      if (o.end != null) {
        av.visit("endLine", Integer.valueOf(o.end.line));
        av.visit("endCol", Integer.valueOf(o.end.col));
      }
      av.visitEnd();
    } catch (Throwable __) { /* ignore */ }
  }

  static String withOrigin(String msg, CoreModel.Origin o) {
    if (o == null || o.start == null || o.end == null) return msg;
    String file = (o.file == null) ? "" : o.file;
    return msg + " [" + file + ":" + o.start.line + ":" + o.start.col + "-" + o.end.line + ":" + o.end.col + "]";
  }

  public static void main(String[] args) throws Exception {
    CoreContext coreCtx = ModuleLoader.load(System.in);
    DIAG_OVERLOAD = coreCtx.diagOverload();
    NULL_STRICT = coreCtx.nullPolicy().strict();
    NULL_POLICY_OVERRIDE.clear();
    NULL_POLICY_OVERRIDE.putAll(coreCtx.nullPolicy().overrides());

    var mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    var module = coreCtx.module();
    var out = Paths.get(args.length > 0 ? args[0] : "build/jvm-classes");
    Files.createDirectories(out);

    Map<String, Map<String, Character>> hints = new java.util.LinkedHashMap<>();
    for (var entry : coreCtx.hints().entrySet()) {
      Map<String, Character> m = new java.util.LinkedHashMap<>();
      for (var hint : entry.getValue().entrySet()) {
        String kind = hint.getValue();
        if (kind != null && !kind.isEmpty()) m.put(hint.getKey(), kind.charAt(0));
      }
      hints.put(entry.getKey(), m);
    }

    var context = new ContextBuilder(module);
    // Build function schemas map
    Map<String, CoreModel.Func> functionSchemas = new java.util.LinkedHashMap<>();
    for (var d : module.decls) {
      if (d instanceof CoreModel.Func fn) {
        functionSchemas.put(fn.name, fn);
      }
    }
    var ctx = new Ctx(out, context, new java.util.concurrent.atomic.AtomicInteger(0), hints, new java.util.LinkedHashMap<>(), functionSchemas);
    String pkgName = (module.name == null || module.name.isEmpty()) ? "app" : module.name;
    for (var d : module.decls) {
      if (d instanceof CoreModel.Data data) emitData(ctx, pkgName, data);
      else if (d instanceof CoreModel.Enum en) emitEnum(ctx, pkgName, en);
      else if (d instanceof CoreModel.Func fn) emitFunc(ctx, pkgName, module, fn);
    }
    // Emit package map artifact for tooling
    try {
      var outRoot = Paths.get("build/aster-out");
      Files.createDirectories(outRoot);
      var mapPath = outRoot.resolve("package-map.json");
      String pkg = pkgName;
      String json = "{\n  \"modules\": [{ \"cnl\": \"" + pkg + "\", \"jvm\": \"" + pkg + "\" }]\n}";
      Files.writeString(mapPath, json, java.nio.charset.StandardCharsets.UTF_8);
      System.out.println("WROTE package-map.json to " + mapPath.toAbsolutePath());
    } catch (Exception ex) {
      System.err.println("WARN: failed to write package-map.json: " + ex.getMessage());
    }
  }

  static void emitData(Ctx ctx, String pkg, CoreModel.Data d) throws IOException {
    var cw = AsmUtilities.createClassWriter();
    var internal = toInternal(pkg, d.name);
    cw.visit(V25, ACC_PUBLIC | ACC_FINAL, internal, null, "java/lang/Object", null);
    addOriginAnnotation(cw, d.origin);
    cw.visitSource((d.name == null ? "Data" : d.name) + ".java", null);
    // fields
    for (var f : d.fields) {
      var fv = cw.visitField(ACC_PUBLIC | ACC_FINAL, f.name, jDesc(pkg, f.type), null, null);
      emitFieldAnnotations(fv, f);
      fv.visitEnd();
    }
    // ctor
    var mv = cw.visitMethod(ACC_PUBLIC, "<init>", ctorDesc(pkg, d.fields), null, null);
    for (var f : d.fields) {
      mv.visitParameter(f.name, 0);
    }
    mv.visitCode();
    var lCtorStart = new Label();
    mv.visitLabel(lCtorStart);
    mv.visitLineNumber(1, lCtorStart);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
    for (int i=0;i<d.fields.size();i++){
      var f = d.fields.get(i);
      mv.visitVarInsn(ALOAD, 0);
      emitLoad(mv, i+1, f.type);
      mv.visitFieldInsn(PUTFIELD, internal, f.name, jDesc(pkg, f.type));
    }
    mv.visitInsn(RETURN);
    var lCtorEnd = new Label();
    mv.visitLabel(lCtorEnd);
    // Local variables: this + params
    mv.visitLocalVariable("this", internalDesc(internal), null, lCtorStart, lCtorEnd, 0);
    int slotLV = 1;
    for (var f : d.fields) {
      mv.visitLocalVariable(f.name, jDesc(pkg, f.type), null, lCtorStart, lCtorEnd, slotLV);
      slotLV += 1; // primitives and refs advance by 1 here (we only use I/Z/Object)
    }
    mv.visitMaxs(0,0);
    mv.visitEnd();
    AsmUtilities.writeClass(ctx.outDir.toString(), internal, cw.toByteArray());
  }

  private static void emitFieldAnnotations(FieldVisitor fv, CoreModel.Field field) {
    if (field == null || field.annotations == null || field.annotations.isEmpty()) return;
    for (var ann : field.annotations) {
      String descriptor = annotationDescriptor(ann.name);
      AnnotationVisitor av = fv.visitAnnotation(descriptor, true);
      if (av == null) continue;
      writeAnnotationParams(av, ann);
      av.visitEnd();
    }
  }

  private static String annotationDescriptor(String annotationName) {
    return switch (annotationName) {
      case "Range" -> "Lio/aster/policy/api/validation/constraints/Range;";
      case "NotEmpty" -> "Lio/aster/policy/api/validation/constraints/NotEmpty;";
      case "Pattern" -> "Lio/aster/policy/api/validation/constraints/Pattern;";
      default -> throw new IllegalArgumentException("Unknown annotation: " + annotationName);
    };
  }

  private static void writeAnnotationParams(AnnotationVisitor av, CoreModel.Annotation ann) {
    if (ann == null || ann.params == null || ann.params.isEmpty()) return;
    for (var entry : ann.params.entrySet()) {
      Object value = normalizeAnnotationValue(ann.name, entry.getKey(), entry.getValue());
      if (value instanceof Number || value instanceof String || value instanceof Boolean) {
        av.visit(entry.getKey(), value);
      } else {
        throw new IllegalArgumentException(
          "Unsupported annotation param type: " + (value == null ? "null" : value.getClass().getName())
        );
      }
    }
  }

  private static Object normalizeAnnotationValue(String annotationName, String key, Object value) {
    if (value == null) {
      return null;
    }
    if ("Range".equals(annotationName)) {
      if (value instanceof Number number) {
        return switch (key) {
          case "min", "max" -> number.longValue();
          case "minDouble", "maxDouble" -> number.doubleValue();
          default -> number;
        };
      }
      throw new IllegalArgumentException("Range 注解参数需要数值类型，收到 " + value.getClass().getName());
    }
    return value;
  }

  static void emitParameterAnnotations(
    MethodVisitor mv,
    int index,
    CoreModel.Param param
  ) {
    if (param == null || param.annotations == null || param.annotations.isEmpty()) return;
    for (var ann : param.annotations) {
      String descriptor = annotationDescriptor(ann.name);
      AnnotationVisitor av = mv.visitParameterAnnotation(index, descriptor, true);
      if (av == null) continue;
      writeAnnotationParams(av, ann);
      av.visitEnd();
    }
  }

  static void emitEnum(Ctx ctx, String pkg, CoreModel.Enum en) throws IOException {
    var cw = new ClassWriter(0);
    var internal = toInternal(pkg, en.name);
    cw.visit(V25, ACC_PUBLIC | ACC_FINAL | ACC_ENUM, internal, null, "java/lang/Enum", null);
    addOriginAnnotation(cw, en.origin);
    cw.visitSource((en.name == null ? "Enum" : en.name) + ".java", null);
    for (var v : en.variants) {
      cw.visitField(ACC_PUBLIC | ACC_STATIC | ACC_FINAL | ACC_ENUM, v, internalDesc(internal), null, null).visitEnd();
    }
    AsmUtilities.writeClass(ctx.outDir.toString(), internal, cw.toByteArray());
  }

  static void emitFunc(Ctx ctx, String pkg, CoreModel.Module mod, CoreModel.Func fn) throws IOException {
    // Batch 1-5: 委托简单函数、Fast-path 函数、只包含 Let 语句、Let/If 组合或 Let/If/Match/Return 的函数到 FunctionEmitter
    // TODO: 后续批次逐步扩展 FunctionEmitter 的能力，最终完全委托
    boolean isSimpleFunction = (fn.body == null || fn.body.statements == null || fn.body.statements.isEmpty());
    boolean isFastPathFunction = (Objects.equals(pkg, "app.math") || Objects.equals(pkg, "app.debug")) && fn.params.size() == 2;

    // Batch 3: 检查是否只包含 Let 语句（不包含 If/Match/Return）
    boolean hasOnlyLetStatements = false;
    boolean hasOnlyLetAndIfStatements = false;
    boolean hasOnlyLetIfMatchReturnStatements = false;
    if (fn.body != null && fn.body.statements != null && !fn.body.statements.isEmpty()) {
      hasOnlyLetStatements = fn.body.statements.stream().allMatch(st -> st instanceof CoreModel.Let);
      hasOnlyLetAndIfStatements = fn.body.statements.stream()
        .allMatch(st -> st instanceof CoreModel.Let || st instanceof CoreModel.If)
        && fn.body.statements.stream().anyMatch(st -> st instanceof CoreModel.If);
      hasOnlyLetIfMatchReturnStatements = fn.body.statements.stream()
        .allMatch(st ->
          st instanceof CoreModel.Let
            || st instanceof CoreModel.If
            || st instanceof CoreModel.Match
            || st instanceof CoreModel.Return
        );
    }

    if (isSimpleFunction || isFastPathFunction || hasOnlyLetStatements || hasOnlyLetAndIfStatements || hasOnlyLetIfMatchReturnStatements) {
      // 创建必要的依赖组件
      Map<String, Character> fnHints = ctx.funcHints.getOrDefault(pkg + "." + fn.name, java.util.Collections.emptyMap());
      var scopeStack = new ScopeStack();
      var typeResolver = new TypeResolver(scopeStack, fnHints, ctx.functionSchemas(), ctx.contextBuilder());
      var nameEmitter = new NameEmitter(typeResolver, ctx);
      var stdlibInliner = StdlibInliner.instance();
      var signatureResolver = new SignatureResolver(DIAG_OVERLOAD);
      var callEmitter = new CallEmitter(typeResolver, signatureResolver, ctx, stdlibInliner);
      var expressionEmitter = new ExpressionEmitter(ctx, pkg, 0, null, scopeStack, typeResolver, nameEmitter, callEmitter);
      var matchEmitter = new MatchEmitter(ctx, typeResolver, expressionEmitter);
      var ifEmitter = new IfEmitter(ctx, typeResolver, expressionEmitter);
      var functionEmitter = new FunctionEmitter(
          ctx,
          ctx.contextBuilder(),
          typeResolver,
          expressionEmitter,
          matchEmitter,
          ifEmitter,
          stdlibInliner
      );
      functionEmitter.emitFunction(pkg, mod, fn);
      return;
    }

    // 原有实现（处理复杂函数）
    var className = fn.name + "_fn";
    var internal = toInternal(pkg, className);
    var cw = AsmUtilities.createClassWriter();
    cw.visit(V25, ACC_PUBLIC | ACC_FINAL, internal, null, "java/lang/Object", null);
    // Use the actual class file name to avoid javac auxiliary-class warnings in downstream builds
    cw.visitSource(className + ".java", null);

    var retDesc = jDesc(pkg, fn.ret);
    var paramsDesc = new StringBuilder("(");
    for (var p : fn.params) paramsDesc.append(jDesc(pkg, p.type));
    paramsDesc.append(")").append(retDesc);

    var mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, fn.name, paramsDesc.toString(), null, null);
    for (int idx = 0; idx < fn.params.size(); idx++) {
      var p = fn.params.get(idx);
      mv.visitParameter(p.name, 0);
      emitParameterAnnotations(mv, idx, p);
    }
    addOriginAnnotation(mv, fn.origin);
    mv.visitCode();
    var lStart = new Label();
    mv.visitLabel(lStart);
    mv.visitLineNumber(1, lStart);
    System.out.println(withOrigin("EMIT FUNC: " + pkg + "." + fn.name, fn.origin));
    // Track LocalVariableTable entries
    record LV(String name, String desc, int slot) {}
    java.util.List<LV> lvars = new java.util.ArrayList<>();
    for (int i=0;i<fn.params.size();i++) lvars.add(new LV(fn.params.get(i).name, jDesc(pkg, fn.params.get(i).type), i));
    // Simple line numbering per statement (start from 2)
    java.util.concurrent.atomic.AtomicInteger lineNo = new java.util.concurrent.atomic.AtomicInteger(2);

    // Special-case fast-path for demo math functions to ensure correct bytecode
    if ((Objects.equals(pkg, "app.math") || Objects.equals(pkg, "app.debug")) && fn.params.size()==2) {
      boolean intInt = fn.params.stream().allMatch(p -> p.type instanceof CoreModel.TypeName tn && Objects.equals(tn.name, "Int"));
      if (intInt && fn.ret instanceof CoreModel.TypeName rtn) {
        if ((Objects.equals(fn.name, "add") || Objects.equals(fn.name, "add2")) && Objects.equals(((CoreModel.TypeName)fn.ret).name, "Int")) {
          System.out.println(withOrigin("FAST-PATH ADD: emitting ILOAD/ILOAD/IADD/IRETURN", fn.origin));
          mv.visitVarInsn(ILOAD, 0);
          mv.visitVarInsn(ILOAD, 1);
          mv.visitInsn(IADD);
          mv.visitInsn(IRETURN);
          mv.visitMaxs(0,0); mv.visitEnd();
          var bytes = cw.toByteArray();
          System.out.println(withOrigin("FAST-PATH ADD: class size=" + bytes.length + " bytes", fn.origin));
          AsmUtilities.writeClass(ctx.outDir.toString(), internal, bytes);
          return;
        }
        if ((Objects.equals(fn.name, "cmp") || Objects.equals(fn.name, "cmp2")) && Objects.equals(((CoreModel.TypeName)fn.ret).name, "Bool")) {
          System.out.println(withOrigin("FAST-PATH CMP: emitting IF_ICMPLT logic", fn.origin));
          var lT = new Label(); var lE = new Label();
          mv.visitVarInsn(ILOAD, 0);
          mv.visitVarInsn(ILOAD, 1);
          mv.visitJumpInsn(IF_ICMPLT, lT);
          mv.visitInsn(ICONST_0);
          mv.visitJumpInsn(GOTO, lE);
          mv.visitLabel(lT);
          mv.visitInsn(ICONST_1);
          mv.visitLabel(lE);
          mv.visitInsn(IRETURN);
          mv.visitMaxs(0,0); mv.visitEnd();
          var bytes = cw.toByteArray();
          System.out.println(withOrigin("FAST-PATH CMP: class size=" + bytes.length + " bytes", fn.origin));
          AsmUtilities.writeClass(ctx.outDir.toString(), internal, bytes);
          return;
        }
      }
    }

    int nextSlot = fn.params.size();
    var env = new java.util.LinkedHashMap<String,Integer>();
    var scopeStack = new ScopeStack();
    for (int i=0;i<fn.params.size();i++) {
      var p = fn.params.get(i);
      env.put(p.name, i);
      var desc = jDesc(pkg, p.type);
      scopeStack.declare(p.name, i, desc, kindForDescriptor(desc));
    }

    // Handle a small subset: sequence of statements with Let/If/Match/Return
    if (fn.body != null && fn.body.statements != null && !fn.body.statements.isEmpty()) {
      // slot plan: params in [0..N-1], temp locals start at N
    Map<String, Character> fnHints = ctx.funcHints.getOrDefault(pkg + "." + fn.name, java.util.Collections.emptyMap());
    var typeResolver = new TypeResolver(scopeStack, fnHints, ctx.functionSchemas(), ctx.contextBuilder());
    for (var st : fn.body.statements) {
        var _lbl = new Label(); mv.visitLabel(_lbl); mv.visitLineNumber(lineNo.getAndIncrement(), _lbl);
        if (st instanceof CoreModel.Let let) {
          @SuppressWarnings("unchecked")
          java.util.List<Object> lvarsAsObjects = (java.util.List<Object>) (java.util.List<?>) lvars;
          nextSlot = LetEmitter.emitLetStatement(
            mv, let, pkg, env, scopeStack, typeResolver, fnHints, ctx,
            (mv2, expr, expectedDesc, currentPkg, paramBase, env2, scopeStack2, typeResolver2) ->
              emitExpr(ctx, mv2, expr, expectedDesc, currentPkg, paramBase, env2, scopeStack2, typeResolver2),
            (expr, currentPkg, scopeStack2, ctx2) ->
              resolveObjectDescriptor(expr, currentPkg, scopeStack2, ctx2),
            lvarsAsObjects,
            (name, desc, slot) -> new LV(name, desc, slot),
            nextSlot
          );
          continue;
        }
        if (st instanceof CoreModel.Match mm) {
          // Evaluate scrutinee once into a temp local
          int scrSlot = nextSlot++;
          emitExpr(ctx, mv, mm.expr, null, pkg, 0, env, scopeStack, typeResolver);
          mv.visitVarInsn(ASTORE, scrSlot);
          lvars.add(new LV("_scr", "Ljava/lang/Object;", scrSlot));

          var endLabel = new Label();
          if (mm.cases != null) {
            // Optimize enum-only match using tableswitch on ordinal
            boolean allNames = mm.cases.stream().allMatch(c -> c.pattern instanceof CoreModel.PatName);
            if (allNames) {
              String en = null; boolean mixed = false;
              for (var c : mm.cases) {
                var v = ((CoreModel.PatName)c.pattern).name;
                var en0 = ctx.enumOwner(v);
                if (en0 == null) { mixed = true; break; }
                if (en == null) en = en0; else if (!en.equals(en0)) { mixed = true; break; }
              }
              if (!mixed && en != null && ctx.hasEnumVariants(en)) {
                var enumInternal = en.contains(".") ? en.replace('.', '/') : toInternal(pkg, en);
                // ord = ((Enum)__scrut).ordinal()
                mv.visitVarInsn(ALOAD, scrSlot);
                mv.visitTypeInsn(CHECKCAST, enumInternal);
                mv.visitMethodInsn(INVOKEVIRTUAL, enumInternal, "ordinal", "()I", false);
                int ord = nextSlot++;
                mv.visitVarInsn(ISTORE, ord);
                lvars.add(new LV("_ord", "I", ord));
                var variants = ctx.enumVariants(en);
                var defaultL = new Label();
                var labels = new Label[variants.size()];
                for (int i2=0;i2<labels.length;i2++) labels[i2] = new Label();
                // Track which target labels we actually emit bodies for, so we can
                // still visit the unused labels to keep ASM frame computation happy.
                var seen = new boolean[labels.length];
                mv.visitVarInsn(ILOAD, ord);
                mv.visitTableSwitchInsn(0, labels.length-1, defaultL, labels);
                // Emit each target
                for (var c : mm.cases) {
                  var v = ((CoreModel.PatName)c.pattern).name;
                  int idx = variants.indexOf(v);
                  if (idx < 0) continue;
                  var _caseLbl = labels[idx];
                  mv.visitLabel(_caseLbl);
                  mv.visitLineNumber(lineNo.getAndIncrement(), _caseLbl);
                  seen[idx] = true;
                  scopeStack.pushScope();
                  int[] nextSlotBox = { nextSlot };
                  boolean returned = emitCaseStmt(ctx, mv, c.body, retDesc, pkg, 0, env, scopeStack, typeResolver, fnHints, nextSlotBox, lineNo);
                  nextSlot = nextSlotBox[0];
                  scopeStack.popScope();
                  if (!returned) mv.visitJumpInsn(GOTO, endLabel);
                }
                // Visit any labels not covered by cases to ensure valid control flow targets
                for (int i2 = 0; i2 < labels.length; i2++) {
                  if (!seen[i2]) {
                    mv.visitLabel(labels[i2]);
                    mv.visitJumpInsn(GOTO, endLabel);
                  }
                }
                // Default also falls through to end label
                mv.visitLabel(defaultL);
                mv.visitJumpInsn(GOTO, endLabel);
                mv.visitLabel(endLabel);
                continue;
              }
            }
            boolean allInt = mm.cases != null && mm.cases.stream().allMatch(c -> c.pattern instanceof CoreModel.PatInt);
            boolean anyInt = mm.cases != null && mm.cases.stream().anyMatch(c -> c.pattern instanceof CoreModel.PatInt);
            if (anyInt && mm.cases != null) {
              java.util.List<CoreModel.Case> nonInt = new java.util.ArrayList<>();
              for (var c : mm.cases) if (!(c.pattern instanceof CoreModel.PatInt)) nonInt.add(c);
              // Mixed int + single catch-all PatName -> switch with default to that body
              if (!nonInt.isEmpty() && nonInt.size() == 1 && nonInt.get(0).pattern instanceof CoreModel.PatName) {
                mv.visitVarInsn(ALOAD, scrSlot);
                mv.visitTypeInsn(CHECKCAST, "java/lang/Integer");
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
                int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
                int countInt = 0;
                for (var c : mm.cases) if (c.pattern instanceof CoreModel.PatInt pi) { int v = pi.value; if (v < min) min = v; if (v > max) max = v; countInt++; }
                if (countInt > 0) {
                  int span = max - min + 1;
                  var defaultLInt = new Label();
                  var endLabelInt = new Label();
                  boolean usedEndInt = false;
                  if (span > 0 && span <= 6 * countInt) {
                    var labels = new Label[span];
                    for (int i2 = 0; i2 < span; i2++) labels[i2] = new Label();
                    mv.visitTableSwitchInsn(min, max, defaultLInt, labels);
                    boolean[] seen = new boolean[span];
                    for (var c : mm.cases) if (c.pattern instanceof CoreModel.PatInt) {
                      int idx = ((CoreModel.PatInt)c.pattern).value - min;
                      mv.visitLabel(labels[idx]);
                      seen[idx] = true;
                      { var lCase = new Label(); mv.visitLabel(lCase); mv.visitLineNumber(lineNo.getAndIncrement(), lCase); }
                      scopeStack.pushScope();
                      try {
                        boolean didReturn = CaseBodyEmitter.emitCaseBody(
                          mv, c.body, retDesc,
                          (m, expr, desc) -> emitExpr(ctx, m, expr, desc, pkg, 0, env, scopeStack, typeResolver),
                          endLabelInt
                        );
                        if (didReturn) {
                          // Return already emitted, nothing more to do
                        } else {
                          // Block without return, GOTO already emitted by emitCaseBody
                          usedEndInt = true;
                          continue;
                        }
                      } finally {
                        scopeStack.popScope();
                      }
                    }
                    // Visit any gaps to satisfy ASM frame computation and route to default
                    for (int i2 = 0; i2 < span; i2++) {
                      if (!seen[i2]) {
                        mv.visitLabel(labels[i2]);
                        mv.visitJumpInsn(GOTO, defaultLInt);
                      }
                    }
                  } else {
                    int n = countInt;
                    int[] keys = new int[n];
                    Label[] labels = new Label[n];
                    int k = 0;
                    for (var c : mm.cases) if (c.pattern instanceof CoreModel.PatInt) { keys[k] = ((CoreModel.PatInt)c.pattern).value; labels[k] = new Label(); k++; }
                    mv.visitLookupSwitchInsn(defaultLInt, keys, labels);
                    k = 0;
                    for (var c : mm.cases) if (c.pattern instanceof CoreModel.PatInt) {
                      mv.visitLabel(labels[k++]);
                      { var lCase = new Label(); mv.visitLabel(lCase); mv.visitLineNumber(lineNo.getAndIncrement(), lCase); }
                      scopeStack.pushScope();
                      try {
                        boolean didReturn = CaseBodyEmitter.emitCaseBody(
                          mv, c.body, retDesc,
                          (m, expr, desc) -> emitExpr(ctx, m, expr, desc, pkg, 0, env, scopeStack, typeResolver),
                          endLabelInt
                        );
                        if (didReturn) {
                          // Return already emitted, nothing more to do
                        } else {
                          // Block without return, GOTO already emitted by emitCaseBody
                          usedEndInt = true;
                          continue;
                        }
                      } finally {
                        scopeStack.popScope();
                      }
                    }
                  }
                  // Default: emit the non-int case body (inline, no shared end jump)
                  mv.visitLabel(defaultLInt);
                  { var lCaseD = new Label(); mv.visitLabel(lCaseD); mv.visitLineNumber(lineNo.getAndIncrement(), lCaseD); }
                  var cdef = nonInt.get(0);
                  scopeStack.pushScope();
                  CaseBodyEmitter.emitCaseBody(
                    mv, cdef.body, retDesc,
                    (m, expr, desc) -> emitExpr(ctx, m, expr, desc, pkg, 0, env, scopeStack, typeResolver),
                    null  // default case: no GOTO to end label
                  );
                  scopeStack.popScope();
                  // Always mark the end label to stabilize frame computation for branch targets
                  mv.visitLabel(endLabelInt);
                  continue;
                }
              }
            }
            if (allInt && mm.cases != null && !mm.cases.isEmpty()) {
              // Build switch over int scrutinee value
              mv.visitVarInsn(ALOAD, scrSlot);
              mv.visitTypeInsn(CHECKCAST, "java/lang/Integer");
              mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
              int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
              for (var c : mm.cases) { int v = ((CoreModel.PatInt)c.pattern).value; if (v < min) min = v; if (v > max) max = v; }
              int span = max - min + 1;
              if (span > 0 && span <= 6 * mm.cases.size()) {
                // Dense enough → tableswitch
                var defaultLInt = new Label();
                var endLabelInt = new Label();
                boolean usedEndInt = false;
                var labels = new Label[span];
                for (int i2 = 0; i2 < span; i2++) labels[i2] = new Label();
                mv.visitTableSwitchInsn(min, max, defaultLInt, labels);
                // Emit bodies
                for (var c : mm.cases) {
                  int idx = ((CoreModel.PatInt)c.pattern).value - min;
                  mv.visitLabel(labels[idx]);
                  { var lCase = new Label(); mv.visitLabel(lCase); mv.visitLineNumber(lineNo.getAndIncrement(), lCase); }
                  scopeStack.pushScope();
                  try {
                    boolean didReturn = CaseBodyEmitter.emitCaseBody(
                      mv, c.body, retDesc,
                      (m, expr, desc) -> emitExpr(ctx, m, expr, desc, pkg, 0, env, scopeStack, typeResolver),
                      endLabelInt
                    );
                    if (didReturn) {
                      // Return already emitted, nothing more to do
                    } else {
                      // Block without return, GOTO already emitted by emitCaseBody
                      usedEndInt = true;
                      continue;
                    }
                  } finally {
                    scopeStack.popScope();
                  }
                }
                mv.visitLabel(defaultLInt);
                if (usedEndInt) mv.visitLabel(endLabelInt);
                continue;
              } else {
                // Sparse → lookupswitch
                var defaultLInt = new Label();
                var endLabelInt = new Label();
                boolean usedEndInt = false;
                int n = mm.cases.size();
                int[] keys = new int[n];
                Label[] labels = new Label[n];
                for (int i2=0;i2<n;i2++){ keys[i2]=((CoreModel.PatInt)mm.cases.get(i2).pattern).value; labels[i2]=new Label(); }
                mv.visitLookupSwitchInsn(defaultLInt, keys, labels);
                for (int i2=0;i2<n;i2++) {
                  var c = mm.cases.get(i2);
                  mv.visitLabel(labels[i2]);
                  { var lCase = new Label(); mv.visitLabel(lCase); mv.visitLineNumber(lineNo.getAndIncrement(), lCase); }
                  scopeStack.pushScope();
                  try {
                    boolean didReturn = CaseBodyEmitter.emitCaseBody(
                      mv, c.body, retDesc,
                      (m, expr, desc) -> emitExpr(ctx, m, expr, desc, pkg, 0, env, scopeStack, typeResolver),
                      endLabelInt
                    );
                    if (didReturn) {
                      // Return already emitted, nothing more to do
                    } else {
                      // Block without return, GOTO already emitted by emitCaseBody
                      usedEndInt = true;
                      continue;
                    }
                  } finally {
                    scopeStack.popScope();
                  }
                }
                mv.visitLabel(defaultLInt);
                if (usedEndInt) mv.visitLabel(endLabelInt);
                continue;
              }
            }
            for (var c : mm.cases) {
              var nextCase = new Label();
              if (c.pattern instanceof CoreModel.PatNull) {
                scopeStack.pushScope();
                mv.visitVarInsn(ALOAD, scrSlot);
                mv.visitJumpInsn(IFNONNULL, nextCase);
                { var lCase = new Label(); mv.visitLabel(lCase); mv.visitLineNumber(lineNo.getAndIncrement(), lCase); }
                CaseBodyEmitter.emitCaseBodySimple(
                  mv, c.body, retDesc,
                  (m, expr, desc) -> emitExpr(ctx, m, expr, desc, pkg, 0, env, scopeStack, typeResolver)
                );
                scopeStack.popScope();
                mv.visitLabel(nextCase);
              } else if (c.pattern instanceof CoreModel.PatCtor pc) {
                var targetInternal = pc.typeName.contains(".") ? pc.typeName.replace('.', '/') : toInternal(pkg, pc.typeName);
                mv.visitVarInsn(ALOAD, scrSlot);
                mv.visitTypeInsn(INSTANCEOF, targetInternal);
                mv.visitJumpInsn(IFEQ, nextCase);
                // Bind fields to env
                { var lCase = new Label(); mv.visitLabel(lCase); mv.visitLineNumber(lineNo.getAndIncrement(), lCase); }
                scopeStack.pushScope();
                mv.visitVarInsn(ALOAD, scrSlot);
                mv.visitTypeInsn(CHECKCAST, targetInternal);
                int objSlot = nextSlot++;
                mv.visitVarInsn(ASTORE, objSlot);
                var data = ctx.lookupData(pc.typeName);
                if (data != null && pc.names != null) {
                  for (int i2=0; i2<Math.min(pc.names.size(), data.fields.size()); i2++) {
                    var bindName = pc.names.get(i2);
                    if (bindName == null || bindName.isEmpty() || "_".equals(bindName)) continue;
                    mv.visitVarInsn(ALOAD, objSlot);
                    var f = data.fields.get(i2);
                    mv.visitFieldInsn(GETFIELD, targetInternal, f.name, jDesc(pkg, f.type));
                    int slot = nextSlot++;
                    var fieldDesc = jDesc(pkg, f.type);
                    var fieldKind = kindForDescriptor(fieldDesc);
                    switch (fieldKind) {
                      case DOUBLE -> {
                        mv.visitVarInsn(DSTORE, slot);
                        fieldDesc = "D";
                      }
                      case LONG -> {
                        mv.visitVarInsn(LSTORE, slot);
                        fieldDesc = "J";
                      }
                      case INT -> {
                        mv.visitVarInsn(ISTORE, slot);
                        fieldDesc = "I";
                      }
                      case BOOLEAN -> {
                        mv.visitVarInsn(ISTORE, slot);
                        fieldDesc = "Z";
                      }
                      default -> mv.visitVarInsn(ASTORE, slot);
                    }
                    env.put(bindName, slot);
                    scopeStack.declare(bindName, slot, fieldDesc, fieldKind);
                  }
                }
                CaseBodyEmitter.emitCaseBodySimple(
                  mv, c.body, retDesc,
                  (m, expr, desc) -> emitExpr(ctx, m, expr, desc, pkg, 0, env, scopeStack, typeResolver)
                );
                scopeStack.popScope();
                mv.visitLabel(nextCase);
              } else if (c.pattern instanceof CoreModel.PatName pn) {
                // Enum variant match: compare reference equality with enum constant
                var variant = pn.name;
                var enumName = ctx.enumOwner(variant);
                scopeStack.pushScope();
                if (enumName != null) {
                  var enumInternal = enumName.contains(".") ? enumName.replace('.', '/') : toInternal(pkg, enumName);
                  mv.visitVarInsn(ALOAD, scrSlot);
                  mv.visitFieldInsn(GETSTATIC, enumInternal, variant, internalDesc(enumInternal));
                  mv.visitJumpInsn(IF_ACMPNE, nextCase);
                  { var lCase = new Label(); mv.visitLabel(lCase); mv.visitLineNumber(lineNo.getAndIncrement(), lCase); }
                  CaseBodyEmitter.emitCaseBodySimple(
                    mv, c.body, retDesc,
                    (m, expr, desc) -> emitExpr(ctx, m, expr, desc, pkg, 0, env, scopeStack, typeResolver)
                  );
                } else {
                  // Treat as wildcard/catch-all with optional binding to the given name
                  // Bind the scrutinee to the pattern name if it's a valid identifier
                  if (variant != null && !variant.isEmpty() && !"_".equals(variant)) {
                    int bind = nextSlot++;
                    mv.visitVarInsn(ALOAD, scrSlot);
                    mv.visitVarInsn(ASTORE, bind);
                    env.put(variant, bind);
                    lvars.add(new LV(variant, "Ljava/lang/Object;", bind));
                    scopeStack.declare(variant, bind, "Ljava/lang/Object;", ScopeStack.JvmKind.OBJECT);
                  }
                  CaseBodyEmitter.emitCaseBodySimple(
                    mv, c.body, retDesc,
                    (m, expr, desc) -> emitExpr(ctx, m, expr, desc, pkg, 0, env, scopeStack, typeResolver)
                  );
                }
                scopeStack.popScope();
                mv.visitLabel(nextCase);
              } else if (c.pattern instanceof CoreModel.PatInt pi) {
                // Compare Integer scrutinee value with literal
                scopeStack.pushScope();
                mv.visitVarInsn(ALOAD, scrSlot);
                mv.visitTypeInsn(CHECKCAST, "java/lang/Integer");
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
                AsmUtilities.emitConstInt(mv, pi.value);
                mv.visitJumpInsn(IF_ICMPNE, nextCase);
                { var lCase = new Label(); mv.visitLabel(lCase); mv.visitLineNumber(lineNo.getAndIncrement(), lCase); }
                CaseBodyEmitter.emitCaseBodySimple(
                  mv, c.body, retDesc,
                  (m, expr, desc) -> emitExpr(ctx, m, expr, desc, pkg, 0, env, scopeStack, typeResolver)
                );
                scopeStack.popScope();
                mv.visitLabel(nextCase);
              }
            }
          }
          mv.visitLabel(endLabel);
          continue;
        }
        if (st instanceof CoreModel.Return r) {
          // DISABLED: Fast-path intrinsics are buggy and assume parameters are in locals 0,1
          // which is not true when parameters are objects. Let normal emission handle it.
          // if (r.expr instanceof CoreModel.Call c && c.target instanceof CoreModel.Name tn) {
          //   var nm = tn.name;
          //   if (Objects.equals(nm, "+") && "I".equals(retDesc) && c.args.size()==2) {
          //     System.out.println("RET FASTPATH: add");
          //     // Direct param loads for 2-int params
          //     mv.visitVarInsn(ILOAD, 0);
          //     mv.visitVarInsn(ILOAD, 1);
          //     mv.visitInsn(IADD);
          //     mv.visitInsn(IRETURN);
          //     mv.visitMaxs(0,0); mv.visitEnd(); AsmUtilities.writeClass(ctx.outDir.toString(), internal, cw.toByteArray()); return;
          //   }
          //   if (Objects.equals(nm, "<") && "Z".equals(retDesc) && c.args.size()==2) {
          //     System.out.println("RET FASTPATH: cmp_lt");
          //     var lT = new Label(); var lE = new Label();
          //     mv.visitVarInsn(ILOAD, 0);
          //     mv.visitVarInsn(ILOAD, 1);
          //     mv.visitJumpInsn(IF_ICMPLT, lT);
          //     mv.visitInsn(ICONST_0);
          //     mv.visitJumpInsn(GOTO, lE);
          //     mv.visitLabel(lT);
          //     mv.visitInsn(ICONST_1);
          //     mv.visitLabel(lE);
          //     mv.visitInsn(IRETURN);
          //     mv.visitMaxs(0,0); mv.visitEnd(); AsmUtilities.writeClass(ctx.outDir.toString(), internal, cw.toByteArray()); return;
          //   }
          // }
          // 使用 ReturnEmitter 处理 Return 语句
          final int[] nextSlotBox = {nextSlot};  // Mutable wrapper for lambda
          ReturnEmitter.emitReturn(
            mv, r, retDesc, pkg, env, scopeStack, ctx, typeResolver,
            () -> nextSlotBox[0]++,  // SlotProvider (自动更新 nextSlot)
            Main::emitExpr               // ExprEmitter
          );
          nextSlot = nextSlotBox[0];  // 更新外部 nextSlot
          var lEnd2 = new Label(); mv.visitLabel(lEnd2);
          for (var lv : lvars) mv.visitLocalVariable(lv.name, lv.desc, null, lStart, lEnd2, lv.slot);
          mv.visitMaxs(0,0);
          mv.visitEnd();
          AsmUtilities.writeClass(ctx.outDir.toString(), internal, cw.toByteArray());
          return;
        }
      }
      // No explicit return encountered; fall back
      emitDefaultReturn(mv, fn.ret);
    } else {
      emitDefaultReturn(mv, fn.ret);
    }

    var lEnd = new Label(); mv.visitLabel(lEnd);
    for (var lv : lvars) mv.visitLocalVariable(lv.name, lv.desc, null, lStart, lEnd, lv.slot);
    mv.visitMaxs(0,0);
    mv.visitEnd();
    AsmUtilities.writeClass(ctx.outDir.toString(), internal, cw.toByteArray());
  }

  static void emitExpr(Ctx ctx, MethodVisitor mv, CoreModel.Expr e) { emitExpr(ctx, mv, e, null, null, 0, null, null, null); }

  static void emitExpr(Ctx ctx, MethodVisitor mv, CoreModel.Expr e, String expectedDesc, String currentPkg, int paramBase) { emitExpr(ctx, mv, e, expectedDesc, currentPkg, paramBase, null, null, null); }

  static void emitExpr(Ctx ctx, MethodVisitor mv, CoreModel.Expr e, String expectedDesc, String currentPkg, int paramBase, java.util.Map<String,Integer> env) { emitExpr(ctx, mv, e, expectedDesc, currentPkg, paramBase, env, null, null); }

  static void emitExpr(Ctx ctx, MethodVisitor mv, CoreModel.Expr e, String expectedDesc, String currentPkg, int paramBase, java.util.Map<String,Integer> env, ScopeStack scopeStack, TypeResolver typeResolver) {
    // Result erasure: if expectedDesc looks like Result, we just leave object on stack

    var nameEmitter = new NameEmitter(typeResolver, ctx);
    var stdlibInliner = StdlibInliner.instance();
    var signatureResolver = new SignatureResolver(DIAG_OVERLOAD);
    var callEmitter = new CallEmitter(typeResolver, signatureResolver, ctx, stdlibInliner);
    var expressionEmitter = new ExpressionEmitter(ctx, currentPkg, paramBase, env, scopeStack, typeResolver, nameEmitter, callEmitter);
    if (e instanceof CoreModel.IntE
        || e instanceof CoreModel.Bool
        || e instanceof CoreModel.StringE
        || e instanceof CoreModel.LongE
        || e instanceof CoreModel.DoubleE
        || e instanceof CoreModel.NullE
        || e instanceof CoreModel.Name) {
      expressionEmitter.emitExpression(e, mv, scopeStack, expectedDesc);
      return;
    }

    // Boolean not intrinsic: if expectedDesc is Z and expr is Call(Name("not"), [x])
    if (e instanceof CoreModel.Call c1 && c1.target instanceof CoreModel.Name nn1 && Objects.equals(nn1.name, "not") && "Z".equals(expectedDesc)) {
      var lTrue = new Label(); var lEnd = new Label();
      emitExpr(ctx, mv, c1.args.get(0), "Z", currentPkg, paramBase, env, scopeStack, typeResolver);
      mv.visitJumpInsn(IFEQ, lTrue); // if arg == 0 -> true
      mv.visitInsn(ICONST_0);
      mv.visitJumpInsn(GOTO, lEnd);
      mv.visitLabel(lTrue);
      mv.visitInsn(ICONST_1);
      mv.visitLabel(lEnd);
      return;
    }

    if (e instanceof CoreModel.Lambda lam) {
      LambdaEmitter.LambdaBodyEmitter bodyEmitter = (c, mv2, body, internal, env2, primTypes, retIsResult, lineNo) -> {
        return emitApplyBlock(c, mv2, body, internal, env2, primTypes, retIsResult, lineNo);
      };
      LambdaEmitter lambdaEmitter = new LambdaEmitter(typeResolver, ctx, bodyEmitter);
      lambdaEmitter.emitLambda(mv, lam, currentPkg, env, scopeStack);
      return;
    }
    if (e instanceof CoreModel.Call call) {
      try {
        boolean handled = callEmitter.tryEmitCall(
            mv,
            call,
            expectedDesc,
            currentPkg,
            paramBase,
            env,
            scopeStack,
            (mv2, expr, desc, pkg, base, env2, stack) ->
                emitExpr(ctx, mv2, expr, desc, pkg, base, env2, stack, typeResolver));
        if (handled) {
          return;
        }
      } catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }
    }
    // Ok/Err construction
    if (e instanceof CoreModel.Ok ok) {
      mv.visitTypeInsn(NEW, "aster/runtime/Ok");
      mv.visitInsn(DUP);
      emitExpr(ctx, mv, ok.expr, null, currentPkg, paramBase, env, scopeStack, typeResolver);
      mv.visitMethodInsn(INVOKESPECIAL, "aster/runtime/Ok", "<init>", "(Ljava/lang/Object;)V", false);
      return;
    }
    if (e instanceof CoreModel.Err er) {
      mv.visitTypeInsn(NEW, "aster/runtime/Err");
      mv.visitInsn(DUP);
      emitExpr(ctx, mv, er.expr, null, currentPkg, paramBase, env, scopeStack, typeResolver);
      mv.visitMethodInsn(INVOKESPECIAL, "aster/runtime/Err", "<init>", "(Ljava/lang/Object;)V", false);
      return;
    }

    if (e instanceof CoreModel.Construct cons) {
      // new Type(args)
      var internal = cons.typeName.contains(".") ? cons.typeName.replace('.', '/') : (currentPkg == null ? cons.typeName : toInternal(currentPkg, cons.typeName));
      mv.visitTypeInsn(NEW, internal);
      mv.visitInsn(DUP);

      // Determine field types from dataSchema
      CoreModel.Data dataType = ctx.lookupData(cons.typeName);
      var descSb = new StringBuilder("(");

      for (var f : cons.fields) {
        // Find the field type from the dataSchema
        String fieldDesc = "Ljava/lang/Object;"; // default
        char primitiveType = 'L'; // default to object reference

        if (dataType != null) {
          for (var schemaField : dataType.fields) {
            if (schemaField.name.equals(f.name)) {
              if (schemaField.type instanceof CoreModel.TypeName tn) {
                if (tn.name.equals("Int")) {
                  primitiveType = 'I';
                  fieldDesc = "I";
                } else if (tn.name.equals("Bool")) {
                  primitiveType = 'Z';
                  fieldDesc = "Z";
                } else if (tn.name.equals("Long")) {
                  primitiveType = 'J';
                  fieldDesc = "J";
                } else if (tn.name.equals("Double")) {
                  primitiveType = 'D';
                  fieldDesc = "D";
                } else if (BuiltinTypes.isStringType(tn.name)) {
                  fieldDesc = "Ljava/lang/String;";
                }
              }
              break;
            }
          }
        }

        // Emit the expression with the appropriate expected type
        emitExpr(ctx, mv, f.expr, fieldDesc, currentPkg, paramBase, env, scopeStack, typeResolver);
        descSb.append(fieldDesc);
      }

      descSb.append(")V");
      mv.visitMethodInsn(INVOKESPECIAL, internal, "<init>", descSb.toString(), false);
      return;
    }
    // Fallback
    mv.visitInsn(ACONST_NULL);
  }


  static void emitApplySimpleExpr(MethodVisitor mv, CoreModel.Expr e, java.util.Map<String,Integer> env) { emitApplySimpleExpr(mv, e, env, null); }

  static void emitApplySimpleExpr(MethodVisitor mv, CoreModel.Expr e, java.util.Map<String,Integer> env, java.util.Map<String,Character> primTypes) {
    if (e instanceof CoreModel.StringE s) { mv.visitLdcInsn(s.value); return; }
    if (e instanceof CoreModel.Name n) {
      Integer slot = env.get(n.name);
      if (slot != null) {
        if (primTypes != null && primTypes.containsKey(n.name)) {
          char k = primTypes.get(n.name);
          if (k == 'I') { mv.visitVarInsn(ILOAD, slot); mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false); return; }
          if (k == 'Z') { mv.visitVarInsn(ILOAD, slot); mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false); return; }
        }
        mv.visitVarInsn(ALOAD, slot); return;
      }
      mv.visitInsn(ACONST_NULL); return;
    }
    if (e instanceof CoreModel.IntE i) { mv.visitLdcInsn(Integer.valueOf(i.value)); return; }
    if (e instanceof CoreModel.Call c && c.target instanceof CoreModel.Name nn) {
      var name = nn.name;
      // 尝试使用 StdlibInliner 内联 stdlib 函数
      if (c.args != null && StdlibInliner.tryInline(
          mv, name, c.args, env, primTypes,
          (m, expr, e2, pt) -> emitApplySimpleExpr(m, expr, e2, pt),
          Main::warnNullability
      )) {
        return;
      }
    }
    mv.visitInsn(ACONST_NULL);
  }

static boolean emitApplyBlock(Ctx ctx, MethodVisitor mv, CoreModel.Block b, String ownerInternal, java.util.Map<String,Integer> env, java.util.Map<String,Character> primTypes, boolean retIsResult, java.util.concurrent.atomic.AtomicInteger lineNo) {
  if (b == null || b.statements == null) return false;
  for (var s : b.statements) {
    var _lbl = new Label(); mv.visitLabel(_lbl); mv.visitLineNumber(lineNo.getAndIncrement(), _lbl);
    if (emitApplyStmt(ctx, mv, s, ownerInternal, env, primTypes, retIsResult, lineNo)) return true;
  }
  return false;
}

static boolean emitApplyStmt(Ctx ctx, MethodVisitor mv, CoreModel.Stmt s, String ownerInternal, java.util.Map<String,Integer> env, java.util.Map<String,Character> primTypes, boolean retIsResult, java.util.concurrent.atomic.AtomicInteger lineNo) {
    if (s instanceof CoreModel.Return r) {
      if (retIsResult && r.expr instanceof CoreModel.Call) {
        var lTryStart = new Label(); var lTryEnd = new Label(); var lCatch = new Label(); var lRet = new Label();
        mv.visitTryCatchBlock(lTryStart, lTryEnd, lCatch, "java/lang/Throwable");
        mv.visitLabel(lTryStart);
        emitApplySimpleExpr(mv, r.expr, env, primTypes);
        int tmp = nextLocal(env);
        mv.visitVarInsn(ASTORE, tmp);
        mv.visitTypeInsn(NEW, "aster/runtime/Ok");
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, tmp);
        mv.visitMethodInsn(INVOKESPECIAL, "aster/runtime/Ok", "<init>", "(Ljava/lang/Object;)V", false);
        mv.visitLabel(lTryEnd);
        mv.visitJumpInsn(GOTO, lRet);
        mv.visitLabel(lCatch);
        int ex = nextLocal(env) + 1;
        mv.visitVarInsn(ASTORE, ex);
        mv.visitTypeInsn(NEW, "aster/runtime/Err");
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, ex);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Throwable", "toString", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(INVOKESPECIAL, "aster/runtime/Err", "<init>", "(Ljava/lang/Object;)V", false);
        mv.visitLabel(lRet);
        // LVT for tmp/ex across try/catch
        mv.visitLocalVariable("_tmp", "Ljava/lang/Object;", null, lTryStart, lRet, tmp);
        mv.visitLocalVariable("_ex", "Ljava/lang/Throwable;", null, lCatch, lRet, ex);
        mv.visitInsn(ARETURN);
        return true;
      } else {
        emitApplySimpleExpr(mv, r.expr, env, primTypes);
        mv.visitInsn(ARETURN);
        return true;
      }
    }
  if (s instanceof CoreModel.Let let) {
      emitApplySimpleExpr(mv, let.expr, env, primTypes);
      int slot = nextLocal(env);
      mv.visitVarInsn(ASTORE, slot);
      env.put(let.name, slot);
      return false;
    }
  if (s instanceof CoreModel.If iff) {
      return IfEmitter.emitIfApply(
        mv, iff.cond, iff.thenBlock, iff.elseBlock,
        (m, expr) -> emitApplySimpleExpr(m, expr, env, primTypes),
        (m, block) -> emitApplyBlock(ctx, m, block, ownerInternal, env, primTypes, retIsResult, lineNo),
        lineNo
      );
  }
  if (s instanceof CoreModel.Match mm) {
      return LambdaMatchEmitter.emitMatch(
        ctx,
        mv,
        mm,
        ownerInternal,
        env,
        primTypes,
        retIsResult,
        lineNo,
        (m, expr, e, pt) -> emitApplySimpleExpr(m, expr, e, pt),
        (c, m, body, oi, e, pt, rir, ln) -> emitApplyCaseBody(c, m, body, oi, e, pt, rir, ln),
        (c, m, pattern, valSlot, oi, e, pt, failLabel) -> emitApplyPatMatchAndBind(c, m, pattern, valSlot, oi, e, pt, failLabel)
      );
  }
  return false;
}

static boolean emitApplyCaseBody(Ctx ctx, MethodVisitor mv, CoreModel.Stmt body, String ownerInternal, java.util.Map<String,Integer> env, java.util.Map<String,Character> primTypes, boolean retIsResult, java.util.concurrent.atomic.AtomicInteger lineNo) {
  if (body instanceof CoreModel.Return r) {
    emitApplySimpleExpr(mv, r.expr, env, primTypes);
    mv.visitInsn(ARETURN);
    return true;
  } else if (body instanceof CoreModel.If iff) {
    return emitApplyStmt(ctx, mv, body, ownerInternal, env, primTypes, retIsResult, lineNo);
  } else if (body instanceof CoreModel.Scope sc) {
    CoreModel.Block b = new CoreModel.Block(); b.statements = sc.statements;
    return emitApplyBlock(ctx, mv, b, ownerInternal, env, primTypes, retIsResult, lineNo);
  }
  return false;
}

  static int nextLocal(java.util.Map<String,Integer> env) {
    int max = 0;
    for (var v : env.values()) if (v != null && v > max) max = v;
    return max + 1;
  }

  // Recursively match a pattern against a value in local 'valSlot'; bind names into 'env'. On failure, jump to 'failLabel'.
  static void emitApplyPatMatchAndBind(
      Ctx ctx,
      MethodVisitor mv,
      CoreModel.Pattern pat,
      int valSlot,
      String ownerInternal,
      java.util.Map<String,Integer> env,
      java.util.Map<String,Character> primTypes,
      Label failLabel) {
    PatMatchEmitter.emitPatMatch(
      mv,
      pat,
      valSlot,
      ownerInternal,
      env,
      primTypes,
      failLabel,
      typeName -> ctx.lookupData(typeName)
    );
  }


  static String ctorDesc(String pkg, List<CoreModel.Field> fields) {
    var sb = new StringBuilder("(");
    for (var f : fields) sb.append(jDesc(pkg, f.type));
    return sb.append(")V").toString();
  }

  static void emitDefaultReturn(MethodVisitor mv, CoreModel.Type t) {
    if (t instanceof CoreModel.TypeName tn) {
      switch (tn.name) {
        case "Int": mv.visitInsn(ICONST_0); mv.visitInsn(IRETURN); return;
        case "Bool": mv.visitInsn(ICONST_0); mv.visitInsn(IRETURN); return;
        default: mv.visitInsn(ACONST_NULL); mv.visitInsn(ARETURN); return;
      }
    }
    mv.visitInsn(ACONST_NULL); mv.visitInsn(ARETURN);
  }

  static void emitLoad(MethodVisitor mv, int slot, CoreModel.Type t) {
    if (t instanceof CoreModel.TypeName tn && Objects.equals(tn.name, "Int")) mv.visitVarInsn(ILOAD, slot);
    else if (t instanceof CoreModel.TypeName tn2 && Objects.equals(tn2.name, "Bool")) mv.visitVarInsn(ILOAD, slot);
    else mv.visitVarInsn(ALOAD, slot);
  }

  static String toInternal(String pkg, String cls) {
    if (pkg == null || pkg.isEmpty()) return cls;
    return pkg.replace('.', '/') + "/" + cls;
  }
  static String internalDesc(String internal) { return "L" + internal + ';'; }

  /** Map built-in operator names to Builtins field names */
  static String getBuiltinField(String operatorName) {
    return switch (operatorName) {
      case "=" -> "EQUALS";
      case "!=" -> "NOT_EQUALS";
      case "<" -> "LESS_THAN";
      case "<=" -> "LESS_THAN_OR_EQUAL";
      case ">" -> "GREATER_THAN";
      case ">=" -> "GREATER_THAN_OR_EQUAL";
      case "+" -> "ADD";
      case "-" -> "SUBTRACT";
      case "*" -> "MULTIPLY";
      case "/" -> "DIVIDE";
      case "%" -> "MODULO";
      case "and" -> "AND";
      case "or" -> "OR";
      case "not" -> "NOT";
      default -> null;
    };
  }
  static Character classifyNumeric(CoreModel.Expr e, ScopeStack scopeStack, TypeResolver typeResolver, Ctx ctx) {
    if (e instanceof CoreModel.DoubleE) return 'D';
    if (e instanceof CoreModel.LongE) return 'J';
    if (e instanceof CoreModel.IntE || e instanceof CoreModel.Bool) return 'I';

    if (typeResolver != null) {
      Character inferred = typeResolver.inferType(e);
      if (inferred != null) return inferred == 'Z' ? 'I' : inferred;
    }

    if (e instanceof CoreModel.Name n && scopeStack != null) {
      Character kind = scopeStack.getType(n.name);
      if (kind != null) return kind == 'Z' ? 'I' : kind;
    }

    if (e instanceof CoreModel.Call c && c.target instanceof CoreModel.Name nn) {
      String op = nn.name;
      if (c.args != null && c.args.size() == 2 && isNumericBinary(op)) {
        Character k0 = classifyNumeric(c.args.get(0), scopeStack, typeResolver, ctx);
        Character k1 = classifyNumeric(c.args.get(1), scopeStack, typeResolver, ctx);
        if (k0 != null && k1 != null) {
          if (k0 == 'D' || k1 == 'D') return 'D';
          if (k0 == 'J' || k1 == 'J') return 'J';
          return 'I';
        }
      }
      if (ctx != null && ctx.functionSchemas().containsKey(op)) {
        var schema = ctx.functionSchemas().get(op);
        if (schema.ret instanceof CoreModel.TypeName rtn) {
          return switch (rtn.name) {
            case "Int", "Bool" -> 'I';
            case "Long" -> 'J';
            case "Double" -> 'D';
            default -> null;
          };
        }
      }
    }
    return null;
  }

  static Character classifyNumeric(CoreModel.Expr e, ScopeStack scopeStack, TypeResolver typeResolver) {
    return classifyNumeric(e, scopeStack, typeResolver, null);
  }

  static String resolveObjectDescriptor(CoreModel.Expr expr, String pkg, ScopeStack scopeStack, Ctx ctx) {
    if (expr instanceof CoreModel.StringE) return "Ljava/lang/String;";
    if (expr instanceof CoreModel.Construct cons) {
      String internal = cons.typeName.contains(".") ? cons.typeName.replace('.', '/') : toInternal(pkg, cons.typeName);
      return "L" + internal + ';';
    }
    if (expr instanceof CoreModel.Name name) {
      if (scopeStack != null) {
        String desc = scopeStack.getDescriptor(name.name);
        if (desc != null && desc.startsWith("L")) return desc;
      }
      if (scopeStack != null) {
        int dot = name.name.lastIndexOf('.');
        if (dot > 0) {
          String base = name.name.substring(0, dot);
          String field = name.name.substring(dot + 1);
          String ownerDesc = scopeStack.getDescriptor(base);
          if (ownerDesc != null && ownerDesc.startsWith("L") && ownerDesc.endsWith(";")) {
            String resolved = resolveFieldDescriptor(ctx, pkg, ownerDesc.substring(1, ownerDesc.length() - 1), field);
            if (resolved != null) {
              if (resolved.length() == 1) return "Ljava/lang/Object;";
              return resolved;
            }
          }
        }
      }
    }
    if (expr instanceof CoreModel.Call call && call.target instanceof CoreModel.Name target && ctx != null) {
      var schema = ctx.functionSchemas().get(target.name);
      if (schema != null && schema.ret instanceof CoreModel.TypeName rtn) {
        if (BuiltinTypes.isStringType(rtn.name)) {
          return "Ljava/lang/String;";
        }
        String internal = rtn.name.contains(".") ? rtn.name.replace('.', '/') : toInternal(pkg, rtn.name);
        return "L" + internal + ';';
      }
    }
    return "Ljava/lang/Object;";
  }

  private static String resolveFieldDescriptor(Ctx ctx, String pkg, String ownerInternal, String fieldName) {
    if (ctx == null) return null;
    var data = lookupData(ctx, ownerInternal);
    if (data == null || data.fields == null) return null;
    for (var field : data.fields) {
      if (Objects.equals(field.name, fieldName)) {
        return jDesc(pkg, field.type);
      }
    }
    return null;
  }

  private static CoreModel.Data lookupData(Ctx ctx, String ownerInternal) {
    if (ctx == null || ownerInternal == null || ownerInternal.isEmpty()) return null;
    String dotName = ownerInternal.replace('/', '.');
    String current = dotName;
    while (current != null && !current.isEmpty()) {
      var data = ctx.lookupData(current);
      if (data != null) return data;
      int idx = current.indexOf('.');
      if (idx < 0) break;
      current = current.substring(idx + 1);
    }
    int lastDot = dotName.lastIndexOf('.');
    if (lastDot >= 0) {
      var data = ctx.lookupData(dotName.substring(lastDot + 1));
      if (data != null) return data;
    }
    return null;
  }

  private static boolean isNumericBinary(String op) {
    return "+".equals(op)
        || "-".equals(op)
        || "*".equals(op)
        || "/".equals(op)
        || "times".equals(op)
        || "divided by".equals(op);
  }

  static void emitDefaultValue(MethodVisitor mv, String desc) {
    if (desc == null || desc.isEmpty()) {
      mv.visitInsn(ACONST_NULL);
      return;
    }
    char c = desc.charAt(0);
    switch (c) {
      case 'D' -> mv.visitInsn(DCONST_0);
      case 'J' -> mv.visitInsn(LCONST_0);
      case 'F' -> mv.visitInsn(FCONST_0);
      case 'I', 'Z' -> mv.visitInsn(ICONST_0);
      default -> mv.visitInsn(ACONST_NULL);
    }
  }
  static String javaTypeToDesc(Class<?> t) {
    if (t == void.class) return "V";
    if (t == int.class) return "I";
    if (t == boolean.class) return "Z";
    if (t == long.class) return "J";
    if (t == double.class) return "D";
    if (t.isArray()) return t.getName().replace('.', '/');
    return "L" + t.getName().replace('.', '/') + ";";
  }
  static boolean[] nullPolicy(String dotted) {
    if (NULL_POLICY_OVERRIDE.containsKey(dotted)) return NULL_POLICY_OVERRIDE.get(dotted);
    return switch (dotted) {
      case "aster.runtime.Interop.pick" -> new boolean[]{ true };
      case "aster.runtime.Interop.sum" -> new boolean[]{ false, false };
      case "Text.concat" -> new boolean[]{ false, false };
      case "Text.contains" -> new boolean[]{ false, false };
      case "Text.equals" -> new boolean[]{ true, true };
      case "Text.toUpper" -> new boolean[]{ false };
      case "Text.toLower" -> new boolean[]{ false };
      case "Text.length" -> new boolean[]{ false };
      case "Text.indexOf" -> new boolean[]{ false, false };
      case "Text.startsWith" -> new boolean[]{ false, false };
      case "Text.endsWith" -> new boolean[]{ false, false };
      case "Text.replace" -> new boolean[]{ false, false, false };
      case "Text.split" -> new boolean[]{ false, false };
      case "List.length" -> new boolean[]{ false };
      case "List.isEmpty" -> new boolean[]{ false };
      case "List.get" -> new boolean[]{ false, false };
      case "Map.get" -> new boolean[]{ false, true };
      case "Map.containsKey" -> new boolean[]{ false, true };
      case "Set.contains" -> new boolean[]{ false, true };
      case "Set.add" -> new boolean[]{ false, true };
      case "Set.remove" -> new boolean[]{ false, true };
      default -> null;
    };
  }
  static void warnNullability(String dotted, java.util.List<CoreModel.Expr> args) {
    boolean[] policy = nullPolicy(dotted);
    if (policy == null) return;
    int n = Math.min(policy.length, args == null ? 0 : args.size());
    for (int i = 0; i < n; i++) {
      var a = args.get(i);
      if (a instanceof CoreModel.NullE && policy[i] == false) {
        String msg = "NULLABILITY: parameter " + (i+1) + " of '" + dotted + "' is non-null, but null was provided";
        if (NULL_STRICT) throw new IllegalArgumentException(msg);
        System.err.println(msg);
      }
    }
  }

  static String buildMethodDesc(java.lang.reflect.Method m) {
    StringBuilder sb = new StringBuilder("(");
    for (var p : m.getParameterTypes()) sb.append(javaTypeToDesc(p));
    sb.append(")").append(javaTypeToDesc(m.getReturnType()));
    return sb.toString();
  }
  static String internalToPkg(String internal) {
    if (internal == null) return "";
    int i = internal.lastIndexOf('/');
    if (i <= 0) return "";
    return internal.substring(0, i).replace('/', '.');
  }
  static String jDesc(String pkg, CoreModel.Type t) {
    if (t instanceof CoreModel.TypeName tn) {
      return switch (tn.name) {
        case BuiltinTypes.STRING, BuiltinTypes.TEXT -> "Ljava/lang/String;";
        case BuiltinTypes.INT -> "I";
        case BuiltinTypes.BOOL -> "Z";
        case BuiltinTypes.LONG -> "J";
        case BuiltinTypes.DOUBLE -> "D";
        case BuiltinTypes.NUMBER -> "Ljava/lang/Double;"; // Map primitive Number to boxed Double
        default -> {
          String internal = (tn.name.contains(".")) ? tn.name.replace('.', '/') : toInternal(pkg, tn.name);
          yield "L" + internal + ';';
        }
      };
    }
    if (t instanceof CoreModel.ListT) return "Ljava/util/List;";
    if (t instanceof CoreModel.MapT) return "Ljava/util/Map;";
    if (t instanceof CoreModel.Result) return "Laster/runtime/Result;"; // erasure for now
    return "Ljava/lang/Object;";
  }

  static ScopeStack.JvmKind kindForDescriptor(String desc) {
    if (desc == null || desc.isEmpty()) return ScopeStack.JvmKind.UNKNOWN;
    return switch (desc.charAt(0)) {
      case 'I' -> ScopeStack.JvmKind.INT;
      case 'Z' -> ScopeStack.JvmKind.BOOLEAN;
      case 'J' -> ScopeStack.JvmKind.LONG;
      case 'D' -> ScopeStack.JvmKind.DOUBLE;
      default -> ScopeStack.JvmKind.OBJECT;
    };
  }

  static void emitSet(
    Ctx ctx,
    MethodVisitor mv,
    CoreModel.Set set,
    String pkg,
    int paramBase,
    java.util.Map<String, Integer> env,
    ScopeStack scopeStack,
    TypeResolver typeResolver,
    java.util.Map<String, Character> fnHints
  ) throws java.io.IOException {
    if (env == null) throw new IllegalStateException("Set statement requires environment");
    Integer existingSlot = env.get(set.name);
    if (existingSlot == null) {
      throw new IllegalStateException("Set statement error: variable '" + set.name + "' not declared");
    }

    String existingDesc = scopeStack != null ? scopeStack.getDescriptor(set.name) : null;
    Character existingKind = scopeStack != null ? scopeStack.getType(set.name) : null;

    String expectedDesc = existingDesc;
    if (expectedDesc == null || "Ljava/lang/Object;".equals(expectedDesc)) {
      Character inferred = typeResolver != null ? typeResolver.inferType(set.expr) : null;
      if (inferred == null && fnHints != null) {
        inferred = fnHints.get(set.name);
      }
      if (inferred != null) {
        expectedDesc = switch (inferred) {
          case 'D' -> "D";
          case 'J' -> "J";
          case 'Z' -> "Z";
          case 'I' -> "I";
          default -> "Ljava/lang/Object;";
        };
      }
    }

    int storeOpcode = ASTORE;
    if (existingKind != null) {
      storeOpcode = switch (existingKind) {
        case 'D' -> DSTORE;
        case 'J' -> LSTORE;
        case 'I', 'Z' -> ISTORE;
        default -> ASTORE;
      };
    } else if (expectedDesc != null && !expectedDesc.isEmpty()) {
      storeOpcode = switch (expectedDesc.charAt(0)) {
        case 'D' -> DSTORE;
        case 'J' -> LSTORE;
        case 'I', 'Z' -> ISTORE;
        default -> ASTORE;
      };
    }

    emitExpr(ctx, mv, set.expr, expectedDesc, pkg, paramBase, env, scopeStack, typeResolver);
    mv.visitVarInsn(storeOpcode, existingSlot);
  }

  // Emit a statement body inside a switch case; return true if we emitted a return on all paths
  static boolean emitCaseStmt(
    Ctx ctx,
    MethodVisitor mv,
    CoreModel.Stmt stmt,
    String retDesc,
    String pkg,
    int paramBase,
    java.util.Map<String,Integer> env,
    ScopeStack scopeStack,
    TypeResolver typeResolver,
    java.util.Map<String, Character> fnHints,
    int[] nextSlotBox,
    java.util.concurrent.atomic.AtomicInteger lineNo
  ) throws java.io.IOException {
    if (stmt == null) return false;
    if (stmt instanceof CoreModel.Return r) {
      emitExpr(ctx, mv, r.expr, retDesc, pkg, paramBase, env, scopeStack, typeResolver);
      if (retDesc.equals("I") || retDesc.equals("Z")) mv.visitInsn(IRETURN); else mv.visitInsn(ARETURN);
      return true;
    }
    if (stmt instanceof CoreModel.Block block) {
      return emitCaseBlock(ctx, mv, block, retDesc, pkg, paramBase, env, scopeStack, typeResolver, fnHints, nextSlotBox, lineNo);
    }
    if (stmt instanceof CoreModel.Scope sc) {
      var block = new CoreModel.Block();
      block.statements = sc.statements;
      block.origin = sc.origin;
      return emitCaseBlock(ctx, mv, block, retDesc, pkg, paramBase, env, scopeStack, typeResolver, fnHints, nextSlotBox, lineNo);
    }
    if (stmt instanceof CoreModel.Let let) {
      Character inferred = typeResolver != null ? typeResolver.inferType(let.expr) : null;
      if (inferred == null && Objects.equals(let.name, "ok") && let.expr instanceof CoreModel.Call) {
        inferred = 'Z';
      }
      if (inferred == null && fnHints != null) {
        Character hint = fnHints.get(let.name);
        if (hint != null) inferred = hint;
      }

      String expectedDesc = null;
      String localDesc = "Ljava/lang/Object;";
      int storeOpcode = ASTORE;
      if (inferred != null) {
        switch (inferred) {
          case 'D' -> {
            expectedDesc = "D";
            localDesc = "D";
            storeOpcode = DSTORE;
          }
          case 'J' -> {
            expectedDesc = "J";
            localDesc = "J";
            storeOpcode = LSTORE;
          }
          case 'Z' -> {
            expectedDesc = "Z";
            localDesc = "Z";
            storeOpcode = ISTORE;
          }
          case 'I' -> {
            expectedDesc = "I";
            localDesc = "I";
            storeOpcode = ISTORE;
          }
          default -> { }
        }
      }

      if (expectedDesc != null) {
        emitExpr(ctx, mv, let.expr, expectedDesc, pkg, paramBase, env, scopeStack, typeResolver);
      } else {
        emitExpr(ctx, mv, let.expr, null, pkg, paramBase, env, scopeStack, typeResolver);
        localDesc = resolveObjectDescriptor(let.expr, pkg, scopeStack, ctx);
      }

      int slot = nextSlotBox[0];
      mv.visitVarInsn(storeOpcode, slot);
      env.put(let.name, slot);
      if (scopeStack != null) {
        scopeStack.declare(let.name, slot, localDesc, kindForDescriptor(localDesc));
      }
      nextSlotBox[0] += (storeOpcode == DSTORE || storeOpcode == LSTORE) ? 2 : 1;
      return false;
    }
    if (stmt instanceof CoreModel.Set set) {
      emitSet(ctx, mv, set, pkg, paramBase, env, scopeStack, typeResolver, fnHints);
      return false;
    }
    if (stmt instanceof CoreModel.If iff) {
      return IfEmitter.emitIfStatement(
        mv,
        iff.cond,
        iff.thenBlock,
        iff.elseBlock,
        (m, expr, expectedDesc) -> emitExpr(ctx, m, expr, expectedDesc, pkg, paramBase, env, scopeStack, typeResolver),
        (m, block) -> {
          try {
            return emitCaseBlock(ctx, m, block, retDesc, pkg, paramBase, env, scopeStack, typeResolver, fnHints, nextSlotBox, lineNo);
          } catch (java.io.IOException ex) {
            throw new UncheckedIOException(ex);
          }
        },
        lineNo
      );
    }
    return false;
  }

  static boolean emitCaseBlock(
    Ctx ctx,
    MethodVisitor mv,
    CoreModel.Block block,
    String retDesc,
    String pkg,
    int paramBase,
    java.util.Map<String,Integer> env,
    ScopeStack scopeStack,
    TypeResolver typeResolver,
    java.util.Map<String, Character> fnHints,
    int[] nextSlotBox,
    java.util.concurrent.atomic.AtomicInteger lineNo
  ) throws java.io.IOException {
    if (block == null || block.statements == null || block.statements.isEmpty()) return false;
    boolean managedScope = scopeStack != null;
    if (managedScope) scopeStack.pushScope();
    try {
      for (var st : block.statements) {
        if (st == null) continue;
        { var _lbl = new Label(); mv.visitLabel(_lbl); mv.visitLineNumber(lineNo.getAndIncrement(), _lbl); }
        boolean stmtReturn = emitCaseStmt(ctx, mv, st, retDesc, pkg, paramBase, env, scopeStack, typeResolver, fnHints, nextSlotBox, lineNo);
        if (stmtReturn) {
          return true;
        }
      }
      return false;
    } finally {
      if (managedScope) scopeStack.popScope();
    }
  }
}
