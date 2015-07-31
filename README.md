# ciandt-test-case (Java 8 Lambda + Play + Akka)

Resolução de problema de malha rodoviária (PCV), utilizando Java + Play + Akka actors (Multiplas threads) para processamento assincrono das melhores rotas possiveis de um determinado mapa informado.

## Algoritmos implementados

* Força Bruta (Lento se o mapa for grande, devido a complexidade ser NP-Completo. Possivel crash da maquina virtual)

## Requisitos 

* Java 8
* Play Framework 2.x
* Akka
* Hibernate

## Execução

Os testes poderão ser executados tanto pela Eclipse, ou utilizando os comandos do play. 

* Pelo activator do play framework. Neste caso, deverá ser utilizado um client REST para execução dos métodos.

> [ciandt-test-case] $ activator run

* Para execução dos testes Unitarios e funcionais.
 
> [ciandt-test-case] $ activator test

## RESTs Methods

#### http://localhost:9000/process/{nome do mapa}
##### Method: POST
##### Type: text/plain
Recebe o mapa enviado e persite no banco de dados. O mapa enviado deverá vir no formato: 

```
A B 10 
A H 15
B H 20
B C 20
C G 20
B G 20
H G 30
```

#### http://localhost:9000/process/async/{nome do mapa}
##### Method: POST
##### Type: text/plain
recebe o mapa e processa de forma assincrona e em multipla threads, mapeando todos os melhores caminhos. Utiliza um algoritmo de força bruta (Lento se o mapa for grande) que verifica todas as opçoes possiveis e retorna o menor caminho entre cada rota.
 
```
A B 10 
A H 15
B H 20
B C 20
C G 20
B G 20
H G 30
```

#### http://localhost:9000/check/{nome do mapa}
##### Method: GET
Checa o Status atual do processamento do mapa
> Ex.: {"name":"BH","status":"PROCESSED","message":null}

#### http://localhost:9000/best/{nome do mapa}/{origem}/{destino}/{autonomia do veiculo}
##### Method: GET
Retorna a melhor rota para o usuario.
> EX.: {"path":"A;B;G;M;Q;W;E1;F1;G1;","distance":130.0,"cost":13.0}



