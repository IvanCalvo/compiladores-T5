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
        // Importando as bibliotecas C necessárias.
        saida.append("#include <stdio.h>\n");
        saida.append("#include <stdlib.h>\n");
        saida.append("\n");

        // Visita todas as declarações locais e globais do programa.
        ctx.declaracoes().decl_local_global().forEach(dec -> visitDecl_local_global(dec));

        // Adiciona uma linha em branco para melhor legibilidade do código C.
        saida.append("\n");

        // Início da função principal 'int main()'.
        saida.append("int main() {\n");

        // Visita o corpo do programa.
        visitCorpo(ctx.corpo());

        // Adiciona a instrução de retorno 0 para finalizar o programa com sucesso.
        saida.append("return 0;\n");

        // Fim da função principal 'int main()'.
        saida.append("}\n");
        return null;
    }

    @Override
    public Void visitDecl_local_global(Decl_local_globalContext ctx) {
        // Verifica se a declaração é uma declaração local ou global e chama o
        // respectivo método de visita.
        if (ctx.declaracao_local() != null) {
            visitDeclaracao_local(ctx.declaracao_local());
        } else if (ctx.declaracao_global() != null) {
            visitDeclaracao_global(ctx.declaracao_global());
        }
        // O método não possui retorno explícito, retorna 'null'.
        return null;
    }

    @Override
    public Void visitCorpo(CorpoContext ctx) {
        for (AlgumaParser.Declaracao_localContext dec : ctx.declaracao_local()) {
            visitDeclaracao_local(dec);
        }

        for (AlgumaParser.CmdContext com : ctx.cmd()) {
            visitCmd(com);
        }

        return null;
    }

    @Override
    public Void visitDeclaracao_global(Declaracao_globalContext ctx) {
        // Verifica se a declaração global é um procedimento.
        if (ctx.getText().contains("procedimento")) {
            // Adiciona a palavra-chave 'void' seguida do identificador do procedimento e
            // abre parênteses.
            saida.append("void " + ctx.IDENT().getText() + "(");
        } else {
            // Se a declaração global não for um procedimento, trata-se de uma função.
            // Obtém o tipo C correspondente ao tipo estendido da função.
            String cTipo = AlgumaSemanticoUtils.getCType(ctx.tipo_estendido().getText().replace("^", ""));
            // Obtém o tipo Alguma da função.
            TabelaDeSimbolos.TipoAlguma tipo = AlgumaSemanticoUtils.getTipo(ctx.tipo_estendido().getText());
            // Visita o tipo estendido para processá-lo.
            visitTipo_estendido(ctx.tipo_estendido());
            // Se o tipo for char, adiciona o tamanho [80] ao parâmetro da função.
            if (cTipo == "char") {
                saida.append("[80]");
            }
            // Adiciona o tipo e o identificador da função à saída.
            saida.append(" " + ctx.IDENT().getText() + "(");
            // Adiciona a função à tabela de símbolos.
            tabela.adicionar(ctx.IDENT().getText(), tipo, TabelaDeSimbolos.Structure.FUNC);
        }
        // Visita todos os parâmetros da função.
        ctx.parametros().parametro().forEach(var -> visitParametro(var));
        // Abre o bloco de código da função.
        saida.append("){\n");
        // Visita todas as declarações locais da função.
        ctx.declaracao_local().forEach(var -> visitDeclaracao_local(var));
        // Visita todos os comandos da função.
        ctx.cmd().forEach(var -> visitCmd(var));
        // Fecha o bloco de código da função.
        saida.append("}\n");
        return null;
    }

    @Override
    public Void visitIdentificador(IdentificadorContext ctx) {
        // Adiciona um espaço à saída.
        saida.append(" ");
        // Variável para controlar o número de identificadores no nó.
        int i = 0;
        // Percorre todos os identificadores presentes no contexto.
        for (TerminalNode id : ctx.IDENT()) {
            // Se não for o primeiro identificador, adiciona um ponto antes do
            // identificador.
            if (i++ > 0)
                saida.append(".");
            // Adiciona o identificador à saída.
            saida.append(id.getText());
        }
        // Visita a dimensão dos identificadores, caso existam.
        visitDimensao(ctx.dimensao());

        return null;
    }

    @Override
    public Void visitDimensao(DimensaoContext ctx) {
        // Percorre todas as expressões aritméticas presentes no contexto.
        for (Exp_aritmeticaContext exp : ctx.exp_aritmetica()) {
            // Adiciona um colchete de abertura à saída.
            saida.append("[");
            // Visita a expressão aritmética para processá-la e adicioná-la à saída.
            visitExp_aritmetica(exp);
            // Adiciona um colchete de fechamento à saída.
            saida.append("]");
        }

        return null;
    }

    @Override
    public Void visitParametro(ParametroContext ctx) {
        // Variável para controlar o número de parâmetros no contexto.
        int i = 0;
        // Obtém o tipo C correspondente ao tipo estendido dos parâmetros.
        String cTipo = AlgumaSemanticoUtils.getCType(ctx.tipo_estendido().getText().replace("^", ""));
        // Obtém o tipo Alguma dos parâmetros.
        TabelaDeSimbolos.TipoAlguma tipo = AlgumaSemanticoUtils.getTipo(ctx.tipo_estendido().getText());
        // Percorre todos os identificadores presentes no contexto.
        for (IdentificadorContext id : ctx.identificador()) {
            // Se não for o primeiro parâmetro, adiciona uma vírgula antes do identificador.
            if (i++ > 0)
                saida.append(",");
            // Visita o tipo estendido dos parâmetros para processá-lo e adicioná-lo à
            // saída.
            visitTipo_estendido(ctx.tipo_estendido());
            // Visita o identificador dos parâmetros para processá-lo e adicioná-lo à saída.
            visitIdentificador(id);
            // Se o tipo for char, adiciona o tamanho [80] ao parâmetro.
            if (cTipo.equals("char")) {
                saida.append("[80]");
            }
            // Adiciona o identificador e o tipo à tabela de símbolos como variável.
            tabela.adicionar(id.getText(), tipo, TabelaDeSimbolos.Structure.VAR);
        }

        return null;
    }

    @Override
    public Void visitDeclaracao_local(Declaracao_localContext ctx) {
        // Verifica se a declaração local é uma declaração de variável.
        if (ctx.declaracao_variavel() != null) {
            // Visita a declaração de variável para processá-la.
            visitDeclaracao_variavel(ctx.declaracao_variavel());
        }
        // Verifica se a declaração local é uma declaração de constante.
        if (ctx.declaracao_constante() != null) {
            // Visita a declaração de constante para processá-la.
            visitDeclaracao_constante(ctx.declaracao_constante());
        } else if (ctx.declaracao_tipo() != null) {
            // Caso contrário, verifica se é uma declaração de tipo e visita-a para
            // processá-la.
            visitDeclaracao_tipo(ctx.declaracao_tipo());
        }
        // Não possui retorno explícito, retorna 'null'.
        return null;
    }


    @Override
    public Void visitDeclaracao_tipo(Declaracao_tipoContext ctx) {
        // Adiciona a palavra-chave 'typedef' à saída.
        saida.append("typedef ");
        // Obtém o tipo C correspondente ao tipo da declaração de tipo.
        String cTipo = AlgumaSemanticoUtils.getCType(ctx.tipo().getText().replace("^", ""));
        // Obtém o tipo Alguma da declaração de tipo.
        TabelaDeSimbolos.TipoAlguma tipo = AlgumaSemanticoUtils.getTipo(ctx.tipo().getText());

        // Verifica se a declaração de tipo é uma declaração de registro.
        if (ctx.tipo().getText().contains("registro")) {
            // Para cada subvariável do registro, adiciona os identificadores e seus tipos
            // na tabela de símbolos.
            for (VariavelContext sub : ctx.tipo().registro().variavel()) {
                for (IdentificadorContext idIns : sub.identificador()) {
                    TabelaDeSimbolos.TipoAlguma tipoIns = AlgumaSemanticoUtils.getTipo(sub.tipo().getText());
                    tabela.adicionar(ctx.IDENT().getText() + "." + idIns.getText(), tipoIns,
                            TabelaDeSimbolos.Structure.VAR);
                    tabela.adicionar(ctx.IDENT().getText(), tabela.new EntradaTabelaDeSimbolos(idIns.getText(), tipoIns,
                            TabelaDeSimbolos.Structure.TIPO));
                }
            }
        }
        // Adiciona o tipo declarado e o nome do tipo à tabela de símbolos como
        // variável.
        tabela.adicionar(ctx.IDENT().getText(), tipo, TabelaDeSimbolos.Structure.VAR);
        // Visita o tipo da declaração para processá-lo.
        visitTipo(ctx.tipo());
        // Adiciona o nome do tipo e um ponto-e-vírgula à saída.
        saida.append(ctx.IDENT() + ";\n");
        // Não possui retorno explícito, retorna 'null'.
        return null;
    }

    @Override
    public Void visitDeclaracao_variavel(Declaracao_variavelContext ctx) {
        // Visita a variável para processá-la.
        visitVariavel(ctx.variavel());
        // Não possui retorno explícito, retorna 'null'.
        return null;
    }


    @Override
    public Void visitVariavel(VariavelContext ctx) {
        // Obtém o tipo C correspondente ao tipo da variável.
        String cTipo = AlgumaSemanticoUtils.getCType(ctx.tipo().getText().replace("^", ""));
        // Obtém o tipo Alguma da variável.
        TabelaDeSimbolos.TipoAlguma tipo = AlgumaSemanticoUtils.getTipo(ctx.tipo().getText());
        // Percorre todos os identificadores presentes no contexto.
        for (AlgumaParser.IdentificadorContext id : ctx.identificador()) {
            // Verifica se a variável é uma subvariável de um registro.
            if (ctx.tipo().getText().contains("registro")) {
                // Para cada subvariável do registro, adiciona os identificadores e seus tipos
                // na tabela de símbolos.
                for (VariavelContext sub : ctx.tipo().registro().variavel()) {
                    for (IdentificadorContext idIns : sub.identificador()) {
                        TabelaDeSimbolos.TipoAlguma tipoIns = AlgumaSemanticoUtils.getTipo(sub.tipo().getText());
                        tabela.adicionar(id.getText() + "." + idIns.getText(), tipoIns, TabelaDeSimbolos.Structure.VAR);
                    }
                }
            }
            // Verifica se a variável é um tipo definido pelo usuário e se sim, adiciona
            // seus membros à tabela de símbolos.
            else if (cTipo == null && tipo == null) {
                ArrayList<EntradaTabelaDeSimbolos> arg = tabela.retornaTipo(ctx.tipo().getText());
                if (arg != null) {
                    for (TabelaDeSimbolos.EntradaTabelaDeSimbolos val : arg) {
                        tabela.adicionar(id.getText() + "." + val.nome, val.tipo, TabelaDeSimbolos.Structure.VAR);
                    }
                }
            }
            // Verifica se a variável é um vetor e, se sim, adiciona suas posições à tabela
            // de símbolos.
            if (id.getText().contains("[")) {
                int ini = id.getText().indexOf("[", 0);
                int end = id.getText().indexOf("]", 0);
                String tam;
                if (end - ini == 2)
                    tam = String.valueOf(id.getText().charAt(ini + 1));
                else
                    tam = id.getText().substring(ini + 1, end - 1);
                String nome = id.IDENT().get(0).getText();
                for (int i = 0; i < Integer.parseInt(tam); i++) {
                    tabela.adicionar(nome + "[" + i + "]", tipo, TabelaDeSimbolos.Structure.VAR);
                }

            }
            // Caso contrário, adiciona a variável normalmente à tabela de símbolos.
            else {
                tabela.adicionar(id.getText(), tipo, TabelaDeSimbolos.Structure.VAR);
            }
            // Visita o tipo da variável para processá-lo.
            visitTipo(ctx.tipo());
            // Visita o identificador da variável para processá-lo.
            visitIdentificador(id);
            // Se o tipo for char, adiciona o tamanho [80] à variável na saída.
            if (cTipo == "char") {
                saida.append("[80]");
            }
            // Adiciona um ponto-e-vírgula ao final da declaração da variável na saída.
            saida.append(";\n");
        }
        // Não possui retorno explícito, retorna 'null'.
        return null;
    }


    @Override
    public Void visitTipo(TipoContext ctx) {
        // Obtém o tipo C correspondente ao tipo da declaração e o tipo Alguma do
        // contexto.
        String cTipo = AlgumaSemanticoUtils.getCType(ctx.getText().replace("^", ""));
        TabelaDeSimbolos.TipoAlguma tipo = AlgumaSemanticoUtils.getTipo(ctx.getText());
        // Verifica se o tipo é um ponteiro.
        boolean pointer = ctx.getText().contains("^");
        // Se o tipo for um tipo primitivo, adiciona o tipo C correspondente à saída.
        if (cTipo != null) {
            saida.append(cTipo);
        }
        // Se o tipo for um registro, visita o nó de registro para processá-lo.
        else if (ctx.registro() != null) {
            visitRegistro(ctx.registro());
        }
        // Caso contrário, visita o tipo estendido para processá-lo.
        else {
            visitTipo_estendido(ctx.tipo_estendido());
        }
        // Se o tipo for um ponteiro, adiciona o asterisco (*) à saída.
        if (pointer)
            saida.append("*");
        // Adiciona um espaço em branco à saída.
        saida.append(" ");
        // Não possui retorno explícito, retorna 'null'.
        return null;
    }


    @Override
    public Void visitTipo_estendido(Tipo_estendidoContext ctx) {
        // Visita o tipo básico ou identificador para processá-lo.
        visitTipo_basico_ident(ctx.tipo_basico_ident());
        // Verifica se o tipo é um ponteiro e, se sim, adiciona o asterisco (*) à saída.
        if (ctx.getText().contains("^"))
            saida.append("*");
        // Não possui retorno explícito, retorna 'null'.
        return null;
    }


    @Override
    public Void visitTipo_basico_ident(Tipo_basico_identContext ctx) {
        // Verifica se o nó representa um identificador e adiciona o nome do
        // identificador à saída.
        if (ctx.IDENT() != null) {
            saida.append(ctx.IDENT().getText());
        }
        // Caso contrário, o nó representa um tipo básico e adiciona o tipo C
        // correspondente à saída.
        else {
            saida.append(AlgumaSemanticoUtils.getCType(ctx.getText().replace("^", "")));
        }
        // Não possui retorno explícito, retorna 'null'.
        return null;
    }

 
    @Override
    public Void visitRegistro(RegistroContext ctx) {
        // Adiciona a palavra-chave 'struct' seguida de uma quebra de linha à saída.
        saida.append("struct {\n");
        // Visita cada variável do registro para processá-las.
        ctx.variavel().forEach(var -> visitVariavel(var));
        // Adiciona a chave de fechamento do registro seguida de um espaço em branco à
        // saída.
        saida.append("} ");
        // Não possui retorno explícito, retorna 'null'.
        return null;
    }


    @Override
    public Void visitDeclaracao_constante(Declaracao_constanteContext ctx) {
        // Obtém o tipo C correspondente ao tipo básico da constante e o tipo Alguma do
        // contexto.
        String type = AlgumaSemanticoUtils.getCType(ctx.tipo_basico().getText());
        TabelaDeSimbolos.TipoAlguma typeVar = AlgumaSemanticoUtils.getTipo(ctx.tipo_basico().getText());
        // Adiciona a constante à tabela de símbolos com o tipo Alguma e a estrutura de
        // variável.
        tabela.adicionar(ctx.IDENT().getText(), typeVar, TabelaDeSimbolos.Structure.VAR);
        // Adiciona a declaração constante com a palavra-chave 'const' seguida do tipo C
        // e do identificador à saída.
        saida.append("const " + type + " " + ctx.IDENT().getText() + " = ");
        // Visita o valor constante para processá-lo.
        visitValor_constante(ctx.valor_constante());
        // Adiciona o ponto-e-vírgula ao final da declaração de constante.
        saida.append(";\n");
        // Não possui retorno explícito, retorna 'null'.
        return null;
    }

    
    @Override
    public Void visitValor_constante(Valor_constanteContext ctx) {
        // Verifica o valor constante do contexto e adiciona o valor correspondente à
        // saída.
        if (ctx.getText().equals("verdadeiro")) {
            saida.append("true");
        } else if (ctx.getText().equals("falso")) {
            saida.append("false");
        } else {
            saida.append(ctx.getText());
        }
        // Não possui retorno explícito, retorna 'null'.
        return null;
    }


    @Override
    public Void visitCmd(CmdContext ctx) {
        // Verifica qual é o tipo de comando representado pelo nó e visita o comando
        // correspondente para processá-lo.
        if (ctx.cmdLeia() != null) {
            visitCmdLeia(ctx.cmdLeia());
        } else if (ctx.cmdEscreva() != null) {
            visitCmdEscreva(ctx.cmdEscreva());
        } else if (ctx.cmdAtribuicao() != null) {
            visitCmdAtribuicao(ctx.cmdAtribuicao());
        } else if (ctx.cmdSe() != null) {
            visitCmdSe(ctx.cmdSe());
        } else if (ctx.cmdCaso() != null) {
            visitCmdCaso(ctx.cmdCaso());
        } else if (ctx.cmdPara() != null) {
            visitCmdPara(ctx.cmdPara());
        } else if (ctx.cmdEnquanto() != null) {
            visitCmdEnquanto(ctx.cmdEnquanto());
        } else if (ctx.cmdFaca() != null) {
            visitCmdFaca(ctx.cmdFaca());
        } else if (ctx.cmdChamada() != null) {
            visitCmdChamada(ctx.cmdChamada());
        } else if (ctx.cmdRetorne() != null) {
            visitCmdRetorne(ctx.cmdRetorne());
        }
        // Não possui retorno explícito, retorna 'null'.
        return null;
    }


    @Override
    public Void visitCmdRetorne(CmdRetorneContext ctx) {
        // Adiciona a palavra-chave 'return' à saída seguida da expressão a ser
        // retornada.
        saida.append("return ");
        // Visita a expressão para processá-la.
        visitExpressao(ctx.expressao());
        // Adiciona o ponto-e-vírgula ao final do comando 'retorne'.
        saida.append(";\n");
        // Não possui retorno explícito, retorna 'null'.
        return null;
    }


    @Override
    public Void visitCmdChamada(CmdChamadaContext ctx) {
        // Adiciona o nome da chamada de função à saída.
        saida.append(ctx.IDENT().getText()).append("(");
        int i = 0;
        // Percorre as expressões de argumentos da chamada de função.
        for (ExpressaoContext exp : ctx.expressao()) {
            if (i++ > 0)
                saida.append(",");
            // Visita a expressão e adiciona à saída.
            visitExpressao(exp);
        }
        // Adiciona o fechamento da chamada de função e ponto e vírgula à saída.
        saida.append(");\n");
        // Não possui retorno explícito, retorna 'null'.
        return null;
    }

 
    @Override
    public Void visitCmdLeia(CmdLeiaContext ctx) {
        // Percorre cada identificador da lista de identificadores para leitura.
        for (AlgumaParser.IdentificadorContext id : ctx.identificador()) {
            // Verifica o tipo do identificador na tabela de símbolos.
            TabelaDeSimbolos.TipoAlguma idType = tabela.verificar(id.getText());
            // Verifica se o tipo é diferente de CADEIA, para usar 'scanf'.
            if (idType != TabelaDeSimbolos.TipoAlguma.CADEIA) {
                // Adiciona a string de formatação para 'scanf' na saída.
                saida.append("scanf(\"%").append(AlgumaSemanticoUtils.getCTypeSymbol(idType)).append("\", &");
                // Adiciona o nome do identificador e ponto e vírgula à saída.
                saida.append(id.getText()).append(");\n");
            } else {
                // Se o tipo for CADEIA, usa 'gets' para leitura.
                saida.append("gets(");
                // Visita o identificador para adicionar à saída.
                visitIdentificador(id);
                saida.append(");\n");
            }
        }
        // Não possui retorno explícito, retorna 'null'.
        return null;
    }

    @Override
    public Void visitCmdEscreva(CmdEscrevaContext ctx) {
        // Percorre cada expressão a ser escrita.
        for (AlgumaParser.ExpressaoContext exp : ctx.expressao()) {
            // Cria um novo escopo para verificar o tipo da expressão.
            Escopos escopo = new Escopos(tabela);
            // Obtém o símbolo do tipo da expressão em formato de caractere.
            String cType = AlgumaSemanticoUtils.getCTypeSymbol(AlgumaSemanticoUtils.verificar(escopo, exp));
            // Se a expressão já existe na tabela de símbolos, verifica seu tipo.
            if (tabela.existe(exp.getText())) {
                TabelaDeSimbolos.TipoAlguma tip = tabela.verificar(exp.getText());
                cType = AlgumaSemanticoUtils.getCTypeSymbol(tip);
            }
            // Adiciona a string de formatação para 'printf' na saída.
            saida.append("printf(\"%").append(cType).append("\", ");
            // Adiciona a expressão a ser escrita na saída.
            saida.append(exp.getText());
            saida.append(");\n");
        }
        // Não possui retorno explícito, retorna 'null'.
        return null;
    }


    @Override
    public Void visitCmdAtribuicao(CmdAtribuicaoContext ctx) {
        // Verifica se a atribuição é para um ponteiro e adiciona o símbolo '*' à saída.
        if (ctx.getText().contains("^"))
            saida.append("*");
        try {
            // Obtém o tipo da variável identificada pela atribuição.
            TabelaDeSimbolos.TipoAlguma tip = tabela.verificar(ctx.identificador().getText());
            // Se o tipo for CADEIA, trata como uma atribuição de cadeia de caracteres
            // (strcpy).
            if (tip != null && tip == TabelaDeSimbolos.TipoAlguma.CADEIA) {
                // Adiciona a chamada a 'strcpy' na saída.
                saida.append("strcpy(");
                // Adiciona o identificador da variável e a expressão a ser atribuída na saída.
                visitIdentificador(ctx.identificador());
                saida.append(",").append(ctx.expressao().getText()).append(");\n");
            } else {
                // Caso contrário, é uma atribuição normal.
                // Adiciona o identificador da variável na saída.
                visitIdentificador(ctx.identificador());
                // Adiciona o sinal de igual e a expressão a ser atribuída na saída.
                saida.append(" = ").append(ctx.expressao().getText()).append(";\n");
            }
        } catch (Exception e) {
            // Em caso de exceção, imprime a mensagem de erro.
            System.out.println(e.getMessage());
        }
        // Não possui retorno explícito, retorna 'null'.
        return null;
    }


    @Override
    public Void visitCmdSe(CmdSeContext ctx) {
        // Adiciona a condição do comando 'if' na saída.
        saida.append("if(");
        // Adiciona a expressão de condição na saída.
        visitExpressao(ctx.expressao());
        // Adiciona o fechamento da condição e abre o bloco de comandos do 'if'.
        saida.append(") {\n");
        // Percorre os comandos associados ao 'if' e os adiciona à saída.
        for (CmdContext cmd : ctx.cmd()) {
            visitCmd(cmd);
        }
        // Adiciona o fechamento do bloco de comandos do 'if'.
        saida.append("}\n");
        // Verifica se há um comando 'senão' associado ao 'if'.
        if (ctx.cmdSenao() != null) {
            // Adiciona o comando 'senão' na saída.
            saida.append("else {\n");
            // Percorre os comandos associados ao 'senão' e os adiciona à saída.
            for (CmdContext cmd : ctx.cmdSenao().cmd()) {
                visitCmd(cmd);
            }
            // Adiciona o fechamento do bloco de comandos do 'senão'.
            saida.append("}\n");
        }
        // Não possui retorno explícito, retorna 'null'.
        return null;
    }


    @Override
    public Void visitExpressao(ExpressaoContext ctx) {
        // Verifica se a expressão possui termos lógicos.
        if (ctx.termo_logico() != null) {
            // Adiciona o primeiro termo lógico na saída.
            visitTermo_logico(ctx.termo_logico(0));

            // Percorre os termos lógicos restantes e os adiciona à saída com o operador
            // '||' (ou).
            for (int i = 1; i < ctx.termo_logico().size(); i++) {
                AlgumaParser.Termo_logicoContext termo = ctx.termo_logico(i);
                saida.append(" || ");
                visitTermo_logico(termo);
            }
        }
        // Não possui retorno explícito, retorna 'null'.
        return null;
    }


    @Override
    public Void visitTermo_logico(Termo_logicoContext ctx) {
        // Adiciona o primeiro fator lógico na saída.
        visitFator_logico(ctx.fator_logico(0));

        // Percorre os fatores lógicos restantes e os adiciona à saída com o operador
        // '&&' (e).
        for (int i = 1; i < ctx.fator_logico().size(); i++) {
            AlgumaParser.Fator_logicoContext fator = ctx.fator_logico(i);
            saida.append(" && ");
            visitFator_logico(fator);
        }

        // Não possui retorno explícito, retorna 'null'.
        return null;
    }

    @Override
    public Void visitFator_logico(Fator_logicoContext ctx) {
        // Verifica se o fator lógico possui o operador 'nao' (não) e adiciona '!' na
        // saída.
        if (ctx.getText().startsWith("nao")) {
            saida.append("!");
        }
        // Visita a parcela lógica do fator lógico.
        visitParcela_logica(ctx.parcela_logica());

        // Não possui retorno explícito, retorna 'null'.
        return null;
    }

    @Override
    public Void visitParcela_logica(Parcela_logicaContext ctx) {
        // Verifica se a parcela lógica possui uma expressão relacional.
        if (ctx.exp_relacional() != null) {
            // Visita a expressão relacional e a adiciona à saída.
            visitExp_relacional(ctx.exp_relacional());
        } else {
            // Caso não haja expressão relacional, verifica se a parcela é 'verdadeiro' ou
            // 'falso'.
            if (ctx.getText().equals("verdadeiro")) {
                saida.append("true");
            } else {
                saida.append("false");
            }
        }

        // Não possui retorno explícito, retorna 'null'.
        return null;
    }


    @Override
    public Void visitExp_relacional(Exp_relacionalContext ctx) {
        // Adiciona a primeira expressão aritmética na saída.
        visitExp_aritmetica(ctx.exp_aritmetica(0));

        // Percorre as demais expressões aritméticas e operadores relacionais e os
        // adiciona à saída.
        for (int i = 1; i < ctx.exp_aritmetica().size(); i++) {
            AlgumaParser.Exp_aritmeticaContext termo = ctx.exp_aritmetica(i);
            if (ctx.op_relacional().getText().equals("=")) {
                saida.append(" == ");
            } else {
                saida.append(ctx.op_relacional().getText());
            }
            visitExp_aritmetica(termo);
        }

        // Não possui retorno explícito, retorna 'null'.
        return null;
    }


    @Override
    public Void visitExp_aritmetica(Exp_aritmeticaContext ctx) {
        // Adiciona o primeiro termo na saída.
        visitTermo(ctx.termo(0));

        // Percorre os demais termos e operadores aritméticos e os adiciona à saída.
        for (int i = 1; i < ctx.termo().size(); i++) {
            AlgumaParser.TermoContext termo = ctx.termo(i);
            saida.append(ctx.op1(i - 1).getText());
            visitTermo(termo);
        }

        // Não possui retorno explícito, retorna 'null'.
        return null;
    }

    @Override
    public Void visitTermo(TermoContext ctx) {
        // Adiciona o primeiro fator na saída.
        visitFator(ctx.fator(0));

        // Percorre os demais fatores e operadores '*' ou '/' e os adiciona à saída.
        for (int i = 1; i < ctx.fator().size(); i++) {
            AlgumaParser.FatorContext fator = ctx.fator(i);
            saida.append(ctx.op2(i - 1).getText());
            visitFator(fator);
        }

        // Não possui retorno explícito, retorna 'null'.
        return null;
    }

    @Override
    public Void visitFator(FatorContext ctx) {
        // Adiciona a primeira parcela na saída.
        visitParcela(ctx.parcela(0));

        // Percorre as demais parcelas e operadores '+' ou '-' e os adiciona à saída.
        for (int i = 1; i < ctx.parcela().size(); i++) {
            AlgumaParser.ParcelaContext parcela = ctx.parcela(i);
            saida.append(ctx.op3(i - 1).getText());
            visitParcela(parcela);
        }

        // Não possui retorno explícito, retorna 'null'.
        return null;
    }

    @Override
    public Void visitParcela(ParcelaContext ctx) {
        if (ctx.parcela_unario() != null) {
            if (ctx.op_unario() != null) {
                saida.append(ctx.op_unario().getText());
            }
            visitParcela_unario(ctx.parcela_unario());
        } else {
            visitParcela_nao_unario(ctx.parcela_nao_unario());
        }

        // Não possui retorno explícito, retorna 'null'.
        return null;
    }

  
    @Override
    public Void visitParcela_unario(Parcela_unarioContext ctx) {

        if (ctx.IDENT() != null) {
            saida.append(ctx.IDENT().getText());
            saida.append("(");
            for (int i = 0; i < ctx.expressao().size(); i++) {
                visitExpressao(ctx.expressao(i));
                if (i < ctx.expressao().size() - 1) {
                    saida.append(", ");
                }
            }
            saida.append(")");
        } else if (ctx.parentesis_expressao() != null) {
            saida.append("(");
            visitExpressao(ctx.parentesis_expressao().expressao());
            saida.append(")");
        } else {
            saida.append(ctx.getText());
        }

        // Não possui retorno explícito, retorna 'null'.
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
        if (ctx.cmdSenao() != null) {
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
        // Divide a constante em uma lista de intervalo (se houver)
        ArrayList<String> intervalo = new ArrayList<>(Arrays.asList(ctx.constantes().getText().split("\\.\\.")));
        // Obtém o primeiro e o último valor do intervalo, ou o único valor caso não
        // haja intervalo
        String first = intervalo.size() > 0 ? intervalo.get(0) : ctx.constantes().getText();
        String last = intervalo.size() > 1 ? intervalo.get(1) : intervalo.get(0);
        // Itera sobre os valores no intervalo e gera código para cada um deles
        for (int i = Integer.parseInt(first); i <= Integer.parseInt(last); i++) {
            saida.append("case " + i + ":\n");
            // Visita os comandos dentro do ramo "caso"
            ctx.cmd().forEach(var -> visitCmd(var));
            saida.append("break;\n");
        }
        return null;
    }

  
    @Override
    public Void visitCmdSenao(CmdSenaoContext ctx) {
        saida.append("default:\n");
        // Visita os comandos dentro do ramo "senão"
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
        // Visita os comandos dentro do loop "para"
        ctx.cmd().forEach(var -> visitCmd(var));
        saida.append("}\n");
        return null;
    }


    @Override
    public Void visitCmdEnquanto(CmdEnquantoContext ctx) {
        saida.append("while(");
        // Visita a expressão lógica do comando "enquanto"
        visitExpressao(ctx.expressao());
        saida.append("){\n");
        // Visita os comandos dentro do loop "enquanto"
        ctx.cmd().forEach(var -> visitCmd(var));
        saida.append("}\n");
        return null;
    }


    @Override
    public Void visitCmdFaca(CmdFacaContext ctx) {
        saida.append("do{\n");
        // Visita os comandos dentro do loop "faca"
        ctx.cmd().forEach(var -> visitCmd(var));
        saida.append("} while(");
        // Visita a expressão lógica do comando "faca"
        visitExpressao(ctx.expressao());
        saida.append(");\n");
        return null;
    }

}
