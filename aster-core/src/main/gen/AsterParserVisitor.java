// Generated from /Users/rpang/IdeaProjects/aster-lang/aster-core/src/main/antlr/AsterParser.g4 by ANTLR 4.13.2
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link AsterParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface AsterParserVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link AsterParser#module}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModule(AsterParser.ModuleContext ctx);
	/**
	 * Visit a parse tree produced by {@link AsterParser#moduleHeader}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModuleHeader(AsterParser.ModuleHeaderContext ctx);
	/**
	 * Visit a parse tree produced by {@link AsterParser#qualifiedName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQualifiedName(AsterParser.QualifiedNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link AsterParser#qualifiedSegment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQualifiedSegment(AsterParser.QualifiedSegmentContext ctx);
	/**
	 * Visit a parse tree produced by {@link AsterParser#topLevelDecl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTopLevelDecl(AsterParser.TopLevelDeclContext ctx);
	/**
	 * Visit a parse tree produced by {@link AsterParser#funcDecl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFuncDecl(AsterParser.FuncDeclContext ctx);
	/**
	 * Visit a parse tree produced by {@link AsterParser#typeParamList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeParamList(AsterParser.TypeParamListContext ctx);
	/**
	 * Visit a parse tree produced by {@link AsterParser#typeParam}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeParam(AsterParser.TypeParamContext ctx);
	/**
	 * Visit a parse tree produced by {@link AsterParser#paramList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParamList(AsterParser.ParamListContext ctx);
	/**
	 * Visit a parse tree produced by {@link AsterParser#param}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParam(AsterParser.ParamContext ctx);
	/**
	 * Visit a parse tree produced by {@link AsterParser#capabilityAnnotation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCapabilityAnnotation(AsterParser.CapabilityAnnotationContext ctx);
	/**
	 * Visit a parse tree produced by {@link AsterParser#dataDecl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDataDecl(AsterParser.DataDeclContext ctx);
	/**
	 * Visit a parse tree produced by {@link AsterParser#fieldList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFieldList(AsterParser.FieldListContext ctx);
	/**
	 * Visit a parse tree produced by {@link AsterParser#field}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitField(AsterParser.FieldContext ctx);
	/**
	 * Visit a parse tree produced by {@link AsterParser#nameIdent}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNameIdent(AsterParser.NameIdentContext ctx);
	/**
	 * Visit a parse tree produced by {@link AsterParser#enumDecl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumDecl(AsterParser.EnumDeclContext ctx);
	/**
	 * Visit a parse tree produced by {@link AsterParser#article}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArticle(AsterParser.ArticleContext ctx);
	/**
	 * Visit a parse tree produced by {@link AsterParser#variantList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariantList(AsterParser.VariantListContext ctx);
	/**
	 * Visit a parse tree produced by {@link AsterParser#importDecl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImportDecl(AsterParser.ImportDeclContext ctx);
	/**
	 * Visit a parse tree produced by {@link AsterParser#importAlias}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImportAlias(AsterParser.ImportAliasContext ctx);
	/**
	 * Visit a parse tree produced by {@link AsterParser#typeDecl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeDecl(AsterParser.TypeDeclContext ctx);
	/**
	 * Visit a parse tree produced by {@link AsterParser#annotation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotation(AsterParser.AnnotationContext ctx);
	/**
	 * Visit a parse tree produced by {@link AsterParser#annotationArgs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotationArgs(AsterParser.AnnotationArgsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code NamedAnnotationArg}
	 * labeled alternative in {@link AsterParser#annotationArg}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamedAnnotationArg(AsterParser.NamedAnnotationArgContext ctx);
	/**
	 * Visit a parse tree produced by the {@code PositionalAnnotationArg}
	 * labeled alternative in {@link AsterParser#annotationArg}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPositionalAnnotationArg(AsterParser.PositionalAnnotationArgContext ctx);
	/**
	 * Visit a parse tree produced by {@link AsterParser#annotationValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotationValue(AsterParser.AnnotationValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link AsterParser#annotatedType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotatedType(AsterParser.AnnotatedTypeContext ctx);
	/**
	 * Visit a parse tree produced by the {@code OfGenericType}
	 * labeled alternative in {@link AsterParser#type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOfGenericType(AsterParser.OfGenericTypeContext ctx);
	/**
	 * Visit a parse tree produced by the {@code GenericType}
	 * labeled alternative in {@link AsterParser#type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGenericType(AsterParser.GenericTypeContext ctx);
	/**
	 * Visit a parse tree produced by the {@code TypeName}
	 * labeled alternative in {@link AsterParser#type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeName(AsterParser.TypeNameContext ctx);
	/**
	 * Visit a parse tree produced by the {@code FuncType}
	 * labeled alternative in {@link AsterParser#type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFuncType(AsterParser.FuncTypeContext ctx);
	/**
	 * Visit a parse tree produced by the {@code MaybeType}
	 * labeled alternative in {@link AsterParser#type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMaybeType(AsterParser.MaybeTypeContext ctx);
	/**
	 * Visit a parse tree produced by the {@code MapType}
	 * labeled alternative in {@link AsterParser#type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMapType(AsterParser.MapTypeContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ParenType}
	 * labeled alternative in {@link AsterParser#type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParenType(AsterParser.ParenTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link AsterParser#typeList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeList(AsterParser.TypeListContext ctx);
	/**
	 * Visit a parse tree produced by {@link AsterParser#block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlock(AsterParser.BlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link AsterParser#stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStmt(AsterParser.StmtContext ctx);
	/**
	 * Visit a parse tree produced by the {@code LetLambdaStmt}
	 * labeled alternative in {@link AsterParser#letStmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLetLambdaStmt(AsterParser.LetLambdaStmtContext ctx);
	/**
	 * Visit a parse tree produced by the {@code LetExprStmt}
	 * labeled alternative in {@link AsterParser#letStmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLetExprStmt(AsterParser.LetExprStmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link AsterParser#defineStmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDefineStmt(AsterParser.DefineStmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link AsterParser#startStmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStartStmt(AsterParser.StartStmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link AsterParser#waitStmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWaitStmt(AsterParser.WaitStmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link AsterParser#returnStmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReturnStmt(AsterParser.ReturnStmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link AsterParser#ifStmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIfStmt(AsterParser.IfStmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link AsterParser#matchStmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMatchStmt(AsterParser.MatchStmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link AsterParser#matchCase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMatchCase(AsterParser.MatchCaseContext ctx);
	/**
	 * Visit a parse tree produced by the {@code PatternNull}
	 * labeled alternative in {@link AsterParser#pattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPatternNull(AsterParser.PatternNullContext ctx);
	/**
	 * Visit a parse tree produced by the {@code PatternCtor}
	 * labeled alternative in {@link AsterParser#pattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPatternCtor(AsterParser.PatternCtorContext ctx);
	/**
	 * Visit a parse tree produced by the {@code PatternInt}
	 * labeled alternative in {@link AsterParser#pattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPatternInt(AsterParser.PatternIntContext ctx);
	/**
	 * Visit a parse tree produced by the {@code PatternName}
	 * labeled alternative in {@link AsterParser#pattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPatternName(AsterParser.PatternNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link AsterParser#exprStmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExprStmt(AsterParser.ExprStmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link AsterParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpr(AsterParser.ExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link AsterParser#comparisonExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComparisonExpr(AsterParser.ComparisonExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link AsterParser#additiveExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdditiveExpr(AsterParser.AdditiveExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link AsterParser#multiplicativeExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiplicativeExpr(AsterParser.MultiplicativeExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code NotExpr}
	 * labeled alternative in {@link AsterParser#unaryExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNotExpr(AsterParser.NotExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code PostfixUnary}
	 * labeled alternative in {@link AsterParser#unaryExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPostfixUnary(AsterParser.PostfixUnaryContext ctx);
	/**
	 * Visit a parse tree produced by {@link AsterParser#postfixExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPostfixExpr(AsterParser.PostfixExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CallSuffix}
	 * labeled alternative in {@link AsterParser#postfixSuffix}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCallSuffix(AsterParser.CallSuffixContext ctx);
	/**
	 * Visit a parse tree produced by the {@code MemberSuffix}
	 * labeled alternative in {@link AsterParser#postfixSuffix}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMemberSuffix(AsterParser.MemberSuffixContext ctx);
	/**
	 * Visit a parse tree produced by {@link AsterParser#argumentList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArgumentList(AsterParser.ArgumentListContext ctx);
	/**
	 * Visit a parse tree produced by the {@code LambdaExprAlt}
	 * labeled alternative in {@link AsterParser#primaryExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLambdaExprAlt(AsterParser.LambdaExprAltContext ctx);
	/**
	 * Visit a parse tree produced by the {@code OperatorCallExpr}
	 * labeled alternative in {@link AsterParser#primaryExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOperatorCallExpr(AsterParser.OperatorCallExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ConstructExprAlt}
	 * labeled alternative in {@link AsterParser#primaryExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstructExprAlt(AsterParser.ConstructExprAltContext ctx);
	/**
	 * Visit a parse tree produced by the {@code WrapExprAlt}
	 * labeled alternative in {@link AsterParser#primaryExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWrapExprAlt(AsterParser.WrapExprAltContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ListLiteralExpr}
	 * labeled alternative in {@link AsterParser#primaryExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitListLiteralExpr(AsterParser.ListLiteralExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code VarExpr}
	 * labeled alternative in {@link AsterParser#primaryExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVarExpr(AsterParser.VarExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code TypeIdentExpr}
	 * labeled alternative in {@link AsterParser#primaryExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeIdentExpr(AsterParser.TypeIdentExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code MapIdentExpr}
	 * labeled alternative in {@link AsterParser#primaryExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMapIdentExpr(AsterParser.MapIdentExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code StringExpr}
	 * labeled alternative in {@link AsterParser#primaryExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStringExpr(AsterParser.StringExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code IntExpr}
	 * labeled alternative in {@link AsterParser#primaryExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIntExpr(AsterParser.IntExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code FloatExpr}
	 * labeled alternative in {@link AsterParser#primaryExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFloatExpr(AsterParser.FloatExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code LongExpr}
	 * labeled alternative in {@link AsterParser#primaryExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLongExpr(AsterParser.LongExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code BoolExpr}
	 * labeled alternative in {@link AsterParser#primaryExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBoolExpr(AsterParser.BoolExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code NullExpr}
	 * labeled alternative in {@link AsterParser#primaryExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNullExpr(AsterParser.NullExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ParenExpr}
	 * labeled alternative in {@link AsterParser#primaryExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParenExpr(AsterParser.ParenExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link AsterParser#constructExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstructExpr(AsterParser.ConstructExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link AsterParser#constructFieldList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstructFieldList(AsterParser.ConstructFieldListContext ctx);
	/**
	 * Visit a parse tree produced by {@link AsterParser#constructField}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstructField(AsterParser.ConstructFieldContext ctx);
	/**
	 * Visit a parse tree produced by {@link AsterParser#operatorCall}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOperatorCall(AsterParser.OperatorCallContext ctx);
	/**
	 * Visit a parse tree produced by {@link AsterParser#wrapExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWrapExpr(AsterParser.WrapExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link AsterParser#listLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitListLiteral(AsterParser.ListLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link AsterParser#lambdaExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLambdaExpr(AsterParser.LambdaExprContext ctx);
}