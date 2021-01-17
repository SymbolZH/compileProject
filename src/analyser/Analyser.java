package analyser;
import error.*;
import instruction.*;
import tokenizer.*;

import java.util.*;

public final class Analyser {

    Tokenizer tokenizer;
    ArrayList<Instruction> cuinstructions;
    public SymbolTable _startTable=new SymbolTable();
    public SymbolEntry _start;
    public SymbolEntry cufn=null;
    public SymbolEntry main=null;
    public SymbolTable globalTable=new SymbolTable();
    private SymbolTable fnTable=_startTable;
    private SymbolTable varTable=globalTable;
    private SymbolTable paraTable =null;
    int stack=0;
    int stackTop=0;
    List <String> libs=Arrays.asList("getint","getdouble","getchar","putint","putdouble","putchar","putstr","putln");
    /** 当前偷看的 token */
    Token peekedToken = null;

    public Analyser(Tokenizer tokenizer) throws AnalyzeError {
        this.tokenizer = tokenizer;
        this.cuinstructions=new ArrayList<>();
        this._start=new SymbolEntry(null,false,fnTable.getNextVariableOffset(),SymbolKind.FN,
                IdentType.VOID,null,null,0,this.cuinstructions,null);
        fnTable.addSymbol(_start,null);
        for(String lib:libs){
            globalTable.addSymbol(new SymbolEntry(lib,true,globalTable.getNextVariableOffset(),SymbolKind.CONST,IdentType.STRING,lib),null);
        }
    }

    public void analyse() throws CompileError {
        analyseProgram();
    }

    /**
     * 查看下一个 Token
     *
     * @return Token
     */
    private Token peek() throws TokenizeError {
        if (peekedToken == null) {
            peekedToken = tokenizer.nextToken();
        }
        return peekedToken;
    }

    /**
     * 获取下一个 Token
     * 
     * @return Token
     */
    private Token next() throws TokenizeError {
        if (peekedToken != null) {
            var token = peekedToken;
            peekedToken = null;
            return token;
        } else {
            return tokenizer.nextToken();
        }
    }

    /**
     * 如果下一个 token 的类型是 tt，则返回 true
     * 
     * @param tt 类型
     * @return Token
     */
    private boolean check(TokenType tt) throws TokenizeError {
        var token = peek();
        return token.getTokenType() == tt;
    }

    /**
     * 如果下一个 token 的类型是 tt，则前进一个 token 并返回这个 token
     * 
     * @param tt 类型
     * @return 如果匹配则返回这个 token，否则返回 null
     */
    private Token nextIf(TokenType tt) throws TokenizeError {
        var token = peek();
        if (token.getTokenType() == tt) {
            return next();
        } else {
            return null;
        }
    }

    /**
     * 如果下一个 token 的类型是 tt，则前进一个 token 并返回，否则抛出异常
     * 
     * @param tt 类型
     * @return 这个 token
     * @throws CompileError 如果类型不匹配
     */

    private Token expect(TokenType tt) throws CompileError {
        var token = peek();
        if (token.getTokenType() == tt) {
            return next();
        } else {
            throw new ExpectedTokenError(tt, token);
        }
    }


    /**
     * <程序> ::= 'begin'<主过程>'end'
     */
    private void analyseProgram() throws CompileError {
        analyseMain();
        expect(TokenType.EOF);
    }

    private void analyseMain() throws CompileError {
        while(check(TokenType.CONST_KW)||check(TokenType.LET_KW) ){
            analyseDeclaration();
        }
        while (check(TokenType.FN_KW)) {
            analyseFunction();
        }

    }
    private void analyseDeclaration() throws CompileError{
        if(check(TokenType.CONST_KW)){
            analyseConstantDeclaration();
        }
        else if(check(TokenType.LET_KW)){
            analyseLetDeclaration();
        }
        else{
            throw new ExpectedTokenError(List.of(TokenType.CONST_KW,TokenType.LET_KW), next());
        }

    }
    private void analyseLetDeclaration() throws CompileError{
        if(nextIf(TokenType.LET_KW)!=null){
            var nameToken = expect(TokenType.IDENT);
            IdentType type = null;
            SymbolEntry symbol=new SymbolEntry((String)nameToken.getValue(),false,varTable.getNextVariableOffset(),SymbolKind.LET
                    ,IdentType.INT,0L);
            expect(TokenType.COLON);
            if(check(TokenType.DOUBLE)||check(TokenType.INT)){
                if(nextIf(TokenType.DOUBLE)!=null){
                    type=IdentType.DOUBLE;
                    symbol.setType(IdentType.DOUBLE);
                }
                else if(nextIf(TokenType.INT)!=null){
                    type=IdentType.INT;
                    symbol.setType(IdentType.INT);
                }
            }
            else {
                throw new ExpectedTokenError(List.of(TokenType.DOUBLE,TokenType.INT),next());
            }

            if(check(TokenType.ASSIGN)){
                expect(TokenType.ASSIGN);
                symbol.setInitialized(true);
                long off=symbol.getStackOffset();
                if(varTable.isStart()){
                    cuinstructions.add(new Instruction(Operation.globa,off));
                }
                else{
                    cuinstructions.add(new Instruction(Operation.loca ,off));
                }

                IdentType expressionType=analyseExpression();
                if(expressionType!=type){
                    throw new Error("wrong type at"+next().getStartPos());
                }
                cuinstructions.add(new Instruction(Operation.stroe64));
            }
            expect(TokenType.SEMICOLON);

            varTable.addSymbol(symbol,nameToken.getStartPos());
        }
    }
    private void analyseConstantDeclaration() throws CompileError {
        if (nextIf(TokenType.CONST_KW) != null) {
            var nameToken = expect(TokenType.IDENT);
            IdentType type=null;
            expect(TokenType.COLON);
            if(check(TokenType.DOUBLE)||check(TokenType.INT)){
                if(nextIf(TokenType.DOUBLE)!=null){
                    type=IdentType.DOUBLE;
                }
                else if(nextIf(TokenType.INT)!=null){
                    type=IdentType.INT;
                }
            }
            expect(TokenType.ASSIGN);
            SymbolEntry symbol=new SymbolEntry((String)nameToken.getValue(),true,varTable.getNextVariableOffset(),SymbolKind.CONST
                    ,type,0L);
            long off=symbol.getStackOffset();
            if(varTable.isStart()){
                cuinstructions.add(new Instruction(Operation.globa,off));
            }
            else{
                cuinstructions.add(new Instruction(Operation.loca ,off));
            }
            IdentType expressionType=analyseExpression();
            cuinstructions.add(new Instruction(Operation.stroe64));
            if(expressionType!=type){
                throw new Error("wrong type at"+next().getStartPos());
            }
            expect(TokenType.SEMICOLON);

            varTable.addSymbol(symbol,nameToken.getStartPos());
        }
    }

    private void analyseFunction() throws CompileError {
        SymbolEntry symbol=new SymbolEntry(fnTable.getNextVariableOffset());
        this.cufn=symbol;
        ArrayList<Instruction> instructions=new ArrayList<>();
        cuinstructions=instructions;
        symbol.setInstruction(instructions);


        SymbolTable locTable=new SymbolTable();
        varTable=locTable;
        symbol.setLoc(locTable);


        symbol.setKind(SymbolKind.FN);

        symbol.setInitialized(true);

        IdentType type;
        expect(TokenType.FN_KW);
        var nameToken = expect(TokenType.IDENT);

        symbol.setName((String)nameToken.getValue());

        SymbolTable paratable=new SymbolTable();
        symbol.setParam(paratable);
        locTable.setLastTable(paratable);

        expect(TokenType.L_PAREN);
        if(check(TokenType.CONST_KW)||check(TokenType.IDENT)){
            this.paraTable=paratable;
            analyseParamList();
            this.paraTable=null;
        }
        expect(TokenType.R_PAREN);
        expect(TokenType.ARROW);
        if(nextIf(TokenType.INT)!=null){
            type=IdentType.INT;
        }
        else if(nextIf(TokenType.VOID)!=null){
            type=IdentType.VOID;
        }
        else if(nextIf(TokenType.DOUBLE)!=null){
            type=IdentType.DOUBLE;
        }
        else{
            throw new ExpectedTokenError(List.of(TokenType.INT,TokenType.VOID), next());
        }
        symbol.setType(type);
        if((nameToken.getValue()).equals("main")){
            this.main=symbol;
            if(symbol.getType()==IdentType.VOID){
                _start.getInstruction().add(new Instruction(Operation.stackalloc, 0L));
            }
            else if(symbol.getType()==IdentType.INT||symbol.getType()==IdentType.DOUBLE){
                _start.getInstruction().add(new Instruction(Operation.stackalloc,1L));
            }
            _start.getInstruction().add(new Instruction(Operation.call,symbol.getStackOffset()));
        }
        fnTable.addSymbol(symbol,nameToken.getStartPos());
        analyseBlockStatement(-1);
        if(symbol.getType()==IdentType.VOID||symbol.getInstruction().get(symbol.getInstruction().size()-1).getOpt()!=Operation.ret){
            symbol.getInstruction().add(new Instruction(Operation.ret));
        }
    }

    private void analyseParamList() throws CompileError{
        SymbolEntry symbol=new SymbolEntry(cufn.getParam().getNextVariableOffset());
        SymbolKind isConstant=SymbolKind.LET;
        if(nextIf(TokenType.CONST_KW)!=null){
            isConstant=SymbolKind.CONST;
        }
        var nameToken = expect(TokenType.IDENT);
        expect(TokenType.COLON);
        if(check(TokenType.DOUBLE)||check(TokenType.INT)){
            if(nextIf(TokenType.DOUBLE)!=null) {
                symbol.setType(IdentType.DOUBLE);
            }
            else if(nextIf(TokenType.INT)!=null){
                symbol.setType(IdentType.INT);
            }
        }
        else{
            throw new ExpectedTokenError(List.of(TokenType.DOUBLE,TokenType.INT),next());
        }
        symbol.setName((String) nameToken.getValue());
        symbol.setInitialized(true);
        symbol.setKind(isConstant);

        this.paraTable.addSymbol(symbol,nameToken.getStartPos());
        if(nextIf(TokenType.COMMA)!=null){
            analyseParamList();
        }
    }
    private Instruction[] analyseBlockStatement(int offbooleanexpression) throws CompileError{
        ArrayList<Instruction> brList=new ArrayList<>();

        expect(TokenType.L_BRACE);
        while(check(TokenType.IF_KW)||check(TokenType.WHILE_KW)||check(TokenType.RETURN_KW)
                ||check(TokenType.SEMICOLON)||check(TokenType.MINUS)||check(TokenType.IDENT)
                ||check(TokenType.LET_KW)||check(TokenType.CONST_KW)||check(TokenType.CONTINUE_KW)||check(TokenType.BREAK_KW)) {
            Instruction[]br=analyseStatement(offbooleanexpression);
            if(br!=null){
                brList.addAll(Arrays.asList(br));
            }
        }
        expect(TokenType.R_BRACE);
        return brList.toArray(new Instruction[0]);
    }
    private Instruction[] analyseStatement(int offbooleanexpression) throws CompileError {
            if(check(TokenType.IF_KW)){
                return analyseIfStatement(offbooleanexpression);
            }
            else if(check(TokenType.WHILE_KW)){
                analyseWhileStatement();
                return null;
            }
            else if(check(TokenType.RETURN_KW)){
                analyseReturnStatement();
                return null;
            }
            else if (check(TokenType.SEMICOLON)){
                expect(TokenType.SEMICOLON);
                return null;
            }
            else if(check(TokenType.IDENT)){
                analyseIdentStatement();
                return null;
            }
            else if(check(TokenType.LET_KW)||check(TokenType.CONST_KW)){
                analyseDeclaration();
                return null;
            }
            else if(check(TokenType.CONTINUE_KW)){
                analyseContinueStatement(offbooleanexpression);
                return null;
            }
            else if(check(TokenType.BREAK_KW)){
                return new Instruction[]{analyseBreakStatement()};
            }
            else{
                throw new ExpectedTokenError(List.of(TokenType.IF_KW,TokenType.WHILE_KW,TokenType.RETURN_KW,
                        TokenType.SEMICOLON,TokenType.IDENT, TokenType.LET_KW,TokenType.CONST_KW,TokenType.CONTINUE_KW,TokenType.BREAK_KW),next());
            }
    }
    private Instruction analyseBreakStatement() throws CompileError {
        expect(TokenType.BREAK_KW);
        expect(TokenType.SEMICOLON);
        Instruction br=new Instruction(Operation.br);
        cuinstructions.add(br);
        br.setX(cufn.getInstruction().size());
        return br;
    }
    private void analyseContinueStatement(int offbooleanexpression) throws CompileError{
        expect(TokenType.CONTINUE_KW);
        expect(TokenType.SEMICOLON);
        if(offbooleanexpression==-1){
            throw new Error("wrong continue statement");
        }

        Instruction br=new Instruction(Operation.br);
        cuinstructions.add(br);
        br.setX(offbooleanexpression-cufn.getInstruction().size());
    }
    private void analyseReturnStatement() throws CompileError {
        expect(TokenType.RETURN_KW);
        if(cufn.getType()!=IdentType.VOID){
            cuinstructions.add(new Instruction(Operation.arga, 0L));
            IdentType type=analyseExpression();
            cuinstructions.add(new Instruction(Operation.stroe64));
            if(type!=cufn.getType()){
                throw new Error("wrong return at"+next().getStartPos());
            }
        }

        cuinstructions.add(new Instruction(Operation.ret));
        expect(TokenType.SEMICOLON);
    }
    private Instruction[] analyseIfStatement(int offbooleanexpression) throws CompileError {

        expect(TokenType.IF_KW);

        IdentType type=analyseBlooeanExpression();
        if(type!=IdentType.VOID){
            cuinstructions.add(new Instruction(Operation.brtrue,1L));
        }
        Instruction passblock1=new Instruction(Operation.br);
        cuinstructions.add(passblock1);
        int off1=cufn.getInstruction().size();

        Instruction[] ifbr =analyseBlockStatement(offbooleanexpression);
        List<Instruction> brList = new ArrayList<>(Arrays.asList(ifbr));

        Instruction passblock2=new Instruction(Operation.br, 0L);
        cuinstructions.add(passblock2);
        int off2=cufn.getInstruction().size();

        passblock1.setX(off2-off1);

        if(check(TokenType.ELSE_KW)){
            expect(TokenType.ELSE_KW);
            if (check(TokenType.L_BRACE)) {
                ifbr=analyseBlockStatement(offbooleanexpression);
                brList.addAll(Arrays.asList(ifbr));
            } else if (check(TokenType.IF_KW)) {
                ifbr=analyseIfStatement(offbooleanexpression);
                brList.addAll(Arrays.asList(ifbr));
            }

            int off3=cufn.getInstruction().size();
            passblock2.setX(off3-off2);
        }
        return brList.toArray(new Instruction[0]);
    }
    private void analyseWhileStatement() throws CompileError {
        expect(TokenType.WHILE_KW);
        cuinstructions.add(new Instruction(Operation.br, 0L));
        int off1=cufn.getInstruction().size();

        IdentType type=analyseBlooeanExpression();
        if(type!=IdentType.VOID){
            cuinstructions.add(new Instruction(Operation.brtrue,1L));
        }
        Instruction passblock=new Instruction(Operation.br);
        cuinstructions.add(passblock);
        int off2=cufn.getInstruction().size();

        Instruction[] breakbr =analyseBlockStatement(off1);

        Instruction back=new Instruction(Operation.br);
        cuinstructions.add(back);
        int off3=cufn.getInstruction().size();

        for (Instruction br:breakbr){
            br.setX(off3-(int) br.getX());
        }

        back.setX(off1-off3);
        passblock.setX(off3-off2);
    }
    private IdentType analyseBlooeanExpression() throws CompileError {
        IdentType type=analyseExpression();
        IdentType subtype;
        if(check(TokenType.LT)){
            expect(TokenType.LT);
            subtype=analyseExpression();
            if(type==IdentType.INT){
                cuinstructions.add(new Instruction(Operation.cmpi));
            }
            else if(type==IdentType.DOUBLE){
                cuinstructions.add(new Instruction(Operation.cmpf));
            }
            cuinstructions.add(new Instruction(Operation.setlt));
            cuinstructions.add(new Instruction(Operation.brtrue, 1L));
            if(subtype!=type){
                throw new Error("wrong type at"+next().getStartPos());
            }
            return IdentType.VOID;
        }
        else if(check(TokenType.LE)){
            expect(TokenType.LE);
            subtype=analyseExpression();
            if(type==IdentType.INT){
                cuinstructions.add(new Instruction(Operation.cmpi));
            }
            else if(type==IdentType.DOUBLE){
                cuinstructions.add(new Instruction(Operation.cmpf));
            }
            cuinstructions.add(new Instruction(Operation.setgt));
            cuinstructions.add(new Instruction(Operation.brfalse, 1L));
            if(subtype!=type){
                throw new Error("wrong type at"+next().getStartPos());
            }
            return IdentType.VOID;
        }
        else if(check(TokenType.GE)){
            expect(TokenType.GE);
            subtype=analyseExpression();
            if(type==IdentType.INT){
                cuinstructions.add(new Instruction(Operation.cmpi));
            }
            else if(type==IdentType.DOUBLE){
                cuinstructions.add(new Instruction(Operation.cmpf));
            }
            cuinstructions.add(new Instruction(Operation.setlt));
            cuinstructions.add(new Instruction(Operation.brfalse, 1L));
            if(subtype!=type){
                throw new Error("wrong type at"+next().getStartPos());
            }
            return IdentType.VOID;
        }
        else if(check(TokenType.GT)){
            expect(TokenType.GT);
            subtype=analyseExpression();
            if(type==IdentType.INT){
                cuinstructions.add(new Instruction(Operation.cmpi));
            }
            else if(type==IdentType.DOUBLE){
                cuinstructions.add(new Instruction(Operation.cmpf));
            }
            cuinstructions.add(new Instruction(Operation.setgt));
            cuinstructions.add(new Instruction(Operation.brtrue, 1L));
            if(subtype!=type){
                throw new Error("wrong type at"+next().getStartPos());
            }
            return IdentType.VOID;
        }
        else if(check(TokenType.NEQ)){
            expect(TokenType.NEQ);
            subtype=analyseExpression();
            if(type==IdentType.INT){
                cuinstructions.add(new Instruction(Operation.cmpi));
            }
            else if(type==IdentType.DOUBLE){
                cuinstructions.add(new Instruction(Operation.cmpf));
            }
            cuinstructions.add(new Instruction(Operation.brtrue, 1L));
            if(subtype!=type){
                throw new Error("wrong type at"+next().getStartPos());
            }
            return IdentType.VOID;
        }
        else if(check(TokenType.EQ)){
            expect(TokenType.EQ);
            subtype=analyseExpression();
            if(type==IdentType.INT){
                cuinstructions.add(new Instruction(Operation.cmpi));
            }
            else if(type==IdentType.DOUBLE){
                cuinstructions.add(new Instruction(Operation.cmpf));
            }
            cuinstructions.add(new Instruction(Operation.brfalse, 1L));
            if(subtype!=type){
                throw new Error("wrong type at"+next().getStartPos());
            }
            return IdentType.VOID;
        }
        return type;

    }
    private void analyseIdentStatement() throws CompileError{
        var nameToken=expect(TokenType.IDENT);
        if(check(TokenType.L_PAREN)){
                analysefn(nameToken);
        }
        else if(check(TokenType.ASSIGN)){
            SymbolEntry entry=getvar(nameToken);
            if(entry.isConstant()){
                throw new Error();
            }
            expect(TokenType.ASSIGN);
            entry.setInitialized(true);
            IdentType type=analyseExpression();
            if(type!=entry.getType()){
                throw new Error("wrong type at"+next().getStartPos());
            }
            cuinstructions.add(new Instruction(Operation.stroe64));
        }
        else{
            throw new ExpectedTokenError(List.of(TokenType.IDENT,TokenType.ASSIGN) ,next());
        }
        expect(TokenType.SEMICOLON);
    }

    private IdentType analyseExpression() throws CompileError{
        IdentType type=analyseTerm();
        while(check(TokenType.MINUS)||check(TokenType.PLUS)){
            if(nextIf(TokenType.MINUS)!=null){
                IdentType subtype=analyseTerm();
                if(subtype!=type){
                    throw new Error("wrong type at"+next().getStartPos());
                }
                if(type==IdentType.DOUBLE){
                    cuinstructions.add(new Instruction(Operation.subf));
                }
                else if(type==IdentType.INT){
                    cuinstructions.add(new Instruction(Operation.subi));
                }
            }
            else if(nextIf(TokenType.PLUS)!=null){
                IdentType subtype=analyseTerm();
                if(subtype!=type){
                    throw new Error("wrong type at"+next().getStartPos());
                }
                if(type==IdentType.DOUBLE){
                    cuinstructions.add(new Instruction(Operation.addf));
                }
                else if(type==IdentType.INT){
                    cuinstructions.add(new Instruction(Operation.addi));
                }
            }
        }
        return type;
        //throw new Error("Not implemented");
    }
    private IdentType analyseTerm() throws CompileError {
        IdentType type=analyseFactor();
        while(check(TokenType.MUL)||check(TokenType.DIV)){
            if(nextIf(TokenType.MUL)!=null){
                IdentType subtype=analyseFactor();
                if(subtype!=type){
                    throw new Error("wrong type at"+next().getStartPos());
                }
                if(type==IdentType.DOUBLE){
                    cuinstructions.add(new Instruction(Operation.mulf));
                }
                else if(type==IdentType.INT){
                    cuinstructions.add(new Instruction(Operation.muli));
                }
            }
            else if(nextIf(TokenType.DIV)!=null){
                IdentType subtype=analyseFactor();
                if(subtype!=type){
                    throw new Error("wrong type at"+next().getStartPos());
                }
                if(type==IdentType.DOUBLE){
                    cuinstructions.add(new Instruction(Operation.divf));
                }
                else if(type==IdentType.INT){
                    cuinstructions.add(new Instruction(Operation.divi));
                }
            }
        }
        return type;
        //throw new Error("Not implemented");
    }

    private IdentType analyseFactor() throws CompileError {
        int negate = 0;
        IdentType type;
        while (check(TokenType.MINUS) ||check(TokenType.PLUS)) {
            if(nextIf(TokenType.MINUS)!=null){
                negate = negate+1;
                // 计算结果需要被 0 减
            }
            else{
                nextIf(TokenType.PLUS);
            }
        }

        if (check(TokenType.IDENT)) {
            var nameToken=expect(TokenType.IDENT);
            if(check(TokenType.L_PAREN)){
                type=analysefn(nameToken);

            }
            else{
                SymbolEntry entry=getvar(nameToken);
                cuinstructions.add(new Instruction(Operation.load64,entry.stackOffset));
                type=entry.getType();
            }
        } else if (check(TokenType.UINT_LITERAL)) {
            var nameToken=next();
            cuinstructions.add(new Instruction(Operation.push,(Long) nameToken.getValue()));
            type=IdentType.INT;

        } else if (check(TokenType.DOUBLE_LITERAL)) {
            var nameToken=next();
            cuinstructions.add(new Instruction(Operation.push,(Long) nameToken.getValue()));
            type=IdentType.DOUBLE;
            // 调用相应的处理函数
        }else if(check(TokenType.STRING_LITERAL)){
            var nameToken=next();
            SymbolEntry symbol=new SymbolEntry((String)nameToken.getValue(),true,globalTable.getNextVariableOffset(),SymbolKind.CONST
                    ,IdentType.STRING,nameToken.getValue());
            globalTable.addSymbol(symbol,nameToken.getStartPos());
            cuinstructions.add(new Instruction(Operation.push,globalTable.getOffset((String)nameToken.getValue(),nameToken.getStartPos())));
            type=null;
        } else if (check(TokenType.L_PAREN)) {
            expect(TokenType.L_PAREN);
            type=analyseBlooeanExpression();
            expect(TokenType.R_PAREN);
        } else {
            // 都不是，摸了
            throw new ExpectedTokenError(List.of(TokenType.IDENT, TokenType.UINT_LITERAL, TokenType.L_PAREN), next());
        }

        while(check(TokenType.AS_KW)){
            expect(TokenType.AS_KW);
            if(check(TokenType.INT)||check(TokenType.DOUBLE)) {
                if(check(TokenType.INT)||type==IdentType.DOUBLE){
                    expect(TokenType.INT);
                    type=IdentType.INT;
                    cuinstructions.add(new Instruction(Operation.ftoi));
                }
                else if(check(TokenType.DOUBLE)||type==IdentType.INT){
                    expect(TokenType.DOUBLE);
                    type=IdentType.DOUBLE;
                    cuinstructions.add(new Instruction(Operation.itof));
                }
            }
            else{
                throw new Error("wrong type");
            }
        }
        for(int i=0;i<negate;i++){
            if(type==IdentType.DOUBLE){
                cuinstructions.add(new Instruction(Operation.negf));
            }
            else if(type==IdentType.INT){
                cuinstructions.add(new Instruction(Operation.negi));
            }
        }
        return type;
        //throw new Error("Not implemented");
    }
    private IdentType analysefn(Token nameToken) throws CompileError {
        IdentType type=null;
        if(nameToken.getValue().equals("putint")||nameToken.getValue().equals("putdouble")
                ||nameToken.getValue().equals("putchar")||nameToken.getValue().equals("putstr")){

            SymbolEntry entry=globalTable.getsymbol(nameToken.getValue(),nameToken.getStartPos());
            cuinstructions.add(new Instruction(Operation.stackalloc,0L));
            expect(TokenType.L_PAREN);
            if (check(TokenType.MINUS) || check(TokenType.IDENT) || check(TokenType.L_PAREN)
                    || check(TokenType.UINT_LITERAL) || check(TokenType.STRING_LITERAL)||check(TokenType.DOUBLE_LITERAL)) {
                analyseExpression();
            }
            expect(TokenType.R_PAREN);

            cuinstructions.add(new Instruction(Operation.callname,entry.getStackOffset()));
            type=IdentType.VOID;
        }
        else if(nameToken.getValue().equals("getint")||nameToken.getValue().equals("getdouble")||nameToken.getValue().equals("getchar")){
            SymbolEntry entry=globalTable.getsymbol(nameToken.getValue(),nameToken.getStartPos());
            expect(TokenType.L_PAREN);
            expect(TokenType.R_PAREN);
            cuinstructions.add(new Instruction(Operation.stackalloc, 1L));
            cuinstructions.add(new Instruction(Operation.callname,entry.getStackOffset()));
            if(nameToken.getValue().equals("getint")||nameToken.getValue().equals("getchar")){
                type=IdentType.INT;
            }
            else if(nameToken.getValue().equals("getdouble")){
                type=IdentType.DOUBLE;
            }
        }
        else if(nameToken.getValue().equals("putln")){
            SymbolEntry entry=globalTable.getsymbol(nameToken.getValue(),nameToken.getStartPos());
            expect(TokenType.L_PAREN);
            expect(TokenType.R_PAREN);
            cuinstructions.add(new Instruction(Operation.stackalloc, 0L));
            cuinstructions.add(new Instruction(Operation.callname,entry.getStackOffset()));
            type=IdentType.VOID;
        }
        else{
            SymbolEntry entry=fnTable.getsymbol(nameToken.getValue(),nameToken.getStartPos());
            stackAlloc(entry);
            expect(TokenType.L_PAREN);
            if (check(TokenType.MINUS) || check(TokenType.IDENT) || check(TokenType.L_PAREN)
                    || check(TokenType.UINT_LITERAL) || check(TokenType.STRING_LITERAL)||check(TokenType.DOUBLE_LITERAL)) {
                analyseExpression();
                for(int i=0;i<entry.getParam().getCount()-1;i++){
                    expect(TokenType.COMMA);
                    analyseExpression();
                }
            }
            expect(TokenType.R_PAREN);
            cuinstructions.add(new Instruction(Operation.call,entry.getStackOffset()));
            type=entry.getType();
        }
        return type;
    }
    private SymbolEntry getvar(Token nameToken) throws AnalyzeError {
        SymbolEntry entry=varTable.getsymbol(nameToken.getValue(),nameToken.getStartPos());
        if(entry==null){
            entry=cufn.getParam().getsymbol(nameToken.getValue(),nameToken.getStartPos());
            if(entry==null){
                entry=globalTable.getsymbol(nameToken.getValue(),nameToken.getStartPos());
                if(entry==null){
                    throw new AnalyzeError(ErrorCode.NotDeclared,nameToken.getStartPos());
                }
                else{
                    cuinstructions.add(new Instruction(Operation.globa,entry.stackOffset));
                }
            }
            else{
                if(cufn.getType()==IdentType.VOID){
                    cuinstructions.add(new Instruction(Operation.arga,entry.stackOffset));
                }
                else{
                    cuinstructions.add(new Instruction(Operation.arga,entry.stackOffset+1));
                }
            }
        }
        else{
            cuinstructions.add(new Instruction(Operation.loca,entry.stackOffset));
        }
        return entry;
    }

    private void stackAlloc(SymbolEntry symbol){
        if(symbol.getType()==IdentType.VOID){
            cuinstructions.add(new Instruction(Operation.stackalloc, 0L));
        }
        else if(symbol.getType()==IdentType.INT||symbol.getType()==IdentType.DOUBLE){
            cuinstructions.add(new Instruction(Operation.stackalloc, 1L));
        }
    }
}
