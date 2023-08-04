package compiladores;

import java.util.LinkedList;
import java.util.List;

public class Escopos {

    private LinkedList<TabelaDeSimbolos> pilhaDeTabelas;

    public Escopos(TabelaDeSimbolos.TipoAlguma tipo) {
        pilhaDeTabelas = new LinkedList<>();
        criarNovoEscopo(tipo);
    }

    public Escopos(TabelaDeSimbolos t){
        pilhaDeTabelas = new LinkedList<>();
        pilhaDeTabelas.push(t);
    }

    public void criarNovoEscopo(TabelaDeSimbolos.TipoAlguma tipo) {
        pilhaDeTabelas.push(new TabelaDeSimbolos(tipo));
    }

    public TabelaDeSimbolos obterEscopoAtual() {
        return pilhaDeTabelas.peek();
    }

    public List<TabelaDeSimbolos> percorrerEscoposAninhados() {
        return pilhaDeTabelas;
    }

    public void abandonarEscopo() {
        pilhaDeTabelas.pop();
    }

    public boolean existeIdent(String nome) {
        for(TabelaDeSimbolos escopo : pilhaDeTabelas) {
            if(!escopo.existe(nome)) {
                return true;
            }
        }
        return false;
    }   
}
