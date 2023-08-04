## Trabalho 5 da disciplina de Compiladores

## Autores

  Ivan Duarte Calvo - RA: 790739  
  João Ricardo Lopes Lovato - RA: 772138  
  Vinícius Borges de Lima - RA: 795316  

## Programas necessários

  O código foi escrito na linguagem *Java* utilizando também o *Maven*, e também foi utilizado o *ANTLR 4.11.1*. Os arquivos e instruções para *downloads* e instalações podem ser encontrados nos seguintes endereços:  
  https://www.antlr.org/download.html
    
## Compilação e execução do programa
  
  Os arquivos gerados pelos *ANTLR* podem ser criados através do seguinte comando:

    mvn generate-sources

Após isso, a compilação e execução do código é realizada com o comando:

    mvn compile
    mvn package
    java -jar compiladores-compiladores-jar-with-dependencies.jar entrada.txt saida.txt
