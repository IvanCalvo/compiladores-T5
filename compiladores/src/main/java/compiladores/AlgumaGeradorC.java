package compiladores;

import java.util.ArrayList;
import java.util.Arrays;

import org.antlr.v4.runtime.tree.TerminalNode;

import compiladores.AlgumaParser.CmdAtribuicaoContext;
import compiladores.AlgumaParser.CmdCasoContext;
import compiladores.AlgumaParser.CmdChamadaContext;
import compiladores.AlgumaParser.CmdContext;
import compiladores.AlgumaParser.CmdEnquantoContext;
import compiladores.AlgumaParser.CmdEscrevaContext;
import compiladores.AlgumaParser.CmdFacaContext;
import compiladores.AlgumaParser.CmdLeiaContext;
import compiladores.AlgumaParser.CmdParaContext;
import compiladores.AlgumaParser.CmdRetorneContext;
import compiladores.AlgumaParser.CmdSeContext;
import compiladores.AlgumaParser.CmdSenaoContext;
import compiladores.AlgumaParser.CorpoContext;
import compiladores.AlgumaParser.Decl_local_globalContext;
import compiladores.AlgumaParser.Declaracao_constanteContext;
import compiladores.AlgumaParser.Declaracao_globalContext;
import compiladores.AlgumaParser.Declaracao_localContext;
import compiladores.AlgumaParser.Declaracao_tipoContext;
import compiladores.AlgumaParser.Declaracao_variavelContext;
import compiladores.AlgumaParser.DimensaoContext;
import compiladores.AlgumaParser.Exp_aritmeticaContext;
import compiladores.AlgumaParser.Exp_relacionalContext;
import compiladores.AlgumaParser.ExpressaoContext;
import compiladores.AlgumaParser.FatorContext;
import compiladores.AlgumaParser.Fator_logicoContext;
import compiladores.AlgumaParser.IdentificadorContext;
import compiladores.AlgumaParser.Item_selecaoContext;
import compiladores.AlgumaParser.ParametroContext;
import compiladores.AlgumaParser.ParcelaContext;
import compiladores.AlgumaParser.Parcela_logicaContext;
import compiladores.AlgumaParser.Parcela_nao_unarioContext;
import compiladores.AlgumaParser.Parcela_unarioContext;
import compiladores.AlgumaParser.RegistroContext;
import compiladores.AlgumaParser.SelecaoContext;
import compiladores.AlgumaParser.TermoContext;
import compiladores.AlgumaParser.Termo_logicoContext;
import compiladores.AlgumaParser.TipoContext;
import compiladores.AlgumaParser.Tipo_basico_identContext;
import compiladores.AlgumaParser.Tipo_estendidoContext;
import compiladores.AlgumaParser.Valor_constanteContext;
import compiladores.AlgumaParser.VariavelContext;
import compiladores.TabelaDeSimbolos.EntradaTabelaDeSimbolos;

public class AlgumaGeradorC extends AlgumaBaseVisitor<Void> {
    StringBuilder saida;
    TabelaDeSimbolos tabela;

    public AlgumaGeradorC() {
        saida = new StringBuilder();
        this.tabela = new TabelaDeSimbolos();
    }

    @Override
    public Void visitPrograma(AlgumaParser.ProgramaContext ctx) {
        saida.append("#include <stdio.h>\n");
        saida.append("#include <stdlib.h>\n");
        saida.append("\n");
        ctx.declaracoes().decl_local_global().forEach(dec -> visitDecl_local_global(dec));
        saida.append("\n");
        saida.append("int main() {\n");

        visitCorpo(ctx.corpo());
        saida.append("return 0;\n");
        saida.append("}\n");
        return null;
    }

    @Override
    public Void visitDecl_local_global(Decl_local_globalContext ctx) {
        
        if(ctx.declaracao_local() != null){
            visitDeclaracao_local(ctx.declaracao_local());
        }
        else if(ctx.declaracao_global() != null){
            visitDeclaracao_global(ctx.declaracao_global());
        }
        return null;
    }

    @Override
    public Void visitCorpo(CorpoContext ctx) {
        for(AlgumaParser.Declaracao_localContext dec : ctx.declaracao_local()) {
            visitDeclaracao_local(dec);
        }

        for(AlgumaParser.CmdContext com : ctx.cmd()) {
            visitCmd(com);
        }

        return null;
    }

    @Override
    public Void visitDeclaracao_global(Declaracao_globalContext ctx) {
        
        if(ctx.getText().contains("procedimento")){
            saida.append("void " + ctx.IDENT().getText() + "(");
        }
        else{
            String cTipo = AlgumaSemanticoUtils.getCType(ctx.tipo_estendido().getText().replace("^", ""));
            TabelaDeSimbolos.TipoAlguma tipo = AlgumaSemanticoUtils.getTipo(ctx.tipo_estendido().getText());
            visitTipo_estendido(ctx.tipo_estendido());
            if(cTipo == "char"){
                saida.append("[80]");
            }
            saida.append(" " + ctx.IDENT().getText() + "(");
            tabela.adicionar(ctx.IDENT().getText(), tipo, TabelaDeSimbolos.Structure.FUNC);
        }
            ctx.parametros().parametro().forEach(var -> visitParametro(var));
            saida.append("){\n");
            ctx.declaracao_local().forEach(var -> visitDeclaracao_local(var));
            ctx.cmd().forEach(var -> visitCmd(var));
            saida.append("}\n");

        return null;
    }

    @Override
    public Void visitIdentificador(IdentificadorContext ctx) {
        
        saida.append(" ");
        int i = 0;
        for(TerminalNode id : ctx.IDENT()){
            if(i++ > 0)
                saida.append(".");
            saida.append(id.getText());
        }
        visitDimensao(ctx.dimensao());
        return null;
    }

    @Override
    public Void visitDimensao(DimensaoContext ctx) {
        
        for(Exp_aritmeticaContext exp : ctx.exp_aritmetica()){
            saida.append("[");
            visitExp_aritmetica(exp);
            saida.append("]");
        }

        return null;
    }

    @Override
    public Void visitParametro(ParametroContext ctx) {
        
        int i = 0;
        String cTipo = AlgumaSemanticoUtils.getCType(ctx.tipo_estendido().getText().replace("^", ""));
        TabelaDeSimbolos.TipoAlguma tipo = AlgumaSemanticoUtils.getTipo(ctx.tipo_estendido().getText());
        for(IdentificadorContext id : ctx.identificador()){
            if(i++ > 0)
                saida.append(",");
            visitTipo_estendido(ctx.tipo_estendido());
            
            visitIdentificador(id);

            if(cTipo == "char"){
                saida.append("[80]");
            }
            tabela.adicionar(id.getText(),tipo,TabelaDeSimbolos.Structure.VAR);
        }
        return null;
    }

    @Override
    public Void visitDeclaracao_local(Declaracao_localContext ctx) {
        System.out.println("Declaring " + ctx.getText());
        if(ctx.declaracao_variavel() != null){
            visitDeclaracao_variavel(ctx.declaracao_variavel());
        }
        if(ctx.declaracao_constante() != null){
            visitDeclaracao_constante(ctx.declaracao_constante());
        } 
        else if(ctx.declaracao_tipo() != null){
            visitDeclaracao_tipo(ctx.declaracao_tipo());
        }

        return null;
    }

    @Override
    public Void visitDeclaracao_tipo(Declaracao_tipoContext ctx) {
        
        saida.append("typedef ");
        String cTipo = AlgumaSemanticoUtils.getCType(ctx.tipo().getText().replace("^", ""));
        TabelaDeSimbolos.TipoAlguma tipo = AlgumaSemanticoUtils.getTipo(ctx.tipo().getText());
       
        if(ctx.tipo().getText().contains("registro")){
            for(VariavelContext sub : ctx.tipo().registro().variavel()){
                for(IdentificadorContext idIns : sub.identificador()){
                    TabelaDeSimbolos.TipoAlguma tipoIns = AlgumaSemanticoUtils.getTipo(sub.tipo().getText());
                    System.out.println("Inserting reg " + sub.getText() + "." + idIns.getText());
                    tabela.adicionar(ctx.IDENT().getText() + "." + idIns.getText(), tipoIns, TabelaDeSimbolos.Structure.VAR);
                    tabela.adicionar(ctx.IDENT().getText(), tabela.new EntradaTabelaDeSimbolos(idIns.getText(), tipoIns, TabelaDeSimbolos.Structure.TIPO));
                }
            }
        }
        tabela.adicionar(ctx.IDENT().getText(), tipo, TabelaDeSimbolos.Structure.VAR);
        visitTipo(ctx.tipo());
        saida.append(ctx.IDENT() + ";\n");
        return null;
    }

    @Override
    public Void visitDeclaracao_variavel(Declaracao_variavelContext ctx) {
        visitVariavel(ctx.variavel());
        return null;
    }

    @Override
    public Void visitVariavel(VariavelContext ctx) {
        
        String cTipo = AlgumaSemanticoUtils.getCType(ctx.tipo().getText().replace("^", ""));
        System.out.println("Visiting " + ctx.getText());
        TabelaDeSimbolos.TipoAlguma tipo = AlgumaSemanticoUtils.getTipo(ctx.tipo().getText());
        for(AlgumaParser.IdentificadorContext id: ctx.identificador()) {
            if(ctx.tipo().getText().contains("registro")){
                for(VariavelContext sub : ctx.tipo().registro().variavel()){
                    for(IdentificadorContext idIns : sub.identificador()){
                        TabelaDeSimbolos.TipoAlguma tipoIns = AlgumaSemanticoUtils.getTipo(sub.tipo().getText());
                        tabela.adicionar(id.getText() + "." + idIns.getText(), tipoIns, TabelaDeSimbolos.Structure.VAR);
                    }
                }
            }
            else if(cTipo == null && tipo == null){
                ArrayList<EntradaTabelaDeSimbolos> arg = tabela.retornaTipo(ctx.tipo().getText());
                if(arg != null){
                    for(TabelaDeSimbolos.EntradaTabelaDeSimbolos val : arg){
                        tabela.adicionar(id.getText() + "." + val.nome, val.tipo, TabelaDeSimbolos.Structure.VAR);
                    }
                }
            }
            if(id.getText().contains("[")){
                int ini = id.getText().indexOf("[", 0);
                int end = id.getText().indexOf("]", 0);
                System.out.println("ini = " + (ini+1) + " end = " + (end-1) + " out of " + id.getText());
                String tam;
                if(end-ini == 2)
                    tam = String.valueOf(id.getText().charAt(ini+1));
                else
                    tam = id.getText().substring(ini + 1, end - 1);
                String nome = id.IDENT().get(0).getText();
                for(int i = 0; i < Integer.parseInt(tam); i++){
                    System.out.println("Cadastrano " + nome + "[" + i + "]");
                    tabela.adicionar(nome + "[" + i + "]", tipo, TabelaDeSimbolos.Structure.VAR);
                }

            }
            else{
                tabela.adicionar(id.getText(), tipo, TabelaDeSimbolos.Structure.VAR);
            }
            visitTipo(ctx.tipo());
            
            visitIdentificador(id);
            if(cTipo == "char"){
                saida.append("[80]");
            }
            saida.append(";\n");
        }
        return null;
    }

    @Override
    public Void visitTipo(TipoContext ctx) {
        
        String cTipo = AlgumaSemanticoUtils.getCType(ctx.getText().replace("^", ""));
        TabelaDeSimbolos.TipoAlguma tipo = AlgumaSemanticoUtils.getTipo(ctx.getText());
        boolean pointer = ctx.getText().contains("^");
        if(cTipo != null){
            saida.append(cTipo);
        }
        else if(ctx.registro() != null){
            visitRegistro(ctx.registro());
        }
        else{
            visitTipo_estendido(ctx.tipo_estendido());
        }
        if(pointer)
            saida.append("*");
        saida.append(" ");

        return null;
    }
    @Override
    public Void visitTipo_estendido(Tipo_estendidoContext ctx) {
        
        visitTipo_basico_ident(ctx.tipo_basico_ident());
        if(ctx.getText().contains("^"))
            saida.append("*");
        return null;
    }
    @Override
    public Void visitTipo_basico_ident(Tipo_basico_identContext ctx) {
        
        if(ctx.IDENT() != null){
            saida.append(ctx.IDENT().getText());
        }
        else{
            saida.append(AlgumaSemanticoUtils.getCType(ctx.getText().replace("^", "")));
        }
        return null;
    }

    @Override
    public Void visitRegistro(RegistroContext ctx) {
        
        saida.append("struct {\n");
        ctx.variavel().forEach(var -> visitVariavel(var));
        saida.append("} ");
        return null;
    }

    @Override
    public Void visitDeclaracao_constante(Declaracao_constanteContext ctx) {
        
        String type = AlgumaSemanticoUtils.getCType(ctx.tipo_basico().getText());
        TabelaDeSimbolos.TipoAlguma typeVar = AlgumaSemanticoUtils.getTipo(ctx.tipo_basico().getText());
        tabela.adicionar(ctx.IDENT().getText(),typeVar,TabelaDeSimbolos.Structure.VAR);
        saida.append("const " + type + " " + ctx.IDENT().getText() + " = ");
        visitValor_constante(ctx.valor_constante());
        saida.append(";\n");
        return null;
    }

    @Override
    public Void visitValor_constante(Valor_constanteContext ctx) {
        
        if(ctx.getText().equals("verdadeiro")){
            saida.append("true");
        }
        else if(ctx.getText().equals("falso")){
            saida.append("false");
        }
        else{
            saida.append(ctx.getText());
        }
        return null;
    }

    @Override
    public Void visitCmd(CmdContext ctx) {
        if(ctx.cmdLeia() != null){
            visitCmdLeia(ctx.cmdLeia());
        } else if(ctx.cmdEscreva() != null){
            visitCmdEscreva(ctx.cmdEscreva());
        } else if(ctx.cmdAtribuicao() != null){
            visitCmdAtribuicao(ctx.cmdAtribuicao());
        } 
        else if(ctx.cmdSe() != null){
            visitCmdSe(ctx.cmdSe());
        }
        else if(ctx.cmdCaso() != null){
            visitCmdCaso(ctx.cmdCaso());
        }
        else if(ctx.cmdPara() != null){
            visitCmdPara(ctx.cmdPara());
        }
        else if(ctx.cmdEnquanto() != null){
            visitCmdEnquanto(ctx.cmdEnquanto());
        }
        else if(ctx.cmdFaca() != null){
            visitCmdFaca(ctx.cmdFaca());
        }
        else if(ctx.cmdChamada() != null){
            visitCmdChamada(ctx.cmdChamada());
        }
        else if(ctx.cmdRetorne() != null){
            visitCmdRetorne(ctx.cmdRetorne());
        }
        return null;
    }

    @Override
    public Void visitCmdRetorne(CmdRetorneContext ctx) {
        
        saida.append("return ");
        visitExpressao(ctx.expressao());
        saida.append(";\n");
        return null;
    }

    @Override
    public Void visitCmdChamada(CmdChamadaContext ctx) {
        
        saida.append(ctx.IDENT().getText() + "(");
        int i = 0;
        for(ExpressaoContext exp : ctx.expressao()){
            if(i++ > 0)
                saida.append(",");
            visitExpressao(exp);
        }
        saida.append(");\n");
        return null;
    }

    @Override
    public Void visitCmdLeia(CmdLeiaContext ctx) {
        for(AlgumaParser.IdentificadorContext id: ctx.identificador()) {
            TabelaDeSimbolos.TipoAlguma idType = tabela.verificar(id.getText());
            if(idType != TabelaDeSimbolos.TipoAlguma.CADEIA){
                saida.append("scanf(\"%");
                saida.append(AlgumaSemanticoUtils.getCTypeSymbol(idType));
                saida.append("\", &");
                saida.append(id.getText());
                saida.append(");\n");
            } else {
                saida.append("gets(");
                
                visitIdentificador(id);
                saida.append(");\n");
            }
        }
        
        return null;
    }

    @Override
    public Void visitCmdEscreva(CmdEscrevaContext ctx) { 
        for(AlgumaParser.ExpressaoContext exp: ctx.expressao()) {
                Escopos escopo = new Escopos(tabela);
                System.out.println("Searching for " + exp.getText());
                System.out.println("Does it exists in Table? " + tabela.existe(exp.getText()));
                String cType = AlgumaSemanticoUtils.getCTypeSymbol(AlgumaSemanticoUtils.verificar(escopo, exp));
                if(tabela.existe(exp.getText())){
                    TabelaDeSimbolos.TipoAlguma tip = tabela.verificar(exp.getText());
                    cType = AlgumaSemanticoUtils.getCTypeSymbol(tip);
                }
                saida.append("printf(\"%");
                saida.append(cType);
                saida.append("\", ");
                saida.append(exp.getText());
                saida.append(");\n");
        }
        return null;
    }

    @Override
    public Void visitCmdAtribuicao(CmdAtribuicaoContext ctx) {
        if(ctx.getText().contains("^"))
            saida.append("*");
        try{
            TabelaDeSimbolos.TipoAlguma tip = tabela.verificar(ctx.identificador().getText());

            if(tip != null && tip == TabelaDeSimbolos.TipoAlguma.CADEIA){
                
                saida.append("strcpy(");
                visitIdentificador(ctx.identificador());
                saida.append(","+ctx.expressao().getText()+");\n");
            }
            else{
                
                visitIdentificador(ctx.identificador());
                saida.append(" = ");
                saida.append(ctx.expressao().getText());
                saida.append(";\n");
            }
        }
        catch(Exception e){
            System.out.println(e.getMessage() +  " q ocorreu");
        }
        return null;
    }

    @Override
    public Void visitCmdSe(CmdSeContext ctx) {
        saida.append("if(");
        visitExpressao(ctx.expressao());
        saida.append(") {\n");
        for(CmdContext cmd : ctx.cmd()) {
            visitCmd(cmd);
        }
        saida.append("}\n");
        if(ctx.cmdSenao() != null){
            saida.append("else {\n");
            for(CmdContext cmd : ctx.cmdSenao().cmd()) {
                visitCmd(cmd);
            }
            saida.append("}\n");
        }
        
        return null;
    }

    @Override
    public Void visitExpressao(ExpressaoContext ctx) {
        if(ctx.termo_logico() != null){
            visitTermo_logico(ctx.termo_logico(0));

            for(int i = 1; i < ctx.termo_logico().size(); i++){
                AlgumaParser.Termo_logicoContext termo = ctx.termo_logico(i);
                saida.append(" || ");
                visitTermo_logico(termo);
            }
        }

        return null;
    }

    @Override
    public Void visitTermo_logico(Termo_logicoContext ctx) {
        visitFator_logico(ctx.fator_logico(0));

        for(int i = 1; i < ctx.fator_logico().size(); i++){
            AlgumaParser.Fator_logicoContext fator = ctx.fator_logico(i);
            saida.append(" && ");
            visitFator_logico(fator);
        }
        
        return null;
    }

    @Override
    public Void visitFator_logico(Fator_logicoContext ctx) {
        if(ctx.getText().startsWith("nao")){
            saida.append("!");
        }
        visitParcela_logica(ctx.parcela_logica());
        
        return null;
    }

    @Override
    public Void visitParcela_logica(Parcela_logicaContext ctx) {
        if(ctx.exp_relacional() != null){
            visitExp_relacional(ctx.exp_relacional());
        } else{
            if(ctx.getText() == "verdadeiro"){
                saida.append("true");
            } else {
                saida.append("false");
            }
        }
        
        return null;
    }

    
    @Override
    public Void visitExp_relacional(Exp_relacionalContext ctx) {
         visitExp_aritmetica(ctx.exp_aritmetica(0));
        for(int i = 1; i < ctx.exp_aritmetica().size(); i++){
            AlgumaParser.Exp_aritmeticaContext termo = ctx.exp_aritmetica(i);
            if(ctx.op_relacional().getText().equals("=")){
                saida.append(" == ");
            } else{
                saida.append(ctx.op_relacional().getText());
            }
            visitExp_aritmetica(termo);
        }
        
        return null;
    }

    @Override
    public Void visitExp_aritmetica(Exp_aritmeticaContext ctx) {
        visitTermo(ctx.termo(0));

        for(int i = 1; i < ctx.termo().size(); i++){
            AlgumaParser.TermoContext termo = ctx.termo(i);
            saida.append(ctx.op1(i-1).getText());
            visitTermo(termo);
        }
        return null;
    }

    @Override
    public Void visitTermo(TermoContext ctx) {
       visitFator(ctx.fator(0));

        for(int i = 1; i < ctx.fator().size(); i++){
            AlgumaParser.FatorContext fator = ctx.fator(i);
            saida.append(ctx.op2(i-1).getText());
            visitFator(fator);
        }
        return null;
    }

    @Override
    public Void visitFator(FatorContext ctx) {
        visitParcela(ctx.parcela(0));

        for(int i = 1; i < ctx.parcela().size(); i++){
            AlgumaParser.ParcelaContext parcela = ctx.parcela(i);
            saida.append(ctx.op3(i-1).getText());
            visitParcela(parcela);
        }
        return null;
    }

    @Override
    public Void visitParcela(ParcelaContext ctx) {
        if(ctx.parcela_unario() != null){
            if(ctx.op_unario() != null){
                saida.append(ctx.op_unario().getText());
            }
            visitParcela_unario(ctx.parcela_unario());
        } else{
            visitParcela_nao_unario(ctx.parcela_nao_unario());
        }
        
        return null;
    }

    @Override
    public Void visitParcela_unario(Parcela_unarioContext ctx) {
        
        if(ctx.IDENT() != null){
            saida.append(ctx.IDENT().getText());
            saida.append("(");
            for(int i = 0; i < ctx.expressao().size(); i++){
                visitExpressao(ctx.expressao(i));
                if(i < ctx.expressao().size()-1){
                    saida.append(", ");
                }
            }
        } else if(ctx.parentesis_expressao() != null){
            saida.append("(");
            visitExpressao(ctx.parentesis_expressao().expressao());
            saida.append(")");
        }
        else {
            saida.append(ctx.getText());
        }
        
        return null;
    }

    @Override
    public Void visitParcela_nao_unario(Parcela_nao_unarioContext ctx) {
        
        saida.append(ctx.getText());
        return null;
    }

    @Override
    public Void visitCmdCaso(CmdCasoContext ctx) {
        
        saida.append("switch(");
        visit(ctx.exp_aritmetica());
        saida.append("){\n");
        visit(ctx.selecao());
        if(ctx.cmdSenao() != null){
            visit(ctx.cmdSenao());
        }
        saida.append("}\n");
        return null;
    }
    @Override
    public Void visitSelecao(SelecaoContext ctx) {
        
        ctx.item_selecao().forEach(var -> visitItem_selecao(var));
        return null;
    }
    @Override
    public Void visitItem_selecao(Item_selecaoContext ctx) {
        
        ArrayList<String> intervalo = new ArrayList<>(Arrays.asList(ctx.constantes().getText().split("\\.\\.")));
        String first = intervalo.size() > 0 ? intervalo.get(0) : ctx.constantes().getText();
        String last = intervalo.size() > 1 ? intervalo.get(1) : intervalo.get(0);
        for(int i = Integer.parseInt(first); i <= Integer.parseInt(last); i++){
            saida.append("case " + i + ":\n");
            ctx.cmd().forEach(var -> visitCmd(var));
            saida.append("break;\n");
        }
        return null;
    }
    @Override
    public Void visitCmdSenao(CmdSenaoContext ctx) {
        
        saida.append("default:\n");
        ctx.cmd().forEach(var -> visitCmd(var));
        saida.append("break;\n");
        return null;
    }

    @Override
    public Void visitCmdPara(CmdParaContext ctx) {
        
        String id = ctx.IDENT().getText();
        saida.append("for(" + id + " = ");
        visitExp_aritmetica(ctx.exp_aritmetica(0));
        saida.append("; " + id + " <= ");
        visitExp_aritmetica(ctx.exp_aritmetica(1));
        saida.append("; " + id + "++){\n");
        ctx.cmd().forEach(var -> visitCmd(var));
        saida.append("}\n");
        return null;
    }

    @Override
    public Void visitCmdEnquanto(CmdEnquantoContext ctx) {
        
        saida.append("while(");
        visitExpressao(ctx.expressao());
        saida.append("){\n");
        ctx.cmd().forEach(var -> visitCmd(var));
        saida.append("}\n");
        return null;
    }

    @Override
    public Void visitCmdFaca(CmdFacaContext ctx) {
        
        saida.append("do{\n");
        ctx.cmd().forEach(var -> visitCmd(var));
        saida.append("} while(");
        visitExpressao(ctx.expressao());
        saida.append(");\n");
        return null;
    }


}
