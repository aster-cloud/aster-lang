package aster.emitter;

import aster.core.ir.CoreModel;
import aster.core.typecheck.BuiltinTypes;

import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DLOAD;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.IADD;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.IDIV;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.IF_ICMPGT;
import static org.objectweb.asm.Opcodes.IF_ICMPLT;
import static org.objectweb.asm.Opcodes.IMUL;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.LLOAD;
import static org.objectweb.asm.Opcodes.ISUB;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

/**
 * Call 表达式字节码生成器，负责处理内置运算符、标准库互操作、静态方法、用户定义函数以及闭包调用。
 */
public class CallEmitter {
  private final TypeResolver typeResolver;
  private final SignatureResolver signatureResolver;
  private final Main.Ctx ctx;
  private final StdlibInliner stdlibInliner;

  public CallEmitter(
      TypeResolver typeResolver,
      SignatureResolver signatureResolver,
      Main.Ctx ctx,
      StdlibInliner stdlibInliner
  ) {
    this.typeResolver = typeResolver;
    this.signatureResolver = signatureResolver;
    this.ctx = ctx;
    this.stdlibInliner = Objects.requireNonNull(stdlibInliner, "stdlibInliner");
  }

  /**
   * 尝试发射 Call 表达式字节码。
   *
   * @return true 如果已经处理，false 需要回退到 Main.emitExpr
   */
  public boolean tryEmitCall(
      MethodVisitor mv,
      CoreModel.Call call,
      String expectedDesc,
      String currentPkg,
      int paramBase,
      Map<String, Integer> env,
      ScopeStack scopeStack,
      ExprEmitterCallback exprEmitter
  ) throws IOException {
    Objects.requireNonNull(mv, "mv");
    Objects.requireNonNull(call, "call");
    Objects.requireNonNull(exprEmitter, "exprEmitter");

    if (call.target instanceof CoreModel.Name tn) {
      String name = tn.name;

      // Boolean not intrinsic: not(arg) -> !arg
      // Phase 22: 移除 expectedDesc == "Z" 限制，允许在任何期望类型下发射 not 操作
      if (Objects.equals(name, "not") && argsSize(call, 1)) {
        Label lTrue = new Label();
        Label lEnd = new Label();
        exprEmitter.emitExpr(mv, call.args.get(0), "Z", currentPkg, paramBase, env, scopeStack);
        mv.visitJumpInsn(org.objectweb.asm.Opcodes.IFEQ, lTrue);  // if arg == 0 -> true
        mv.visitInsn(ICONST_0);
        mv.visitJumpInsn(org.objectweb.asm.Opcodes.GOTO, lEnd);
        mv.visitLabel(lTrue);
        mv.visitInsn(ICONST_1);
        mv.visitLabel(lEnd);
        AsmUtilities.boxPrimitiveResult(mv, 'Z', expectedDesc);
        return true;
      }

      // Intrinsic int arithmetic/comparison
      if (Objects.equals(name, "+")) {
        if (call.args != null && call.args.size() == 2) {
          exprEmitter.emitExpr(mv, call.args.get(0), "I", currentPkg, paramBase, env, scopeStack);
          exprEmitter.emitExpr(mv, call.args.get(1), "I", currentPkg, paramBase, env, scopeStack);
          mv.visitInsn(IADD);
          AsmUtilities.boxPrimitiveResult(mv, 'I', expectedDesc);
          return true;
        }
      }
      if (Objects.equals(name, "-")) {
        if (call.args != null && call.args.size() == 2) {
          exprEmitter.emitExpr(mv, call.args.get(0), "I", currentPkg, paramBase, env, scopeStack);
          exprEmitter.emitExpr(mv, call.args.get(1), "I", currentPkg, paramBase, env, scopeStack);
          mv.visitInsn(ISUB);
          AsmUtilities.boxPrimitiveResult(mv, 'I', expectedDesc);
          return true;
        }
      }
      if (Objects.equals(name, "*")) {
        if (call.args != null && call.args.size() == 2) {
          exprEmitter.emitExpr(mv, call.args.get(0), "I", currentPkg, paramBase, env, scopeStack);
          exprEmitter.emitExpr(mv, call.args.get(1), "I", currentPkg, paramBase, env, scopeStack);
          mv.visitInsn(IMUL);
          AsmUtilities.boxPrimitiveResult(mv, 'I', expectedDesc);
          return true;
        }
      }
      if (Objects.equals(name, "/")) {
        if (call.args != null && call.args.size() == 2) {
          exprEmitter.emitExpr(mv, call.args.get(0), "I", currentPkg, paramBase, env, scopeStack);
          exprEmitter.emitExpr(mv, call.args.get(1), "I", currentPkg, paramBase, env, scopeStack);
          mv.visitInsn(IDIV);
          AsmUtilities.boxPrimitiveResult(mv, 'I', expectedDesc);
          return true;
        }
      }
      if (Objects.equals(name, "<")) {
        if (call.args != null && call.args.size() == 2) {
          Label lTrue = new Label();
          Label lEnd = new Label();
          exprEmitter.emitExpr(mv, call.args.get(0), "I", currentPkg, paramBase, env, scopeStack);
          exprEmitter.emitExpr(mv, call.args.get(1), "I", currentPkg, paramBase, env, scopeStack);
          mv.visitJumpInsn(IF_ICMPLT, lTrue);
          mv.visitInsn(ICONST_0);
          mv.visitJumpInsn(GOTO, lEnd);
          mv.visitLabel(lTrue);
          mv.visitInsn(ICONST_1);
          mv.visitLabel(lEnd);
          AsmUtilities.boxPrimitiveResult(mv, 'Z', expectedDesc);
          return true;
        }
      }
      if (Objects.equals(name, ">")) {
        if (call.args != null && call.args.size() == 2) {
          Label lTrue = new Label();
          Label lEnd = new Label();
          exprEmitter.emitExpr(mv, call.args.get(0), "I", currentPkg, paramBase, env, scopeStack);
          exprEmitter.emitExpr(mv, call.args.get(1), "I", currentPkg, paramBase, env, scopeStack);
          mv.visitJumpInsn(IF_ICMPGT, lTrue);
          mv.visitInsn(ICONST_0);
          mv.visitJumpInsn(GOTO, lEnd);
          mv.visitLabel(lTrue);
          mv.visitInsn(ICONST_1);
          mv.visitLabel(lEnd);
          AsmUtilities.boxPrimitiveResult(mv, 'Z', expectedDesc);
          return true;
        }
      }

      if (call.args != null && !isArithmeticIntrinsic(name) && shouldApplyStdlibInline(name, expectedDesc)) {
        Map<String, Character> inlinePrimTypes = snapshotPrimitiveHints(scopeStack);
        StdlibInliner.SimpleExprEmitter inlineEmitter = (inlineMv, expr, inlineEnv, inlineHints) ->
            emitInlineOperand(
                inlineMv,
                expr,
                inlineEnv != null ? inlineEnv : env,
                inlineHints != null ? inlineHints : inlinePrimTypes,
                currentPkg,
                paramBase,
                scopeStack
            );
        boolean inlined = stdlibInliner.tryInline(
            name,
            mv,
            call.args,
            env,
            inlinePrimTypes,
            inlineEmitter,
            CallEmitter::warnNullability
        );
        if (inlined) {
          return true;
        }
      }

      // Text/String interop mappings (MVP)
      if (Objects.equals(name, "Text.concat") && argsSize(call, 2)) {
        warnNullability("Text.concat", call.args);
        exprEmitter.emitExpr(mv, call.args.get(0), "Ljava/lang/String;", currentPkg, paramBase, env, scopeStack);
        exprEmitter.emitExpr(mv, call.args.get(1), "Ljava/lang/String;", currentPkg, paramBase, env, scopeStack);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "concat", "(Ljava/lang/String;)Ljava/lang/String;", false);
        return true;
      }
      if (Objects.equals(name, "Text.contains") && argsSize(call, 2)) {
        warnNullability("Text.contains", call.args);
        exprEmitter.emitExpr(mv, call.args.get(0), "Ljava/lang/String;", currentPkg, paramBase, env, scopeStack);
        exprEmitter.emitExpr(mv, call.args.get(1), "Ljava/lang/String;", currentPkg, paramBase, env, scopeStack);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "contains", "(Ljava/lang/CharSequence;)Z", false);
        AsmUtilities.boxPrimitiveResult(mv, 'Z', expectedDesc);
        return true;
      }
      if (Objects.equals(name, "Text.equals") && argsSize(call, 2)) {
        warnNullability("Text.equals", call.args);
        exprEmitter.emitExpr(mv, call.args.get(0), "Ljava/lang/String;", currentPkg, paramBase, env, scopeStack);
        exprEmitter.emitExpr(mv, call.args.get(1), "Ljava/lang/String;", currentPkg, paramBase, env, scopeStack);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
        AsmUtilities.boxPrimitiveResult(mv, 'Z', expectedDesc);
        return true;
      }
      if (Objects.equals(name, "Text.toUpper") && argsSize(call, 1)) {
        warnNullability("Text.toUpper", call.args);
        exprEmitter.emitExpr(mv, call.args.get(0), "Ljava/lang/String;", currentPkg, paramBase, env, scopeStack);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "toUpperCase", "()Ljava/lang/String;", false);
        return true;
      }
      if (Objects.equals(name, "Text.toLower") && argsSize(call, 1)) {
        warnNullability("Text.toLower", call.args);
        exprEmitter.emitExpr(mv, call.args.get(0), "Ljava/lang/String;", currentPkg, paramBase, env, scopeStack);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "toLowerCase", "()Ljava/lang/String;", false);
        return true;
      }
      if (Objects.equals(name, "Text.length") && argsSize(call, 1)) {
        warnNullability("Text.length", call.args);
        exprEmitter.emitExpr(mv, call.args.get(0), "Ljava/lang/String;", currentPkg, paramBase, env, scopeStack);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "length", "()I", false);
        AsmUtilities.boxPrimitiveResult(mv, 'I', expectedDesc);
        return true;
      }
      if (Objects.equals(name, "Text.indexOf") && argsSize(call, 2)) {
        warnNullability("Text.indexOf", call.args);
        exprEmitter.emitExpr(mv, call.args.get(0), "Ljava/lang/String;", currentPkg, paramBase, env, scopeStack);
        exprEmitter.emitExpr(mv, call.args.get(1), "Ljava/lang/String;", currentPkg, paramBase, env, scopeStack);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "indexOf", "(Ljava/lang/String;)I", false);
        AsmUtilities.boxPrimitiveResult(mv, 'I', expectedDesc);
        return true;
      }
      if (Objects.equals(name, "Text.startsWith") && argsSize(call, 2)) {
        warnNullability("Text.startsWith", call.args);
        exprEmitter.emitExpr(mv, call.args.get(0), "Ljava/lang/String;", currentPkg, paramBase, env, scopeStack);
        exprEmitter.emitExpr(mv, call.args.get(1), "Ljava/lang/String;", currentPkg, paramBase, env, scopeStack);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "startsWith", "(Ljava/lang/String;)Z", false);
        AsmUtilities.boxPrimitiveResult(mv, 'Z', expectedDesc);
        return true;
      }
      if (Objects.equals(name, "Text.endsWith") && argsSize(call, 2)) {
        warnNullability("Text.endsWith", call.args);
        exprEmitter.emitExpr(mv, call.args.get(0), "Ljava/lang/String;", currentPkg, paramBase, env, scopeStack);
        exprEmitter.emitExpr(mv, call.args.get(1), "Ljava/lang/String;", currentPkg, paramBase, env, scopeStack);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "endsWith", "(Ljava/lang/String;)Z", false);
        AsmUtilities.boxPrimitiveResult(mv, 'Z', expectedDesc);
        return true;
      }

      // List/Map interop
      if (Objects.equals(name, "List.length") && argsSize(call, 1)) {
        warnNullability("List.length", call.args);
        exprEmitter.emitExpr(mv, call.args.get(0), "Ljava/util/List;", currentPkg, paramBase, env, scopeStack);
        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "size", "()I", true);
        AsmUtilities.boxPrimitiveResult(mv, 'I', expectedDesc);
        return true;
      }
      if (Objects.equals(name, "List.isEmpty") && argsSize(call, 1)) {
        warnNullability("List.isEmpty", call.args);
        exprEmitter.emitExpr(mv, call.args.get(0), "Ljava/util/List;", currentPkg, paramBase, env, scopeStack);
        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "isEmpty", "()Z", true);
        AsmUtilities.boxPrimitiveResult(mv, 'Z', expectedDesc);
        return true;
      }
      if (Objects.equals(name, "List.get") && argsSize(call, 2)) {
        warnNullability("List.get", call.args);
        exprEmitter.emitExpr(mv, call.args.get(0), "Ljava/util/List;", currentPkg, paramBase, env, scopeStack);
        exprEmitter.emitExpr(mv, call.args.get(1), "I", currentPkg, paramBase, env, scopeStack);
        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "get", "(I)Ljava/lang/Object;", true);
        if ("Ljava/lang/String;".equals(expectedDesc)) {
          mv.visitTypeInsn(CHECKCAST, "java/lang/String");
        }
        return true;
      }
      if (Objects.equals(name, "Map.get") && argsSize(call, 2)) {
        warnNullability("Map.get", call.args);
        exprEmitter.emitExpr(mv, call.args.get(0), "Ljava/util/Map;", currentPkg, paramBase, env, scopeStack);
        exprEmitter.emitExpr(mv, call.args.get(1), "Ljava/lang/Object;", currentPkg, paramBase, env, scopeStack);
        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
        if ("Ljava/lang/String;".equals(expectedDesc)) {
          mv.visitTypeInsn(CHECKCAST, "java/lang/String");
        }
        return true;
      }

      // Static method interop (dotted name)
      int dot = name.lastIndexOf('.');
      if (dot > 0 && currentPkg != null) {
        String cls = name.substring(0, dot);
        String method = name.substring(dot + 1);
        String ownerInternal = cls.contains(".") ? cls.replace('.', '/') : toInternal(currentPkg, cls);

        if (Objects.equals(name, "AuthRepo.verify") && argsSize(call, 2)) {
          for (CoreModel.Expr arg : call.args) {
            exprEmitter.emitExpr(mv, arg, null, currentPkg, paramBase, env, scopeStack);
          }
          mv.visitMethodInsn(INVOKESTATIC, ownerInternal, method, "(Ljava/lang/Object;Ljava/lang/Object;)Z", false);
          AsmUtilities.boxPrimitiveResult(mv, 'Z', expectedDesc);
          return true;
        }

        warnNullability(name, call.args);
        StringBuilder descriptorBuilder = new StringBuilder("(");
        boolean hasLong = false;
        boolean hasDouble = false;
        if (call.args != null) {
          for (CoreModel.Expr arg : call.args) {
            Character kind = classifyNumeric(arg, scopeStack);
            if (kind != null) {
              if (kind == 'D') {
                hasDouble = true;
              } else if (kind == 'J') {
                hasLong = true;
              }
            }
          }
        }
        if (hasDouble) {
          hasLong = false;
        }
        List<String> argDescs = new ArrayList<>();
        if (call.args != null) {
          for (CoreModel.Expr arg : call.args) {
            String ad = "Ljava/lang/Object;";
            if (arg instanceof CoreModel.NullE) {
              ad = "Ljava/lang/Object;";
            } else if (arg instanceof CoreModel.Bool) {
              ad = "Z";
            } else if (arg instanceof CoreModel.StringE) {
              ad = "Ljava/lang/String;";
            } else {
              Character kind = classifyNumeric(arg, scopeStack);
              if (kind != null) {
                if (kind == 'D' || hasDouble) {
                  ad = "D";
                } else if (kind == 'J' || hasLong) {
                  ad = "J";
                } else {
                  ad = "I";
                }
              }
            }
            argDescs.add(ad);
            descriptorBuilder.append(ad);
          }
        }
        String returnDesc = ("I".equals(expectedDesc) || "Z".equals(expectedDesc) || "Ljava/lang/String;".equals(expectedDesc))
            ? expectedDesc
            : "Ljava/lang/String;";

        String resolvedDesc = signatureResolver.resolveMethodSignature(ownerInternal, method, argDescs, returnDesc);
        String finalDesc;
        if (resolvedDesc != null) {
          finalDesc = resolvedDesc;
        } else {
          finalDesc = descriptorBuilder.append(")").append(returnDesc).toString();
          if (Main.DIAG_OVERLOAD) {
            boolean anyPrim = false;
            for (String ad : argDescs) {
              if ("I".equals(ad) || "J".equals(ad) || "D".equals(ad) || "Z".equals(ad)) {
                anyPrim = true;
                break;
              }
            }
            if (!anyPrim) {
              System.err.println("HEURISTIC OVERLOAD: no primitive signal for "
                  + ownerInternal.replace('/', '.') + "." + method
                  + "(" + String.join(",", argDescs) + ") using heuristic " + finalDesc);
            }
          }
        }
        if (call.args != null) {
          for (int i = 0; i < call.args.size(); i++) {
            CoreModel.Expr arg = call.args.get(i);
            String ad = argDescs.get(i);
            exprEmitter.emitExpr(mv, arg, ad, currentPkg, paramBase, env, scopeStack);
          }
        }
        mv.visitMethodInsn(INVOKESTATIC, ownerInternal, method, finalDesc, false);
        if (finalDesc.endsWith(")I")) {
          AsmUtilities.boxPrimitiveResult(mv, 'I', expectedDesc);
        } else if (finalDesc.endsWith(")Z")) {
          AsmUtilities.boxPrimitiveResult(mv, 'Z', expectedDesc);
        } else if (finalDesc.endsWith(")J")) {
          AsmUtilities.boxPrimitiveResult(mv, 'J', expectedDesc);
        } else if (finalDesc.endsWith(")D")) {
          AsmUtilities.boxPrimitiveResult(mv, 'D', expectedDesc);
        }
        return true;
      }
    }

    CoreModel.Call cgen = call;
    if (cgen.target instanceof CoreModel.Name targetName) {
      String funcName = targetName.name;
      if (ctx != null && ctx.functionSchemas().containsKey(funcName) && currentPkg != null) {
        CoreModel.Func funcSchema = ctx.functionSchemas().get(funcName);
        String ownerInternal = toInternal(currentPkg, funcName + "_fn");
        StringBuilder descriptorBuilder = new StringBuilder("(");

        int arity = (cgen.args == null) ? 0 : cgen.args.size();
        for (int i = 0; i < arity && i < funcSchema.params.size(); i++) {
          CoreModel.Param param = funcSchema.params.get(i);
          String paramDesc = "Ljava/lang/Object;";
          if (param.type instanceof CoreModel.TypeName tn) {
            if (tn.name.equals("Int")) {
              paramDesc = "I";
            } else if (tn.name.equals("Bool")) {
              paramDesc = "Z";
            } else if (tn.name.equals("Long")) {
              paramDesc = "J";
            } else if (tn.name.equals("Double")) {
              paramDesc = "D";
            } else if (BuiltinTypes.isStringType(tn.name)) {
              paramDesc = "Ljava/lang/String;";
            } else {
              String internal = tn.name.contains(".") ? tn.name.replace('.', '/') : toInternal(currentPkg, tn.name);
              paramDesc = internalDesc(internal);
            }
          }
          exprEmitter.emitExpr(mv, cgen.args.get(i), paramDesc, currentPkg, paramBase, env, scopeStack);
          descriptorBuilder.append(paramDesc);
        }

        String retDesc = "Ljava/lang/Object;";
        if (funcSchema.ret instanceof CoreModel.TypeName rtn) {
          if (rtn.name.equals("Int")) {
            retDesc = "I";
          } else if (rtn.name.equals("Bool")) {
            retDesc = "Z";
          } else if (rtn.name.equals("Long")) {
            retDesc = "J";
          } else if (rtn.name.equals("Double")) {
            retDesc = "D";
          } else if (BuiltinTypes.isStringType(rtn.name)) {
            retDesc = "Ljava/lang/String;";
          } else {
            String internal = rtn.name.contains(".") ? rtn.name.replace('.', '/') : toInternal(currentPkg, rtn.name);
            retDesc = internalDesc(internal);
          }
        }
        descriptorBuilder.append(")").append(retDesc);
        mv.visitMethodInsn(INVOKESTATIC, ownerInternal, funcName, descriptorBuilder.toString(), false);
        if ("I".equals(retDesc)) {
          AsmUtilities.boxPrimitiveResult(mv, 'I', expectedDesc);
        } else if ("Z".equals(retDesc)) {
          AsmUtilities.boxPrimitiveResult(mv, 'Z', expectedDesc);
        } else if ("J".equals(retDesc)) {
          AsmUtilities.boxPrimitiveResult(mv, 'J', expectedDesc);
        } else if ("D".equals(retDesc)) {
          AsmUtilities.boxPrimitiveResult(mv, 'D', expectedDesc);
        }
        return true;
      }
    }

    int arity = (cgen.args == null) ? 0 : cgen.args.size();
    exprEmitter.emitExpr(mv, cgen.target, null, currentPkg, paramBase, env, scopeStack);
    String intf;
    String desc;
    if (arity == 0) {
      intf = "aster/runtime/Fn0";
      desc = "()Ljava/lang/Object;";
    } else if (arity == 1) {
      intf = "aster/runtime/Fn1";
      desc = "(Ljava/lang/Object;)Ljava/lang/Object;";
    } else if (arity == 2) {
      intf = "aster/runtime/Fn2";
      desc = "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;";
    } else if (arity == 3) {
      intf = "aster/runtime/Fn3";
      desc = "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;";
    } else {
      intf = "aster/runtime/Fn1";
      desc = "(Ljava/lang/Object;)Ljava/lang/Object;";
    }
    for (int i = 0; i < arity; i++) {
      exprEmitter.emitExpr(mv, cgen.args.get(i), "Ljava/lang/Object;", currentPkg, paramBase, env, scopeStack);
    }
    mv.visitMethodInsn(INVOKEINTERFACE, intf, "apply", desc, true);
    if ("Ljava/lang/String;".equals(expectedDesc)) {
      mv.visitTypeInsn(CHECKCAST, "java/lang/String");
    } else if ("I".equals(expectedDesc)) {
      mv.visitTypeInsn(CHECKCAST, "java/lang/Integer");
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
    } else if ("Z".equals(expectedDesc)) {
      mv.visitTypeInsn(CHECKCAST, "java/lang/Boolean");
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
    }
    return true;
  }

  private boolean isArithmeticIntrinsic(String name) {
    return Objects.equals(name, "+")
        || Objects.equals(name, "-")
        || Objects.equals(name, "*")
        || Objects.equals(name, "/")
        || Objects.equals(name, "<")
        || Objects.equals(name, ">")
        || Objects.equals(name, "==");
  }

  private boolean shouldApplyStdlibInline(String name, String expectedDesc) {
    // Skip StdlibInliner for List/Map methods when expectedDesc is null or primitive
    // to use the manual CallEmitter implementation which respects expectedDesc for boxing
    if (expectedDesc == null || expectedDesc.isEmpty()) {
      // When expectedDesc is null, skip StdlibInliner for List/Map methods
      // because StdlibInliner always boxes, but manual implementation respects null
      if (Objects.equals(name, "List.length") ||
          Objects.equals(name, "List.isEmpty") ||
          Objects.equals(name, "Map.get") ||
          Objects.equals(name, "List.get")) {
        return false;
      }
      return true;
    }
    if (Objects.equals(expectedDesc, "Ljava/lang/Object;")) {
      // When expectedDesc is Object (boxed), StdlibInliner is OK
      return true;
    }
    // For primitive expectedDesc (I, Z, etc.), use manual implementation
    if (Objects.equals(name, "List.length") && Objects.equals(expectedDesc, "I")) {
      return false;
    }
    if (Objects.equals(name, "List.isEmpty") && Objects.equals(expectedDesc, "Z")) {
      return false;
    }
    return true;
  }

  private Map<String, Character> snapshotPrimitiveHints(ScopeStack scopeStack) {
    if (scopeStack == null) {
      return Map.of();
    }
    Map<String, Character> hints = new HashMap<>();
    for (ScopeStack.TypedLocal local : scopeStack.getAllVariables()) {
      Character kind = switch (local.kind()) {
        case INT -> 'I';
        case BOOLEAN -> 'Z';
        case LONG -> 'J';
        case DOUBLE -> 'D';
        default -> null;
      };
      if (kind != null) {
        hints.put(local.name(), kind);
      }
    }
    return hints.isEmpty() ? Map.of() : hints;
  }

  private void emitInlineOperand(
      MethodVisitor mv,
      CoreModel.Expr expr,
      Map<String, Integer> env,
      Map<String, Character> primTypes,
      String currentPkg,
      int paramBase,
      ScopeStack scopeStack
  ) {
    Map<String, Integer> effectiveEnv = (env == null) ? Map.of() : env;
    Map<String, Character> effectivePrimTypes = (primTypes == null) ? Map.of() : primTypes;
    if (expr instanceof CoreModel.StringE se) {
      mv.visitLdcInsn(se.value);
      return;
    }
    if (expr instanceof CoreModel.IntE ie) {
      mv.visitLdcInsn(Integer.valueOf(ie.value));
      return;
    }
    if (expr instanceof CoreModel.Bool be) {
      mv.visitLdcInsn(Boolean.valueOf(be.value));
      return;
    }
    if (expr instanceof CoreModel.LongE le) {
      mv.visitLdcInsn(Long.valueOf(le.value));
      return;
    }
    if (expr instanceof CoreModel.DoubleE de) {
      mv.visitLdcInsn(Double.valueOf(de.value));
      return;
    }
    if (expr instanceof CoreModel.NullE) {
      mv.visitInsn(ACONST_NULL);
      return;
    }
    if (expr instanceof CoreModel.Name name) {
      emitInlineNameOperand(mv, name, effectiveEnv, effectivePrimTypes, scopeStack);
      return;
    }
    if (expr instanceof CoreModel.Call nested
        && nested.target instanceof CoreModel.Name nestedName
        && nested.args != null) {
      boolean nestedInlined = stdlibInliner.tryInline(
          nestedName.name,
          mv,
          nested.args,
          effectiveEnv,
          effectivePrimTypes,
          (inlineMv, inlineExpr, inlineEnv, inlineHints) -> emitInlineOperand(
              inlineMv,
              inlineExpr,
              inlineEnv != null ? inlineEnv : effectiveEnv,
              inlineHints != null ? inlineHints : effectivePrimTypes,
              currentPkg,
              paramBase,
              scopeStack
          ),
          CallEmitter::warnNullability
      );
      if (nestedInlined) {
        return;
      }
    }
    Main.emitExpr(ctx, mv, expr, null, currentPkg, paramBase, effectiveEnv, scopeStack, typeResolver);
  }

  private void emitInlineNameOperand(
      MethodVisitor mv,
      CoreModel.Name name,
      Map<String, Integer> env,
      Map<String, Character> primTypes,
      ScopeStack scopeStack
  ) {
    if (!env.containsKey(name.name)) {
      mv.visitInsn(ACONST_NULL);
      return;
    }
    int slot = env.get(name.name);
    Character kind = primTypes.get(name.name);
    if (kind == null && scopeStack != null) {
      kind = scopeStack.getType(name.name);
      if (kind == null) {
        kind = scopeStack.getType(slot);
      }
    }
    if (kind != null) {
      switch (kind) {
        case 'I' -> {
          mv.visitVarInsn(ILOAD, slot);
          mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
          return;
        }
        case 'Z' -> {
          mv.visitVarInsn(ILOAD, slot);
          mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
          return;
        }
        case 'J' -> {
          mv.visitVarInsn(LLOAD, slot);
          mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
          return;
        }
        case 'D' -> {
          mv.visitVarInsn(DLOAD, slot);
          mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
          return;
        }
        default -> {
        }
      }
    }
    mv.visitVarInsn(ALOAD, slot);
  }

  private boolean argsSize(CoreModel.Call call, int size) {
    return call.args != null && call.args.size() == size;
  }

  private Character classifyNumeric(CoreModel.Expr expr, ScopeStack scopeStack) {
    return classifyNumeric(expr, scopeStack, null);
  }

  private Character classifyNumeric(CoreModel.Expr expr, ScopeStack scopeStack, Main.Ctx ctxOverride) {
    if (expr instanceof CoreModel.DoubleE) return 'D';
    if (expr instanceof CoreModel.LongE) return 'J';
    if (expr instanceof CoreModel.IntE || expr instanceof CoreModel.Bool) return 'I';

    if (typeResolver != null) {
      Character inferred = typeResolver.inferType(expr);
      if (inferred != null) {
        return inferred == 'Z' ? 'I' : inferred;
      }
    }

    if (expr instanceof CoreModel.Name name && scopeStack != null) {
      Character kind = scopeStack.getType(name.name);
      if (kind != null) {
        return kind == 'Z' ? 'I' : kind;
      }
    }

    if (expr instanceof CoreModel.Call call && call.target instanceof CoreModel.Name callName) {
      String op = callName.name;
      if (call.args != null && call.args.size() == 2 && isNumericBinary(op)) {
        Character k0 = classifyNumeric(call.args.get(0), scopeStack, ctxOverride);
        Character k1 = classifyNumeric(call.args.get(1), scopeStack, ctxOverride);
        if (k0 != null && k1 != null) {
          if (k0 == 'D' || k1 == 'D') return 'D';
          if (k0 == 'J' || k1 == 'J') return 'J';
          return 'I';
        }
      }
      if (ctxOverride != null && ctxOverride.functionSchemas().containsKey(op)) {
        CoreModel.Func schema = ctxOverride.functionSchemas().get(op);
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

  private static boolean isNumericBinary(String op) {
    return Objects.equals(op, "+") || Objects.equals(op, "-")
        || Objects.equals(op, "*") || Objects.equals(op, "/")
        || Objects.equals(op, "%");
  }

  private static void warnNullability(String dotted, List<CoreModel.Expr> args) {
    boolean[] policy = nullPolicy(dotted);
    if (policy == null) return;
    int n = Math.min(policy.length, args == null ? 0 : args.size());
    for (int i = 0; i < n; i++) {
      CoreModel.Expr a = args.get(i);
      if (a instanceof CoreModel.NullE && !policy[i]) {
        String msg = "NULLABILITY: parameter " + (i + 1) + " of '" + dotted + "' is non-null, but null was provided";
        if (Main.NULL_STRICT) {
          throw new IllegalArgumentException(msg);
        }
        System.err.println(msg);
      }
    }
  }

  private static boolean[] nullPolicy(String dotted) {
    if (Main.NULL_POLICY_OVERRIDE.containsKey(dotted)) {
      return Main.NULL_POLICY_OVERRIDE.get(dotted);
    }
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

  private static String toInternal(String pkg, String cls) {
    if (pkg == null || pkg.isEmpty()) return cls;
    return pkg.replace('.', '/') + "/" + cls;
  }

  private static String internalDesc(String internal) {
    return "L" + internal + ';';
  }
}
