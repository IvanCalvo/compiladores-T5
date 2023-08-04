package compiladores;

import compiladores.AlgumaParser.Declaracao_globalContext;
import compiladores.AlgumaParser.Declaracao_constanteContext;
import compiladores.AlgumaParser.Declaracao_tipoContext;
import compiladores.AlgumaParser.Declaracao_variavelContext;
import compiladores.AlgumaParser.ProgramaContext;
import compiladores.AlgumaParser.IdentificadorContext;
import compiladores.AlgumaParser.ParametroContext;
import compiladores.AlgumaParser.Parcela_unarioContext;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.tree.TerminalNode;

import compiladores.AlgumaParser.CmdAtribuicaoContext;
import compiladores.AlgumaParser.CmdRetorneContext;
import compiladores.AlgumaParser.Tipo_basico_identContext;
import compiladores.AlgumaParser.VariavelContext;
import compiladores.TabelaDeSimbolos.EntradaTabelaDeSimbolos;
import compiladores.TabelaDeSimbolos;

public class AlgumaSemantico extends AlgumaBaseVisitor {
    
    //Criando o objeto do escopo
    Escopos escopos = new Escopos(TabelaDeSimbolos.TipoAlguma.VOID);

    @Override
    public Object visitPrograma(ProgramaContext ctx) {
        return super.visitPrograma(ctx);
    }

    //verifica se a constante foi declarada anteriormente (ela não pode ser alterada por se tratar de uma constante)
    @Override
    public Object visitDeclaracao_constante(Declaracao_constanteContext ctx) {
        TabelaDeSimbolos escopoAtual = escopos.obterEscopoAtual();
        if (escopoAtual.existe(ctx.IDENT().getText())) {
            AlgumaSemanticoUtils.adicionarErroSemantico(ctx.start, "constante" + ctx.IDENT().getText()
                    + " ja declarado anteriormente");
        } else {
            TabelaDeSimbolos.TipoAlguma tipo = TabelaDeSimbolos.TipoAlguma.INTEIRO;
            TabelaDeSimbolos.TipoAlguma aux = AlgumaSemanticoUtils.getTipo(ctx.tipo_basico().getText()) ;
            if(aux != null)
                tipo = aux;
            escopoAtual.adicionar(ctx.IDENT().getText(), tipo, TabelaDeSimbolos.Structure.CONST);
        }

        return super.visitDeclaracao_constante(ctx);
    }

    //verifica se o tipo foi declarado duas vezes
    @Override
    public Object visitDeclaracao_tipo(Declaracao_tipoContext ctx) {
        TabelaDeSimbolos escopoAtual = escopos.obterEscopoAtual();

        if (escopoAtual.existe(ctx.IDENT().getText())) {
             AlgumaSemanticoUtils.adicionarErroSemantico(ctx.start, "tipo " + ctx.IDENT().getText()
                    + " declarado duas vezes num mesmo escopo");
        } else {
            TabelaDeSimbolos.TipoAlguma tipo = AlgumaSemanticoUtils.getTipo(ctx.tipo().getText());
            if(tipo != null)
                escopoAtual.adicionar(ctx.IDENT().getText(), tipo, TabelaDeSimbolos.Structure.TIPO);
            else if(ctx.tipo().registro() != null){
                ArrayList<TabelaDeSimbolos.EntradaTabelaDeSimbolos> varReg = new ArrayList<>();
                for(VariavelContext va : ctx.tipo().registro().variavel()){
                    TabelaDeSimbolos.TipoAlguma tipoReg =  AlgumaSemanticoUtils.getTipo(va.tipo().getText());
                    for(IdentificadorContext id2 : va.identificador()){
                        varReg.add(escopoAtual.new EntradaTabelaDeSimbolos(id2.getText(), tipoReg, TabelaDeSimbolos.Structure.TIPO));
                    }

                }

                if (escopoAtual.existe(ctx.IDENT().getText())) {
                    AlgumaSemanticoUtils.adicionarErroSemantico(ctx.start, "identificador " + ctx.IDENT().getText()
                            + " ja declarado anteriormente");
                }
                else{
                    escopoAtual.adicionar(ctx.IDENT().getText(), TabelaDeSimbolos.TipoAlguma.REG, TabelaDeSimbolos.Structure.TIPO);
                }

                for(TabelaDeSimbolos.EntradaTabelaDeSimbolos re : varReg){
                    String nameVar = ctx.IDENT().getText() + '.' + re.nome;
                    if (escopoAtual.existe(nameVar)) {
                        AlgumaSemanticoUtils.adicionarErroSemantico(ctx.start, "identificador " + nameVar
                                + " ja declarado anteriormente");
                    }
                    else{
                        escopoAtual.adicionar(re);
                        escopoAtual.adicionar(ctx.IDENT().getText(), re);
                    }
                }
            }
            TabelaDeSimbolos.TipoAlguma t =  AlgumaSemanticoUtils.getTipo(ctx.tipo().getText());
            escopoAtual.adicionar(ctx.IDENT().getText(), t, TabelaDeSimbolos.Structure.TIPO);
        }
        return super.visitDeclaracao_tipo(ctx);
    }

    //verifica se a variável declarada já foi declarada anteriormente no escopo atual
    @Override
    public Object visitDeclaracao_variavel(Declaracao_variavelContext ctx) {
        TabelaDeSimbolos escopoAtual = escopos.obterEscopoAtual();
        for (IdentificadorContext id : ctx.variavel().identificador()) {
            String nomeId = "";
            int i = 0;
            for(TerminalNode ident : id.IDENT()){
                if(i++ > 0)
                    nomeId += ".";
                nomeId += ident.getText();
            }
            if (escopoAtual.existe(nomeId)) {
                AlgumaSemanticoUtils.adicionarErroSemantico(id.start, "identificador " + nomeId
                        + " ja declarado anteriormente");
            } else {
                TabelaDeSimbolos.TipoAlguma tipo = AlgumaSemanticoUtils.getTipo(ctx.variavel().tipo().getText());
                if(tipo != null)
                    escopoAtual.adicionar(nomeId, tipo, TabelaDeSimbolos.Structure.VAR);
                else{
                    TerminalNode identTipo =    ctx.variavel().tipo() != null
                                                && ctx.variavel().tipo().tipo_estendido() != null 
                                                && ctx.variavel().tipo().tipo_estendido().tipo_basico_ident() != null  
                                                && ctx.variavel().tipo().tipo_estendido().tipo_basico_ident().IDENT() != null 
                                                ? ctx.variavel().tipo().tipo_estendido().tipo_basico_ident().IDENT() : null;
                    if(identTipo != null){
                        ArrayList<TabelaDeSimbolos.EntradaTabelaDeSimbolos> regVars = null;
                        boolean found = false;
                        for(TabelaDeSimbolos t: escopos.percorrerEscoposAninhados()){
                            if(!found){
                                if(t.existe(identTipo.getText())){
                                    regVars = t.retornaTipo(identTipo.getText());
                                    found = true;
                                }
                            }
                        }
                        if(escopoAtual.existe(nomeId)){
                            AlgumaSemanticoUtils.adicionarErroSemantico(id.start, "identificador " + nomeId
                                        + " ja declarado anteriormente");
                        } else{
                            escopoAtual.adicionar(nomeId, TabelaDeSimbolos.TipoAlguma.REG, TabelaDeSimbolos.Structure.VAR);
                            for(TabelaDeSimbolos.EntradaTabelaDeSimbolos s: regVars){
                                escopoAtual.adicionar(nomeId + "." + s.nome, s.tipo, TabelaDeSimbolos.Structure.VAR);
                            }   
                        }
                    }
                    else if(ctx.variavel().tipo().registro() != null){
                        ArrayList<TabelaDeSimbolos.EntradaTabelaDeSimbolos> varReg = new ArrayList<>();
                        for(VariavelContext va : ctx.variavel().tipo().registro().variavel()){
                            TabelaDeSimbolos.TipoAlguma tipoReg =  AlgumaSemanticoUtils.getTipo(va.tipo().getText());
                            for(IdentificadorContext id2 : va.identificador()){
                                varReg.add(escopoAtual.new EntradaTabelaDeSimbolos(id2.getText(), tipoReg, TabelaDeSimbolos.Structure.VAR));
                            }
                        }  
                        escopoAtual.adicionar(nomeId, TabelaDeSimbolos.TipoAlguma.REG, TabelaDeSimbolos.Structure.VAR);

                        for(TabelaDeSimbolos.EntradaTabelaDeSimbolos re : varReg){
                            String nameVar = nomeId + '.' + re.nome;
                            if (escopoAtual.existe(nameVar)) {
                                AlgumaSemanticoUtils.adicionarErroSemantico(id.start, "identificador " + nameVar
                                        + " ja declarado anteriormente");
                            }
                            else{
                                escopoAtual.adicionar(re);
                                escopoAtual.adicionar(nameVar, re.tipo, TabelaDeSimbolos.Structure.VAR);
                            }
                        }

                    }
                    else{//tipo registro estendido
                        escopoAtual.adicionar(id.getText(), TabelaDeSimbolos.TipoAlguma.INTEIRO, TabelaDeSimbolos.Structure.VAR);
                    }
                }
            }
        }
        return super.visitDeclaracao_variavel(ctx);
    }

//verifica se a variável global já foi declarada 
    public Object visitDeclaracao_global(Declaracao_globalContext ctx) {
        TabelaDeSimbolos escopoAtual = escopos.obterEscopoAtual();
        Object ret;
        if (escopoAtual.existe(ctx.IDENT().getText())) {
            AlgumaSemanticoUtils.adicionarErroSemantico(ctx.start, ctx.IDENT().getText()
                    + " ja declarado anteriormente");
            ret = super.visitDeclaracao_global(ctx);
        } else {
            TabelaDeSimbolos.TipoAlguma returnTypeFunc = TabelaDeSimbolos.TipoAlguma.VOID;
            if(ctx.getText().startsWith("funcao")){
                returnTypeFunc = AlgumaSemanticoUtils.getTipo(ctx.tipo_estendido().getText());
                escopoAtual.adicionar(ctx.IDENT().getText(), returnTypeFunc, TabelaDeSimbolos.Structure.FUNC);
            }
            else{
                returnTypeFunc = TabelaDeSimbolos.TipoAlguma.VOID;
                escopoAtual.adicionar(ctx.IDENT().getText(), returnTypeFunc, TabelaDeSimbolos.Structure.PROC);
            }
            escopos.criarNovoEscopo(returnTypeFunc);
            TabelaDeSimbolos escopoAntigo = escopoAtual;
            escopoAtual = escopos.obterEscopoAtual();
            if(ctx.parametros() != null){
                for(ParametroContext p : ctx.parametros().parametro()){
                    for (IdentificadorContext id : p.identificador()) {
                        String nomeId = "";
                        int i = 0;
                        for(TerminalNode ident : id.IDENT()){
                            if(i++ > 0)
                                nomeId += ".";
                            nomeId += ident.getText();
                        }
                        if (escopoAtual.existe(nomeId)) {
                            AlgumaSemanticoUtils.adicionarErroSemantico(id.start, "identificador " + nomeId
                                    + " ja declarado anteriormente");
                        } else {
                            TabelaDeSimbolos.TipoAlguma tipo = AlgumaSemanticoUtils.getTipo(p.tipo_estendido().getText());
                            if(tipo != null){
                                EntradaTabelaDeSimbolos in = escopoAtual.new EntradaTabelaDeSimbolos(nomeId, tipo, TabelaDeSimbolos.Structure.VAR);
                                escopoAtual.adicionar(in);
                                escopoAntigo.adicionar(ctx.IDENT().getText(), in);
                            }
                            else{
                                TerminalNode identTipo =    p.tipo_estendido().tipo_basico_ident() != null  
                                                            && p.tipo_estendido().tipo_basico_ident().IDENT() != null 
                                                            ? p.tipo_estendido().tipo_basico_ident().IDENT() : null;
                                if(identTipo != null){
                                    ArrayList<TabelaDeSimbolos.EntradaTabelaDeSimbolos> regVars = null;
                                    boolean found = false;
                                    for(TabelaDeSimbolos t: escopos.percorrerEscoposAninhados()){
                                        if(!found){
                                            if(t.existe(identTipo.getText())){
                                                regVars = t.retornaTipo(identTipo.getText());
                                                found = true;
                                            }
                                        }
                                    }
                                    if(escopoAtual.existe(nomeId)){
                                        AlgumaSemanticoUtils.adicionarErroSemantico(id.start, "identificador " + nomeId
                                                    + " ja declarado anteriormente");
                                    } else{
                                        EntradaTabelaDeSimbolos in = escopoAtual.new EntradaTabelaDeSimbolos(nomeId, TabelaDeSimbolos.TipoAlguma.REG, TabelaDeSimbolos.Structure.VAR);
                                        escopoAtual.adicionar(in);
                                        escopoAntigo.adicionar(ctx.IDENT().getText(), in);

                                        for(TabelaDeSimbolos.EntradaTabelaDeSimbolos s: regVars){
                                            escopoAtual.adicionar(nomeId + "." + s.nome, s.tipo, TabelaDeSimbolos.Structure.VAR);
                                        }   
                                    }
                                }
                            }
                        }
                    }
                }
            }
            ret = super.visitDeclaracao_global(ctx);
            escopos.abandonarEscopo();

        }
        return ret;
    }


      @Override
    public Object visitTipo_basico_ident(Tipo_basico_identContext ctx) {
        if(ctx.IDENT() != null){
            boolean exists = false;
            for(TabelaDeSimbolos escopo : escopos.percorrerEscoposAninhados()) {
                if(escopo.existe(ctx.IDENT().getText())) {
                    exists = true;
                }
            }
            if(!exists){
                AlgumaSemanticoUtils.adicionarErroSemantico(ctx.start, "tipo " + ctx.IDENT().getText()
                            + " nao declarado");
            }
        }
        return super.visitTipo_basico_ident(ctx);
    }



    //verifica se o identificador existe

    @Override
    public Object visitIdentificador(IdentificadorContext ctx) {
        String nomeVar = "";
        int i = 0;
        for(TerminalNode id : ctx.IDENT()){
            if(i++ > 0)
                nomeVar += ".";
            nomeVar += id.getText();
        }
        boolean erro = true;
        for(TabelaDeSimbolos escopo : escopos.percorrerEscoposAninhados()) {

            if(escopo.existe(nomeVar)) {
                erro = false;
            }
        }
        if(erro)
            AlgumaSemanticoUtils.adicionarErroSemantico(ctx.start, "identificador " + nomeVar + " nao declarado");
        return super.visitIdentificador(ctx);
    }

    //verifica se a atribuição é válida
    @Override
    public Object visitCmdAtribuicao(CmdAtribuicaoContext ctx) {
        TabelaDeSimbolos.TipoAlguma tipoExpressao = AlgumaSemanticoUtils.verificar(escopos, ctx.expressao());
        boolean error = false;
        String pointerChar = ctx.getText().charAt(0) == '^' ? "^" : "";
        String nomeVar = "";
        int i = 0;
        for(TerminalNode id : ctx.identificador().IDENT()){
            if(i++ > 0)
                nomeVar += ".";
            nomeVar += id.getText();
        }
        if (tipoExpressao != TabelaDeSimbolos.TipoAlguma.INVALIDO) {
            boolean found = false;
            for(TabelaDeSimbolos escopo : escopos.percorrerEscoposAninhados()){
                if (escopo.existe(nomeVar) && !found)  {
                    found = true;
                    TabelaDeSimbolos.TipoAlguma tipoVariavel = AlgumaSemanticoUtils.verificar(escopos, nomeVar);
                    Boolean varNumeric = tipoVariavel == TabelaDeSimbolos.TipoAlguma.REAL || tipoVariavel == TabelaDeSimbolos.TipoAlguma.INTEIRO;
                    Boolean expNumeric = tipoExpressao == TabelaDeSimbolos.TipoAlguma.REAL || tipoExpressao == TabelaDeSimbolos.TipoAlguma.INTEIRO;
                    if  (!(varNumeric && expNumeric) && tipoVariavel != tipoExpressao && tipoExpressao != TabelaDeSimbolos.TipoAlguma.INVALIDO) {
                        error = true;
                    }
                } 
            }
        } else{
            error = true;
        }

        if(error){
            nomeVar = ctx.identificador().getText();
            AlgumaSemanticoUtils.adicionarErroSemantico(ctx.identificador().start, "atribuicao nao compativel para " + pointerChar + nomeVar );
        }

        return super.visitCmdAtribuicao(ctx);
    }
    @Override
    public Object visitCmdRetorne(CmdRetorneContext ctx) {
        if(escopos.obterEscopoAtual().tipo == TabelaDeSimbolos.TipoAlguma.VOID){
            AlgumaSemanticoUtils.adicionarErroSemantico(ctx.start, "comando retorne nao permitido nesse escopo");
        } 
        return super.visitCmdRetorne(ctx);
    }

     @Override
    public Object visitParcela_unario(Parcela_unarioContext ctx) {
        TabelaDeSimbolos escopoAtual = escopos.obterEscopoAtual();
        if(ctx.IDENT() != null){
            String name = ctx.IDENT().getText();
            if(escopoAtual.existe(ctx.IDENT().getText())){
                List<EntradaTabelaDeSimbolos> params = escopoAtual.retornaTipo(name);
                boolean error = false;
                if(params.size() != ctx.expressao().size()){
                    error = true;
                } else {
                    for(int i = 0; i < params.size(); i++){
                        if(params.get(i).tipo != AlgumaSemanticoUtils.verificar(escopos, ctx.expressao().get(i))){
                            error = true;
                        }
                    }
                }
                if(error){
                    AlgumaSemanticoUtils.adicionarErroSemantico(ctx.start, "incompatibilidade de parametros na chamada de " + name);
                }
            }
        }

        return super.visitParcela_unario(ctx);
    }
}
