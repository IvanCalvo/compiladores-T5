/*
    Autores:
    Ivan Duarte Calvo RA: 790739
    João Ricardo Lopes Lovato RA: 772138
    Vinícius Borges de Lima RA: 795316

    COMPILAÇÃO:
    o programa pode ser compilado e rodado através dos seguintes comandos
        mvn compile
        mvn package
        java -jar compiladores-compiladores-jar-with-dependencies.jar entrada.txt saida.txt
*/

package compiladores;
import java.io.File;
import java.io.PrintWriter;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;

public class App {

    public static void main(String args[]) {
        try(PrintWriter p = new PrintWriter(new File(args[1]))) {//saida
            CharStream c = CharStreams.fromFileName(args[0]);//entrada
            AlgumaLexer lex = new AlgumaLexer(c);
            CommonTokenStream cs = new CommonTokenStream(lex); //conversão para token stream
            AlgumaParser parser = new AlgumaParser(cs);            
            AlgumaParser.ProgramaContext arvore = parser.programa();   
            AlgumaSemantico as = new AlgumaSemantico();  
            as.visitPrograma(arvore);
            for(String error: AlgumaSemanticoUtils.errosSemanticos){
                p.println(error);
            }
            if(AlgumaSemanticoUtils.errosSemanticos.isEmpty()) {
                AlgumaGeradorC agc = new AlgumaGeradorC();
                agc.visitPrograma(arvore);
                try(PrintWriter pw = new PrintWriter(args[1])) {
                    pw.print(agc.saida.toString());
                }
            }
            p.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
}