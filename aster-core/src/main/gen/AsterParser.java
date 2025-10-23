// Generated from /Users/rpang/IdeaProjects/aster-lang/aster-core/src/main/antlr/AsterParser.g4 by ANTLR 4.13.2
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue", "this-escape"})
public class AsterParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.13.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		DOT=1, COLON=2, COMMA=3, LPAREN=4, RPAREN=5, LBRACKET=6, RBRACKET=7, LTE=8, 
		GTE=9, NEQ=10, EQUALS=11, PLUS=12, STAR=13, MINUS=14, SLASH=15, LT=16, 
		GT=17, QUESTION=18, AT=19, ARROW=20, STRING_LITERAL=21, BOOL_LITERAL=22, 
		NULL_LITERAL=23, LONG_LITERAL=24, FLOAT_LITERAL=25, INT_LITERAL=26, THIS=27, 
		MODULE=28, IS=29, TO=30, WITH=31, AND=32, OR=33, PRODUCE=34, DEFINE=35, 
		TYPE=36, AS=37, ONE=38, OF=39, USE=40, LET=41, BE=42, RETURN=43, IF=44, 
		ELSE=45, MATCH=46, WHEN=47, NOT=48, START=49, WAIT=50, FOR=51, ASYNC=52, 
		FUNCTION=53, MAP=54, IT=55, PERFORMS=56, TYPE_IDENT=57, IDENT=58, COMMENT=59, 
		NEWLINE=60, WS=61, INDENT=62, DEDENT=63;
	public static final int
		RULE_module = 0, RULE_moduleHeader = 1, RULE_qualifiedName = 2, RULE_qualifiedSegment = 3, 
		RULE_topLevelDecl = 4, RULE_funcDecl = 5, RULE_typeParamList = 6, RULE_typeParam = 7, 
		RULE_paramList = 8, RULE_param = 9, RULE_capabilityAnnotation = 10, RULE_dataDecl = 11, 
		RULE_fieldList = 12, RULE_field = 13, RULE_nameIdent = 14, RULE_enumDecl = 15, 
		RULE_article = 16, RULE_variantList = 17, RULE_importDecl = 18, RULE_importAlias = 19, 
		RULE_typeDecl = 20, RULE_annotation = 21, RULE_annotationArgs = 22, RULE_annotationArg = 23, 
		RULE_annotationValue = 24, RULE_annotatedType = 25, RULE_type = 26, RULE_typeList = 27, 
		RULE_block = 28, RULE_stmt = 29, RULE_letStmt = 30, RULE_defineStmt = 31, 
		RULE_startStmt = 32, RULE_waitStmt = 33, RULE_returnStmt = 34, RULE_ifStmt = 35, 
		RULE_matchStmt = 36, RULE_matchCase = 37, RULE_pattern = 38, RULE_exprStmt = 39, 
		RULE_expr = 40, RULE_comparisonExpr = 41, RULE_additiveExpr = 42, RULE_multiplicativeExpr = 43, 
		RULE_unaryExpr = 44, RULE_postfixExpr = 45, RULE_postfixSuffix = 46, RULE_argumentList = 47, 
		RULE_primaryExpr = 48, RULE_constructExpr = 49, RULE_constructFieldList = 50, 
		RULE_constructField = 51, RULE_operatorCall = 52, RULE_wrapExpr = 53, 
		RULE_listLiteral = 54, RULE_lambdaExpr = 55;
	private static String[] makeRuleNames() {
		return new String[] {
			"module", "moduleHeader", "qualifiedName", "qualifiedSegment", "topLevelDecl", 
			"funcDecl", "typeParamList", "typeParam", "paramList", "param", "capabilityAnnotation", 
			"dataDecl", "fieldList", "field", "nameIdent", "enumDecl", "article", 
			"variantList", "importDecl", "importAlias", "typeDecl", "annotation", 
			"annotationArgs", "annotationArg", "annotationValue", "annotatedType", 
			"type", "typeList", "block", "stmt", "letStmt", "defineStmt", "startStmt", 
			"waitStmt", "returnStmt", "ifStmt", "matchStmt", "matchCase", "pattern", 
			"exprStmt", "expr", "comparisonExpr", "additiveExpr", "multiplicativeExpr", 
			"unaryExpr", "postfixExpr", "postfixSuffix", "argumentList", "primaryExpr", 
			"constructExpr", "constructFieldList", "constructField", "operatorCall", 
			"wrapExpr", "listLiteral", "lambdaExpr"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'.'", "':'", "','", "'('", "')'", "'['", "']'", "'<='", "'>='", 
			"'!='", "'='", "'+'", "'*'", "'-'", "'/'", "'<'", "'>'", "'?'", "'@'", 
			"'->'", null, null, "'null'", null, null, null, null, "'module'", "'is'", 
			null, "'with'", "'and'", "'or'", "'produce'", "'Define'", "'type'", "'as'", 
			"'one'", "'of'", null, "'Let'", "'be'", "'Return'", "'If'", null, "'Match'", 
			"'When'", "'not'", "'Start'", "'Wait'", "'for'", "'async'", "'function'", 
			"'Map'", "'It'", "'performs'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "DOT", "COLON", "COMMA", "LPAREN", "RPAREN", "LBRACKET", "RBRACKET", 
			"LTE", "GTE", "NEQ", "EQUALS", "PLUS", "STAR", "MINUS", "SLASH", "LT", 
			"GT", "QUESTION", "AT", "ARROW", "STRING_LITERAL", "BOOL_LITERAL", "NULL_LITERAL", 
			"LONG_LITERAL", "FLOAT_LITERAL", "INT_LITERAL", "THIS", "MODULE", "IS", 
			"TO", "WITH", "AND", "OR", "PRODUCE", "DEFINE", "TYPE", "AS", "ONE", 
			"OF", "USE", "LET", "BE", "RETURN", "IF", "ELSE", "MATCH", "WHEN", "NOT", 
			"START", "WAIT", "FOR", "ASYNC", "FUNCTION", "MAP", "IT", "PERFORMS", 
			"TYPE_IDENT", "IDENT", "COMMENT", "NEWLINE", "WS", "INDENT", "DEDENT"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "AsterParser.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public AsterParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ModuleContext extends ParserRuleContext {
		public TerminalNode EOF() { return getToken(AsterParser.EOF, 0); }
		public List<TerminalNode> NEWLINE() { return getTokens(AsterParser.NEWLINE); }
		public TerminalNode NEWLINE(int i) {
			return getToken(AsterParser.NEWLINE, i);
		}
		public ModuleHeaderContext moduleHeader() {
			return getRuleContext(ModuleHeaderContext.class,0);
		}
		public List<TopLevelDeclContext> topLevelDecl() {
			return getRuleContexts(TopLevelDeclContext.class);
		}
		public TopLevelDeclContext topLevelDecl(int i) {
			return getRuleContext(TopLevelDeclContext.class,i);
		}
		public ModuleContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_module; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterModule(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitModule(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitModule(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ModuleContext module() throws RecognitionException {
		ModuleContext _localctx = new ModuleContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_module);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(115);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,0,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(112);
					match(NEWLINE);
					}
					} 
				}
				setState(117);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,0,_ctx);
			}
			setState(119);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==THIS) {
				{
				setState(118);
				moduleHeader();
				}
			}

			setState(124);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==NEWLINE) {
				{
				{
				setState(121);
				match(NEWLINE);
				}
				}
				setState(126);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(136);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 1203665108992L) != 0)) {
				{
				{
				setState(127);
				topLevelDecl();
				setState(131);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==NEWLINE) {
					{
					{
					setState(128);
					match(NEWLINE);
					}
					}
					setState(133);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				}
				setState(138);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(139);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ModuleHeaderContext extends ParserRuleContext {
		public TerminalNode THIS() { return getToken(AsterParser.THIS, 0); }
		public TerminalNode MODULE() { return getToken(AsterParser.MODULE, 0); }
		public TerminalNode IS() { return getToken(AsterParser.IS, 0); }
		public QualifiedNameContext qualifiedName() {
			return getRuleContext(QualifiedNameContext.class,0);
		}
		public TerminalNode DOT() { return getToken(AsterParser.DOT, 0); }
		public ModuleHeaderContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_moduleHeader; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterModuleHeader(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitModuleHeader(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitModuleHeader(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ModuleHeaderContext moduleHeader() throws RecognitionException {
		ModuleHeaderContext _localctx = new ModuleHeaderContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_moduleHeader);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(141);
			match(THIS);
			setState(142);
			match(MODULE);
			setState(143);
			match(IS);
			setState(144);
			qualifiedName();
			setState(145);
			match(DOT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class QualifiedNameContext extends ParserRuleContext {
		public List<QualifiedSegmentContext> qualifiedSegment() {
			return getRuleContexts(QualifiedSegmentContext.class);
		}
		public QualifiedSegmentContext qualifiedSegment(int i) {
			return getRuleContext(QualifiedSegmentContext.class,i);
		}
		public List<TerminalNode> DOT() { return getTokens(AsterParser.DOT); }
		public TerminalNode DOT(int i) {
			return getToken(AsterParser.DOT, i);
		}
		public QualifiedNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_qualifiedName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterQualifiedName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitQualifiedName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitQualifiedName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QualifiedNameContext qualifiedName() throws RecognitionException {
		QualifiedNameContext _localctx = new QualifiedNameContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_qualifiedName);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(147);
			qualifiedSegment();
			setState(152);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,5,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(148);
					match(DOT);
					setState(149);
					qualifiedSegment();
					}
					} 
				}
				setState(154);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,5,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class QualifiedSegmentContext extends ParserRuleContext {
		public TerminalNode IDENT() { return getToken(AsterParser.IDENT, 0); }
		public TerminalNode TYPE_IDENT() { return getToken(AsterParser.TYPE_IDENT, 0); }
		public QualifiedSegmentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_qualifiedSegment; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterQualifiedSegment(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitQualifiedSegment(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitQualifiedSegment(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QualifiedSegmentContext qualifiedSegment() throws RecognitionException {
		QualifiedSegmentContext _localctx = new QualifiedSegmentContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_qualifiedSegment);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(155);
			_la = _input.LA(1);
			if ( !(_la==TYPE_IDENT || _la==IDENT) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TopLevelDeclContext extends ParserRuleContext {
		public FuncDeclContext funcDecl() {
			return getRuleContext(FuncDeclContext.class,0);
		}
		public DataDeclContext dataDecl() {
			return getRuleContext(DataDeclContext.class,0);
		}
		public EnumDeclContext enumDecl() {
			return getRuleContext(EnumDeclContext.class,0);
		}
		public TypeDeclContext typeDecl() {
			return getRuleContext(TypeDeclContext.class,0);
		}
		public ImportDeclContext importDecl() {
			return getRuleContext(ImportDeclContext.class,0);
		}
		public TopLevelDeclContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_topLevelDecl; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterTopLevelDecl(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitTopLevelDecl(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitTopLevelDecl(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TopLevelDeclContext topLevelDecl() throws RecognitionException {
		TopLevelDeclContext _localctx = new TopLevelDeclContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_topLevelDecl);
		try {
			setState(162);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,6,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(157);
				funcDecl();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(158);
				dataDecl();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(159);
				enumDecl();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(160);
				typeDecl();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(161);
				importDecl();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class FuncDeclContext extends ParserRuleContext {
		public TerminalNode TO() { return getToken(AsterParser.TO, 0); }
		public TerminalNode IDENT() { return getToken(AsterParser.IDENT, 0); }
		public TerminalNode PRODUCE() { return getToken(AsterParser.PRODUCE, 0); }
		public AnnotatedTypeContext annotatedType() {
			return getRuleContext(AnnotatedTypeContext.class,0);
		}
		public TerminalNode DOT() { return getToken(AsterParser.DOT, 0); }
		public TerminalNode COLON() { return getToken(AsterParser.COLON, 0); }
		public TerminalNode NEWLINE() { return getToken(AsterParser.NEWLINE, 0); }
		public BlockContext block() {
			return getRuleContext(BlockContext.class,0);
		}
		public TypeParamListContext typeParamList() {
			return getRuleContext(TypeParamListContext.class,0);
		}
		public ParamListContext paramList() {
			return getRuleContext(ParamListContext.class,0);
		}
		public TerminalNode COMMA() { return getToken(AsterParser.COMMA, 0); }
		public CapabilityAnnotationContext capabilityAnnotation() {
			return getRuleContext(CapabilityAnnotationContext.class,0);
		}
		public FuncDeclContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_funcDecl; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterFuncDecl(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitFuncDecl(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitFuncDecl(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FuncDeclContext funcDecl() throws RecognitionException {
		FuncDeclContext _localctx = new FuncDeclContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_funcDecl);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(164);
			match(TO);
			setState(165);
			match(IDENT);
			setState(167);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==OF) {
				{
				setState(166);
				typeParamList();
				}
			}

			setState(170);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WITH) {
				{
				setState(169);
				paramList();
				}
			}

			setState(173);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COMMA) {
				{
				setState(172);
				match(COMMA);
				}
			}

			setState(175);
			match(PRODUCE);
			setState(176);
			annotatedType();
			setState(188);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,11,_ctx) ) {
			case 1:
				{
				setState(177);
				match(DOT);
				setState(179);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==IT) {
					{
					setState(178);
					capabilityAnnotation();
					}
				}

				setState(181);
				match(COLON);
				setState(182);
				match(NEWLINE);
				setState(183);
				block();
				}
				break;
			case 2:
				{
				setState(184);
				match(COLON);
				setState(185);
				match(NEWLINE);
				setState(186);
				block();
				}
				break;
			case 3:
				{
				setState(187);
				match(DOT);
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TypeParamListContext extends ParserRuleContext {
		public TerminalNode OF() { return getToken(AsterParser.OF, 0); }
		public List<TypeParamContext> typeParam() {
			return getRuleContexts(TypeParamContext.class);
		}
		public TypeParamContext typeParam(int i) {
			return getRuleContext(TypeParamContext.class,i);
		}
		public List<TerminalNode> AND() { return getTokens(AsterParser.AND); }
		public TerminalNode AND(int i) {
			return getToken(AsterParser.AND, i);
		}
		public List<TerminalNode> COMMA() { return getTokens(AsterParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(AsterParser.COMMA, i);
		}
		public TypeParamListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeParamList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterTypeParamList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitTypeParamList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitTypeParamList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeParamListContext typeParamList() throws RecognitionException {
		TypeParamListContext _localctx = new TypeParamListContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_typeParamList);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(190);
			match(OF);
			setState(191);
			typeParam();
			setState(196);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,12,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(192);
					_la = _input.LA(1);
					if ( !(_la==COMMA || _la==AND) ) {
					_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					setState(193);
					typeParam();
					}
					} 
				}
				setState(198);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,12,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TypeParamContext extends ParserRuleContext {
		public TerminalNode TYPE_IDENT() { return getToken(AsterParser.TYPE_IDENT, 0); }
		public TerminalNode IDENT() { return getToken(AsterParser.IDENT, 0); }
		public TypeParamContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeParam; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterTypeParam(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitTypeParam(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitTypeParam(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeParamContext typeParam() throws RecognitionException {
		TypeParamContext _localctx = new TypeParamContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_typeParam);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(199);
			_la = _input.LA(1);
			if ( !(_la==TYPE_IDENT || _la==IDENT) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ParamListContext extends ParserRuleContext {
		public TerminalNode WITH() { return getToken(AsterParser.WITH, 0); }
		public List<ParamContext> param() {
			return getRuleContexts(ParamContext.class);
		}
		public ParamContext param(int i) {
			return getRuleContext(ParamContext.class,i);
		}
		public List<TerminalNode> AND() { return getTokens(AsterParser.AND); }
		public TerminalNode AND(int i) {
			return getToken(AsterParser.AND, i);
		}
		public List<TerminalNode> COMMA() { return getTokens(AsterParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(AsterParser.COMMA, i);
		}
		public ParamListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_paramList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterParamList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitParamList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitParamList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ParamListContext paramList() throws RecognitionException {
		ParamListContext _localctx = new ParamListContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_paramList);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(201);
			match(WITH);
			setState(202);
			param();
			setState(207);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,13,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(203);
					_la = _input.LA(1);
					if ( !(_la==COMMA || _la==AND) ) {
					_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					setState(204);
					param();
					}
					} 
				}
				setState(209);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,13,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ParamContext extends ParserRuleContext {
		public NameIdentContext nameIdent() {
			return getRuleContext(NameIdentContext.class,0);
		}
		public TerminalNode COLON() { return getToken(AsterParser.COLON, 0); }
		public AnnotatedTypeContext annotatedType() {
			return getRuleContext(AnnotatedTypeContext.class,0);
		}
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
		}
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
		}
		public ParamContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_param; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterParam(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitParam(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitParam(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ParamContext param() throws RecognitionException {
		ParamContext _localctx = new ParamContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_param);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(213);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT) {
				{
				{
				setState(210);
				annotation();
				}
				}
				setState(215);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(216);
			nameIdent();
			setState(217);
			match(COLON);
			setState(218);
			annotatedType();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class CapabilityAnnotationContext extends ParserRuleContext {
		public TerminalNode IT() { return getToken(AsterParser.IT, 0); }
		public TerminalNode PERFORMS() { return getToken(AsterParser.PERFORMS, 0); }
		public TerminalNode IDENT() { return getToken(AsterParser.IDENT, 0); }
		public TerminalNode LBRACKET() { return getToken(AsterParser.LBRACKET, 0); }
		public List<TerminalNode> TYPE_IDENT() { return getTokens(AsterParser.TYPE_IDENT); }
		public TerminalNode TYPE_IDENT(int i) {
			return getToken(AsterParser.TYPE_IDENT, i);
		}
		public TerminalNode RBRACKET() { return getToken(AsterParser.RBRACKET, 0); }
		public List<TerminalNode> COMMA() { return getTokens(AsterParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(AsterParser.COMMA, i);
		}
		public CapabilityAnnotationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_capabilityAnnotation; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterCapabilityAnnotation(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitCapabilityAnnotation(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitCapabilityAnnotation(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CapabilityAnnotationContext capabilityAnnotation() throws RecognitionException {
		CapabilityAnnotationContext _localctx = new CapabilityAnnotationContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_capabilityAnnotation);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(220);
			match(IT);
			setState(221);
			match(PERFORMS);
			setState(222);
			match(IDENT);
			setState(233);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LBRACKET) {
				{
				setState(223);
				match(LBRACKET);
				setState(224);
				match(TYPE_IDENT);
				setState(229);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(225);
					match(COMMA);
					setState(226);
					match(TYPE_IDENT);
					}
					}
					setState(231);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(232);
				match(RBRACKET);
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DataDeclContext extends ParserRuleContext {
		public TerminalNode DEFINE() { return getToken(AsterParser.DEFINE, 0); }
		public TerminalNode TYPE_IDENT() { return getToken(AsterParser.TYPE_IDENT, 0); }
		public TerminalNode WITH() { return getToken(AsterParser.WITH, 0); }
		public FieldListContext fieldList() {
			return getRuleContext(FieldListContext.class,0);
		}
		public TerminalNode DOT() { return getToken(AsterParser.DOT, 0); }
		public ArticleContext article() {
			return getRuleContext(ArticleContext.class,0);
		}
		public DataDeclContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dataDecl; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterDataDecl(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitDataDecl(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitDataDecl(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DataDeclContext dataDecl() throws RecognitionException {
		DataDeclContext _localctx = new DataDeclContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_dataDecl);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(235);
			match(DEFINE);
			setState(237);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==IDENT) {
				{
				setState(236);
				article();
				}
			}

			setState(239);
			match(TYPE_IDENT);
			setState(240);
			match(WITH);
			setState(241);
			fieldList();
			setState(242);
			match(DOT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class FieldListContext extends ParserRuleContext {
		public List<FieldContext> field() {
			return getRuleContexts(FieldContext.class);
		}
		public FieldContext field(int i) {
			return getRuleContext(FieldContext.class,i);
		}
		public List<TerminalNode> AND() { return getTokens(AsterParser.AND); }
		public TerminalNode AND(int i) {
			return getToken(AsterParser.AND, i);
		}
		public List<TerminalNode> COMMA() { return getTokens(AsterParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(AsterParser.COMMA, i);
		}
		public FieldListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fieldList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterFieldList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitFieldList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitFieldList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FieldListContext fieldList() throws RecognitionException {
		FieldListContext _localctx = new FieldListContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_fieldList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(244);
			field();
			setState(249);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA || _la==AND) {
				{
				{
				setState(245);
				_la = _input.LA(1);
				if ( !(_la==COMMA || _la==AND) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(246);
				field();
				}
				}
				setState(251);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class FieldContext extends ParserRuleContext {
		public NameIdentContext nameIdent() {
			return getRuleContext(NameIdentContext.class,0);
		}
		public TerminalNode COLON() { return getToken(AsterParser.COLON, 0); }
		public AnnotatedTypeContext annotatedType() {
			return getRuleContext(AnnotatedTypeContext.class,0);
		}
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
		}
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
		}
		public FieldContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_field; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterField(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitField(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitField(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FieldContext field() throws RecognitionException {
		FieldContext _localctx = new FieldContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_field);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(255);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT) {
				{
				{
				setState(252);
				annotation();
				}
				}
				setState(257);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(258);
			nameIdent();
			setState(259);
			match(COLON);
			setState(260);
			annotatedType();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class NameIdentContext extends ParserRuleContext {
		public TerminalNode IDENT() { return getToken(AsterParser.IDENT, 0); }
		public TerminalNode TYPE() { return getToken(AsterParser.TYPE, 0); }
		public NameIdentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nameIdent; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterNameIdent(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitNameIdent(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitNameIdent(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NameIdentContext nameIdent() throws RecognitionException {
		NameIdentContext _localctx = new NameIdentContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_nameIdent);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(262);
			_la = _input.LA(1);
			if ( !(_la==TYPE || _la==IDENT) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class EnumDeclContext extends ParserRuleContext {
		public TerminalNode DEFINE() { return getToken(AsterParser.DEFINE, 0); }
		public TerminalNode TYPE_IDENT() { return getToken(AsterParser.TYPE_IDENT, 0); }
		public TerminalNode AS() { return getToken(AsterParser.AS, 0); }
		public TerminalNode ONE() { return getToken(AsterParser.ONE, 0); }
		public TerminalNode OF() { return getToken(AsterParser.OF, 0); }
		public VariantListContext variantList() {
			return getRuleContext(VariantListContext.class,0);
		}
		public TerminalNode DOT() { return getToken(AsterParser.DOT, 0); }
		public ArticleContext article() {
			return getRuleContext(ArticleContext.class,0);
		}
		public EnumDeclContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_enumDecl; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterEnumDecl(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitEnumDecl(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitEnumDecl(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EnumDeclContext enumDecl() throws RecognitionException {
		EnumDeclContext _localctx = new EnumDeclContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_enumDecl);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(264);
			match(DEFINE);
			setState(266);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==IDENT) {
				{
				setState(265);
				article();
				}
			}

			setState(268);
			match(TYPE_IDENT);
			setState(269);
			match(AS);
			setState(270);
			match(ONE);
			setState(271);
			match(OF);
			setState(272);
			variantList();
			setState(273);
			match(DOT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ArticleContext extends ParserRuleContext {
		public TerminalNode IDENT() { return getToken(AsterParser.IDENT, 0); }
		public ArticleContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_article; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterArticle(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitArticle(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitArticle(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArticleContext article() throws RecognitionException {
		ArticleContext _localctx = new ArticleContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_article);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(275);
			match(IDENT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class VariantListContext extends ParserRuleContext {
		public List<TerminalNode> TYPE_IDENT() { return getTokens(AsterParser.TYPE_IDENT); }
		public TerminalNode TYPE_IDENT(int i) {
			return getToken(AsterParser.TYPE_IDENT, i);
		}
		public List<TerminalNode> COMMA() { return getTokens(AsterParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(AsterParser.COMMA, i);
		}
		public List<TerminalNode> OR() { return getTokens(AsterParser.OR); }
		public TerminalNode OR(int i) {
			return getToken(AsterParser.OR, i);
		}
		public List<TerminalNode> AND() { return getTokens(AsterParser.AND); }
		public TerminalNode AND(int i) {
			return getToken(AsterParser.AND, i);
		}
		public VariantListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variantList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterVariantList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitVariantList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitVariantList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VariantListContext variantList() throws RecognitionException {
		VariantListContext _localctx = new VariantListContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_variantList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(277);
			match(TYPE_IDENT);
			setState(282);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 12884901896L) != 0)) {
				{
				{
				setState(278);
				_la = _input.LA(1);
				if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 12884901896L) != 0)) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(279);
				match(TYPE_IDENT);
				}
				}
				setState(284);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ImportDeclContext extends ParserRuleContext {
		public TerminalNode USE() { return getToken(AsterParser.USE, 0); }
		public QualifiedNameContext qualifiedName() {
			return getRuleContext(QualifiedNameContext.class,0);
		}
		public TerminalNode DOT() { return getToken(AsterParser.DOT, 0); }
		public TerminalNode AS() { return getToken(AsterParser.AS, 0); }
		public ImportAliasContext importAlias() {
			return getRuleContext(ImportAliasContext.class,0);
		}
		public ImportDeclContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_importDecl; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterImportDecl(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitImportDecl(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitImportDecl(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ImportDeclContext importDecl() throws RecognitionException {
		ImportDeclContext _localctx = new ImportDeclContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_importDecl);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(285);
			match(USE);
			setState(286);
			qualifiedName();
			setState(289);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==AS) {
				{
				setState(287);
				match(AS);
				setState(288);
				importAlias();
				}
			}

			setState(291);
			match(DOT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ImportAliasContext extends ParserRuleContext {
		public TerminalNode TYPE_IDENT() { return getToken(AsterParser.TYPE_IDENT, 0); }
		public TerminalNode IDENT() { return getToken(AsterParser.IDENT, 0); }
		public ImportAliasContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_importAlias; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterImportAlias(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitImportAlias(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitImportAlias(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ImportAliasContext importAlias() throws RecognitionException {
		ImportAliasContext _localctx = new ImportAliasContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_importAlias);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(293);
			_la = _input.LA(1);
			if ( !(_la==TYPE_IDENT || _la==IDENT) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TypeDeclContext extends ParserRuleContext {
		public TerminalNode TYPE() { return getToken(AsterParser.TYPE, 0); }
		public TerminalNode AS() { return getToken(AsterParser.AS, 0); }
		public AnnotatedTypeContext annotatedType() {
			return getRuleContext(AnnotatedTypeContext.class,0);
		}
		public TerminalNode DOT() { return getToken(AsterParser.DOT, 0); }
		public TerminalNode TYPE_IDENT() { return getToken(AsterParser.TYPE_IDENT, 0); }
		public TerminalNode IDENT() { return getToken(AsterParser.IDENT, 0); }
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
		}
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
		}
		public TypeDeclContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeDecl; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterTypeDecl(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitTypeDecl(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitTypeDecl(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeDeclContext typeDecl() throws RecognitionException {
		TypeDeclContext _localctx = new TypeDeclContext(_ctx, getState());
		enterRule(_localctx, 40, RULE_typeDecl);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(298);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT) {
				{
				{
				setState(295);
				annotation();
				}
				}
				setState(300);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(301);
			match(TYPE);
			setState(302);
			_la = _input.LA(1);
			if ( !(_la==TYPE_IDENT || _la==IDENT) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(303);
			match(AS);
			setState(304);
			annotatedType();
			setState(305);
			match(DOT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class AnnotationContext extends ParserRuleContext {
		public TerminalNode AT() { return getToken(AsterParser.AT, 0); }
		public TerminalNode IDENT() { return getToken(AsterParser.IDENT, 0); }
		public TerminalNode TYPE_IDENT() { return getToken(AsterParser.TYPE_IDENT, 0); }
		public AnnotationArgsContext annotationArgs() {
			return getRuleContext(AnnotationArgsContext.class,0);
		}
		public AnnotationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_annotation; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterAnnotation(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitAnnotation(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitAnnotation(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AnnotationContext annotation() throws RecognitionException {
		AnnotationContext _localctx = new AnnotationContext(_ctx, getState());
		enterRule(_localctx, 42, RULE_annotation);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(307);
			match(AT);
			setState(308);
			_la = _input.LA(1);
			if ( !(_la==TYPE_IDENT || _la==IDENT) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(310);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,24,_ctx) ) {
			case 1:
				{
				setState(309);
				annotationArgs();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class AnnotationArgsContext extends ParserRuleContext {
		public TerminalNode LPAREN() { return getToken(AsterParser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(AsterParser.RPAREN, 0); }
		public List<AnnotationArgContext> annotationArg() {
			return getRuleContexts(AnnotationArgContext.class);
		}
		public AnnotationArgContext annotationArg(int i) {
			return getRuleContext(AnnotationArgContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(AsterParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(AsterParser.COMMA, i);
		}
		public AnnotationArgsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_annotationArgs; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterAnnotationArgs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitAnnotationArgs(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitAnnotationArgs(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AnnotationArgsContext annotationArgs() throws RecognitionException {
		AnnotationArgsContext _localctx = new AnnotationArgsContext(_ctx, getState());
		enterRule(_localctx, 44, RULE_annotationArgs);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(312);
			match(LPAREN);
			setState(321);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 432345564351299584L) != 0)) {
				{
				setState(313);
				annotationArg();
				setState(318);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(314);
					match(COMMA);
					setState(315);
					annotationArg();
					}
					}
					setState(320);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(323);
			match(RPAREN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class AnnotationArgContext extends ParserRuleContext {
		public AnnotationArgContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_annotationArg; }
	 
		public AnnotationArgContext() { }
		public void copyFrom(AnnotationArgContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class NamedAnnotationArgContext extends AnnotationArgContext {
		public TerminalNode IDENT() { return getToken(AsterParser.IDENT, 0); }
		public TerminalNode COLON() { return getToken(AsterParser.COLON, 0); }
		public AnnotationValueContext annotationValue() {
			return getRuleContext(AnnotationValueContext.class,0);
		}
		public NamedAnnotationArgContext(AnnotationArgContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterNamedAnnotationArg(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitNamedAnnotationArg(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitNamedAnnotationArg(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class PositionalAnnotationArgContext extends AnnotationArgContext {
		public AnnotationValueContext annotationValue() {
			return getRuleContext(AnnotationValueContext.class,0);
		}
		public PositionalAnnotationArgContext(AnnotationArgContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterPositionalAnnotationArg(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitPositionalAnnotationArg(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitPositionalAnnotationArg(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AnnotationArgContext annotationArg() throws RecognitionException {
		AnnotationArgContext _localctx = new AnnotationArgContext(_ctx, getState());
		enterRule(_localctx, 46, RULE_annotationArg);
		try {
			setState(329);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,27,_ctx) ) {
			case 1:
				_localctx = new NamedAnnotationArgContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(325);
				match(IDENT);
				setState(326);
				match(COLON);
				setState(327);
				annotationValue();
				}
				break;
			case 2:
				_localctx = new PositionalAnnotationArgContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(328);
				annotationValue();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class AnnotationValueContext extends ParserRuleContext {
		public TerminalNode STRING_LITERAL() { return getToken(AsterParser.STRING_LITERAL, 0); }
		public TerminalNode INT_LITERAL() { return getToken(AsterParser.INT_LITERAL, 0); }
		public TerminalNode FLOAT_LITERAL() { return getToken(AsterParser.FLOAT_LITERAL, 0); }
		public TerminalNode LONG_LITERAL() { return getToken(AsterParser.LONG_LITERAL, 0); }
		public TerminalNode BOOL_LITERAL() { return getToken(AsterParser.BOOL_LITERAL, 0); }
		public TerminalNode IDENT() { return getToken(AsterParser.IDENT, 0); }
		public TerminalNode TYPE_IDENT() { return getToken(AsterParser.TYPE_IDENT, 0); }
		public AnnotationValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_annotationValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterAnnotationValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitAnnotationValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitAnnotationValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AnnotationValueContext annotationValue() throws RecognitionException {
		AnnotationValueContext _localctx = new AnnotationValueContext(_ctx, getState());
		enterRule(_localctx, 48, RULE_annotationValue);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(331);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 432345564351299584L) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class AnnotatedTypeContext extends ParserRuleContext {
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
		}
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
		}
		public AnnotatedTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_annotatedType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterAnnotatedType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitAnnotatedType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitAnnotatedType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AnnotatedTypeContext annotatedType() throws RecognitionException {
		AnnotatedTypeContext _localctx = new AnnotatedTypeContext(_ctx, getState());
		enterRule(_localctx, 50, RULE_annotatedType);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(336);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT) {
				{
				{
				setState(333);
				annotation();
				}
				}
				setState(338);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(339);
			type(0);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TypeContext extends ParserRuleContext {
		public TypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_type; }
	 
		public TypeContext() { }
		public void copyFrom(TypeContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class OfGenericTypeContext extends TypeContext {
		public TerminalNode OF() { return getToken(AsterParser.OF, 0); }
		public List<AnnotatedTypeContext> annotatedType() {
			return getRuleContexts(AnnotatedTypeContext.class);
		}
		public AnnotatedTypeContext annotatedType(int i) {
			return getRuleContext(AnnotatedTypeContext.class,i);
		}
		public TerminalNode TYPE_IDENT() { return getToken(AsterParser.TYPE_IDENT, 0); }
		public TerminalNode IDENT() { return getToken(AsterParser.IDENT, 0); }
		public List<TerminalNode> AND() { return getTokens(AsterParser.AND); }
		public TerminalNode AND(int i) {
			return getToken(AsterParser.AND, i);
		}
		public List<TerminalNode> COMMA() { return getTokens(AsterParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(AsterParser.COMMA, i);
		}
		public OfGenericTypeContext(TypeContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterOfGenericType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitOfGenericType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitOfGenericType(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class GenericTypeContext extends TypeContext {
		public TerminalNode TYPE_IDENT() { return getToken(AsterParser.TYPE_IDENT, 0); }
		public TerminalNode LT() { return getToken(AsterParser.LT, 0); }
		public TypeListContext typeList() {
			return getRuleContext(TypeListContext.class,0);
		}
		public TerminalNode GT() { return getToken(AsterParser.GT, 0); }
		public GenericTypeContext(TypeContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterGenericType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitGenericType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitGenericType(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class TypeNameContext extends TypeContext {
		public TerminalNode TYPE_IDENT() { return getToken(AsterParser.TYPE_IDENT, 0); }
		public TypeNameContext(TypeContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterTypeName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitTypeName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitTypeName(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class FuncTypeContext extends TypeContext {
		public TerminalNode LPAREN() { return getToken(AsterParser.LPAREN, 0); }
		public TypeListContext typeList() {
			return getRuleContext(TypeListContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(AsterParser.RPAREN, 0); }
		public TerminalNode ARROW() { return getToken(AsterParser.ARROW, 0); }
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public FuncTypeContext(TypeContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterFuncType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitFuncType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitFuncType(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class MaybeTypeContext extends TypeContext {
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public TerminalNode QUESTION() { return getToken(AsterParser.QUESTION, 0); }
		public MaybeTypeContext(TypeContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterMaybeType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitMaybeType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitMaybeType(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class MapTypeContext extends TypeContext {
		public TerminalNode MAP() { return getToken(AsterParser.MAP, 0); }
		public List<AnnotatedTypeContext> annotatedType() {
			return getRuleContexts(AnnotatedTypeContext.class);
		}
		public AnnotatedTypeContext annotatedType(int i) {
			return getRuleContext(AnnotatedTypeContext.class,i);
		}
		public TerminalNode TO() { return getToken(AsterParser.TO, 0); }
		public MapTypeContext(TypeContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterMapType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitMapType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitMapType(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ParenTypeContext extends TypeContext {
		public TerminalNode LPAREN() { return getToken(AsterParser.LPAREN, 0); }
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(AsterParser.RPAREN, 0); }
		public ParenTypeContext(TypeContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterParenType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitParenType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitParenType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeContext type() throws RecognitionException {
		return type(0);
	}

	private TypeContext type(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		TypeContext _localctx = new TypeContext(_ctx, _parentState);
		TypeContext _prevctx = _localctx;
		int _startState = 52;
		enterRecursionRule(_localctx, 52, RULE_type, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(373);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,30,_ctx) ) {
			case 1:
				{
				_localctx = new FuncTypeContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;

				setState(342);
				match(LPAREN);
				setState(343);
				typeList();
				setState(344);
				match(RPAREN);
				setState(345);
				match(ARROW);
				setState(346);
				type(6);
				}
				break;
			case 2:
				{
				_localctx = new MapTypeContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(348);
				match(MAP);
				setState(349);
				annotatedType();
				setState(350);
				match(TO);
				setState(351);
				annotatedType();
				}
				break;
			case 3:
				{
				_localctx = new OfGenericTypeContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(353);
				_la = _input.LA(1);
				if ( !(_la==TYPE_IDENT || _la==IDENT) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(354);
				match(OF);
				setState(355);
				annotatedType();
				setState(360);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,29,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(356);
						_la = _input.LA(1);
						if ( !(_la==COMMA || _la==AND) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(357);
						annotatedType();
						}
						} 
					}
					setState(362);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,29,_ctx);
				}
				}
				break;
			case 4:
				{
				_localctx = new GenericTypeContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(363);
				match(TYPE_IDENT);
				setState(364);
				match(LT);
				setState(365);
				typeList();
				setState(366);
				match(GT);
				}
				break;
			case 5:
				{
				_localctx = new TypeNameContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(368);
				match(TYPE_IDENT);
				}
				break;
			case 6:
				{
				_localctx = new ParenTypeContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(369);
				match(LPAREN);
				setState(370);
				type(0);
				setState(371);
				match(RPAREN);
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(379);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,31,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					{
					_localctx = new MaybeTypeContext(new TypeContext(_parentctx, _parentState));
					pushNewRecursionContext(_localctx, _startState, RULE_type);
					setState(375);
					if (!(precpred(_ctx, 7))) throw new FailedPredicateException(this, "precpred(_ctx, 7)");
					setState(376);
					match(QUESTION);
					}
					} 
				}
				setState(381);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,31,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TypeListContext extends ParserRuleContext {
		public List<AnnotatedTypeContext> annotatedType() {
			return getRuleContexts(AnnotatedTypeContext.class);
		}
		public AnnotatedTypeContext annotatedType(int i) {
			return getRuleContext(AnnotatedTypeContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(AsterParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(AsterParser.COMMA, i);
		}
		public TypeListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterTypeList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitTypeList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitTypeList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeListContext typeList() throws RecognitionException {
		TypeListContext _localctx = new TypeListContext(_ctx, getState());
		enterRule(_localctx, 54, RULE_typeList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(382);
			annotatedType();
			setState(387);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(383);
				match(COMMA);
				setState(384);
				annotatedType();
				}
				}
				setState(389);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class BlockContext extends ParserRuleContext {
		public TerminalNode INDENT() { return getToken(AsterParser.INDENT, 0); }
		public List<StmtContext> stmt() {
			return getRuleContexts(StmtContext.class);
		}
		public StmtContext stmt(int i) {
			return getRuleContext(StmtContext.class,i);
		}
		public TerminalNode DEDENT() { return getToken(AsterParser.DEDENT, 0); }
		public List<TerminalNode> NEWLINE() { return getTokens(AsterParser.NEWLINE); }
		public TerminalNode NEWLINE(int i) {
			return getToken(AsterParser.NEWLINE, i);
		}
		public BlockContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_block; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterBlock(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitBlock(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitBlock(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BlockContext block() throws RecognitionException {
		BlockContext _localctx = new BlockContext(_ctx, getState());
		enterRule(_localctx, 56, RULE_block);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(390);
			match(INDENT);
			setState(391);
			stmt();
			setState(401);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,35,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					setState(399);
					_errHandler.sync(this);
					switch (_input.LA(1)) {
					case NEWLINE:
						{
						{
						setState(393); 
						_errHandler.sync(this);
						_la = _input.LA(1);
						do {
							{
							{
							setState(392);
							match(NEWLINE);
							}
							}
							setState(395); 
							_errHandler.sync(this);
							_la = _input.LA(1);
						} while ( _la==NEWLINE );
						setState(397);
						stmt();
						}
						}
						break;
					case LPAREN:
					case LBRACKET:
					case LTE:
					case GTE:
					case NEQ:
					case EQUALS:
					case PLUS:
					case STAR:
					case MINUS:
					case SLASH:
					case LT:
					case GT:
					case STRING_LITERAL:
					case BOOL_LITERAL:
					case NULL_LITERAL:
					case LONG_LITERAL:
					case FLOAT_LITERAL:
					case INT_LITERAL:
					case DEFINE:
					case LET:
					case RETURN:
					case IF:
					case MATCH:
					case NOT:
					case START:
					case WAIT:
					case FUNCTION:
					case MAP:
					case TYPE_IDENT:
					case IDENT:
						{
						setState(398);
						stmt();
						}
						break;
					default:
						throw new NoViableAltException(this);
					}
					} 
				}
				setState(403);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,35,_ctx);
			}
			setState(407);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==NEWLINE) {
				{
				{
				setState(404);
				match(NEWLINE);
				}
				}
				setState(409);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(410);
			match(DEDENT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class StmtContext extends ParserRuleContext {
		public LetStmtContext letStmt() {
			return getRuleContext(LetStmtContext.class,0);
		}
		public DefineStmtContext defineStmt() {
			return getRuleContext(DefineStmtContext.class,0);
		}
		public StartStmtContext startStmt() {
			return getRuleContext(StartStmtContext.class,0);
		}
		public WaitStmtContext waitStmt() {
			return getRuleContext(WaitStmtContext.class,0);
		}
		public ReturnStmtContext returnStmt() {
			return getRuleContext(ReturnStmtContext.class,0);
		}
		public IfStmtContext ifStmt() {
			return getRuleContext(IfStmtContext.class,0);
		}
		public MatchStmtContext matchStmt() {
			return getRuleContext(MatchStmtContext.class,0);
		}
		public ExprStmtContext exprStmt() {
			return getRuleContext(ExprStmtContext.class,0);
		}
		public StmtContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_stmt; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterStmt(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitStmt(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitStmt(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StmtContext stmt() throws RecognitionException {
		StmtContext _localctx = new StmtContext(_ctx, getState());
		enterRule(_localctx, 58, RULE_stmt);
		try {
			setState(420);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LET:
				enterOuterAlt(_localctx, 1);
				{
				setState(412);
				letStmt();
				}
				break;
			case DEFINE:
				enterOuterAlt(_localctx, 2);
				{
				setState(413);
				defineStmt();
				}
				break;
			case START:
				enterOuterAlt(_localctx, 3);
				{
				setState(414);
				startStmt();
				}
				break;
			case WAIT:
				enterOuterAlt(_localctx, 4);
				{
				setState(415);
				waitStmt();
				}
				break;
			case RETURN:
				enterOuterAlt(_localctx, 5);
				{
				setState(416);
				returnStmt();
				}
				break;
			case IF:
				enterOuterAlt(_localctx, 6);
				{
				setState(417);
				ifStmt();
				}
				break;
			case MATCH:
				enterOuterAlt(_localctx, 7);
				{
				setState(418);
				matchStmt();
				}
				break;
			case LPAREN:
			case LBRACKET:
			case LTE:
			case GTE:
			case NEQ:
			case EQUALS:
			case PLUS:
			case STAR:
			case MINUS:
			case SLASH:
			case LT:
			case GT:
			case STRING_LITERAL:
			case BOOL_LITERAL:
			case NULL_LITERAL:
			case LONG_LITERAL:
			case FLOAT_LITERAL:
			case INT_LITERAL:
			case NOT:
			case FUNCTION:
			case MAP:
			case TYPE_IDENT:
			case IDENT:
				enterOuterAlt(_localctx, 8);
				{
				setState(419);
				exprStmt();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class LetStmtContext extends ParserRuleContext {
		public LetStmtContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_letStmt; }
	 
		public LetStmtContext() { }
		public void copyFrom(LetStmtContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class LetExprStmtContext extends LetStmtContext {
		public TerminalNode LET() { return getToken(AsterParser.LET, 0); }
		public TerminalNode IDENT() { return getToken(AsterParser.IDENT, 0); }
		public TerminalNode BE() { return getToken(AsterParser.BE, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public TerminalNode DOT() { return getToken(AsterParser.DOT, 0); }
		public LetExprStmtContext(LetStmtContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterLetExprStmt(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitLetExprStmt(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitLetExprStmt(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class LetLambdaStmtContext extends LetStmtContext {
		public TerminalNode LET() { return getToken(AsterParser.LET, 0); }
		public TerminalNode IDENT() { return getToken(AsterParser.IDENT, 0); }
		public TerminalNode BE() { return getToken(AsterParser.BE, 0); }
		public LambdaExprContext lambdaExpr() {
			return getRuleContext(LambdaExprContext.class,0);
		}
		public LetLambdaStmtContext(LetStmtContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterLetLambdaStmt(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitLetLambdaStmt(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitLetLambdaStmt(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LetStmtContext letStmt() throws RecognitionException {
		LetStmtContext _localctx = new LetStmtContext(_ctx, getState());
		enterRule(_localctx, 60, RULE_letStmt);
		try {
			setState(432);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,38,_ctx) ) {
			case 1:
				_localctx = new LetLambdaStmtContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(422);
				match(LET);
				setState(423);
				match(IDENT);
				setState(424);
				match(BE);
				setState(425);
				lambdaExpr();
				}
				break;
			case 2:
				_localctx = new LetExprStmtContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(426);
				match(LET);
				setState(427);
				match(IDENT);
				setState(428);
				match(BE);
				setState(429);
				expr();
				setState(430);
				match(DOT);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DefineStmtContext extends ParserRuleContext {
		public TerminalNode DEFINE() { return getToken(AsterParser.DEFINE, 0); }
		public NameIdentContext nameIdent() {
			return getRuleContext(NameIdentContext.class,0);
		}
		public TerminalNode AS() { return getToken(AsterParser.AS, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public TerminalNode DOT() { return getToken(AsterParser.DOT, 0); }
		public DefineStmtContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_defineStmt; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterDefineStmt(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitDefineStmt(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitDefineStmt(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DefineStmtContext defineStmt() throws RecognitionException {
		DefineStmtContext _localctx = new DefineStmtContext(_ctx, getState());
		enterRule(_localctx, 62, RULE_defineStmt);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(434);
			match(DEFINE);
			setState(435);
			nameIdent();
			setState(436);
			match(AS);
			setState(437);
			expr();
			setState(438);
			match(DOT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class StartStmtContext extends ParserRuleContext {
		public TerminalNode START() { return getToken(AsterParser.START, 0); }
		public NameIdentContext nameIdent() {
			return getRuleContext(NameIdentContext.class,0);
		}
		public TerminalNode AS() { return getToken(AsterParser.AS, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public TerminalNode DOT() { return getToken(AsterParser.DOT, 0); }
		public TerminalNode ASYNC() { return getToken(AsterParser.ASYNC, 0); }
		public StartStmtContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_startStmt; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterStartStmt(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitStartStmt(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitStartStmt(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StartStmtContext startStmt() throws RecognitionException {
		StartStmtContext _localctx = new StartStmtContext(_ctx, getState());
		enterRule(_localctx, 64, RULE_startStmt);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(440);
			match(START);
			setState(441);
			nameIdent();
			setState(442);
			match(AS);
			setState(444);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ASYNC) {
				{
				setState(443);
				match(ASYNC);
				}
			}

			setState(446);
			expr();
			setState(447);
			match(DOT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class WaitStmtContext extends ParserRuleContext {
		public TerminalNode WAIT() { return getToken(AsterParser.WAIT, 0); }
		public TerminalNode FOR() { return getToken(AsterParser.FOR, 0); }
		public List<NameIdentContext> nameIdent() {
			return getRuleContexts(NameIdentContext.class);
		}
		public NameIdentContext nameIdent(int i) {
			return getRuleContext(NameIdentContext.class,i);
		}
		public TerminalNode DOT() { return getToken(AsterParser.DOT, 0); }
		public List<TerminalNode> AND() { return getTokens(AsterParser.AND); }
		public TerminalNode AND(int i) {
			return getToken(AsterParser.AND, i);
		}
		public WaitStmtContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_waitStmt; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterWaitStmt(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitWaitStmt(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitWaitStmt(this);
			else return visitor.visitChildren(this);
		}
	}

	public final WaitStmtContext waitStmt() throws RecognitionException {
		WaitStmtContext _localctx = new WaitStmtContext(_ctx, getState());
		enterRule(_localctx, 66, RULE_waitStmt);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(449);
			match(WAIT);
			setState(450);
			match(FOR);
			setState(451);
			nameIdent();
			setState(456);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AND) {
				{
				{
				setState(452);
				match(AND);
				setState(453);
				nameIdent();
				}
				}
				setState(458);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(459);
			match(DOT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ReturnStmtContext extends ParserRuleContext {
		public TerminalNode RETURN() { return getToken(AsterParser.RETURN, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public TerminalNode DOT() { return getToken(AsterParser.DOT, 0); }
		public ReturnStmtContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_returnStmt; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterReturnStmt(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitReturnStmt(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitReturnStmt(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ReturnStmtContext returnStmt() throws RecognitionException {
		ReturnStmtContext _localctx = new ReturnStmtContext(_ctx, getState());
		enterRule(_localctx, 68, RULE_returnStmt);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(461);
			match(RETURN);
			setState(462);
			expr();
			setState(463);
			match(DOT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class IfStmtContext extends ParserRuleContext {
		public TerminalNode IF() { return getToken(AsterParser.IF, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public List<TerminalNode> COLON() { return getTokens(AsterParser.COLON); }
		public TerminalNode COLON(int i) {
			return getToken(AsterParser.COLON, i);
		}
		public List<TerminalNode> NEWLINE() { return getTokens(AsterParser.NEWLINE); }
		public TerminalNode NEWLINE(int i) {
			return getToken(AsterParser.NEWLINE, i);
		}
		public List<BlockContext> block() {
			return getRuleContexts(BlockContext.class);
		}
		public BlockContext block(int i) {
			return getRuleContext(BlockContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(AsterParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(AsterParser.COMMA, i);
		}
		public TerminalNode ELSE() { return getToken(AsterParser.ELSE, 0); }
		public IfStmtContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_ifStmt; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterIfStmt(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitIfStmt(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitIfStmt(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IfStmtContext ifStmt() throws RecognitionException {
		IfStmtContext _localctx = new IfStmtContext(_ctx, getState());
		enterRule(_localctx, 70, RULE_ifStmt);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(465);
			match(IF);
			setState(466);
			expr();
			setState(468);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COMMA) {
				{
				setState(467);
				match(COMMA);
				}
			}

			setState(470);
			match(COLON);
			setState(471);
			match(NEWLINE);
			setState(472);
			block();
			setState(483);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,44,_ctx) ) {
			case 1:
				{
				setState(474);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==NEWLINE) {
					{
					setState(473);
					match(NEWLINE);
					}
				}

				setState(476);
				match(ELSE);
				setState(478);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==COMMA) {
					{
					setState(477);
					match(COMMA);
					}
				}

				setState(480);
				match(COLON);
				setState(481);
				match(NEWLINE);
				setState(482);
				block();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class MatchStmtContext extends ParserRuleContext {
		public TerminalNode MATCH() { return getToken(AsterParser.MATCH, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public TerminalNode COLON() { return getToken(AsterParser.COLON, 0); }
		public List<TerminalNode> NEWLINE() { return getTokens(AsterParser.NEWLINE); }
		public TerminalNode NEWLINE(int i) {
			return getToken(AsterParser.NEWLINE, i);
		}
		public TerminalNode INDENT() { return getToken(AsterParser.INDENT, 0); }
		public List<MatchCaseContext> matchCase() {
			return getRuleContexts(MatchCaseContext.class);
		}
		public MatchCaseContext matchCase(int i) {
			return getRuleContext(MatchCaseContext.class,i);
		}
		public TerminalNode DEDENT() { return getToken(AsterParser.DEDENT, 0); }
		public MatchStmtContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_matchStmt; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterMatchStmt(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitMatchStmt(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitMatchStmt(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MatchStmtContext matchStmt() throws RecognitionException {
		MatchStmtContext _localctx = new MatchStmtContext(_ctx, getState());
		enterRule(_localctx, 72, RULE_matchStmt);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(485);
			match(MATCH);
			setState(486);
			expr();
			setState(487);
			match(COLON);
			setState(488);
			match(NEWLINE);
			setState(489);
			match(INDENT);
			setState(490);
			matchCase();
			setState(499);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,46,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(492); 
					_errHandler.sync(this);
					_la = _input.LA(1);
					do {
						{
						{
						setState(491);
						match(NEWLINE);
						}
						}
						setState(494); 
						_errHandler.sync(this);
						_la = _input.LA(1);
					} while ( _la==NEWLINE );
					setState(496);
					matchCase();
					}
					} 
				}
				setState(501);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,46,_ctx);
			}
			setState(505);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==NEWLINE) {
				{
				{
				setState(502);
				match(NEWLINE);
				}
				}
				setState(507);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(508);
			match(DEDENT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class MatchCaseContext extends ParserRuleContext {
		public TerminalNode WHEN() { return getToken(AsterParser.WHEN, 0); }
		public PatternContext pattern() {
			return getRuleContext(PatternContext.class,0);
		}
		public TerminalNode COMMA() { return getToken(AsterParser.COMMA, 0); }
		public ReturnStmtContext returnStmt() {
			return getRuleContext(ReturnStmtContext.class,0);
		}
		public BlockContext block() {
			return getRuleContext(BlockContext.class,0);
		}
		public MatchCaseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_matchCase; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterMatchCase(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitMatchCase(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitMatchCase(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MatchCaseContext matchCase() throws RecognitionException {
		MatchCaseContext _localctx = new MatchCaseContext(_ctx, getState());
		enterRule(_localctx, 74, RULE_matchCase);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(510);
			match(WHEN);
			setState(511);
			pattern();
			setState(512);
			match(COMMA);
			setState(515);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case RETURN:
				{
				setState(513);
				returnStmt();
				}
				break;
			case INDENT:
				{
				setState(514);
				block();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class PatternContext extends ParserRuleContext {
		public PatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_pattern; }
	 
		public PatternContext() { }
		public void copyFrom(PatternContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class PatternNullContext extends PatternContext {
		public TerminalNode NULL_LITERAL() { return getToken(AsterParser.NULL_LITERAL, 0); }
		public PatternNullContext(PatternContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterPatternNull(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitPatternNull(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitPatternNull(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class PatternIntContext extends PatternContext {
		public TerminalNode INT_LITERAL() { return getToken(AsterParser.INT_LITERAL, 0); }
		public PatternIntContext(PatternContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterPatternInt(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitPatternInt(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitPatternInt(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class PatternCtorContext extends PatternContext {
		public TerminalNode TYPE_IDENT() { return getToken(AsterParser.TYPE_IDENT, 0); }
		public TerminalNode LPAREN() { return getToken(AsterParser.LPAREN, 0); }
		public List<PatternContext> pattern() {
			return getRuleContexts(PatternContext.class);
		}
		public PatternContext pattern(int i) {
			return getRuleContext(PatternContext.class,i);
		}
		public TerminalNode RPAREN() { return getToken(AsterParser.RPAREN, 0); }
		public List<TerminalNode> COMMA() { return getTokens(AsterParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(AsterParser.COMMA, i);
		}
		public PatternCtorContext(PatternContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterPatternCtor(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitPatternCtor(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitPatternCtor(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class PatternNameContext extends PatternContext {
		public TerminalNode IDENT() { return getToken(AsterParser.IDENT, 0); }
		public PatternNameContext(PatternContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterPatternName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitPatternName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitPatternName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PatternContext pattern() throws RecognitionException {
		PatternContext _localctx = new PatternContext(_ctx, getState());
		enterRule(_localctx, 76, RULE_pattern);
		int _la;
		try {
			setState(534);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case NULL_LITERAL:
				_localctx = new PatternNullContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(517);
				match(NULL_LITERAL);
				}
				break;
			case TYPE_IDENT:
				_localctx = new PatternCtorContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(518);
				match(TYPE_IDENT);
				setState(530);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==LPAREN) {
					{
					setState(519);
					match(LPAREN);
					setState(520);
					pattern();
					setState(525);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==COMMA) {
						{
						{
						setState(521);
						match(COMMA);
						setState(522);
						pattern();
						}
						}
						setState(527);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					setState(528);
					match(RPAREN);
					}
				}

				}
				break;
			case INT_LITERAL:
				_localctx = new PatternIntContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(532);
				match(INT_LITERAL);
				}
				break;
			case IDENT:
				_localctx = new PatternNameContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(533);
				match(IDENT);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ExprStmtContext extends ParserRuleContext {
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public TerminalNode DOT() { return getToken(AsterParser.DOT, 0); }
		public ExprStmtContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_exprStmt; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterExprStmt(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitExprStmt(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitExprStmt(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExprStmtContext exprStmt() throws RecognitionException {
		ExprStmtContext _localctx = new ExprStmtContext(_ctx, getState());
		enterRule(_localctx, 78, RULE_exprStmt);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(536);
			expr();
			setState(537);
			match(DOT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ExprContext extends ParserRuleContext {
		public ComparisonExprContext comparisonExpr() {
			return getRuleContext(ComparisonExprContext.class,0);
		}
		public ExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExprContext expr() throws RecognitionException {
		ExprContext _localctx = new ExprContext(_ctx, getState());
		enterRule(_localctx, 80, RULE_expr);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(539);
			comparisonExpr();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ComparisonExprContext extends ParserRuleContext {
		public Token op;
		public List<AdditiveExprContext> additiveExpr() {
			return getRuleContexts(AdditiveExprContext.class);
		}
		public AdditiveExprContext additiveExpr(int i) {
			return getRuleContext(AdditiveExprContext.class,i);
		}
		public TerminalNode LT() { return getToken(AsterParser.LT, 0); }
		public TerminalNode GT() { return getToken(AsterParser.GT, 0); }
		public TerminalNode LTE() { return getToken(AsterParser.LTE, 0); }
		public TerminalNode GTE() { return getToken(AsterParser.GTE, 0); }
		public TerminalNode NEQ() { return getToken(AsterParser.NEQ, 0); }
		public ComparisonExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_comparisonExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterComparisonExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitComparisonExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitComparisonExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ComparisonExprContext comparisonExpr() throws RecognitionException {
		ComparisonExprContext _localctx = new ComparisonExprContext(_ctx, getState());
		enterRule(_localctx, 82, RULE_comparisonExpr);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(541);
			additiveExpr();
			setState(544);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,52,_ctx) ) {
			case 1:
				{
				setState(542);
				((ComparisonExprContext)_localctx).op = _input.LT(1);
				_la = _input.LA(1);
				if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 198400L) != 0)) ) {
					((ComparisonExprContext)_localctx).op = (Token)_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(543);
				additiveExpr();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class AdditiveExprContext extends ParserRuleContext {
		public Token op;
		public List<MultiplicativeExprContext> multiplicativeExpr() {
			return getRuleContexts(MultiplicativeExprContext.class);
		}
		public MultiplicativeExprContext multiplicativeExpr(int i) {
			return getRuleContext(MultiplicativeExprContext.class,i);
		}
		public List<TerminalNode> PLUS() { return getTokens(AsterParser.PLUS); }
		public TerminalNode PLUS(int i) {
			return getToken(AsterParser.PLUS, i);
		}
		public List<TerminalNode> MINUS() { return getTokens(AsterParser.MINUS); }
		public TerminalNode MINUS(int i) {
			return getToken(AsterParser.MINUS, i);
		}
		public AdditiveExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_additiveExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterAdditiveExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitAdditiveExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitAdditiveExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AdditiveExprContext additiveExpr() throws RecognitionException {
		AdditiveExprContext _localctx = new AdditiveExprContext(_ctx, getState());
		enterRule(_localctx, 84, RULE_additiveExpr);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(546);
			multiplicativeExpr();
			setState(551);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,53,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(547);
					((AdditiveExprContext)_localctx).op = _input.LT(1);
					_la = _input.LA(1);
					if ( !(_la==PLUS || _la==MINUS) ) {
						((AdditiveExprContext)_localctx).op = (Token)_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					setState(548);
					multiplicativeExpr();
					}
					} 
				}
				setState(553);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,53,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class MultiplicativeExprContext extends ParserRuleContext {
		public Token op;
		public List<UnaryExprContext> unaryExpr() {
			return getRuleContexts(UnaryExprContext.class);
		}
		public UnaryExprContext unaryExpr(int i) {
			return getRuleContext(UnaryExprContext.class,i);
		}
		public List<TerminalNode> STAR() { return getTokens(AsterParser.STAR); }
		public TerminalNode STAR(int i) {
			return getToken(AsterParser.STAR, i);
		}
		public List<TerminalNode> SLASH() { return getTokens(AsterParser.SLASH); }
		public TerminalNode SLASH(int i) {
			return getToken(AsterParser.SLASH, i);
		}
		public MultiplicativeExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_multiplicativeExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterMultiplicativeExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitMultiplicativeExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitMultiplicativeExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MultiplicativeExprContext multiplicativeExpr() throws RecognitionException {
		MultiplicativeExprContext _localctx = new MultiplicativeExprContext(_ctx, getState());
		enterRule(_localctx, 86, RULE_multiplicativeExpr);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(554);
			unaryExpr();
			setState(559);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,54,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(555);
					((MultiplicativeExprContext)_localctx).op = _input.LT(1);
					_la = _input.LA(1);
					if ( !(_la==STAR || _la==SLASH) ) {
						((MultiplicativeExprContext)_localctx).op = (Token)_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					setState(556);
					unaryExpr();
					}
					} 
				}
				setState(561);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,54,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class UnaryExprContext extends ParserRuleContext {
		public UnaryExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_unaryExpr; }
	 
		public UnaryExprContext() { }
		public void copyFrom(UnaryExprContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class PostfixUnaryContext extends UnaryExprContext {
		public PostfixExprContext postfixExpr() {
			return getRuleContext(PostfixExprContext.class,0);
		}
		public PostfixUnaryContext(UnaryExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterPostfixUnary(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitPostfixUnary(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitPostfixUnary(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class NotExprContext extends UnaryExprContext {
		public TerminalNode NOT() { return getToken(AsterParser.NOT, 0); }
		public UnaryExprContext unaryExpr() {
			return getRuleContext(UnaryExprContext.class,0);
		}
		public NotExprContext(UnaryExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterNotExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitNotExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitNotExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final UnaryExprContext unaryExpr() throws RecognitionException {
		UnaryExprContext _localctx = new UnaryExprContext(_ctx, getState());
		enterRule(_localctx, 88, RULE_unaryExpr);
		try {
			setState(565);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case NOT:
				_localctx = new NotExprContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(562);
				match(NOT);
				setState(563);
				unaryExpr();
				}
				break;
			case LPAREN:
			case LBRACKET:
			case LTE:
			case GTE:
			case NEQ:
			case EQUALS:
			case PLUS:
			case STAR:
			case MINUS:
			case SLASH:
			case LT:
			case GT:
			case STRING_LITERAL:
			case BOOL_LITERAL:
			case NULL_LITERAL:
			case LONG_LITERAL:
			case FLOAT_LITERAL:
			case INT_LITERAL:
			case FUNCTION:
			case MAP:
			case TYPE_IDENT:
			case IDENT:
				_localctx = new PostfixUnaryContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(564);
				postfixExpr();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class PostfixExprContext extends ParserRuleContext {
		public PrimaryExprContext primaryExpr() {
			return getRuleContext(PrimaryExprContext.class,0);
		}
		public List<PostfixSuffixContext> postfixSuffix() {
			return getRuleContexts(PostfixSuffixContext.class);
		}
		public PostfixSuffixContext postfixSuffix(int i) {
			return getRuleContext(PostfixSuffixContext.class,i);
		}
		public PostfixExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_postfixExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterPostfixExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitPostfixExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitPostfixExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PostfixExprContext postfixExpr() throws RecognitionException {
		PostfixExprContext _localctx = new PostfixExprContext(_ctx, getState());
		enterRule(_localctx, 90, RULE_postfixExpr);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(567);
			primaryExpr();
			setState(571);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,56,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(568);
					postfixSuffix();
					}
					} 
				}
				setState(573);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,56,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class PostfixSuffixContext extends ParserRuleContext {
		public PostfixSuffixContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_postfixSuffix; }
	 
		public PostfixSuffixContext() { }
		public void copyFrom(PostfixSuffixContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class CallSuffixContext extends PostfixSuffixContext {
		public TerminalNode LPAREN() { return getToken(AsterParser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(AsterParser.RPAREN, 0); }
		public ArgumentListContext argumentList() {
			return getRuleContext(ArgumentListContext.class,0);
		}
		public CallSuffixContext(PostfixSuffixContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterCallSuffix(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitCallSuffix(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitCallSuffix(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class MemberSuffixContext extends PostfixSuffixContext {
		public TerminalNode DOT() { return getToken(AsterParser.DOT, 0); }
		public TerminalNode IDENT() { return getToken(AsterParser.IDENT, 0); }
		public TerminalNode TYPE_IDENT() { return getToken(AsterParser.TYPE_IDENT, 0); }
		public MemberSuffixContext(PostfixSuffixContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterMemberSuffix(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitMemberSuffix(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitMemberSuffix(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PostfixSuffixContext postfixSuffix() throws RecognitionException {
		PostfixSuffixContext _localctx = new PostfixSuffixContext(_ctx, getState());
		enterRule(_localctx, 92, RULE_postfixSuffix);
		int _la;
		try {
			setState(581);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LPAREN:
				_localctx = new CallSuffixContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(574);
				match(LPAREN);
				setState(576);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 459648637100883792L) != 0)) {
					{
					setState(575);
					argumentList();
					}
				}

				setState(578);
				match(RPAREN);
				}
				break;
			case DOT:
				_localctx = new MemberSuffixContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(579);
				match(DOT);
				setState(580);
				_la = _input.LA(1);
				if ( !(_la==TYPE_IDENT || _la==IDENT) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ArgumentListContext extends ParserRuleContext {
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(AsterParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(AsterParser.COMMA, i);
		}
		public ArgumentListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_argumentList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterArgumentList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitArgumentList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitArgumentList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArgumentListContext argumentList() throws RecognitionException {
		ArgumentListContext _localctx = new ArgumentListContext(_ctx, getState());
		enterRule(_localctx, 94, RULE_argumentList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(583);
			expr();
			setState(588);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(584);
				match(COMMA);
				setState(585);
				expr();
				}
				}
				setState(590);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class PrimaryExprContext extends ParserRuleContext {
		public PrimaryExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_primaryExpr; }
	 
		public PrimaryExprContext() { }
		public void copyFrom(PrimaryExprContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class StringExprContext extends PrimaryExprContext {
		public TerminalNode STRING_LITERAL() { return getToken(AsterParser.STRING_LITERAL, 0); }
		public StringExprContext(PrimaryExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterStringExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitStringExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitStringExpr(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class BoolExprContext extends PrimaryExprContext {
		public TerminalNode BOOL_LITERAL() { return getToken(AsterParser.BOOL_LITERAL, 0); }
		public BoolExprContext(PrimaryExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterBoolExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitBoolExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitBoolExpr(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class FloatExprContext extends PrimaryExprContext {
		public TerminalNode FLOAT_LITERAL() { return getToken(AsterParser.FLOAT_LITERAL, 0); }
		public FloatExprContext(PrimaryExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterFloatExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitFloatExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitFloatExpr(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ConstructExprAltContext extends PrimaryExprContext {
		public ConstructExprContext constructExpr() {
			return getRuleContext(ConstructExprContext.class,0);
		}
		public ConstructExprAltContext(PrimaryExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterConstructExprAlt(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitConstructExprAlt(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitConstructExprAlt(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class TypeIdentExprContext extends PrimaryExprContext {
		public TerminalNode TYPE_IDENT() { return getToken(AsterParser.TYPE_IDENT, 0); }
		public TypeIdentExprContext(PrimaryExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterTypeIdentExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitTypeIdentExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitTypeIdentExpr(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class NullExprContext extends PrimaryExprContext {
		public TerminalNode NULL_LITERAL() { return getToken(AsterParser.NULL_LITERAL, 0); }
		public NullExprContext(PrimaryExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterNullExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitNullExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitNullExpr(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class OperatorCallExprContext extends PrimaryExprContext {
		public OperatorCallContext operatorCall() {
			return getRuleContext(OperatorCallContext.class,0);
		}
		public OperatorCallExprContext(PrimaryExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterOperatorCallExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitOperatorCallExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitOperatorCallExpr(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ListLiteralExprContext extends PrimaryExprContext {
		public ListLiteralContext listLiteral() {
			return getRuleContext(ListLiteralContext.class,0);
		}
		public ListLiteralExprContext(PrimaryExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterListLiteralExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitListLiteralExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitListLiteralExpr(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class MapIdentExprContext extends PrimaryExprContext {
		public TerminalNode MAP() { return getToken(AsterParser.MAP, 0); }
		public MapIdentExprContext(PrimaryExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterMapIdentExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitMapIdentExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitMapIdentExpr(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class VarExprContext extends PrimaryExprContext {
		public TerminalNode IDENT() { return getToken(AsterParser.IDENT, 0); }
		public VarExprContext(PrimaryExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterVarExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitVarExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitVarExpr(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class WrapExprAltContext extends PrimaryExprContext {
		public WrapExprContext wrapExpr() {
			return getRuleContext(WrapExprContext.class,0);
		}
		public WrapExprAltContext(PrimaryExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterWrapExprAlt(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitWrapExprAlt(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitWrapExprAlt(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class IntExprContext extends PrimaryExprContext {
		public TerminalNode INT_LITERAL() { return getToken(AsterParser.INT_LITERAL, 0); }
		public IntExprContext(PrimaryExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterIntExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitIntExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitIntExpr(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ParenExprContext extends PrimaryExprContext {
		public TerminalNode LPAREN() { return getToken(AsterParser.LPAREN, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(AsterParser.RPAREN, 0); }
		public ParenExprContext(PrimaryExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterParenExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitParenExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitParenExpr(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class LambdaExprAltContext extends PrimaryExprContext {
		public LambdaExprContext lambdaExpr() {
			return getRuleContext(LambdaExprContext.class,0);
		}
		public LambdaExprAltContext(PrimaryExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterLambdaExprAlt(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitLambdaExprAlt(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitLambdaExprAlt(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class LongExprContext extends PrimaryExprContext {
		public TerminalNode LONG_LITERAL() { return getToken(AsterParser.LONG_LITERAL, 0); }
		public LongExprContext(PrimaryExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterLongExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitLongExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitLongExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PrimaryExprContext primaryExpr() throws RecognitionException {
		PrimaryExprContext _localctx = new PrimaryExprContext(_ctx, getState());
		enterRule(_localctx, 96, RULE_primaryExpr);
		try {
			setState(609);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,60,_ctx) ) {
			case 1:
				_localctx = new LambdaExprAltContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(591);
				lambdaExpr();
				}
				break;
			case 2:
				_localctx = new OperatorCallExprContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(592);
				operatorCall();
				}
				break;
			case 3:
				_localctx = new ConstructExprAltContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(593);
				constructExpr();
				}
				break;
			case 4:
				_localctx = new WrapExprAltContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(594);
				wrapExpr();
				}
				break;
			case 5:
				_localctx = new ListLiteralExprContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(595);
				listLiteral();
				}
				break;
			case 6:
				_localctx = new VarExprContext(_localctx);
				enterOuterAlt(_localctx, 6);
				{
				setState(596);
				match(IDENT);
				}
				break;
			case 7:
				_localctx = new TypeIdentExprContext(_localctx);
				enterOuterAlt(_localctx, 7);
				{
				setState(597);
				match(TYPE_IDENT);
				}
				break;
			case 8:
				_localctx = new MapIdentExprContext(_localctx);
				enterOuterAlt(_localctx, 8);
				{
				setState(598);
				match(MAP);
				}
				break;
			case 9:
				_localctx = new StringExprContext(_localctx);
				enterOuterAlt(_localctx, 9);
				{
				setState(599);
				match(STRING_LITERAL);
				}
				break;
			case 10:
				_localctx = new IntExprContext(_localctx);
				enterOuterAlt(_localctx, 10);
				{
				setState(600);
				match(INT_LITERAL);
				}
				break;
			case 11:
				_localctx = new FloatExprContext(_localctx);
				enterOuterAlt(_localctx, 11);
				{
				setState(601);
				match(FLOAT_LITERAL);
				}
				break;
			case 12:
				_localctx = new LongExprContext(_localctx);
				enterOuterAlt(_localctx, 12);
				{
				setState(602);
				match(LONG_LITERAL);
				}
				break;
			case 13:
				_localctx = new BoolExprContext(_localctx);
				enterOuterAlt(_localctx, 13);
				{
				setState(603);
				match(BOOL_LITERAL);
				}
				break;
			case 14:
				_localctx = new NullExprContext(_localctx);
				enterOuterAlt(_localctx, 14);
				{
				setState(604);
				match(NULL_LITERAL);
				}
				break;
			case 15:
				_localctx = new ParenExprContext(_localctx);
				enterOuterAlt(_localctx, 15);
				{
				setState(605);
				match(LPAREN);
				setState(606);
				expr();
				setState(607);
				match(RPAREN);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ConstructExprContext extends ParserRuleContext {
		public TerminalNode TYPE_IDENT() { return getToken(AsterParser.TYPE_IDENT, 0); }
		public TerminalNode WITH() { return getToken(AsterParser.WITH, 0); }
		public ConstructFieldListContext constructFieldList() {
			return getRuleContext(ConstructFieldListContext.class,0);
		}
		public ConstructExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_constructExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterConstructExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitConstructExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitConstructExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConstructExprContext constructExpr() throws RecognitionException {
		ConstructExprContext _localctx = new ConstructExprContext(_ctx, getState());
		enterRule(_localctx, 98, RULE_constructExpr);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(611);
			match(TYPE_IDENT);
			setState(612);
			match(WITH);
			setState(613);
			constructFieldList();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ConstructFieldListContext extends ParserRuleContext {
		public List<ConstructFieldContext> constructField() {
			return getRuleContexts(ConstructFieldContext.class);
		}
		public ConstructFieldContext constructField(int i) {
			return getRuleContext(ConstructFieldContext.class,i);
		}
		public List<TerminalNode> AND() { return getTokens(AsterParser.AND); }
		public TerminalNode AND(int i) {
			return getToken(AsterParser.AND, i);
		}
		public List<TerminalNode> COMMA() { return getTokens(AsterParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(AsterParser.COMMA, i);
		}
		public ConstructFieldListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_constructFieldList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterConstructFieldList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitConstructFieldList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitConstructFieldList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConstructFieldListContext constructFieldList() throws RecognitionException {
		ConstructFieldListContext _localctx = new ConstructFieldListContext(_ctx, getState());
		enterRule(_localctx, 100, RULE_constructFieldList);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(615);
			constructField();
			setState(620);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,61,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(616);
					_la = _input.LA(1);
					if ( !(_la==COMMA || _la==AND) ) {
					_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					setState(617);
					constructField();
					}
					} 
				}
				setState(622);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,61,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ConstructFieldContext extends ParserRuleContext {
		public TerminalNode IDENT() { return getToken(AsterParser.IDENT, 0); }
		public TerminalNode EQUALS() { return getToken(AsterParser.EQUALS, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public ConstructFieldContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_constructField; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterConstructField(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitConstructField(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitConstructField(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConstructFieldContext constructField() throws RecognitionException {
		ConstructFieldContext _localctx = new ConstructFieldContext(_ctx, getState());
		enterRule(_localctx, 102, RULE_constructField);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(623);
			match(IDENT);
			setState(624);
			match(EQUALS);
			setState(625);
			expr();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class OperatorCallContext extends ParserRuleContext {
		public Token op;
		public TerminalNode LPAREN() { return getToken(AsterParser.LPAREN, 0); }
		public ArgumentListContext argumentList() {
			return getRuleContext(ArgumentListContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(AsterParser.RPAREN, 0); }
		public TerminalNode LT() { return getToken(AsterParser.LT, 0); }
		public TerminalNode GT() { return getToken(AsterParser.GT, 0); }
		public TerminalNode LTE() { return getToken(AsterParser.LTE, 0); }
		public TerminalNode GTE() { return getToken(AsterParser.GTE, 0); }
		public TerminalNode NEQ() { return getToken(AsterParser.NEQ, 0); }
		public TerminalNode EQUALS() { return getToken(AsterParser.EQUALS, 0); }
		public TerminalNode PLUS() { return getToken(AsterParser.PLUS, 0); }
		public TerminalNode MINUS() { return getToken(AsterParser.MINUS, 0); }
		public TerminalNode STAR() { return getToken(AsterParser.STAR, 0); }
		public TerminalNode SLASH() { return getToken(AsterParser.SLASH, 0); }
		public OperatorCallContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_operatorCall; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterOperatorCall(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitOperatorCall(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitOperatorCall(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OperatorCallContext operatorCall() throws RecognitionException {
		OperatorCallContext _localctx = new OperatorCallContext(_ctx, getState());
		enterRule(_localctx, 104, RULE_operatorCall);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(627);
			((OperatorCallContext)_localctx).op = _input.LT(1);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 261888L) != 0)) ) {
				((OperatorCallContext)_localctx).op = (Token)_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(628);
			match(LPAREN);
			setState(629);
			argumentList();
			setState(630);
			match(RPAREN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class WrapExprContext extends ParserRuleContext {
		public TerminalNode IDENT() { return getToken(AsterParser.IDENT, 0); }
		public TerminalNode OF() { return getToken(AsterParser.OF, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public WrapExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_wrapExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterWrapExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitWrapExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitWrapExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final WrapExprContext wrapExpr() throws RecognitionException {
		WrapExprContext _localctx = new WrapExprContext(_ctx, getState());
		enterRule(_localctx, 106, RULE_wrapExpr);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(632);
			match(IDENT);
			setState(633);
			match(OF);
			setState(634);
			expr();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ListLiteralContext extends ParserRuleContext {
		public TerminalNode LBRACKET() { return getToken(AsterParser.LBRACKET, 0); }
		public TerminalNode RBRACKET() { return getToken(AsterParser.RBRACKET, 0); }
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(AsterParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(AsterParser.COMMA, i);
		}
		public ListLiteralContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_listLiteral; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterListLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitListLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitListLiteral(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ListLiteralContext listLiteral() throws RecognitionException {
		ListLiteralContext _localctx = new ListLiteralContext(_ctx, getState());
		enterRule(_localctx, 108, RULE_listLiteral);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(636);
			match(LBRACKET);
			setState(645);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 459648637100883792L) != 0)) {
				{
				setState(637);
				expr();
				setState(642);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(638);
					match(COMMA);
					setState(639);
					expr();
					}
					}
					setState(644);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(647);
			match(RBRACKET);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class LambdaExprContext extends ParserRuleContext {
		public TerminalNode FUNCTION() { return getToken(AsterParser.FUNCTION, 0); }
		public TerminalNode PRODUCE() { return getToken(AsterParser.PRODUCE, 0); }
		public AnnotatedTypeContext annotatedType() {
			return getRuleContext(AnnotatedTypeContext.class,0);
		}
		public TerminalNode COLON() { return getToken(AsterParser.COLON, 0); }
		public ReturnStmtContext returnStmt() {
			return getRuleContext(ReturnStmtContext.class,0);
		}
		public ParamListContext paramList() {
			return getRuleContext(ParamListContext.class,0);
		}
		public TerminalNode COMMA() { return getToken(AsterParser.COMMA, 0); }
		public TerminalNode NEWLINE() { return getToken(AsterParser.NEWLINE, 0); }
		public BlockContext block() {
			return getRuleContext(BlockContext.class,0);
		}
		public LambdaExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_lambdaExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).enterLambdaExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsterParserListener ) ((AsterParserListener)listener).exitLambdaExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsterParserVisitor ) return ((AsterParserVisitor<? extends T>)visitor).visitLambdaExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LambdaExprContext lambdaExpr() throws RecognitionException {
		LambdaExprContext _localctx = new LambdaExprContext(_ctx, getState());
		enterRule(_localctx, 110, RULE_lambdaExpr);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(649);
			match(FUNCTION);
			setState(651);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WITH) {
				{
				setState(650);
				paramList();
				}
			}

			setState(654);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COMMA) {
				{
				setState(653);
				match(COMMA);
				}
			}

			setState(656);
			match(PRODUCE);
			setState(657);
			annotatedType();
			setState(658);
			match(COLON);
			setState(662);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case RETURN:
				{
				setState(659);
				returnStmt();
				}
				break;
			case NEWLINE:
				{
				{
				setState(660);
				match(NEWLINE);
				setState(661);
				block();
				}
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 26:
			return type_sempred((TypeContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean type_sempred(TypeContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return precpred(_ctx, 7);
		}
		return true;
	}

	public static final String _serializedATN =
		"\u0004\u0001?\u0299\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
		"\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004\u0002"+
		"\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007\u0002"+
		"\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b\u0007\u000b\u0002"+
		"\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e\u0002\u000f\u0007\u000f"+
		"\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011\u0002\u0012\u0007\u0012"+
		"\u0002\u0013\u0007\u0013\u0002\u0014\u0007\u0014\u0002\u0015\u0007\u0015"+
		"\u0002\u0016\u0007\u0016\u0002\u0017\u0007\u0017\u0002\u0018\u0007\u0018"+
		"\u0002\u0019\u0007\u0019\u0002\u001a\u0007\u001a\u0002\u001b\u0007\u001b"+
		"\u0002\u001c\u0007\u001c\u0002\u001d\u0007\u001d\u0002\u001e\u0007\u001e"+
		"\u0002\u001f\u0007\u001f\u0002 \u0007 \u0002!\u0007!\u0002\"\u0007\"\u0002"+
		"#\u0007#\u0002$\u0007$\u0002%\u0007%\u0002&\u0007&\u0002\'\u0007\'\u0002"+
		"(\u0007(\u0002)\u0007)\u0002*\u0007*\u0002+\u0007+\u0002,\u0007,\u0002"+
		"-\u0007-\u0002.\u0007.\u0002/\u0007/\u00020\u00070\u00021\u00071\u0002"+
		"2\u00072\u00023\u00073\u00024\u00074\u00025\u00075\u00026\u00076\u0002"+
		"7\u00077\u0001\u0000\u0005\u0000r\b\u0000\n\u0000\f\u0000u\t\u0000\u0001"+
		"\u0000\u0003\u0000x\b\u0000\u0001\u0000\u0005\u0000{\b\u0000\n\u0000\f"+
		"\u0000~\t\u0000\u0001\u0000\u0001\u0000\u0005\u0000\u0082\b\u0000\n\u0000"+
		"\f\u0000\u0085\t\u0000\u0005\u0000\u0087\b\u0000\n\u0000\f\u0000\u008a"+
		"\t\u0000\u0001\u0000\u0001\u0000\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0002\u0001\u0002\u0001\u0002\u0005"+
		"\u0002\u0097\b\u0002\n\u0002\f\u0002\u009a\t\u0002\u0001\u0003\u0001\u0003"+
		"\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0003\u0004"+
		"\u00a3\b\u0004\u0001\u0005\u0001\u0005\u0001\u0005\u0003\u0005\u00a8\b"+
		"\u0005\u0001\u0005\u0003\u0005\u00ab\b\u0005\u0001\u0005\u0003\u0005\u00ae"+
		"\b\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0003\u0005\u00b4"+
		"\b\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001"+
		"\u0005\u0001\u0005\u0003\u0005\u00bd\b\u0005\u0001\u0006\u0001\u0006\u0001"+
		"\u0006\u0001\u0006\u0005\u0006\u00c3\b\u0006\n\u0006\f\u0006\u00c6\t\u0006"+
		"\u0001\u0007\u0001\u0007\u0001\b\u0001\b\u0001\b\u0001\b\u0005\b\u00ce"+
		"\b\b\n\b\f\b\u00d1\t\b\u0001\t\u0005\t\u00d4\b\t\n\t\f\t\u00d7\t\t\u0001"+
		"\t\u0001\t\u0001\t\u0001\t\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001"+
		"\n\u0001\n\u0005\n\u00e4\b\n\n\n\f\n\u00e7\t\n\u0001\n\u0003\n\u00ea\b"+
		"\n\u0001\u000b\u0001\u000b\u0003\u000b\u00ee\b\u000b\u0001\u000b\u0001"+
		"\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001\f\u0001\f\u0001\f\u0005"+
		"\f\u00f8\b\f\n\f\f\f\u00fb\t\f\u0001\r\u0005\r\u00fe\b\r\n\r\f\r\u0101"+
		"\t\r\u0001\r\u0001\r\u0001\r\u0001\r\u0001\u000e\u0001\u000e\u0001\u000f"+
		"\u0001\u000f\u0003\u000f\u010b\b\u000f\u0001\u000f\u0001\u000f\u0001\u000f"+
		"\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u0010\u0001\u0010"+
		"\u0001\u0011\u0001\u0011\u0001\u0011\u0005\u0011\u0119\b\u0011\n\u0011"+
		"\f\u0011\u011c\t\u0011\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0012"+
		"\u0003\u0012\u0122\b\u0012\u0001\u0012\u0001\u0012\u0001\u0013\u0001\u0013"+
		"\u0001\u0014\u0005\u0014\u0129\b\u0014\n\u0014\f\u0014\u012c\t\u0014\u0001"+
		"\u0014\u0001\u0014\u0001\u0014\u0001\u0014\u0001\u0014\u0001\u0014\u0001"+
		"\u0015\u0001\u0015\u0001\u0015\u0003\u0015\u0137\b\u0015\u0001\u0016\u0001"+
		"\u0016\u0001\u0016\u0001\u0016\u0005\u0016\u013d\b\u0016\n\u0016\f\u0016"+
		"\u0140\t\u0016\u0003\u0016\u0142\b\u0016\u0001\u0016\u0001\u0016\u0001"+
		"\u0017\u0001\u0017\u0001\u0017\u0001\u0017\u0003\u0017\u014a\b\u0017\u0001"+
		"\u0018\u0001\u0018\u0001\u0019\u0005\u0019\u014f\b\u0019\n\u0019\f\u0019"+
		"\u0152\t\u0019\u0001\u0019\u0001\u0019\u0001\u001a\u0001\u001a\u0001\u001a"+
		"\u0001\u001a\u0001\u001a\u0001\u001a\u0001\u001a\u0001\u001a\u0001\u001a"+
		"\u0001\u001a\u0001\u001a\u0001\u001a\u0001\u001a\u0001\u001a\u0001\u001a"+
		"\u0001\u001a\u0001\u001a\u0005\u001a\u0167\b\u001a\n\u001a\f\u001a\u016a"+
		"\t\u001a\u0001\u001a\u0001\u001a\u0001\u001a\u0001\u001a\u0001\u001a\u0001"+
		"\u001a\u0001\u001a\u0001\u001a\u0001\u001a\u0001\u001a\u0003\u001a\u0176"+
		"\b\u001a\u0001\u001a\u0001\u001a\u0005\u001a\u017a\b\u001a\n\u001a\f\u001a"+
		"\u017d\t\u001a\u0001\u001b\u0001\u001b\u0001\u001b\u0005\u001b\u0182\b"+
		"\u001b\n\u001b\f\u001b\u0185\t\u001b\u0001\u001c\u0001\u001c\u0001\u001c"+
		"\u0004\u001c\u018a\b\u001c\u000b\u001c\f\u001c\u018b\u0001\u001c\u0001"+
		"\u001c\u0005\u001c\u0190\b\u001c\n\u001c\f\u001c\u0193\t\u001c\u0001\u001c"+
		"\u0005\u001c\u0196\b\u001c\n\u001c\f\u001c\u0199\t\u001c\u0001\u001c\u0001"+
		"\u001c\u0001\u001d\u0001\u001d\u0001\u001d\u0001\u001d\u0001\u001d\u0001"+
		"\u001d\u0001\u001d\u0001\u001d\u0003\u001d\u01a5\b\u001d\u0001\u001e\u0001"+
		"\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001"+
		"\u001e\u0001\u001e\u0001\u001e\u0003\u001e\u01b1\b\u001e\u0001\u001f\u0001"+
		"\u001f\u0001\u001f\u0001\u001f\u0001\u001f\u0001\u001f\u0001 \u0001 \u0001"+
		" \u0001 \u0003 \u01bd\b \u0001 \u0001 \u0001 \u0001!\u0001!\u0001!\u0001"+
		"!\u0001!\u0005!\u01c7\b!\n!\f!\u01ca\t!\u0001!\u0001!\u0001\"\u0001\""+
		"\u0001\"\u0001\"\u0001#\u0001#\u0001#\u0003#\u01d5\b#\u0001#\u0001#\u0001"+
		"#\u0001#\u0003#\u01db\b#\u0001#\u0001#\u0003#\u01df\b#\u0001#\u0001#\u0001"+
		"#\u0003#\u01e4\b#\u0001$\u0001$\u0001$\u0001$\u0001$\u0001$\u0001$\u0004"+
		"$\u01ed\b$\u000b$\f$\u01ee\u0001$\u0005$\u01f2\b$\n$\f$\u01f5\t$\u0001"+
		"$\u0005$\u01f8\b$\n$\f$\u01fb\t$\u0001$\u0001$\u0001%\u0001%\u0001%\u0001"+
		"%\u0001%\u0003%\u0204\b%\u0001&\u0001&\u0001&\u0001&\u0001&\u0001&\u0005"+
		"&\u020c\b&\n&\f&\u020f\t&\u0001&\u0001&\u0003&\u0213\b&\u0001&\u0001&"+
		"\u0003&\u0217\b&\u0001\'\u0001\'\u0001\'\u0001(\u0001(\u0001)\u0001)\u0001"+
		")\u0003)\u0221\b)\u0001*\u0001*\u0001*\u0005*\u0226\b*\n*\f*\u0229\t*"+
		"\u0001+\u0001+\u0001+\u0005+\u022e\b+\n+\f+\u0231\t+\u0001,\u0001,\u0001"+
		",\u0003,\u0236\b,\u0001-\u0001-\u0005-\u023a\b-\n-\f-\u023d\t-\u0001."+
		"\u0001.\u0003.\u0241\b.\u0001.\u0001.\u0001.\u0003.\u0246\b.\u0001/\u0001"+
		"/\u0001/\u0005/\u024b\b/\n/\f/\u024e\t/\u00010\u00010\u00010\u00010\u0001"+
		"0\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u0001"+
		"0\u00010\u00010\u00010\u00030\u0262\b0\u00011\u00011\u00011\u00011\u0001"+
		"2\u00012\u00012\u00052\u026b\b2\n2\f2\u026e\t2\u00013\u00013\u00013\u0001"+
		"3\u00014\u00014\u00014\u00014\u00014\u00015\u00015\u00015\u00015\u0001"+
		"6\u00016\u00016\u00016\u00056\u0281\b6\n6\f6\u0284\t6\u00036\u0286\b6"+
		"\u00016\u00016\u00017\u00017\u00037\u028c\b7\u00017\u00037\u028f\b7\u0001"+
		"7\u00017\u00017\u00017\u00017\u00017\u00037\u0297\b7\u00017\u0000\u0001"+
		"48\u0000\u0002\u0004\u0006\b\n\f\u000e\u0010\u0012\u0014\u0016\u0018\u001a"+
		"\u001c\u001e \"$&(*,.02468:<>@BDFHJLNPRTVXZ\\^`bdfhjln\u0000\t\u0001\u0000"+
		"9:\u0002\u0000\u0003\u0003  \u0002\u0000$$::\u0002\u0000\u0003\u0003 "+
		"!\u0003\u0000\u0015\u0016\u0018\u001a9:\u0002\u0000\b\n\u0010\u0011\u0002"+
		"\u0000\f\f\u000e\u000e\u0002\u0000\r\r\u000f\u000f\u0001\u0000\b\u0011"+
		"\u02c0\u0000s\u0001\u0000\u0000\u0000\u0002\u008d\u0001\u0000\u0000\u0000"+
		"\u0004\u0093\u0001\u0000\u0000\u0000\u0006\u009b\u0001\u0000\u0000\u0000"+
		"\b\u00a2\u0001\u0000\u0000\u0000\n\u00a4\u0001\u0000\u0000\u0000\f\u00be"+
		"\u0001\u0000\u0000\u0000\u000e\u00c7\u0001\u0000\u0000\u0000\u0010\u00c9"+
		"\u0001\u0000\u0000\u0000\u0012\u00d5\u0001\u0000\u0000\u0000\u0014\u00dc"+
		"\u0001\u0000\u0000\u0000\u0016\u00eb\u0001\u0000\u0000\u0000\u0018\u00f4"+
		"\u0001\u0000\u0000\u0000\u001a\u00ff\u0001\u0000\u0000\u0000\u001c\u0106"+
		"\u0001\u0000\u0000\u0000\u001e\u0108\u0001\u0000\u0000\u0000 \u0113\u0001"+
		"\u0000\u0000\u0000\"\u0115\u0001\u0000\u0000\u0000$\u011d\u0001\u0000"+
		"\u0000\u0000&\u0125\u0001\u0000\u0000\u0000(\u012a\u0001\u0000\u0000\u0000"+
		"*\u0133\u0001\u0000\u0000\u0000,\u0138\u0001\u0000\u0000\u0000.\u0149"+
		"\u0001\u0000\u0000\u00000\u014b\u0001\u0000\u0000\u00002\u0150\u0001\u0000"+
		"\u0000\u00004\u0175\u0001\u0000\u0000\u00006\u017e\u0001\u0000\u0000\u0000"+
		"8\u0186\u0001\u0000\u0000\u0000:\u01a4\u0001\u0000\u0000\u0000<\u01b0"+
		"\u0001\u0000\u0000\u0000>\u01b2\u0001\u0000\u0000\u0000@\u01b8\u0001\u0000"+
		"\u0000\u0000B\u01c1\u0001\u0000\u0000\u0000D\u01cd\u0001\u0000\u0000\u0000"+
		"F\u01d1\u0001\u0000\u0000\u0000H\u01e5\u0001\u0000\u0000\u0000J\u01fe"+
		"\u0001\u0000\u0000\u0000L\u0216\u0001\u0000\u0000\u0000N\u0218\u0001\u0000"+
		"\u0000\u0000P\u021b\u0001\u0000\u0000\u0000R\u021d\u0001\u0000\u0000\u0000"+
		"T\u0222\u0001\u0000\u0000\u0000V\u022a\u0001\u0000\u0000\u0000X\u0235"+
		"\u0001\u0000\u0000\u0000Z\u0237\u0001\u0000\u0000\u0000\\\u0245\u0001"+
		"\u0000\u0000\u0000^\u0247\u0001\u0000\u0000\u0000`\u0261\u0001\u0000\u0000"+
		"\u0000b\u0263\u0001\u0000\u0000\u0000d\u0267\u0001\u0000\u0000\u0000f"+
		"\u026f\u0001\u0000\u0000\u0000h\u0273\u0001\u0000\u0000\u0000j\u0278\u0001"+
		"\u0000\u0000\u0000l\u027c\u0001\u0000\u0000\u0000n\u0289\u0001\u0000\u0000"+
		"\u0000pr\u0005<\u0000\u0000qp\u0001\u0000\u0000\u0000ru\u0001\u0000\u0000"+
		"\u0000sq\u0001\u0000\u0000\u0000st\u0001\u0000\u0000\u0000tw\u0001\u0000"+
		"\u0000\u0000us\u0001\u0000\u0000\u0000vx\u0003\u0002\u0001\u0000wv\u0001"+
		"\u0000\u0000\u0000wx\u0001\u0000\u0000\u0000x|\u0001\u0000\u0000\u0000"+
		"y{\u0005<\u0000\u0000zy\u0001\u0000\u0000\u0000{~\u0001\u0000\u0000\u0000"+
		"|z\u0001\u0000\u0000\u0000|}\u0001\u0000\u0000\u0000}\u0088\u0001\u0000"+
		"\u0000\u0000~|\u0001\u0000\u0000\u0000\u007f\u0083\u0003\b\u0004\u0000"+
		"\u0080\u0082\u0005<\u0000\u0000\u0081\u0080\u0001\u0000\u0000\u0000\u0082"+
		"\u0085\u0001\u0000\u0000\u0000\u0083\u0081\u0001\u0000\u0000\u0000\u0083"+
		"\u0084\u0001\u0000\u0000\u0000\u0084\u0087\u0001\u0000\u0000\u0000\u0085"+
		"\u0083\u0001\u0000\u0000\u0000\u0086\u007f\u0001\u0000\u0000\u0000\u0087"+
		"\u008a\u0001\u0000\u0000\u0000\u0088\u0086\u0001\u0000\u0000\u0000\u0088"+
		"\u0089\u0001\u0000\u0000\u0000\u0089\u008b\u0001\u0000\u0000\u0000\u008a"+
		"\u0088\u0001\u0000\u0000\u0000\u008b\u008c\u0005\u0000\u0000\u0001\u008c"+
		"\u0001\u0001\u0000\u0000\u0000\u008d\u008e\u0005\u001b\u0000\u0000\u008e"+
		"\u008f\u0005\u001c\u0000\u0000\u008f\u0090\u0005\u001d\u0000\u0000\u0090"+
		"\u0091\u0003\u0004\u0002\u0000\u0091\u0092\u0005\u0001\u0000\u0000\u0092"+
		"\u0003\u0001\u0000\u0000\u0000\u0093\u0098\u0003\u0006\u0003\u0000\u0094"+
		"\u0095\u0005\u0001\u0000\u0000\u0095\u0097\u0003\u0006\u0003\u0000\u0096"+
		"\u0094\u0001\u0000\u0000\u0000\u0097\u009a\u0001\u0000\u0000\u0000\u0098"+
		"\u0096\u0001\u0000\u0000\u0000\u0098\u0099\u0001\u0000\u0000\u0000\u0099"+
		"\u0005\u0001\u0000\u0000\u0000\u009a\u0098\u0001\u0000\u0000\u0000\u009b"+
		"\u009c\u0007\u0000\u0000\u0000\u009c\u0007\u0001\u0000\u0000\u0000\u009d"+
		"\u00a3\u0003\n\u0005\u0000\u009e\u00a3\u0003\u0016\u000b\u0000\u009f\u00a3"+
		"\u0003\u001e\u000f\u0000\u00a0\u00a3\u0003(\u0014\u0000\u00a1\u00a3\u0003"+
		"$\u0012\u0000\u00a2\u009d\u0001\u0000\u0000\u0000\u00a2\u009e\u0001\u0000"+
		"\u0000\u0000\u00a2\u009f\u0001\u0000\u0000\u0000\u00a2\u00a0\u0001\u0000"+
		"\u0000\u0000\u00a2\u00a1\u0001\u0000\u0000\u0000\u00a3\t\u0001\u0000\u0000"+
		"\u0000\u00a4\u00a5\u0005\u001e\u0000\u0000\u00a5\u00a7\u0005:\u0000\u0000"+
		"\u00a6\u00a8\u0003\f\u0006\u0000\u00a7\u00a6\u0001\u0000\u0000\u0000\u00a7"+
		"\u00a8\u0001\u0000\u0000\u0000\u00a8\u00aa\u0001\u0000\u0000\u0000\u00a9"+
		"\u00ab\u0003\u0010\b\u0000\u00aa\u00a9\u0001\u0000\u0000\u0000\u00aa\u00ab"+
		"\u0001\u0000\u0000\u0000\u00ab\u00ad\u0001\u0000\u0000\u0000\u00ac\u00ae"+
		"\u0005\u0003\u0000\u0000\u00ad\u00ac\u0001\u0000\u0000\u0000\u00ad\u00ae"+
		"\u0001\u0000\u0000\u0000\u00ae\u00af\u0001\u0000\u0000\u0000\u00af\u00b0"+
		"\u0005\"\u0000\u0000\u00b0\u00bc\u00032\u0019\u0000\u00b1\u00b3\u0005"+
		"\u0001\u0000\u0000\u00b2\u00b4\u0003\u0014\n\u0000\u00b3\u00b2\u0001\u0000"+
		"\u0000\u0000\u00b3\u00b4\u0001\u0000\u0000\u0000\u00b4\u00b5\u0001\u0000"+
		"\u0000\u0000\u00b5\u00b6\u0005\u0002\u0000\u0000\u00b6\u00b7\u0005<\u0000"+
		"\u0000\u00b7\u00bd\u00038\u001c\u0000\u00b8\u00b9\u0005\u0002\u0000\u0000"+
		"\u00b9\u00ba\u0005<\u0000\u0000\u00ba\u00bd\u00038\u001c\u0000\u00bb\u00bd"+
		"\u0005\u0001\u0000\u0000\u00bc\u00b1\u0001\u0000\u0000\u0000\u00bc\u00b8"+
		"\u0001\u0000\u0000\u0000\u00bc\u00bb\u0001\u0000\u0000\u0000\u00bd\u000b"+
		"\u0001\u0000\u0000\u0000\u00be\u00bf\u0005\'\u0000\u0000\u00bf\u00c4\u0003"+
		"\u000e\u0007\u0000\u00c0\u00c1\u0007\u0001\u0000\u0000\u00c1\u00c3\u0003"+
		"\u000e\u0007\u0000\u00c2\u00c0\u0001\u0000\u0000\u0000\u00c3\u00c6\u0001"+
		"\u0000\u0000\u0000\u00c4\u00c2\u0001\u0000\u0000\u0000\u00c4\u00c5\u0001"+
		"\u0000\u0000\u0000\u00c5\r\u0001\u0000\u0000\u0000\u00c6\u00c4\u0001\u0000"+
		"\u0000\u0000\u00c7\u00c8\u0007\u0000\u0000\u0000\u00c8\u000f\u0001\u0000"+
		"\u0000\u0000\u00c9\u00ca\u0005\u001f\u0000\u0000\u00ca\u00cf\u0003\u0012"+
		"\t\u0000\u00cb\u00cc\u0007\u0001\u0000\u0000\u00cc\u00ce\u0003\u0012\t"+
		"\u0000\u00cd\u00cb\u0001\u0000\u0000\u0000\u00ce\u00d1\u0001\u0000\u0000"+
		"\u0000\u00cf\u00cd\u0001\u0000\u0000\u0000\u00cf\u00d0\u0001\u0000\u0000"+
		"\u0000\u00d0\u0011\u0001\u0000\u0000\u0000\u00d1\u00cf\u0001\u0000\u0000"+
		"\u0000\u00d2\u00d4\u0003*\u0015\u0000\u00d3\u00d2\u0001\u0000\u0000\u0000"+
		"\u00d4\u00d7\u0001\u0000\u0000\u0000\u00d5\u00d3\u0001\u0000\u0000\u0000"+
		"\u00d5\u00d6\u0001\u0000\u0000\u0000\u00d6\u00d8\u0001\u0000\u0000\u0000"+
		"\u00d7\u00d5\u0001\u0000\u0000\u0000\u00d8\u00d9\u0003\u001c\u000e\u0000"+
		"\u00d9\u00da\u0005\u0002\u0000\u0000\u00da\u00db\u00032\u0019\u0000\u00db"+
		"\u0013\u0001\u0000\u0000\u0000\u00dc\u00dd\u00057\u0000\u0000\u00dd\u00de"+
		"\u00058\u0000\u0000\u00de\u00e9\u0005:\u0000\u0000\u00df\u00e0\u0005\u0006"+
		"\u0000\u0000\u00e0\u00e5\u00059\u0000\u0000\u00e1\u00e2\u0005\u0003\u0000"+
		"\u0000\u00e2\u00e4\u00059\u0000\u0000\u00e3\u00e1\u0001\u0000\u0000\u0000"+
		"\u00e4\u00e7\u0001\u0000\u0000\u0000\u00e5\u00e3\u0001\u0000\u0000\u0000"+
		"\u00e5\u00e6\u0001\u0000\u0000\u0000\u00e6\u00e8\u0001\u0000\u0000\u0000"+
		"\u00e7\u00e5\u0001\u0000\u0000\u0000\u00e8\u00ea\u0005\u0007\u0000\u0000"+
		"\u00e9\u00df\u0001\u0000\u0000\u0000\u00e9\u00ea\u0001\u0000\u0000\u0000"+
		"\u00ea\u0015\u0001\u0000\u0000\u0000\u00eb\u00ed\u0005#\u0000\u0000\u00ec"+
		"\u00ee\u0003 \u0010\u0000\u00ed\u00ec\u0001\u0000\u0000\u0000\u00ed\u00ee"+
		"\u0001\u0000\u0000\u0000\u00ee\u00ef\u0001\u0000\u0000\u0000\u00ef\u00f0"+
		"\u00059\u0000\u0000\u00f0\u00f1\u0005\u001f\u0000\u0000\u00f1\u00f2\u0003"+
		"\u0018\f\u0000\u00f2\u00f3\u0005\u0001\u0000\u0000\u00f3\u0017\u0001\u0000"+
		"\u0000\u0000\u00f4\u00f9\u0003\u001a\r\u0000\u00f5\u00f6\u0007\u0001\u0000"+
		"\u0000\u00f6\u00f8\u0003\u001a\r\u0000\u00f7\u00f5\u0001\u0000\u0000\u0000"+
		"\u00f8\u00fb\u0001\u0000\u0000\u0000\u00f9\u00f7\u0001\u0000\u0000\u0000"+
		"\u00f9\u00fa\u0001\u0000\u0000\u0000\u00fa\u0019\u0001\u0000\u0000\u0000"+
		"\u00fb\u00f9\u0001\u0000\u0000\u0000\u00fc\u00fe\u0003*\u0015\u0000\u00fd"+
		"\u00fc\u0001\u0000\u0000\u0000\u00fe\u0101\u0001\u0000\u0000\u0000\u00ff"+
		"\u00fd\u0001\u0000\u0000\u0000\u00ff\u0100\u0001\u0000\u0000\u0000\u0100"+
		"\u0102\u0001\u0000\u0000\u0000\u0101\u00ff\u0001\u0000\u0000\u0000\u0102"+
		"\u0103\u0003\u001c\u000e\u0000\u0103\u0104\u0005\u0002\u0000\u0000\u0104"+
		"\u0105\u00032\u0019\u0000\u0105\u001b\u0001\u0000\u0000\u0000\u0106\u0107"+
		"\u0007\u0002\u0000\u0000\u0107\u001d\u0001\u0000\u0000\u0000\u0108\u010a"+
		"\u0005#\u0000\u0000\u0109\u010b\u0003 \u0010\u0000\u010a\u0109\u0001\u0000"+
		"\u0000\u0000\u010a\u010b\u0001\u0000\u0000\u0000\u010b\u010c\u0001\u0000"+
		"\u0000\u0000\u010c\u010d\u00059\u0000\u0000\u010d\u010e\u0005%\u0000\u0000"+
		"\u010e\u010f\u0005&\u0000\u0000\u010f\u0110\u0005\'\u0000\u0000\u0110"+
		"\u0111\u0003\"\u0011\u0000\u0111\u0112\u0005\u0001\u0000\u0000\u0112\u001f"+
		"\u0001\u0000\u0000\u0000\u0113\u0114\u0005:\u0000\u0000\u0114!\u0001\u0000"+
		"\u0000\u0000\u0115\u011a\u00059\u0000\u0000\u0116\u0117\u0007\u0003\u0000"+
		"\u0000\u0117\u0119\u00059\u0000\u0000\u0118\u0116\u0001\u0000\u0000\u0000"+
		"\u0119\u011c\u0001\u0000\u0000\u0000\u011a\u0118\u0001\u0000\u0000\u0000"+
		"\u011a\u011b\u0001\u0000\u0000\u0000\u011b#\u0001\u0000\u0000\u0000\u011c"+
		"\u011a\u0001\u0000\u0000\u0000\u011d\u011e\u0005(\u0000\u0000\u011e\u0121"+
		"\u0003\u0004\u0002\u0000\u011f\u0120\u0005%\u0000\u0000\u0120\u0122\u0003"+
		"&\u0013\u0000\u0121\u011f\u0001\u0000\u0000\u0000\u0121\u0122\u0001\u0000"+
		"\u0000\u0000\u0122\u0123\u0001\u0000\u0000\u0000\u0123\u0124\u0005\u0001"+
		"\u0000\u0000\u0124%\u0001\u0000\u0000\u0000\u0125\u0126\u0007\u0000\u0000"+
		"\u0000\u0126\'\u0001\u0000\u0000\u0000\u0127\u0129\u0003*\u0015\u0000"+
		"\u0128\u0127\u0001\u0000\u0000\u0000\u0129\u012c\u0001\u0000\u0000\u0000"+
		"\u012a\u0128\u0001\u0000\u0000\u0000\u012a\u012b\u0001\u0000\u0000\u0000"+
		"\u012b\u012d\u0001\u0000\u0000\u0000\u012c\u012a\u0001\u0000\u0000\u0000"+
		"\u012d\u012e\u0005$\u0000\u0000\u012e\u012f\u0007\u0000\u0000\u0000\u012f"+
		"\u0130\u0005%\u0000\u0000\u0130\u0131\u00032\u0019\u0000\u0131\u0132\u0005"+
		"\u0001\u0000\u0000\u0132)\u0001\u0000\u0000\u0000\u0133\u0134\u0005\u0013"+
		"\u0000\u0000\u0134\u0136\u0007\u0000\u0000\u0000\u0135\u0137\u0003,\u0016"+
		"\u0000\u0136\u0135\u0001\u0000\u0000\u0000\u0136\u0137\u0001\u0000\u0000"+
		"\u0000\u0137+\u0001\u0000\u0000\u0000\u0138\u0141\u0005\u0004\u0000\u0000"+
		"\u0139\u013e\u0003.\u0017\u0000\u013a\u013b\u0005\u0003\u0000\u0000\u013b"+
		"\u013d\u0003.\u0017\u0000\u013c\u013a\u0001\u0000\u0000\u0000\u013d\u0140"+
		"\u0001\u0000\u0000\u0000\u013e\u013c\u0001\u0000\u0000\u0000\u013e\u013f"+
		"\u0001\u0000\u0000\u0000\u013f\u0142\u0001\u0000\u0000\u0000\u0140\u013e"+
		"\u0001\u0000\u0000\u0000\u0141\u0139\u0001\u0000\u0000\u0000\u0141\u0142"+
		"\u0001\u0000\u0000\u0000\u0142\u0143\u0001\u0000\u0000\u0000\u0143\u0144"+
		"\u0005\u0005\u0000\u0000\u0144-\u0001\u0000\u0000\u0000\u0145\u0146\u0005"+
		":\u0000\u0000\u0146\u0147\u0005\u0002\u0000\u0000\u0147\u014a\u00030\u0018"+
		"\u0000\u0148\u014a\u00030\u0018\u0000\u0149\u0145\u0001\u0000\u0000\u0000"+
		"\u0149\u0148\u0001\u0000\u0000\u0000\u014a/\u0001\u0000\u0000\u0000\u014b"+
		"\u014c\u0007\u0004\u0000\u0000\u014c1\u0001\u0000\u0000\u0000\u014d\u014f"+
		"\u0003*\u0015\u0000\u014e\u014d\u0001\u0000\u0000\u0000\u014f\u0152\u0001"+
		"\u0000\u0000\u0000\u0150\u014e\u0001\u0000\u0000\u0000\u0150\u0151\u0001"+
		"\u0000\u0000\u0000\u0151\u0153\u0001\u0000\u0000\u0000\u0152\u0150\u0001"+
		"\u0000\u0000\u0000\u0153\u0154\u00034\u001a\u0000\u01543\u0001\u0000\u0000"+
		"\u0000\u0155\u0156\u0006\u001a\uffff\uffff\u0000\u0156\u0157\u0005\u0004"+
		"\u0000\u0000\u0157\u0158\u00036\u001b\u0000\u0158\u0159\u0005\u0005\u0000"+
		"\u0000\u0159\u015a\u0005\u0014\u0000\u0000\u015a\u015b\u00034\u001a\u0006"+
		"\u015b\u0176\u0001\u0000\u0000\u0000\u015c\u015d\u00056\u0000\u0000\u015d"+
		"\u015e\u00032\u0019\u0000\u015e\u015f\u0005\u001e\u0000\u0000\u015f\u0160"+
		"\u00032\u0019\u0000\u0160\u0176\u0001\u0000\u0000\u0000\u0161\u0162\u0007"+
		"\u0000\u0000\u0000\u0162\u0163\u0005\'\u0000\u0000\u0163\u0168\u00032"+
		"\u0019\u0000\u0164\u0165\u0007\u0001\u0000\u0000\u0165\u0167\u00032\u0019"+
		"\u0000\u0166\u0164\u0001\u0000\u0000\u0000\u0167\u016a\u0001\u0000\u0000"+
		"\u0000\u0168\u0166\u0001\u0000\u0000\u0000\u0168\u0169\u0001\u0000\u0000"+
		"\u0000\u0169\u0176\u0001\u0000\u0000\u0000\u016a\u0168\u0001\u0000\u0000"+
		"\u0000\u016b\u016c\u00059\u0000\u0000\u016c\u016d\u0005\u0010\u0000\u0000"+
		"\u016d\u016e\u00036\u001b\u0000\u016e\u016f\u0005\u0011\u0000\u0000\u016f"+
		"\u0176\u0001\u0000\u0000\u0000\u0170\u0176\u00059\u0000\u0000\u0171\u0172"+
		"\u0005\u0004\u0000\u0000\u0172\u0173\u00034\u001a\u0000\u0173\u0174\u0005"+
		"\u0005\u0000\u0000\u0174\u0176\u0001\u0000\u0000\u0000\u0175\u0155\u0001"+
		"\u0000\u0000\u0000\u0175\u015c\u0001\u0000\u0000\u0000\u0175\u0161\u0001"+
		"\u0000\u0000\u0000\u0175\u016b\u0001\u0000\u0000\u0000\u0175\u0170\u0001"+
		"\u0000\u0000\u0000\u0175\u0171\u0001\u0000\u0000\u0000\u0176\u017b\u0001"+
		"\u0000\u0000\u0000\u0177\u0178\n\u0007\u0000\u0000\u0178\u017a\u0005\u0012"+
		"\u0000\u0000\u0179\u0177\u0001\u0000\u0000\u0000\u017a\u017d\u0001\u0000"+
		"\u0000\u0000\u017b\u0179\u0001\u0000\u0000\u0000\u017b\u017c\u0001\u0000"+
		"\u0000\u0000\u017c5\u0001\u0000\u0000\u0000\u017d\u017b\u0001\u0000\u0000"+
		"\u0000\u017e\u0183\u00032\u0019\u0000\u017f\u0180\u0005\u0003\u0000\u0000"+
		"\u0180\u0182\u00032\u0019\u0000\u0181\u017f\u0001\u0000\u0000\u0000\u0182"+
		"\u0185\u0001\u0000\u0000\u0000\u0183\u0181\u0001\u0000\u0000\u0000\u0183"+
		"\u0184\u0001\u0000\u0000\u0000\u01847\u0001\u0000\u0000\u0000\u0185\u0183"+
		"\u0001\u0000\u0000\u0000\u0186\u0187\u0005>\u0000\u0000\u0187\u0191\u0003"+
		":\u001d\u0000\u0188\u018a\u0005<\u0000\u0000\u0189\u0188\u0001\u0000\u0000"+
		"\u0000\u018a\u018b\u0001\u0000\u0000\u0000\u018b\u0189\u0001\u0000\u0000"+
		"\u0000\u018b\u018c\u0001\u0000\u0000\u0000\u018c\u018d\u0001\u0000\u0000"+
		"\u0000\u018d\u0190\u0003:\u001d\u0000\u018e\u0190\u0003:\u001d\u0000\u018f"+
		"\u0189\u0001\u0000\u0000\u0000\u018f\u018e\u0001\u0000\u0000\u0000\u0190"+
		"\u0193\u0001\u0000\u0000\u0000\u0191\u018f\u0001\u0000\u0000\u0000\u0191"+
		"\u0192\u0001\u0000\u0000\u0000\u0192\u0197\u0001\u0000\u0000\u0000\u0193"+
		"\u0191\u0001\u0000\u0000\u0000\u0194\u0196\u0005<\u0000\u0000\u0195\u0194"+
		"\u0001\u0000\u0000\u0000\u0196\u0199\u0001\u0000\u0000\u0000\u0197\u0195"+
		"\u0001\u0000\u0000\u0000\u0197\u0198\u0001\u0000\u0000\u0000\u0198\u019a"+
		"\u0001\u0000\u0000\u0000\u0199\u0197\u0001\u0000\u0000\u0000\u019a\u019b"+
		"\u0005?\u0000\u0000\u019b9\u0001\u0000\u0000\u0000\u019c\u01a5\u0003<"+
		"\u001e\u0000\u019d\u01a5\u0003>\u001f\u0000\u019e\u01a5\u0003@ \u0000"+
		"\u019f\u01a5\u0003B!\u0000\u01a0\u01a5\u0003D\"\u0000\u01a1\u01a5\u0003"+
		"F#\u0000\u01a2\u01a5\u0003H$\u0000\u01a3\u01a5\u0003N\'\u0000\u01a4\u019c"+
		"\u0001\u0000\u0000\u0000\u01a4\u019d\u0001\u0000\u0000\u0000\u01a4\u019e"+
		"\u0001\u0000\u0000\u0000\u01a4\u019f\u0001\u0000\u0000\u0000\u01a4\u01a0"+
		"\u0001\u0000\u0000\u0000\u01a4\u01a1\u0001\u0000\u0000\u0000\u01a4\u01a2"+
		"\u0001\u0000\u0000\u0000\u01a4\u01a3\u0001\u0000\u0000\u0000\u01a5;\u0001"+
		"\u0000\u0000\u0000\u01a6\u01a7\u0005)\u0000\u0000\u01a7\u01a8\u0005:\u0000"+
		"\u0000\u01a8\u01a9\u0005*\u0000\u0000\u01a9\u01b1\u0003n7\u0000\u01aa"+
		"\u01ab\u0005)\u0000\u0000\u01ab\u01ac\u0005:\u0000\u0000\u01ac\u01ad\u0005"+
		"*\u0000\u0000\u01ad\u01ae\u0003P(\u0000\u01ae\u01af\u0005\u0001\u0000"+
		"\u0000\u01af\u01b1\u0001\u0000\u0000\u0000\u01b0\u01a6\u0001\u0000\u0000"+
		"\u0000\u01b0\u01aa\u0001\u0000\u0000\u0000\u01b1=\u0001\u0000\u0000\u0000"+
		"\u01b2\u01b3\u0005#\u0000\u0000\u01b3\u01b4\u0003\u001c\u000e\u0000\u01b4"+
		"\u01b5\u0005%\u0000\u0000\u01b5\u01b6\u0003P(\u0000\u01b6\u01b7\u0005"+
		"\u0001\u0000\u0000\u01b7?\u0001\u0000\u0000\u0000\u01b8\u01b9\u00051\u0000"+
		"\u0000\u01b9\u01ba\u0003\u001c\u000e\u0000\u01ba\u01bc\u0005%\u0000\u0000"+
		"\u01bb\u01bd\u00054\u0000\u0000\u01bc\u01bb\u0001\u0000\u0000\u0000\u01bc"+
		"\u01bd\u0001\u0000\u0000\u0000\u01bd\u01be\u0001\u0000\u0000\u0000\u01be"+
		"\u01bf\u0003P(\u0000\u01bf\u01c0\u0005\u0001\u0000\u0000\u01c0A\u0001"+
		"\u0000\u0000\u0000\u01c1\u01c2\u00052\u0000\u0000\u01c2\u01c3\u00053\u0000"+
		"\u0000\u01c3\u01c8\u0003\u001c\u000e\u0000\u01c4\u01c5\u0005 \u0000\u0000"+
		"\u01c5\u01c7\u0003\u001c\u000e\u0000\u01c6\u01c4\u0001\u0000\u0000\u0000"+
		"\u01c7\u01ca\u0001\u0000\u0000\u0000\u01c8\u01c6\u0001\u0000\u0000\u0000"+
		"\u01c8\u01c9\u0001\u0000\u0000\u0000\u01c9\u01cb\u0001\u0000\u0000\u0000"+
		"\u01ca\u01c8\u0001\u0000\u0000\u0000\u01cb\u01cc\u0005\u0001\u0000\u0000"+
		"\u01ccC\u0001\u0000\u0000\u0000\u01cd\u01ce\u0005+\u0000\u0000\u01ce\u01cf"+
		"\u0003P(\u0000\u01cf\u01d0\u0005\u0001\u0000\u0000\u01d0E\u0001\u0000"+
		"\u0000\u0000\u01d1\u01d2\u0005,\u0000\u0000\u01d2\u01d4\u0003P(\u0000"+
		"\u01d3\u01d5\u0005\u0003\u0000\u0000\u01d4\u01d3\u0001\u0000\u0000\u0000"+
		"\u01d4\u01d5\u0001\u0000\u0000\u0000\u01d5\u01d6\u0001\u0000\u0000\u0000"+
		"\u01d6\u01d7\u0005\u0002\u0000\u0000\u01d7\u01d8\u0005<\u0000\u0000\u01d8"+
		"\u01e3\u00038\u001c\u0000\u01d9\u01db\u0005<\u0000\u0000\u01da\u01d9\u0001"+
		"\u0000\u0000\u0000\u01da\u01db\u0001\u0000\u0000\u0000\u01db\u01dc\u0001"+
		"\u0000\u0000\u0000\u01dc\u01de\u0005-\u0000\u0000\u01dd\u01df\u0005\u0003"+
		"\u0000\u0000\u01de\u01dd\u0001\u0000\u0000\u0000\u01de\u01df\u0001\u0000"+
		"\u0000\u0000\u01df\u01e0\u0001\u0000\u0000\u0000\u01e0\u01e1\u0005\u0002"+
		"\u0000\u0000\u01e1\u01e2\u0005<\u0000\u0000\u01e2\u01e4\u00038\u001c\u0000"+
		"\u01e3\u01da\u0001\u0000\u0000\u0000\u01e3\u01e4\u0001\u0000\u0000\u0000"+
		"\u01e4G\u0001\u0000\u0000\u0000\u01e5\u01e6\u0005.\u0000\u0000\u01e6\u01e7"+
		"\u0003P(\u0000\u01e7\u01e8\u0005\u0002\u0000\u0000\u01e8\u01e9\u0005<"+
		"\u0000\u0000\u01e9\u01ea\u0005>\u0000\u0000\u01ea\u01f3\u0003J%\u0000"+
		"\u01eb\u01ed\u0005<\u0000\u0000\u01ec\u01eb\u0001\u0000\u0000\u0000\u01ed"+
		"\u01ee\u0001\u0000\u0000\u0000\u01ee\u01ec\u0001\u0000\u0000\u0000\u01ee"+
		"\u01ef\u0001\u0000\u0000\u0000\u01ef\u01f0\u0001\u0000\u0000\u0000\u01f0"+
		"\u01f2\u0003J%\u0000\u01f1\u01ec\u0001\u0000\u0000\u0000\u01f2\u01f5\u0001"+
		"\u0000\u0000\u0000\u01f3\u01f1\u0001\u0000\u0000\u0000\u01f3\u01f4\u0001"+
		"\u0000\u0000\u0000\u01f4\u01f9\u0001\u0000\u0000\u0000\u01f5\u01f3\u0001"+
		"\u0000\u0000\u0000\u01f6\u01f8\u0005<\u0000\u0000\u01f7\u01f6\u0001\u0000"+
		"\u0000\u0000\u01f8\u01fb\u0001\u0000\u0000\u0000\u01f9\u01f7\u0001\u0000"+
		"\u0000\u0000\u01f9\u01fa\u0001\u0000\u0000\u0000\u01fa\u01fc\u0001\u0000"+
		"\u0000\u0000\u01fb\u01f9\u0001\u0000\u0000\u0000\u01fc\u01fd\u0005?\u0000"+
		"\u0000\u01fdI\u0001\u0000\u0000\u0000\u01fe\u01ff\u0005/\u0000\u0000\u01ff"+
		"\u0200\u0003L&\u0000\u0200\u0203\u0005\u0003\u0000\u0000\u0201\u0204\u0003"+
		"D\"\u0000\u0202\u0204\u00038\u001c\u0000\u0203\u0201\u0001\u0000\u0000"+
		"\u0000\u0203\u0202\u0001\u0000\u0000\u0000\u0204K\u0001\u0000\u0000\u0000"+
		"\u0205\u0217\u0005\u0017\u0000\u0000\u0206\u0212\u00059\u0000\u0000\u0207"+
		"\u0208\u0005\u0004\u0000\u0000\u0208\u020d\u0003L&\u0000\u0209\u020a\u0005"+
		"\u0003\u0000\u0000\u020a\u020c\u0003L&\u0000\u020b\u0209\u0001\u0000\u0000"+
		"\u0000\u020c\u020f\u0001\u0000\u0000\u0000\u020d\u020b\u0001\u0000\u0000"+
		"\u0000\u020d\u020e\u0001\u0000\u0000\u0000\u020e\u0210\u0001\u0000\u0000"+
		"\u0000\u020f\u020d\u0001\u0000\u0000\u0000\u0210\u0211\u0005\u0005\u0000"+
		"\u0000\u0211\u0213\u0001\u0000\u0000\u0000\u0212\u0207\u0001\u0000\u0000"+
		"\u0000\u0212\u0213\u0001\u0000\u0000\u0000\u0213\u0217\u0001\u0000\u0000"+
		"\u0000\u0214\u0217\u0005\u001a\u0000\u0000\u0215\u0217\u0005:\u0000\u0000"+
		"\u0216\u0205\u0001\u0000\u0000\u0000\u0216\u0206\u0001\u0000\u0000\u0000"+
		"\u0216\u0214\u0001\u0000\u0000\u0000\u0216\u0215\u0001\u0000\u0000\u0000"+
		"\u0217M\u0001\u0000\u0000\u0000\u0218\u0219\u0003P(\u0000\u0219\u021a"+
		"\u0005\u0001\u0000\u0000\u021aO\u0001\u0000\u0000\u0000\u021b\u021c\u0003"+
		"R)\u0000\u021cQ\u0001\u0000\u0000\u0000\u021d\u0220\u0003T*\u0000\u021e"+
		"\u021f\u0007\u0005\u0000\u0000\u021f\u0221\u0003T*\u0000\u0220\u021e\u0001"+
		"\u0000\u0000\u0000\u0220\u0221\u0001\u0000\u0000\u0000\u0221S\u0001\u0000"+
		"\u0000\u0000\u0222\u0227\u0003V+\u0000\u0223\u0224\u0007\u0006\u0000\u0000"+
		"\u0224\u0226\u0003V+\u0000\u0225\u0223\u0001\u0000\u0000\u0000\u0226\u0229"+
		"\u0001\u0000\u0000\u0000\u0227\u0225\u0001\u0000\u0000\u0000\u0227\u0228"+
		"\u0001\u0000\u0000\u0000\u0228U\u0001\u0000\u0000\u0000\u0229\u0227\u0001"+
		"\u0000\u0000\u0000\u022a\u022f\u0003X,\u0000\u022b\u022c\u0007\u0007\u0000"+
		"\u0000\u022c\u022e\u0003X,\u0000\u022d\u022b\u0001\u0000\u0000\u0000\u022e"+
		"\u0231\u0001\u0000\u0000\u0000\u022f\u022d\u0001\u0000\u0000\u0000\u022f"+
		"\u0230\u0001\u0000\u0000\u0000\u0230W\u0001\u0000\u0000\u0000\u0231\u022f"+
		"\u0001\u0000\u0000\u0000\u0232\u0233\u00050\u0000\u0000\u0233\u0236\u0003"+
		"X,\u0000\u0234\u0236\u0003Z-\u0000\u0235\u0232\u0001\u0000\u0000\u0000"+
		"\u0235\u0234\u0001\u0000\u0000\u0000\u0236Y\u0001\u0000\u0000\u0000\u0237"+
		"\u023b\u0003`0\u0000\u0238\u023a\u0003\\.\u0000\u0239\u0238\u0001\u0000"+
		"\u0000\u0000\u023a\u023d\u0001\u0000\u0000\u0000\u023b\u0239\u0001\u0000"+
		"\u0000\u0000\u023b\u023c\u0001\u0000\u0000\u0000\u023c[\u0001\u0000\u0000"+
		"\u0000\u023d\u023b\u0001\u0000\u0000\u0000\u023e\u0240\u0005\u0004\u0000"+
		"\u0000\u023f\u0241\u0003^/\u0000\u0240\u023f\u0001\u0000\u0000\u0000\u0240"+
		"\u0241\u0001\u0000\u0000\u0000\u0241\u0242\u0001\u0000\u0000\u0000\u0242"+
		"\u0246\u0005\u0005\u0000\u0000\u0243\u0244\u0005\u0001\u0000\u0000\u0244"+
		"\u0246\u0007\u0000\u0000\u0000\u0245\u023e\u0001\u0000\u0000\u0000\u0245"+
		"\u0243\u0001\u0000\u0000\u0000\u0246]\u0001\u0000\u0000\u0000\u0247\u024c"+
		"\u0003P(\u0000\u0248\u0249\u0005\u0003\u0000\u0000\u0249\u024b\u0003P"+
		"(\u0000\u024a\u0248\u0001\u0000\u0000\u0000\u024b\u024e\u0001\u0000\u0000"+
		"\u0000\u024c\u024a\u0001\u0000\u0000\u0000\u024c\u024d\u0001\u0000\u0000"+
		"\u0000\u024d_\u0001\u0000\u0000\u0000\u024e\u024c\u0001\u0000\u0000\u0000"+
		"\u024f\u0262\u0003n7\u0000\u0250\u0262\u0003h4\u0000\u0251\u0262\u0003"+
		"b1\u0000\u0252\u0262\u0003j5\u0000\u0253\u0262\u0003l6\u0000\u0254\u0262"+
		"\u0005:\u0000\u0000\u0255\u0262\u00059\u0000\u0000\u0256\u0262\u00056"+
		"\u0000\u0000\u0257\u0262\u0005\u0015\u0000\u0000\u0258\u0262\u0005\u001a"+
		"\u0000\u0000\u0259\u0262\u0005\u0019\u0000\u0000\u025a\u0262\u0005\u0018"+
		"\u0000\u0000\u025b\u0262\u0005\u0016\u0000\u0000\u025c\u0262\u0005\u0017"+
		"\u0000\u0000\u025d\u025e\u0005\u0004\u0000\u0000\u025e\u025f\u0003P(\u0000"+
		"\u025f\u0260\u0005\u0005\u0000\u0000\u0260\u0262\u0001\u0000\u0000\u0000"+
		"\u0261\u024f\u0001\u0000\u0000\u0000\u0261\u0250\u0001\u0000\u0000\u0000"+
		"\u0261\u0251\u0001\u0000\u0000\u0000\u0261\u0252\u0001\u0000\u0000\u0000"+
		"\u0261\u0253\u0001\u0000\u0000\u0000\u0261\u0254\u0001\u0000\u0000\u0000"+
		"\u0261\u0255\u0001\u0000\u0000\u0000\u0261\u0256\u0001\u0000\u0000\u0000"+
		"\u0261\u0257\u0001\u0000\u0000\u0000\u0261\u0258\u0001\u0000\u0000\u0000"+
		"\u0261\u0259\u0001\u0000\u0000\u0000\u0261\u025a\u0001\u0000\u0000\u0000"+
		"\u0261\u025b\u0001\u0000\u0000\u0000\u0261\u025c\u0001\u0000\u0000\u0000"+
		"\u0261\u025d\u0001\u0000\u0000\u0000\u0262a\u0001\u0000\u0000\u0000\u0263"+
		"\u0264\u00059\u0000\u0000\u0264\u0265\u0005\u001f\u0000\u0000\u0265\u0266"+
		"\u0003d2\u0000\u0266c\u0001\u0000\u0000\u0000\u0267\u026c\u0003f3\u0000"+
		"\u0268\u0269\u0007\u0001\u0000\u0000\u0269\u026b\u0003f3\u0000\u026a\u0268"+
		"\u0001\u0000\u0000\u0000\u026b\u026e\u0001\u0000\u0000\u0000\u026c\u026a"+
		"\u0001\u0000\u0000\u0000\u026c\u026d\u0001\u0000\u0000\u0000\u026de\u0001"+
		"\u0000\u0000\u0000\u026e\u026c\u0001\u0000\u0000\u0000\u026f\u0270\u0005"+
		":\u0000\u0000\u0270\u0271\u0005\u000b\u0000\u0000\u0271\u0272\u0003P("+
		"\u0000\u0272g\u0001\u0000\u0000\u0000\u0273\u0274\u0007\b\u0000\u0000"+
		"\u0274\u0275\u0005\u0004\u0000\u0000\u0275\u0276\u0003^/\u0000\u0276\u0277"+
		"\u0005\u0005\u0000\u0000\u0277i\u0001\u0000\u0000\u0000\u0278\u0279\u0005"+
		":\u0000\u0000\u0279\u027a\u0005\'\u0000\u0000\u027a\u027b\u0003P(\u0000"+
		"\u027bk\u0001\u0000\u0000\u0000\u027c\u0285\u0005\u0006\u0000\u0000\u027d"+
		"\u0282\u0003P(\u0000\u027e\u027f\u0005\u0003\u0000\u0000\u027f\u0281\u0003"+
		"P(\u0000\u0280\u027e\u0001\u0000\u0000\u0000\u0281\u0284\u0001\u0000\u0000"+
		"\u0000\u0282\u0280\u0001\u0000\u0000\u0000\u0282\u0283\u0001\u0000\u0000"+
		"\u0000\u0283\u0286\u0001\u0000\u0000\u0000\u0284\u0282\u0001\u0000\u0000"+
		"\u0000\u0285\u027d\u0001\u0000\u0000\u0000\u0285\u0286\u0001\u0000\u0000"+
		"\u0000\u0286\u0287\u0001\u0000\u0000\u0000\u0287\u0288\u0005\u0007\u0000"+
		"\u0000\u0288m\u0001\u0000\u0000\u0000\u0289\u028b\u00055\u0000\u0000\u028a"+
		"\u028c\u0003\u0010\b\u0000\u028b\u028a\u0001\u0000\u0000\u0000\u028b\u028c"+
		"\u0001\u0000\u0000\u0000\u028c\u028e\u0001\u0000\u0000\u0000\u028d\u028f"+
		"\u0005\u0003\u0000\u0000\u028e\u028d\u0001\u0000\u0000\u0000\u028e\u028f"+
		"\u0001\u0000\u0000\u0000\u028f\u0290\u0001\u0000\u0000\u0000\u0290\u0291"+
		"\u0005\"\u0000\u0000\u0291\u0292\u00032\u0019\u0000\u0292\u0296\u0005"+
		"\u0002\u0000\u0000\u0293\u0297\u0003D\"\u0000\u0294\u0295\u0005<\u0000"+
		"\u0000\u0295\u0297\u00038\u001c\u0000\u0296\u0293\u0001\u0000\u0000\u0000"+
		"\u0296\u0294\u0001\u0000\u0000\u0000\u0297o\u0001\u0000\u0000\u0000Cs"+
		"w|\u0083\u0088\u0098\u00a2\u00a7\u00aa\u00ad\u00b3\u00bc\u00c4\u00cf\u00d5"+
		"\u00e5\u00e9\u00ed\u00f9\u00ff\u010a\u011a\u0121\u012a\u0136\u013e\u0141"+
		"\u0149\u0150\u0168\u0175\u017b\u0183\u018b\u018f\u0191\u0197\u01a4\u01b0"+
		"\u01bc\u01c8\u01d4\u01da\u01de\u01e3\u01ee\u01f3\u01f9\u0203\u020d\u0212"+
		"\u0216\u0220\u0227\u022f\u0235\u023b\u0240\u0245\u024c\u0261\u026c\u0282"+
		"\u0285\u028b\u028e\u0296";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}