# Editor de Desenho Vetorial Interativo (JavaFX)

**Prova N2** – Programação Orientada a Objetos 1
Universidade de Rio Verde (UniRV) – Engenharia de Software – Semestre 2026/1
Docente: Prof. Me. Sergio Souza Novak
Aluno: **João Marcos**

Editor de desenho para computador que permite criar, editar, transformar e salvar
formas geométricas 2D (polígonos, quadrados, círculos e hexágonos) numa tela interativa.
O projeto aplica os padrões de projeto **State**, **Prototype**, **Memento/Command** e
**Composite**, e é dividido nas camadas **Models**, **Controller** e **Views**.

- Repositório: https://github.com/jmsaldanha/editor-vetorial
- Diagrama de classes UML: [`docs/diagrama_classes.pdf`](docs/diagrama_classes.pdf)

---

## Tecnologias

| Item              | Usado no projeto                                                   |
|-------------------|--------------------------------------------------------------------|
| Linguagem         | Java 17 ou superior (desenvolvido e testado com o **JDK 26**)      |
| Interface gráfica | JavaFX 21, montada por código (sem FXML)                           |
| Build             | Apache Maven (`pom.xml`)                                           |
| Salvamento        | Arquivos JSON, com a biblioteca Gson                               |
| IDE utilizada     | IntelliJ IDEA (também abre no VS Code, Eclipse ou NetBeans)        |

> O `pom.xml` compila para Java 17 (`release 17`). Assim o projeto roda em qualquer
> JDK a partir do 17, inclusive no JDK 26 usado no desenvolvimento.
> O JavaFX e o Gson são baixados automaticamente pelo Maven: não é preciso instalar nada além do JDK.

---

## Como executar

### IntelliJ IDEA (recomendado)

1. **File › Open** e escolha a pasta do projeto (a que contém o `pom.xml`). Clique em **Trust Project**.
2. Espere o IntelliJ importar o Maven (barra de progresso no canto inferior).
3. Em **File › Project Structure › Project**, selecione um JDK 17 ou superior.
4. Abra a aba **Maven** (lado direito) › **editor-vetorial › Plugins › javafx** e dê dois cliques em **javafx:run**.

> Não execute pelo botão ▶ do método `main`: como o JavaFX é um módulo separado do Java,
> isso causa o erro *"JavaFX runtime components are missing"*. Use sempre o **javafx:run**.

### Terminal (com Maven instalado)

```bash
mvn clean javafx:run
```

---

## Funcionalidades

| Recurso                   | Como usar                                                                          |
|---------------------------|------------------------------------------------------------------------------------|
| Linha Livre (polígono)    | Clique para adicionar vértices; **duplo clique**, **botão direito** ou **ENTER** finaliza; **ESC** cancela |
| Inserir Quadrado / Círculo / Hexágono | Pressione no centro e arraste para definir o tamanho (clique simples = raio 50) |
| Selecionar                | Clique seleciona (SHIFT soma/remove); arraste a forma para mover; arraste no vazio para a caixa de seleção |
| Escalar / Rotacionar / Cisalhar | Arraste com a ferramenta escolhida (em torno do centro da forma ou do grupo) |
| Editar Vértice            | Clique na forma e arraste um dos vértices (pontos vermelhos)                       |
| Excluir                   | Ferramenta Excluir (clique na forma) ou tecla **Delete** na seleção                |
| Estilos                   | Cor de preenchimento, cor da borda e espessura da borda                            |
| Grade Magnética           | Encaixa as coordenadas numa grade de 20 px                                         |
| Camadas                   | Trazer para frente / Enviar para trás                                              |
| Grupos                    | Agrupar / Desagrupar: formas agrupadas são movidas e transformadas juntas          |
| Desfazer / Refazer        | **Ctrl+Z** / **Ctrl+Y** (ou botões da barra lateral)                               |
| Salvar / Carregar         | **Ctrl+S** / **Ctrl+O**, em arquivo `.json`                                         |
| Barra de status           | Coordenadas do mouse, total de formas, área e perímetro em tempo real              |

---

## Estrutura do projeto

```
src/main/java/org/example
├── Models        → dados e matemática das formas
│   ├── FormaGeometrica.java   interface (contrato das formas)
│   ├── FormaBase.java         classe abstrata: área, perímetro, centroide e transformações
│   ├── Ponto.java             vértice (x, y)
│   ├── Poligono.java          polígono livre (mínimo 3 pontos)
│   ├── Quadrado.java          vértices calculados a partir do centro e do raio
│   ├── Circulo.java           raio obrigatoriamente maior que zero
│   └── Hexagono.java          vértices calculados a partir do centro e do raio
├── Controller    → regras de interação
│   ├── EditorContext.java          estado do editor, seleção, grupos e camadas
│   ├── HistoricoController.java    desfazer / refazer
│   ├── Ferramenta.java             interface das ferramentas (State)
│   ├── FerramentaDesenhar.java     Linha Livre
│   ├── FerramentaSelecionar.java   seleção e movimentação
│   ├── FerramentaTransformar.java  escalar, rotacionar, cisalhar e excluir
│   ├── ModoFerramenta.java         modos da FerramentaTransformar
│   ├── FerramentaInserirForma.java quadrado, círculo e hexágono
│   └── FerramentaEditarVertice.java arrastar vértices
└── Views         → interface gráfica
    ├── ContainerApp.java       janela principal, desenho do canvas e salvar/carregar
    ├── BarraLateralView.java   barra lateral com estilos e ferramentas
    └── PainelStatusView.java   barra de status
```

---

## Padrões de projeto

- **State:** cada ferramenta é uma classe que implementa `Ferramenta`. O `EditorContext`
  guarda a ferramenta ativa, e a tela só repassa os eventos do mouse para ela. Trocar de
  ferramenta é trocar o objeto, sem `if/else` na interface.
- **Memento / Command:** antes de cada alteração, o `HistoricoController` guarda uma cópia
  do desenho numa pilha (`undoStack`). Desfazer e refazer movem esses estados entre as
  pilhas `undoStack` e `redoStack` (limite de 100 estados).
- **Prototype:** `clonar()` cria uma cópia profunda de cada forma (pontos, estilo e grupo),
  usada pelo histórico para que as cópias não mudem junto com o desenho.
- **Composite:** formas com o mesmo `groupId` são selecionadas juntas e transformadas em
  volta do centro do grupo, como se fossem uma única forma.

---

## Regras e cálculos

- Polígono válido tem **no mínimo 3 pontos**.
- Círculo, quadrado e hexágono exigem **raio maior que zero**.
- **Área** pela fórmula de Gauss (fórmula do cadarço).
- **Perímetro** pela soma das distâncias euclidianas entre vértices consecutivos.
- **Translação** soma `dx` e `dy`. **Escala** multiplica a distância de cada ponto ao centroide.
  **Rotação** é feita em radianos, em torno do centroide. **Cisalhamento** também é feito em torno do centroide.
- Cores no formato `#RRGGBB` e espessura de borda positiva são validadas.

---

## Salvamento (JSON) e tratamento de erros

O desenho é salvo em JSON com as configurações do canvas e a lista de formas
(tipo, cores, espessura, pontos e grupo):

```json
{
  "versao": "1.0",
  "canvas": { "largura": 994.0, "altura": 770.0, "corFundo": "#FFFFFF",
              "gridSnapping": false, "tamanhoGrade": 20.0 },
  "formas": [
    {
      "tipo": "Quadrado",
      "corPreenchimento": "#D3D3D3",
      "corBorda": "#000000",
      "espessuraBorda": 1.0,
      "pontos": [ {"x": 50.0, "y": 50.0}, {"x": 150.0, "y": 50.0},
                  {"x": 150.0, "y": 150.0}, {"x": 50.0, "y": 150.0} ]
    }
  ]
}
```

- Leitura e escrita ficam dentro de `try-catch`. Se o arquivo estiver corrompido, aparece
  um alerta explicando o problema e o desenho atual é mantido, sem fechar o programa.
- Ao fechar o editor (ou abrir outro arquivo) com alterações não salvas, o programa pergunta
  se deseja **Salvar**, **Não salvar** ou **Cancelar**. Enquanto houver alterações pendentes,
  o título da janela mostra `*`.
