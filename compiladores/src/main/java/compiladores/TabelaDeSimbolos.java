package compiladores;

import java.util.ArrayList;
import java.util.HashMap;

import org.antlr.v4.parse.ANTLRParser.element_return;

public class TabelaDeSimbolos {

    public TabelaDeSimbolos.TipoAlguma tipo;

    public enum TipoAlguma {
        INTEIRO,
        REAL,
        CADEIA,
        LOGICO,
        INVALIDO,
        REG,
        VOID
    }

    public enum Structure {
        VAR, 
        CONST, 
        PROC, 
        FUNC, 
        TIPO
    }
    
    class EntradaTabelaDeSimbolos {
        TipoAlguma tipo;
        String nome;
        Structure structure;

        public EntradaTabelaDeSimbolos(String nome, TipoAlguma tipo, Structure structure) {
            this.tipo = tipo;
            this.nome = nome;
            this.structure = structure;
        }
    }
    
    private HashMap<String, EntradaTabelaDeSimbolos> tabela;
    private HashMap<String, ArrayList<EntradaTabelaDeSimbolos>> tipoTabela; 
 
    public boolean existe(String nome) {
        return tabela.containsKey(nome);
    }
    
    public TipoAlguma verificar(String nome) {
        return tabela.get(nome).tipo;
    }
    
    public TabelaDeSimbolos(TabelaDeSimbolos.TipoAlguma tipo) {
        tabela = new HashMap<>();
        tipoTabela = new HashMap<>();
        this.tipo = tipo;

    }
    
    public void adicionar(String nome, TipoAlguma tipo, Structure structure){
        EntradaTabelaDeSimbolos entrada = new EntradaTabelaDeSimbolos(nome, tipo, structure);
        tabela.put(nome, entrada);
    }

    public void adicionar(EntradaTabelaDeSimbolos entrada) {
        tabela.put(entrada.nome, entrada);
    }

    public void adicionar(String tipoNome, EntradaTabelaDeSimbolos entrada){
        if(tipoTabela.containsKey(tipoNome)){
            tipoTabela.get(tipoNome).add(entrada);
        }else{
            ArrayList<EntradaTabelaDeSimbolos> list = new ArrayList<>();
            list.add(entrada);
            tipoTabela.put(tipoNome, list);
        }
    }

    public ArrayList<EntradaTabelaDeSimbolos> retornaTipo(String nome){
        return tipoTabela.get(nome);
    }
}
