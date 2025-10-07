parser grammar AsterParser;

options { tokenVocab=AsterLexer; }

compilationUnit
  : moduleDecl importDecl* topLevelDecl* EOF
  ;

moduleDecl
  : MODULE qualifiedName
  ;

importDecl
  : IMPORT qualifiedName
  ;

topLevelDecl
  : typeAlias
  | recordDecl
  | enumDecl
  | capabilitiesDecl
  | fnDecl
  | workflowDecl
  ;

qualifiedName
  : ID (DOT ID)*
  ;

typeAlias
  : TYPE ID EQ typeSpec
  ;

typeSpec
  : qualifiedName (LPAREN typeParamList? RPAREN)? // Decimal(precision=18, scale=2)
  | genericType
  ;

genericType
  : qualifiedName LANGLE typeList RANGLE
  ;

typeList
  : typeSpec (COMMA typeSpec)*
  ;

typeParamList
  : namedArg (COMMA namedArg)*
  ;

namedArg
  : ID EQ literal
  ;

recordDecl
  : RECORD ID LBRACE fields? RBRACE
  ;

fields
  : field (COMMA field)*
  ;

field
  : ID COLON typeSpec
  ;

enumDecl
  : ENUM ID LBRACE enumCases RBRACE
  ;

enumCases
  : ID (COMMA ID)*
  ;

capabilitiesDecl
  : CAPABILITIES USES LBRACK capList? RBRACE
  ;

capList
  : capability (COMMA capability)*
  ;

capability
  : qualifiedName (LBRACK capArgs? RBRACK)?
  ;

capArgs
  : literal (COMMA literal)*
  ;

fnDecl
  : FN ID LPAREN paramList? RPAREN ARROW typeSpec USES LBRACK capList? RBRACK block
  ;

paramList
  : param (COMMA param)*
  ;

param
  : ID COLON typeSpec
  ;

workflowDecl
  : WORKFLOW ID LPAREN paramList? RPAREN USES LBRACK capList? RBRACK LBRACE workflowBody RBRACE
  ;

workflowBody
  : (onStart | stepDecl | stmt)*
  ;

onStart
  : ON START COLON stmt+
  ;

stepDecl
  : STEP ID EQ retryBlock block
  | STEP ID EQ block
  | STEP ID block
  ;

retryBlock
  : RETRY LPAREN retryParams RPAREN
  ;

retryParams
  : namedArg (COMMA namedArg)*
  ;

block
  : LBRACE stmt* RBRACE
  ;

stmt
  : letStmt
  | setStmt
  | ifStmt
  | matchStmt
  | awaitStmt
  | ensureStmt
  | compensateStmt
  | exprStmt
  ;

letStmt
  : LET ID EQ expr
  ;

setStmt
  : SET lvalue EQ expr
  ;

lvalue
  : qualifiedName (DOT ID)*
  ;

ifStmt
  : IF expr block (ELSE block)?
  ;

matchStmt
  : MATCH expr LBRACE matchArm+ RBRACE
  ;

matchArm
  : OK LPAREN ID RPAREN ARROW stmtOrExpr
  | ERR LPAREN ID RPAREN ARROW stmtOrExpr
  ;

stmtOrExpr
  : block
  | expr
  ;

awaitStmt
  : AWAIT expr
  ;

ensureStmt
  : ENSURE expr
  ;

compensateStmt
  : COMPENSATE ID
  ;

exprStmt
  : expr
  ;

// Simplified expression grammar
expr
  : primary ( (DOT ID) | callSuffix )*
  | exprBinary
  ;

callSuffix
  : LPAREN argList? RPAREN
  ;

argList
  : expr (COMMA expr)*
  ;

exprBinary
  : primary
  ;

primary
  : literal
  | ID
  | qualifiedName
  | LPAREN expr RPAREN
  | block
  ;

literal
  : STRING
  | INT
  | DECIMAL
  | BOOL
  | DURATION
  ;
