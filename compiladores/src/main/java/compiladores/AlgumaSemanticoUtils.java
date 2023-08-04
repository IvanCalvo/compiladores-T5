package compiladores;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.Token;

import com.ibm.icu.impl.UResource.Table;

import compiladores.AlgumaParser.Exp_aritmeticaContext;
import compiladores.AlgumaParser.ExpressaoContext;
import compiladores.AlgumaParser.FatorContext;
import compiladores.AlgumaParser.Fator_logicoContext;
import compiladores.AlgumaParser.ParcelaContext;
import compiladores.AlgumaParser.TermoContext;
import compiladores.AlgumaParser.Termo_logicoContext;
import compiladores.TabelaDeSimbolos.TipoAlguma;

public class AlgumaSemanticoUtils {
    // Lista que armazena mensagens de erros semânticos
    public static List<String> errosSemanticos = new ArrayList<>();

    // Método para adicionar uma mensagem de erro semântico à lista
    public static void adicionarErroSemantico(Token t, String mensagem) {
        int linha = t.getLine();
        // Formata a mensagem de erro com a linha do token e a mensagem fornecida
        errosSemanticos.add(String.format("Linha %d: %s", linha, mensagem));
    }

    // Método que realiza a verificação semântica de uma expressão
    public static TabelaDeSimbolos.TipoAlguma verificar(Escopos escopos, AlgumaParser.ExpressaoContext ctx) {
        TabelaDeSimbolos.TipoAlguma ret = null;
        // Percorre todos os termos lógicos presentes na expressão
        for (Termo_logicoContext ta : ctx.termo_logico()) {
            // Realiza a verificação semântica do termo lógico
            TabelaDeSimbolos.TipoAlguma aux = verificar(escopos, ta);
            // Atualiza o tipo de retorno da expressão de acordo com o tipo do termo lógico
            if (ret == null) {
                ret = aux;
            } else if (ret != aux && aux != TabelaDeSimbolos.TipoAlguma.INVALIDO) {
                ret = TabelaDeSimbolos.TipoAlguma.INVALIDO;
            }
        }
        return ret;
    }

    // Método que realiza a verificação semântica de um termo lógico
    public static TabelaDeSimbolos.TipoAlguma verificar(Escopos escopos, AlgumaParser.Termo_logicoContext ctx) {
        TabelaDeSimbolos.TipoAlguma ret = null;
        // Percorre todos os fatores lógicos presentes no termo lógico
        for (Fator_logicoContext ta : ctx.fator_logico()) {
            // Realiza a verificação semântica do fator lógico
            TabelaDeSimbolos.TipoAlguma aux = verificar(escopos, ta);
            // Atualiza o tipo de retorno do termo lógico de acordo com o tipo do fator lógico
            if (ret == null) {
                ret = aux;
            } else if (ret != aux && aux != TabelaDeSimbolos.TipoAlguma.INVALIDO) {
                ret = TabelaDeSimbolos.TipoAlguma.INVALIDO;
            }
        }

        return ret;
    }
    public static TabelaDeSimbolos.TipoAlguma verificar(Escopos escopos, AlgumaParser.Fator_logicoContext ctx) {
        // Verifica o fator lógico chamando o método verificar para a parcela lógica
        return verificar(escopos, ctx.parcela_logica());
    }
    
    public static TabelaDeSimbolos.TipoAlguma verificar(Escopos escopos, AlgumaParser.Parcela_logicaContext ctx) {
        TabelaDeSimbolos.TipoAlguma ret = null;
        // Verifica se a parcela lógica contém uma expressão relacional
        if (ctx.exp_relacional() != null) {
            // Verifica a expressão relacional
            ret = verificar(escopos, ctx.exp_relacional());
        } else {
            // Caso contrário, o tipo de retorno é lógico
            ret = TabelaDeSimbolos.TipoAlguma.LOGICO;
        }
    
        return ret;
    }
    
    public static TabelaDeSimbolos.TipoAlguma verificar(Escopos escopos, AlgumaParser.Exp_relacionalContext ctx) {
        TabelaDeSimbolos.TipoAlguma ret = null;
        // Verifica se a expressão relacional possui um operador relacional
        if (ctx.op_relacional() != null) {
            // Percorre as expressões aritméticas presentes na expressão relacional
            for (Exp_aritmeticaContext ta : ctx.exp_aritmetica()) {
                // Verifica a expressão aritmética
                TabelaDeSimbolos.TipoAlguma aux = verificar(escopos, ta);
                Boolean auxNumeric = aux == TabelaDeSimbolos.TipoAlguma.REAL || aux == TabelaDeSimbolos.TipoAlguma.INTEIRO;
                Boolean retNumeric = ret == TabelaDeSimbolos.TipoAlguma.REAL || ret == TabelaDeSimbolos.TipoAlguma.INTEIRO;
                // Atualiza o tipo de retorno da expressão relacional de acordo com o tipo da expressão aritmética
                if (ret == null) {
                    ret = aux;
                } else if (!(auxNumeric && retNumeric) && aux != ret) {
                    ret = TabelaDeSimbolos.TipoAlguma.INVALIDO;
                }
            }
            // Se o tipo de retorno da expressão relacional não for inválido, ele é do tipo lógico
            if (ret != TabelaDeSimbolos.TipoAlguma.INVALIDO) {
                ret = TabelaDeSimbolos.TipoAlguma.LOGICO;
            }
        } else {
            // Caso contrário, a expressão relacional é reduzida para uma expressão aritmética
            ret = verificar(escopos, ctx.exp_aritmetica(0));
        }
    
        return ret;
    }
    
    public static TabelaDeSimbolos.TipoAlguma verificar(Escopos escopos, AlgumaParser.Exp_aritmeticaContext ctx) {
        TabelaDeSimbolos.TipoAlguma ret = null;
        // Percorre os termos presentes na expressão aritmética
        for (TermoContext ta : ctx.termo()) {
            // Verifica o termo
            TabelaDeSimbolos.TipoAlguma aux = verificar(escopos, ta);
            // Atualiza o tipo de retorno da expressão aritmética de acordo com o tipo do termo
            if (ret == null) {
                ret = aux;
            } else if (ret != aux && aux != TabelaDeSimbolos.TipoAlguma.INVALIDO) {
                ret = TabelaDeSimbolos.TipoAlguma.INVALIDO;
            }
        }
    
        return ret;
    }
    
    public static TabelaDeSimbolos.TipoAlguma verificar(Escopos escopos, AlgumaParser.TermoContext ctx) {
        TabelaDeSimbolos.TipoAlguma ret = null;
        // Percorre os fatores presentes no termo
        for (FatorContext fa : ctx.fator()) {
            // Verifica o fator
            TabelaDeSimbolos.TipoAlguma aux = verificar(escopos, fa);
            Boolean auxNumeric = aux == TabelaDeSimbolos.TipoAlguma.REAL || aux == TabelaDeSimbolos.TipoAlguma.INTEIRO;
            Boolean retNumeric = ret == TabelaDeSimbolos.TipoAlguma.REAL || ret == TabelaDeSimbolos.TipoAlguma.INTEIRO;
            // Atualiza o tipo de retorno do termo de acordo com o tipo do fator
            if (ret == null) {
                ret = aux;
            } else if (!(auxNumeric && retNumeric) && aux != ret) {
                ret = TabelaDeSimbolos.TipoAlguma.INVALIDO;
            }
        }
        return ret;
    }
    
    public static TabelaDeSimbolos.TipoAlguma verificar(Escopos escopos, AlgumaParser.FatorContext ctx) {
        TabelaDeSimbolos.TipoAlguma ret = null;

        for (ParcelaContext fa : ctx.parcela()) {
            TabelaDeSimbolos.TipoAlguma aux = verificar(escopos, fa);
            if (ret == null) {
                ret = aux;
            } else if (ret != aux && aux != TabelaDeSimbolos.TipoAlguma.INVALIDO) {
                ret = TabelaDeSimbolos.TipoAlguma.INVALIDO;
            }
        }
        return ret;
    }
    public static TabelaDeSimbolos.TipoAlguma verificar(Escopos escopos, AlgumaParser.ParcelaContext ctx) {
        TabelaDeSimbolos.TipoAlguma ret = TabelaDeSimbolos.TipoAlguma.INVALIDO;

        if(ctx.parcela_nao_unario() != null){
            ret = verificar(escopos, ctx.parcela_nao_unario());
        }
        else {
            ret = verificar(escopos, ctx.parcela_unario());
        }
        return ret;
    }

    public static TabelaDeSimbolos.TipoAlguma verificar(Escopos escopos, AlgumaParser.Parcela_nao_unarioContext ctx) {
        if (ctx.identificador() != null) {
            return verificar(escopos, ctx.identificador());
        }
        return TabelaDeSimbolos.TipoAlguma.CADEIA;
    }

    public static TabelaDeSimbolos.TipoAlguma verificar(Escopos escopos, AlgumaParser.IdentificadorContext ctx) {//kk suspeitos
        String nomeVar = "";
        TabelaDeSimbolos.TipoAlguma ret = TabelaDeSimbolos.TipoAlguma.INVALIDO;
        for(int i = 0; i < ctx.IDENT().size(); i++){
            nomeVar += ctx.IDENT(i).getText();
            if(i != ctx.IDENT().size() - 1){
                nomeVar += ".";
            }
        }
        for(TabelaDeSimbolos tabela : escopos.percorrerEscoposAninhados()){
            if (tabela.existe(nomeVar)) {
                ret = verificar(escopos, nomeVar);
            }
        }
        System.out.println(nomeVar);
        return ret;
    }
    
    public static TabelaDeSimbolos.TipoAlguma verificar(Escopos escopos, AlgumaParser.Parcela_unarioContext ctx) {
        if (ctx.NUM_INT() != null) {
            return TabelaDeSimbolos.TipoAlguma.INTEIRO;
        }
        if (ctx.NUM_REAL() != null) {
            return TabelaDeSimbolos.TipoAlguma.REAL;
        }
        if(ctx.identificador() != null){
            return verificar(escopos, ctx.identificador());
        }
        if (ctx.IDENT() != null) {
            return verificar(escopos, ctx.IDENT().getText());
        } else {
            TabelaDeSimbolos.TipoAlguma ret = null;
            for (ExpressaoContext fa : ctx.expressao()) {
                TabelaDeSimbolos.TipoAlguma aux = verificar(escopos, fa);
                if (ret == null) {
                    ret = aux;
                } else if (ret != aux && aux != TabelaDeSimbolos.TipoAlguma.INVALIDO) {
                    ret = TabelaDeSimbolos.TipoAlguma.INVALIDO;
                }
            }
            return ret;
        }
    }

    public static TabelaDeSimbolos.TipoAlguma verificar(Escopos escopos, String nomeVar) {
        TabelaDeSimbolos.TipoAlguma type = TabelaDeSimbolos.TipoAlguma.INVALIDO;
        for(TabelaDeSimbolos tabela : escopos.percorrerEscoposAninhados()){
            if(tabela.existe(nomeVar)){
                return tabela.verificar(nomeVar);
            }
        }

        return type;
    }

    public static TabelaDeSimbolos.TipoAlguma getTipo(String val){
        TabelaDeSimbolos.TipoAlguma tipo = null;
                switch(val) {
                    case "literal": 
                        tipo = TabelaDeSimbolos.TipoAlguma.CADEIA;
                        break;
                    case "inteiro": 
                        tipo = TabelaDeSimbolos.TipoAlguma.INTEIRO;
                        break;
                    case "real": 
                        tipo = TabelaDeSimbolos.TipoAlguma.REAL;
                        break;
                    case "logico": 
                        tipo = TabelaDeSimbolos.TipoAlguma.LOGICO;
                        break;
                    default:
                        break;
                }
        return tipo;
    }


    public static String getCType(String val){
        String tipo = null;
                switch(val) {
                    case "literal": 
                        tipo = "char";
                        break;
                    case "inteiro": 
                        tipo = "int";
                        break;
                    case "real": 
                        tipo = "float";
                        break;
                    default:
                        break;
                }
        return tipo;
    }

    public static String getCTypeSymbol(TabelaDeSimbolos.TipoAlguma tipo){
        String type = null;
                switch(tipo) {
                    case CADEIA: 
                        type = "s";
                        break;
                    case INTEIRO: 
                        type = "d";
                        break;
                    case REAL: 
                        type = "f";
                        break;
                    default:
                        break;
                }
        return type;
    }


}
