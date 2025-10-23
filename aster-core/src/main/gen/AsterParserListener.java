// Generated from /Users/rpang/IdeaProjects/aster-lang/aster-core/src/main/antlr/AsterParser.g4 by ANTLR 4.13.2
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link AsterParser}.
 */
public interface AsterParserListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link AsterParser#module}.
	 * @param ctx the parse tree
	 */
	void enterModule(AsterParser.ModuleContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsterParser#module}.
	 * @param ctx the parse tree
	 */
	void exitModule(AsterParser.ModuleContext ctx);
	/**
	 * Enter a parse tree produced by {@link AsterParser#moduleHeader}.
	 * @param ctx the parse tree
	 */
	void enterModuleHeader(AsterParser.ModuleHeaderContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsterParser#moduleHeader}.
	 * @param ctx the parse tree
	 */
	void exitModuleHeader(AsterParser.ModuleHeaderContext ctx);
	/**
	 * Enter a parse tree produced by {@link AsterParser#qualifiedName}.
	 * @param ctx the parse tree
	 */
	void enterQualifiedName(AsterParser.QualifiedNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsterParser#qualifiedName}.
	 * @param ctx the parse tree
	 */
	void exitQualifiedName(AsterParser.QualifiedNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link AsterParser#qualifiedSegment}.
	 * @param ctx the parse tree
	 */
	void enterQualifiedSegment(AsterParser.QualifiedSegmentContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsterParser#qualifiedSegment}.
	 * @param ctx the parse tree
	 */
	void exitQualifiedSegment(AsterParser.QualifiedSegmentContext ctx);
	/**
	 * Enter a parse tree produced by {@link AsterParser#topLevelDecl}.
	 * @param ctx the parse tree
	 */
	void enterTopLevelDecl(AsterParser.TopLevelDeclContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsterParser#topLevelDecl}.
	 * @param ctx the parse tree
	 */
	void exitTopLevelDecl(AsterParser.TopLevelDeclContext ctx);
	/**
	 * Enter a parse tree produced by {@link AsterParser#funcDecl}.
	 * @param ctx the parse tree
	 */
	void enterFuncDecl(AsterParser.FuncDeclContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsterParser#funcDecl}.
	 * @param ctx the parse tree
	 */
	void exitFuncDecl(AsterParser.FuncDeclContext ctx);
	/**
	 * Enter a parse tree produced by {@link AsterParser#typeParamList}.
	 * @param ctx the parse tree
	 */
	void enterTypeParamList(AsterParser.TypeParamListContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsterParser#typeParamList}.
	 * @param ctx the parse tree
	 */
	void exitTypeParamList(AsterParser.TypeParamListContext ctx);
	/**
	 * Enter a parse tree produced by {@link AsterParser#typeParam}.
	 * @param ctx the parse tree
	 */
	void enterTypeParam(AsterParser.TypeParamContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsterParser#typeParam}.
	 * @param ctx the parse tree
	 */
	void exitTypeParam(AsterParser.TypeParamContext ctx);
	/**
	 * Enter a parse tree produced by {@link AsterParser#paramList}.
	 * @param ctx the parse tree
	 */
	void enterParamList(AsterParser.ParamListContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsterParser#paramList}.
	 * @param ctx the parse tree
	 */
	void exitParamList(AsterParser.ParamListContext ctx);
	/**
	 * Enter a parse tree produced by {@link AsterParser#param}.
	 * @param ctx the parse tree
	 */
	void enterParam(AsterParser.ParamContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsterParser#param}.
	 * @param ctx the parse tree
	 */
	void exitParam(AsterParser.ParamContext ctx);
	/**
	 * Enter a parse tree produced by {@link AsterParser#capabilityAnnotation}.
	 * @param ctx the parse tree
	 */
	void enterCapabilityAnnotation(AsterParser.CapabilityAnnotationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsterParser#capabilityAnnotation}.
	 * @param ctx the parse tree
	 */
	void exitCapabilityAnnotation(AsterParser.CapabilityAnnotationContext ctx);
	/**
	 * Enter a parse tree produced by {@link AsterParser#dataDecl}.
	 * @param ctx the parse tree
	 */
	void enterDataDecl(AsterParser.DataDeclContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsterParser#dataDecl}.
	 * @param ctx the parse tree
	 */
	void exitDataDecl(AsterParser.DataDeclContext ctx);
	/**
	 * Enter a parse tree produced by {@link AsterParser#fieldList}.
	 * @param ctx the parse tree
	 */
	void enterFieldList(AsterParser.FieldListContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsterParser#fieldList}.
	 * @param ctx the parse tree
	 */
	void exitFieldList(AsterParser.FieldListContext ctx);
	/**
	 * Enter a parse tree produced by {@link AsterParser#field}.
	 * @param ctx the parse tree
	 */
	void enterField(AsterParser.FieldContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsterParser#field}.
	 * @param ctx the parse tree
	 */
	void exitField(AsterParser.FieldContext ctx);
	/**
	 * Enter a parse tree produced by {@link AsterParser#nameIdent}.
	 * @param ctx the parse tree
	 */
	void enterNameIdent(AsterParser.NameIdentContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsterParser#nameIdent}.
	 * @param ctx the parse tree
	 */
	void exitNameIdent(AsterParser.NameIdentContext ctx);
	/**
	 * Enter a parse tree produced by {@link AsterParser#enumDecl}.
	 * @param ctx the parse tree
	 */
	void enterEnumDecl(AsterParser.EnumDeclContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsterParser#enumDecl}.
	 * @param ctx the parse tree
	 */
	void exitEnumDecl(AsterParser.EnumDeclContext ctx);
	/**
	 * Enter a parse tree produced by {@link AsterParser#article}.
	 * @param ctx the parse tree
	 */
	void enterArticle(AsterParser.ArticleContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsterParser#article}.
	 * @param ctx the parse tree
	 */
	void exitArticle(AsterParser.ArticleContext ctx);
	/**
	 * Enter a parse tree produced by {@link AsterParser#variantList}.
	 * @param ctx the parse tree
	 */
	void enterVariantList(AsterParser.VariantListContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsterParser#variantList}.
	 * @param ctx the parse tree
	 */
	void exitVariantList(AsterParser.VariantListContext ctx);
	/**
	 * Enter a parse tree produced by {@link AsterParser#importDecl}.
	 * @param ctx the parse tree
	 */
	void enterImportDecl(AsterParser.ImportDeclContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsterParser#importDecl}.
	 * @param ctx the parse tree
	 */
	void exitImportDecl(AsterParser.ImportDeclContext ctx);
	/**
	 * Enter a parse tree produced by {@link AsterParser#importAlias}.
	 * @param ctx the parse tree
	 */
	void enterImportAlias(AsterParser.ImportAliasContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsterParser#importAlias}.
	 * @param ctx the parse tree
	 */
	void exitImportAlias(AsterParser.ImportAliasContext ctx);
	/**
	 * Enter a parse tree produced by {@link AsterParser#typeDecl}.
	 * @param ctx the parse tree
	 */
	void enterTypeDecl(AsterParser.TypeDeclContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsterParser#typeDecl}.
	 * @param ctx the parse tree
	 */
	void exitTypeDecl(AsterParser.TypeDeclContext ctx);
	/**
	 * Enter a parse tree produced by {@link AsterParser#annotation}.
	 * @param ctx the parse tree
	 */
	void enterAnnotation(AsterParser.AnnotationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsterParser#annotation}.
	 * @param ctx the parse tree
	 */
	void exitAnnotation(AsterParser.AnnotationContext ctx);
	/**
	 * Enter a parse tree produced by {@link AsterParser#annotationArgs}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationArgs(AsterParser.AnnotationArgsContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsterParser#annotationArgs}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationArgs(AsterParser.AnnotationArgsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code NamedAnnotationArg}
	 * labeled alternative in {@link AsterParser#annotationArg}.
	 * @param ctx the parse tree
	 */
	void enterNamedAnnotationArg(AsterParser.NamedAnnotationArgContext ctx);
	/**
	 * Exit a parse tree produced by the {@code NamedAnnotationArg}
	 * labeled alternative in {@link AsterParser#annotationArg}.
	 * @param ctx the parse tree
	 */
	void exitNamedAnnotationArg(AsterParser.NamedAnnotationArgContext ctx);
	/**
	 * Enter a parse tree produced by the {@code PositionalAnnotationArg}
	 * labeled alternative in {@link AsterParser#annotationArg}.
	 * @param ctx the parse tree
	 */
	void enterPositionalAnnotationArg(AsterParser.PositionalAnnotationArgContext ctx);
	/**
	 * Exit a parse tree produced by the {@code PositionalAnnotationArg}
	 * labeled alternative in {@link AsterParser#annotationArg}.
	 * @param ctx the parse tree
	 */
	void exitPositionalAnnotationArg(AsterParser.PositionalAnnotationArgContext ctx);
	/**
	 * Enter a parse tree produced by {@link AsterParser#annotationValue}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationValue(AsterParser.AnnotationValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsterParser#annotationValue}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationValue(AsterParser.AnnotationValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link AsterParser#annotatedType}.
	 * @param ctx the parse tree
	 */
	void enterAnnotatedType(AsterParser.AnnotatedTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsterParser#annotatedType}.
	 * @param ctx the parse tree
	 */
	void exitAnnotatedType(AsterParser.AnnotatedTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code OfGenericType}
	 * labeled alternative in {@link AsterParser#type}.
	 * @param ctx the parse tree
	 */
	void enterOfGenericType(AsterParser.OfGenericTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code OfGenericType}
	 * labeled alternative in {@link AsterParser#type}.
	 * @param ctx the parse tree
	 */
	void exitOfGenericType(AsterParser.OfGenericTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code GenericType}
	 * labeled alternative in {@link AsterParser#type}.
	 * @param ctx the parse tree
	 */
	void enterGenericType(AsterParser.GenericTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code GenericType}
	 * labeled alternative in {@link AsterParser#type}.
	 * @param ctx the parse tree
	 */
	void exitGenericType(AsterParser.GenericTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code TypeName}
	 * labeled alternative in {@link AsterParser#type}.
	 * @param ctx the parse tree
	 */
	void enterTypeName(AsterParser.TypeNameContext ctx);
	/**
	 * Exit a parse tree produced by the {@code TypeName}
	 * labeled alternative in {@link AsterParser#type}.
	 * @param ctx the parse tree
	 */
	void exitTypeName(AsterParser.TypeNameContext ctx);
	/**
	 * Enter a parse tree produced by the {@code FuncType}
	 * labeled alternative in {@link AsterParser#type}.
	 * @param ctx the parse tree
	 */
	void enterFuncType(AsterParser.FuncTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code FuncType}
	 * labeled alternative in {@link AsterParser#type}.
	 * @param ctx the parse tree
	 */
	void exitFuncType(AsterParser.FuncTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code MaybeType}
	 * labeled alternative in {@link AsterParser#type}.
	 * @param ctx the parse tree
	 */
	void enterMaybeType(AsterParser.MaybeTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code MaybeType}
	 * labeled alternative in {@link AsterParser#type}.
	 * @param ctx the parse tree
	 */
	void exitMaybeType(AsterParser.MaybeTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code MapType}
	 * labeled alternative in {@link AsterParser#type}.
	 * @param ctx the parse tree
	 */
	void enterMapType(AsterParser.MapTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code MapType}
	 * labeled alternative in {@link AsterParser#type}.
	 * @param ctx the parse tree
	 */
	void exitMapType(AsterParser.MapTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ParenType}
	 * labeled alternative in {@link AsterParser#type}.
	 * @param ctx the parse tree
	 */
	void enterParenType(AsterParser.ParenTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ParenType}
	 * labeled alternative in {@link AsterParser#type}.
	 * @param ctx the parse tree
	 */
	void exitParenType(AsterParser.ParenTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link AsterParser#typeList}.
	 * @param ctx the parse tree
	 */
	void enterTypeList(AsterParser.TypeListContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsterParser#typeList}.
	 * @param ctx the parse tree
	 */
	void exitTypeList(AsterParser.TypeListContext ctx);
	/**
	 * Enter a parse tree produced by {@link AsterParser#block}.
	 * @param ctx the parse tree
	 */
	void enterBlock(AsterParser.BlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsterParser#block}.
	 * @param ctx the parse tree
	 */
	void exitBlock(AsterParser.BlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link AsterParser#stmt}.
	 * @param ctx the parse tree
	 */
	void enterStmt(AsterParser.StmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsterParser#stmt}.
	 * @param ctx the parse tree
	 */
	void exitStmt(AsterParser.StmtContext ctx);
	/**
	 * Enter a parse tree produced by the {@code LetLambdaStmt}
	 * labeled alternative in {@link AsterParser#letStmt}.
	 * @param ctx the parse tree
	 */
	void enterLetLambdaStmt(AsterParser.LetLambdaStmtContext ctx);
	/**
	 * Exit a parse tree produced by the {@code LetLambdaStmt}
	 * labeled alternative in {@link AsterParser#letStmt}.
	 * @param ctx the parse tree
	 */
	void exitLetLambdaStmt(AsterParser.LetLambdaStmtContext ctx);
	/**
	 * Enter a parse tree produced by the {@code LetExprStmt}
	 * labeled alternative in {@link AsterParser#letStmt}.
	 * @param ctx the parse tree
	 */
	void enterLetExprStmt(AsterParser.LetExprStmtContext ctx);
	/**
	 * Exit a parse tree produced by the {@code LetExprStmt}
	 * labeled alternative in {@link AsterParser#letStmt}.
	 * @param ctx the parse tree
	 */
	void exitLetExprStmt(AsterParser.LetExprStmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link AsterParser#defineStmt}.
	 * @param ctx the parse tree
	 */
	void enterDefineStmt(AsterParser.DefineStmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsterParser#defineStmt}.
	 * @param ctx the parse tree
	 */
	void exitDefineStmt(AsterParser.DefineStmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link AsterParser#startStmt}.
	 * @param ctx the parse tree
	 */
	void enterStartStmt(AsterParser.StartStmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsterParser#startStmt}.
	 * @param ctx the parse tree
	 */
	void exitStartStmt(AsterParser.StartStmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link AsterParser#waitStmt}.
	 * @param ctx the parse tree
	 */
	void enterWaitStmt(AsterParser.WaitStmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsterParser#waitStmt}.
	 * @param ctx the parse tree
	 */
	void exitWaitStmt(AsterParser.WaitStmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link AsterParser#returnStmt}.
	 * @param ctx the parse tree
	 */
	void enterReturnStmt(AsterParser.ReturnStmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsterParser#returnStmt}.
	 * @param ctx the parse tree
	 */
	void exitReturnStmt(AsterParser.ReturnStmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link AsterParser#ifStmt}.
	 * @param ctx the parse tree
	 */
	void enterIfStmt(AsterParser.IfStmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsterParser#ifStmt}.
	 * @param ctx the parse tree
	 */
	void exitIfStmt(AsterParser.IfStmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link AsterParser#matchStmt}.
	 * @param ctx the parse tree
	 */
	void enterMatchStmt(AsterParser.MatchStmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsterParser#matchStmt}.
	 * @param ctx the parse tree
	 */
	void exitMatchStmt(AsterParser.MatchStmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link AsterParser#matchCase}.
	 * @param ctx the parse tree
	 */
	void enterMatchCase(AsterParser.MatchCaseContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsterParser#matchCase}.
	 * @param ctx the parse tree
	 */
	void exitMatchCase(AsterParser.MatchCaseContext ctx);
	/**
	 * Enter a parse tree produced by the {@code PatternNull}
	 * labeled alternative in {@link AsterParser#pattern}.
	 * @param ctx the parse tree
	 */
	void enterPatternNull(AsterParser.PatternNullContext ctx);
	/**
	 * Exit a parse tree produced by the {@code PatternNull}
	 * labeled alternative in {@link AsterParser#pattern}.
	 * @param ctx the parse tree
	 */
	void exitPatternNull(AsterParser.PatternNullContext ctx);
	/**
	 * Enter a parse tree produced by the {@code PatternCtor}
	 * labeled alternative in {@link AsterParser#pattern}.
	 * @param ctx the parse tree
	 */
	void enterPatternCtor(AsterParser.PatternCtorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code PatternCtor}
	 * labeled alternative in {@link AsterParser#pattern}.
	 * @param ctx the parse tree
	 */
	void exitPatternCtor(AsterParser.PatternCtorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code PatternInt}
	 * labeled alternative in {@link AsterParser#pattern}.
	 * @param ctx the parse tree
	 */
	void enterPatternInt(AsterParser.PatternIntContext ctx);
	/**
	 * Exit a parse tree produced by the {@code PatternInt}
	 * labeled alternative in {@link AsterParser#pattern}.
	 * @param ctx the parse tree
	 */
	void exitPatternInt(AsterParser.PatternIntContext ctx);
	/**
	 * Enter a parse tree produced by the {@code PatternName}
	 * labeled alternative in {@link AsterParser#pattern}.
	 * @param ctx the parse tree
	 */
	void enterPatternName(AsterParser.PatternNameContext ctx);
	/**
	 * Exit a parse tree produced by the {@code PatternName}
	 * labeled alternative in {@link AsterParser#pattern}.
	 * @param ctx the parse tree
	 */
	void exitPatternName(AsterParser.PatternNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link AsterParser#exprStmt}.
	 * @param ctx the parse tree
	 */
	void enterExprStmt(AsterParser.ExprStmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsterParser#exprStmt}.
	 * @param ctx the parse tree
	 */
	void exitExprStmt(AsterParser.ExprStmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link AsterParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterExpr(AsterParser.ExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsterParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitExpr(AsterParser.ExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link AsterParser#comparisonExpr}.
	 * @param ctx the parse tree
	 */
	void enterComparisonExpr(AsterParser.ComparisonExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsterParser#comparisonExpr}.
	 * @param ctx the parse tree
	 */
	void exitComparisonExpr(AsterParser.ComparisonExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link AsterParser#additiveExpr}.
	 * @param ctx the parse tree
	 */
	void enterAdditiveExpr(AsterParser.AdditiveExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsterParser#additiveExpr}.
	 * @param ctx the parse tree
	 */
	void exitAdditiveExpr(AsterParser.AdditiveExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link AsterParser#multiplicativeExpr}.
	 * @param ctx the parse tree
	 */
	void enterMultiplicativeExpr(AsterParser.MultiplicativeExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsterParser#multiplicativeExpr}.
	 * @param ctx the parse tree
	 */
	void exitMultiplicativeExpr(AsterParser.MultiplicativeExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code NotExpr}
	 * labeled alternative in {@link AsterParser#unaryExpr}.
	 * @param ctx the parse tree
	 */
	void enterNotExpr(AsterParser.NotExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code NotExpr}
	 * labeled alternative in {@link AsterParser#unaryExpr}.
	 * @param ctx the parse tree
	 */
	void exitNotExpr(AsterParser.NotExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code PostfixUnary}
	 * labeled alternative in {@link AsterParser#unaryExpr}.
	 * @param ctx the parse tree
	 */
	void enterPostfixUnary(AsterParser.PostfixUnaryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code PostfixUnary}
	 * labeled alternative in {@link AsterParser#unaryExpr}.
	 * @param ctx the parse tree
	 */
	void exitPostfixUnary(AsterParser.PostfixUnaryContext ctx);
	/**
	 * Enter a parse tree produced by {@link AsterParser#postfixExpr}.
	 * @param ctx the parse tree
	 */
	void enterPostfixExpr(AsterParser.PostfixExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsterParser#postfixExpr}.
	 * @param ctx the parse tree
	 */
	void exitPostfixExpr(AsterParser.PostfixExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CallSuffix}
	 * labeled alternative in {@link AsterParser#postfixSuffix}.
	 * @param ctx the parse tree
	 */
	void enterCallSuffix(AsterParser.CallSuffixContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CallSuffix}
	 * labeled alternative in {@link AsterParser#postfixSuffix}.
	 * @param ctx the parse tree
	 */
	void exitCallSuffix(AsterParser.CallSuffixContext ctx);
	/**
	 * Enter a parse tree produced by the {@code MemberSuffix}
	 * labeled alternative in {@link AsterParser#postfixSuffix}.
	 * @param ctx the parse tree
	 */
	void enterMemberSuffix(AsterParser.MemberSuffixContext ctx);
	/**
	 * Exit a parse tree produced by the {@code MemberSuffix}
	 * labeled alternative in {@link AsterParser#postfixSuffix}.
	 * @param ctx the parse tree
	 */
	void exitMemberSuffix(AsterParser.MemberSuffixContext ctx);
	/**
	 * Enter a parse tree produced by {@link AsterParser#argumentList}.
	 * @param ctx the parse tree
	 */
	void enterArgumentList(AsterParser.ArgumentListContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsterParser#argumentList}.
	 * @param ctx the parse tree
	 */
	void exitArgumentList(AsterParser.ArgumentListContext ctx);
	/**
	 * Enter a parse tree produced by the {@code LambdaExprAlt}
	 * labeled alternative in {@link AsterParser#primaryExpr}.
	 * @param ctx the parse tree
	 */
	void enterLambdaExprAlt(AsterParser.LambdaExprAltContext ctx);
	/**
	 * Exit a parse tree produced by the {@code LambdaExprAlt}
	 * labeled alternative in {@link AsterParser#primaryExpr}.
	 * @param ctx the parse tree
	 */
	void exitLambdaExprAlt(AsterParser.LambdaExprAltContext ctx);
	/**
	 * Enter a parse tree produced by the {@code OperatorCallExpr}
	 * labeled alternative in {@link AsterParser#primaryExpr}.
	 * @param ctx the parse tree
	 */
	void enterOperatorCallExpr(AsterParser.OperatorCallExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code OperatorCallExpr}
	 * labeled alternative in {@link AsterParser#primaryExpr}.
	 * @param ctx the parse tree
	 */
	void exitOperatorCallExpr(AsterParser.OperatorCallExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ConstructExprAlt}
	 * labeled alternative in {@link AsterParser#primaryExpr}.
	 * @param ctx the parse tree
	 */
	void enterConstructExprAlt(AsterParser.ConstructExprAltContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ConstructExprAlt}
	 * labeled alternative in {@link AsterParser#primaryExpr}.
	 * @param ctx the parse tree
	 */
	void exitConstructExprAlt(AsterParser.ConstructExprAltContext ctx);
	/**
	 * Enter a parse tree produced by the {@code WrapExprAlt}
	 * labeled alternative in {@link AsterParser#primaryExpr}.
	 * @param ctx the parse tree
	 */
	void enterWrapExprAlt(AsterParser.WrapExprAltContext ctx);
	/**
	 * Exit a parse tree produced by the {@code WrapExprAlt}
	 * labeled alternative in {@link AsterParser#primaryExpr}.
	 * @param ctx the parse tree
	 */
	void exitWrapExprAlt(AsterParser.WrapExprAltContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ListLiteralExpr}
	 * labeled alternative in {@link AsterParser#primaryExpr}.
	 * @param ctx the parse tree
	 */
	void enterListLiteralExpr(AsterParser.ListLiteralExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ListLiteralExpr}
	 * labeled alternative in {@link AsterParser#primaryExpr}.
	 * @param ctx the parse tree
	 */
	void exitListLiteralExpr(AsterParser.ListLiteralExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code VarExpr}
	 * labeled alternative in {@link AsterParser#primaryExpr}.
	 * @param ctx the parse tree
	 */
	void enterVarExpr(AsterParser.VarExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code VarExpr}
	 * labeled alternative in {@link AsterParser#primaryExpr}.
	 * @param ctx the parse tree
	 */
	void exitVarExpr(AsterParser.VarExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code TypeIdentExpr}
	 * labeled alternative in {@link AsterParser#primaryExpr}.
	 * @param ctx the parse tree
	 */
	void enterTypeIdentExpr(AsterParser.TypeIdentExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code TypeIdentExpr}
	 * labeled alternative in {@link AsterParser#primaryExpr}.
	 * @param ctx the parse tree
	 */
	void exitTypeIdentExpr(AsterParser.TypeIdentExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code MapIdentExpr}
	 * labeled alternative in {@link AsterParser#primaryExpr}.
	 * @param ctx the parse tree
	 */
	void enterMapIdentExpr(AsterParser.MapIdentExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code MapIdentExpr}
	 * labeled alternative in {@link AsterParser#primaryExpr}.
	 * @param ctx the parse tree
	 */
	void exitMapIdentExpr(AsterParser.MapIdentExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code StringExpr}
	 * labeled alternative in {@link AsterParser#primaryExpr}.
	 * @param ctx the parse tree
	 */
	void enterStringExpr(AsterParser.StringExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code StringExpr}
	 * labeled alternative in {@link AsterParser#primaryExpr}.
	 * @param ctx the parse tree
	 */
	void exitStringExpr(AsterParser.StringExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code IntExpr}
	 * labeled alternative in {@link AsterParser#primaryExpr}.
	 * @param ctx the parse tree
	 */
	void enterIntExpr(AsterParser.IntExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code IntExpr}
	 * labeled alternative in {@link AsterParser#primaryExpr}.
	 * @param ctx the parse tree
	 */
	void exitIntExpr(AsterParser.IntExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code FloatExpr}
	 * labeled alternative in {@link AsterParser#primaryExpr}.
	 * @param ctx the parse tree
	 */
	void enterFloatExpr(AsterParser.FloatExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code FloatExpr}
	 * labeled alternative in {@link AsterParser#primaryExpr}.
	 * @param ctx the parse tree
	 */
	void exitFloatExpr(AsterParser.FloatExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code LongExpr}
	 * labeled alternative in {@link AsterParser#primaryExpr}.
	 * @param ctx the parse tree
	 */
	void enterLongExpr(AsterParser.LongExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code LongExpr}
	 * labeled alternative in {@link AsterParser#primaryExpr}.
	 * @param ctx the parse tree
	 */
	void exitLongExpr(AsterParser.LongExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code BoolExpr}
	 * labeled alternative in {@link AsterParser#primaryExpr}.
	 * @param ctx the parse tree
	 */
	void enterBoolExpr(AsterParser.BoolExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code BoolExpr}
	 * labeled alternative in {@link AsterParser#primaryExpr}.
	 * @param ctx the parse tree
	 */
	void exitBoolExpr(AsterParser.BoolExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code NullExpr}
	 * labeled alternative in {@link AsterParser#primaryExpr}.
	 * @param ctx the parse tree
	 */
	void enterNullExpr(AsterParser.NullExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code NullExpr}
	 * labeled alternative in {@link AsterParser#primaryExpr}.
	 * @param ctx the parse tree
	 */
	void exitNullExpr(AsterParser.NullExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ParenExpr}
	 * labeled alternative in {@link AsterParser#primaryExpr}.
	 * @param ctx the parse tree
	 */
	void enterParenExpr(AsterParser.ParenExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ParenExpr}
	 * labeled alternative in {@link AsterParser#primaryExpr}.
	 * @param ctx the parse tree
	 */
	void exitParenExpr(AsterParser.ParenExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link AsterParser#constructExpr}.
	 * @param ctx the parse tree
	 */
	void enterConstructExpr(AsterParser.ConstructExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsterParser#constructExpr}.
	 * @param ctx the parse tree
	 */
	void exitConstructExpr(AsterParser.ConstructExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link AsterParser#constructFieldList}.
	 * @param ctx the parse tree
	 */
	void enterConstructFieldList(AsterParser.ConstructFieldListContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsterParser#constructFieldList}.
	 * @param ctx the parse tree
	 */
	void exitConstructFieldList(AsterParser.ConstructFieldListContext ctx);
	/**
	 * Enter a parse tree produced by {@link AsterParser#constructField}.
	 * @param ctx the parse tree
	 */
	void enterConstructField(AsterParser.ConstructFieldContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsterParser#constructField}.
	 * @param ctx the parse tree
	 */
	void exitConstructField(AsterParser.ConstructFieldContext ctx);
	/**
	 * Enter a parse tree produced by {@link AsterParser#operatorCall}.
	 * @param ctx the parse tree
	 */
	void enterOperatorCall(AsterParser.OperatorCallContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsterParser#operatorCall}.
	 * @param ctx the parse tree
	 */
	void exitOperatorCall(AsterParser.OperatorCallContext ctx);
	/**
	 * Enter a parse tree produced by {@link AsterParser#wrapExpr}.
	 * @param ctx the parse tree
	 */
	void enterWrapExpr(AsterParser.WrapExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsterParser#wrapExpr}.
	 * @param ctx the parse tree
	 */
	void exitWrapExpr(AsterParser.WrapExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link AsterParser#listLiteral}.
	 * @param ctx the parse tree
	 */
	void enterListLiteral(AsterParser.ListLiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsterParser#listLiteral}.
	 * @param ctx the parse tree
	 */
	void exitListLiteral(AsterParser.ListLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link AsterParser#lambdaExpr}.
	 * @param ctx the parse tree
	 */
	void enterLambdaExpr(AsterParser.LambdaExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsterParser#lambdaExpr}.
	 * @param ctx the parse tree
	 */
	void exitLambdaExpr(AsterParser.LambdaExprContext ctx);
}