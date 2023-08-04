package compiladores;


import java.io.PrintWriter;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Token;
import java.util.BitSet;



public class MensagemErro implements ANTLRErrorListener {
    public PrintWriter p;

    static boolean error_found = false;
    public MensagemErro(PrintWriter p){
        this.p = p;
    }

    @Override
    public void syntaxError(Recognizer<?, ?> arg0, Object arg1, int arg2, int arg3, String arg4,
            RecognitionException arg5) {
    
        Token t = (Token) arg1;
        String text = t.getText();
        text = (text.equals("<EOF>")) ? "EOF" : text;
        
        String aType = AlgumaLexer.VOCABULARY.getDisplayName(t.getType()); // Converte o tipo desse token para string
        if(!error_found){
            MensagemErro.error_found = true;

            if(aType == "Nao_Fechado"){
                p.println("Linha " + t.getLine() + ": " + "comentario nao fechado");
            }
            else if(aType == "Literal_Nao_Fechada"){
                p.println("Linha " + t.getLine() + ": " + "cadeia literal nao fechada");
            }
            else if(aType == "ERR"){
                p.println("Linha " + t.getLine() + ": " + text + " - simbolo nao identificado");
            }
            else{
                p.println("Linha " + arg2 + ": erro sintatico proximo a " + text);
            }
        }

        
    }

    @Override
    public void reportAmbiguity(Parser recognizer, DFA dfa, int startIndex, int stopIndex, boolean exact,
            BitSet ambigAlts, ATNConfigSet configs) {
    }

    @Override
    public void reportAttemptingFullContext(Parser recognizer, DFA dfa, int startIndex, int stopIndex,
            BitSet conflictingAlts, ATNConfigSet configs) {
        
    }

    @Override
    public void reportContextSensitivity(Parser recognizer, DFA dfa, int startIndex, int stopIndex, int prediction,
            ATNConfigSet configs) {
    
    }
}