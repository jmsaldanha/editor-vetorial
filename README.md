# Editor de Desenho Vetorial Interativo (JavaFX)

Avaliação Prática **N3** – Programação Orientada a Objetos 1
Universidade de Rio Verde (UniRV) – Engenharia de Software – Semestre 2026/1
Docente: Prof. Me. Sergio Souza Novak

Software desktop para criação, edição e persistência de formas geométricas 2D
sobre uma tela interativa, aplicando padrões GoF (State, Prototype, Memento/Command,
Composite) e separação em camadas Models / Controller / Views.

O diagrama de classes UML está em [`docs/diagrama_classes.pdf`](docs/diagrama_classes.pdf).

---

## Requisitos técnicos

| ID   | Requisito        | Atendimento                                              |
|------|------------------|----------------------------------------------------------|
| RT01 | Linguagem        | Java 17 (`maven.compiler.release = 17`)                  |
| RT02 | Interface gráfica| JavaFX 21 (código programático, sem FXML)                |
| RT03 | Gerenciador      | Apache Maven (`pom.xml`)                                 |
| RT04 | Persistência     | JSON (biblioteca Gson)                                   |
| RT05 | Patterns         | State, Prototype, Memento (+ Command e Composite)        |
| RT06 | Arquitetura      | Pacotes `Models`, `Controller` e `Views`                 |

## Como executar

Pré-requisitos: **JDK 17+** e **Maven 3.8+** (as dependências do JavaFX são baixadas pelo Maven).

```bash
mvn clean javafx:run
```

Para apenas compilar: `mvn clean compile`.

---

## Estrutura de pacotes

```
src/main/java/org/example
├── Models
│   ├── FormaGeometrica.java      «interface» contrato das formas (Listing 1)
│   ├── FormaBase.java            «abstract» implementações base (área, perímetro, transformações)
│   ├── Ponto.java                vértice (x, y)
│   ├── Poligono.java             polígono livre (mínimo 3 pontos)
│   ├── Quadrado.java             extends Poligono – vértices calculados automaticamente
│   ├── Circulo.java              extends FormaBase – raio > 0
│   └── Hexagono.java             extends FormaBase – vértices calculados automaticamente
├── Controller
│   ├── EditorContext.java        Contexto do State + seleção/agrupamento (Composite)
│   ├── HistoricoController.java  Desfazer/Refazer (Memento/Command)
│   ├── Ferramenta.java           «interface» Estado
│   ├── FerramentaDesenhar.java   "Linha Livre": polígonos por cliques
│   ├── FerramentaSelecionar.java seleção, caixa de seleção e translação
│   ├── FerramentaTransformar.java escalar / rotacionar / cisalhar / excluir
│   ├── ModoFerramenta.java       modos da FerramentaTransformar
│   ├── FerramentaInserirForma.java quadrado, círculo e hexágono por arrasto
│   └── FerramentaEditarVertice.java arrasto de vértices
└── Views
    ├── ContainerApp.java         janela principal, canvas, renderização e persistência
    ├── BarraLateralView.java     estilos, ferramentas, formas, camadas e sistema
    └── PainelStatusView.java     coordenadas, total de formas, área e perímetro
```

## Padrões de projeto

- **State** – `EditorContext` guarda a `ferramentaAtual` (interface `Ferramenta`). O canvas
  apenas repassa os eventos `aoPressionarMouse`, `aoArrastarMouse`, `aoSoltarMouse` e
  `aoClicar` ao estado ativo; trocar de ferramenta na barra lateral troca o estado,
  sem condicionais na View.
- **Memento / Command** – antes de cada ação que altera o desenho, `registrarEstado`
  empilha uma cópia profunda da lista de formas no `undoStack`. `desfazer` e `refazer`
  movimentam os estados entre `undoStack` e `redoStack` (limite de 100 estados).
- **Prototype** – `FormaGeometrica.clonar()` gera cópias profundas (pontos, estilo e
  groupId), sem compartilhar referências. É usado pelo histórico.
- **Composite** – formas com o mesmo `groupId` são selecionadas juntas e translação,
  escala, rotação e cisalhamento são aplicados em torno do centro do grupo, como se
  fossem uma única forma.

## Regras de negócio implementadas

- Polígono precisa de **no mínimo 3 pontos** (desenhos com menos são descartados e
  arquivos com menos são rejeitados como corrompidos).
- Círculo, quadrado e hexágono exigem **raio estritamente positivo** (`IllegalArgumentException`).
- Quadrado e hexágono **calculam seus vértices automaticamente**.
- **Área** pela fórmula de Gauss; **perímetro** pela soma das distâncias euclidianas.
- **Translação** soma `dx`/`dy`; **escala** multiplica a distância ao centroide;
  **rotação** em radianos em torno do centroide; **cisalhamento** em torno do centroide.
- Cores validadas no formato `#RRGGBB`; espessura de borda positiva.
- O círculo é representado por 72 vértices sobre a circunferência, o que permite
  aplicar todas as transformações (inclusive cisalhamento e edição de vértices).

## Uso

| Ferramenta / ação     | Como usar                                                                 |
|-----------------------|---------------------------------------------------------------------------|
| Linha Livre           | Clique para adicionar vértices; **duplo clique**, **botão direito** ou **ENTER** finaliza; **ESC** cancela |
| Selecionar            | Clique seleciona (SHIFT soma/remove); arraste a forma para mover; arraste no vazio para caixa de seleção |
| Escalar               | Arraste afastando/aproximando do centro da forma                          |
| Rotacionar            | Arraste em volta do centro da forma                                       |
| Cisalhar              | Arraste na horizontal (shx) e/ou vertical (shy)                           |
| Excluir               | Clique na forma                                                           |
| Editar Vértice        | Clique na forma (vértices em vermelho) e arraste um vértice               |
| Inserir Quadrado/Círculo/Hexágono | Pressione no centro e arraste para definir o raio (clique simples = raio 50) |
| Grade Magnética       | Arredonda as coordenadas para a grade de 20 px                            |
| Camadas/Grupo         | Trazer p/ frente, enviar p/ trás, agrupar e desagrupar a seleção          |
| Atalhos               | Ctrl+Z desfazer · Ctrl+Y / Ctrl+Shift+Z refazer · Ctrl+S salvar · Ctrl+O carregar · Delete exclui seleção |

A barra de status mostra as coordenadas do cursor, o total de formas e a área/perímetro
das formas selecionadas (ou da forma em construção) em tempo real.

## Persistência e tratamento de erros

Os desenhos são salvos em **JSON** com as configurações do canvas e a lista de formas
(tipo, estilo, coordenadas e groupId):

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
      "groupId": "5f1c...",
      "pontos": [ {"x": 50.0, "y": 50.0}, {"x": 150.0, "y": 50.0},
                  {"x": 150.0, "y": 150.0}, {"x": 50.0, "y": 150.0} ]
    }
  ]
}
```

- Leitura e escrita protegidas por `try-catch`. Arquivos corrompidos (JSON inválido,
  campos ausentes, tipo desconhecido, cor inválida, polígono com menos de 3 pontos...)
  exibem um **alerta gráfico explicativo** e o desenho atual é mantido, sem interromper
  a aplicação.
- Ao **fechar** o editor (ou carregar outro arquivo) com alterações não salvas, o
  usuário é alertado e pode **Salvar**, **Não salvar** ou **Cancelar**. A barra de título
  exibe `*` enquanto houver alterações pendentes.
