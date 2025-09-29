package aster.emitter;

import com.fasterxml.jackson.annotation.*;
import java.util.*;

public final class CoreModel {
  public static final class Position { public int line; public int col; }
  public static final class Origin { public String file; public Position start; public Position end; }
  public static final class Module {
    public String name;
    public List<Decl> decls;
    public Origin origin;
  }

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
  @JsonSubTypes({
    @JsonSubTypes.Type(value = Data.class, name = "Data"),
    @JsonSubTypes.Type(value = Enum.class, name = "Enum"),
    @JsonSubTypes.Type(value = Func.class, name = "Func")
  })
  public sealed interface Decl permits Data, Enum, Func {}

  @JsonTypeName("Data")
  public static final class Data implements Decl {
    public String name;
    public List<Field> fields;
    public Origin origin;
  }

  @JsonTypeName("Enum")
  public static final class Enum implements Decl {
    public String name;
    public List<String> variants;
    public Origin origin;
  }

  @JsonTypeName("Func")
  public static final class Func implements Decl {
    public String name;
    public List<Param> params;
    public Type ret;
    public List<String> effects;
    public Block body;
    public Origin origin;
  }

  public static final class Field { public String name; public Type type; }
  public static final class Param { public String name; public Type type; }

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
  @JsonSubTypes({
    @JsonSubTypes.Type(value = TypeName.class, name = "TypeName"),
    @JsonSubTypes.Type(value = Result.class, name = "Result"),
    @JsonSubTypes.Type(value = Maybe.class, name = "Maybe"),
    @JsonSubTypes.Type(value = Option.class, name = "Option"),
    @JsonSubTypes.Type(value = ListT.class, name = "List"),
    @JsonSubTypes.Type(value = MapT.class, name = "Map"),
    @JsonSubTypes.Type(value = FuncType.class, name = "FuncType")
  })
  public sealed interface Type permits TypeName, Result, Maybe, Option, ListT, MapT, FuncType {}

  @JsonTypeName("TypeName")
  public static final class TypeName implements Type { public String name; public Origin origin; }
  @JsonTypeName("Result")
  public static final class Result implements Type { public Type ok; public Type err; public Origin origin; }
  @JsonTypeName("Maybe")
  public static final class Maybe implements Type { public Type type; public Origin origin; }
  @JsonTypeName("Option")
  public static final class Option implements Type { public Type type; public Origin origin; }
  @JsonTypeName("List")
  public static final class ListT implements Type { public Type type; public Origin origin; }
  @JsonTypeName("Map")
  public static final class MapT implements Type { public Type key; public Type val; public Origin origin; }
  @JsonTypeName("FuncType")
  public static final class FuncType implements Type { public java.util.List<Type> params; public Type ret; public Origin origin; }

  @JsonTypeName("Block")
  public static final class Block implements Stmt { public List<Stmt> statements; public Origin origin; }

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
  @JsonSubTypes({
    @JsonSubTypes.Type(value = Let.class, name = "Let"),
    @JsonSubTypes.Type(value = Set.class, name = "Set"),
    @JsonSubTypes.Type(value = Return.class, name = "Return"),
    @JsonSubTypes.Type(value = If.class, name = "If"),
    @JsonSubTypes.Type(value = Match.class, name = "Match"),
    @JsonSubTypes.Type(value = Scope.class, name = "Scope"),
    @JsonSubTypes.Type(value = Block.class, name = "Block")
  })
  public sealed interface Stmt permits Let, Set, Return, If, Match, Scope, Block {}

  @JsonTypeName("Let")
  public static final class Let implements Stmt { public String name; public Expr expr; public Origin origin; }
  @JsonTypeName("Set")
  public static final class Set implements Stmt { public String name; public Expr expr; public Origin origin; }
  @JsonTypeName("Return")
  public static final class Return implements Stmt { public Expr expr; public Origin origin; }
  @JsonTypeName("If")
  public static final class If implements Stmt { public Expr cond; public Block thenBlock; public Block elseBlock; public Origin origin; }
  @JsonTypeName("Match")
  public static final class Match implements Stmt { public Expr expr; public List<Case> cases; public Origin origin; }
  @JsonTypeName("Scope")
  public static final class Scope implements Stmt { public List<Stmt> statements; public Origin origin; }

  @JsonTypeName("Case")
  public static final class Case { public Pattern pattern; public Stmt body; public Origin origin; }

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
  @JsonSubTypes({
    @JsonSubTypes.Type(value = PatNull.class, name = "PatNull"),
    @JsonSubTypes.Type(value = PatCtor.class, name = "PatCtor"),
    @JsonSubTypes.Type(value = PatName.class, name = "PatName"),
    @JsonSubTypes.Type(value = PatInt.class, name = "PatInt")
  })
  public sealed interface Pattern permits PatNull, PatCtor, PatName, PatInt {}

  @JsonTypeName("PatNull")
  public static final class PatNull implements Pattern { public Origin origin; }
  @JsonTypeName("PatCtor")
  public static final class PatCtor implements Pattern {
    public String typeName;
    // Legacy positional bindings by name (kept for back-compat)
    public List<String> names;
    // New: nested patterns for positional fields
    public List<Pattern> args;
    public Origin origin;
  }
  @JsonTypeName("PatName")
  public static final class PatName implements Pattern { public String name; public Origin origin; }
  @JsonTypeName("PatInt")
  public static final class PatInt implements Pattern { public int value; public Origin origin; }

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
  @JsonSubTypes({
    @JsonSubTypes.Type(value = Name.class, name = "Name"),
    @JsonSubTypes.Type(value = Bool.class, name = "Bool"),
    @JsonSubTypes.Type(value = IntE.class, name = "Int"),
    @JsonSubTypes.Type(value = LongE.class, name = "Long"),
    @JsonSubTypes.Type(value = DoubleE.class, name = "Double"),
    @JsonSubTypes.Type(value = StringE.class, name = "String"),
    @JsonSubTypes.Type(value = NullE.class, name = "Null"),
    @JsonSubTypes.Type(value = Ok.class, name = "Ok"),
    @JsonSubTypes.Type(value = Err.class, name = "Err"),
    @JsonSubTypes.Type(value = Some.class, name = "Some"),
    @JsonSubTypes.Type(value = NoneE.class, name = "None"),
    @JsonSubTypes.Type(value = Construct.class, name = "Construct"),
    @JsonSubTypes.Type(value = Call.class, name = "Call"),
    @JsonSubTypes.Type(value = Lambda.class, name = "Lambda")
  })
  public sealed interface Expr permits Name, Bool, IntE, LongE, DoubleE, StringE, NullE, Ok, Err, Some, NoneE, Construct, Call, Lambda {}

  @JsonTypeName("Name")
  public static final class Name implements Expr { public String name; public Origin origin; }
  @JsonTypeName("Bool")
  public static final class Bool implements Expr { public boolean value; public Origin origin; }
  @JsonTypeName("Int")
  public static final class IntE implements Expr { public int value; public Origin origin; }
  @JsonTypeName("Long")
  public static final class LongE implements Expr { public long value; public Origin origin; }
  @JsonTypeName("Double")
  public static final class DoubleE implements Expr { public double value; public Origin origin; }
  @JsonTypeName("String")
  public static final class StringE implements Expr { public String value; public Origin origin; }
  @JsonTypeName("Null")
  public static final class NullE implements Expr { public Origin origin; }
  @JsonTypeName("Ok")
  public static final class Ok implements Expr { public Expr expr; public Origin origin; }
  @JsonTypeName("Err")
  public static final class Err implements Expr { public Expr expr; public Origin origin; }
  @JsonTypeName("Some")
  public static final class Some implements Expr { public Expr expr; public Origin origin; }
  @JsonTypeName("None")
  public static final class NoneE implements Expr { public Origin origin; }
  @JsonTypeName("Construct")
  public static final class Construct implements Expr { public String typeName; public List<FieldInit> fields; public Origin origin; }
  public static final class FieldInit { public String name; public Expr expr; }
  @JsonTypeName("Call")
  public static final class Call implements Expr { public Expr target; public List<Expr> args; public Origin origin; }
  @JsonTypeName("Lambda")
  public static final class Lambda implements Expr { public List<Param> params; public Type ret; public Block body; public List<String> captures; public Origin origin; }
}
