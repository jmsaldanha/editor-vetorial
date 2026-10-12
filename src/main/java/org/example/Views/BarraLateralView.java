package org.example.Views;

import java.util.List;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import org.example.Controller.EditorContext;
import org.example.Controller.Ferramenta;
import org.example.Controller.FerramentaDesenhar;
import org.example.Controller.FerramentaEditarVertice;
import org.example.Controller.FerramentaInserirForma;
import org.example.Controller.FerramentaSelecionar;
import org.example.Controller.FerramentaTransformar;
import org.example.Controller.ModoFerramenta;
import org.example.Models.FormaGeometrica;

/**
 * Barra lateral: estilos, ferramentas (toggle), formas, camadas/grupo e sistema.
 * Conversa apenas com o {@link EditorContext}; salvar/carregar são recebidos
 * como ações da janela principal.
 */
public class BarraLateralView extends ScrollPane {

    private static final double LARGURA = 190;

    private final EditorContext contexto;
    private final Runnable acaoSalvar;
    private final Runnable acaoCarregar;

    private final VBox conteudo = new VBox(4);
    private final ToggleGroup grupoFerramentas = new ToggleGroup();

    private ColorPicker seletorPreenchimento;
    private ColorPicker seletorBorda;
    private Slider sliderEspessura;
    private CheckBox checkGrade;

    /** Evita que a atualização da interface dispare alterações nas formas. */
    private boolean atualizandoInterface;

    public BarraLateralView(EditorContext contexto, Runnable acaoSalvar, Runnable acaoCarregar) {
        this.contexto = contexto;
        this.acaoSalvar = acaoSalvar;
        this.acaoCarregar = acaoCarregar;

        conteudo.setPadding(new Insets(8));
        conteudo.setAlignment(Pos.TOP_CENTER);
        conteudo.setPrefWidth(LARGURA);

        construirSecaoEstilos();
        construirSecaoFerramentas();
        construirSecaoCamadas();

        setContent(conteudo);
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        setPrefWidth(LARGURA + 16);
        setMinWidth(LARGURA + 16);
        setStyle("-fx-background: #F0F0F0; -fx-border-color: #CCCCCC; -fx-border-width: 0 1 0 0;");
    }

    /**
     * Sincroniza os controles de estilo com a forma selecionada e o estado da grade.
     */
    public void atualizarCoresInterface() {
        atualizandoInterface = true;
        try {
            List<FormaGeometrica> selecionados = contexto.getSelecionados();
            if (!selecionados.isEmpty()) {
                FormaGeometrica f = selecionados.get(0);
                seletorPreenchimento.setValue(Color.web(f.getCorPreenchimentoHex()));
                seletorBorda.setValue(Color.web(f.getCorBordaHex()));
                sliderEspessura.setValue(f.getEspessuraBorda());
            } else {
                seletorPreenchimento.setValue(Color.web(contexto.getCorPreenchimentoAtual()));
                seletorBorda.setValue(Color.web(contexto.getCorBordaAtual()));
                sliderEspessura.setValue(contexto.getEspessuraBordaAtual());
            }
            checkGrade.setSelected(contexto.isGridSnappingHabilitado());
        } finally {
            atualizandoInterface = false;
        }
    }

    // ================================================================ Seções

    private void construirSecaoEstilos() {
        conteudo.getChildren().add(titulo("--- ESTILOS ---"));

        seletorPreenchimento = new ColorPicker(Color.web(contexto.getCorPreenchimentoAtual()));
        seletorPreenchimento.setMaxWidth(Double.MAX_VALUE);
        seletorPreenchimento.setOnAction(e -> {
            if (atualizandoInterface) {
                return;
            }
            String hex = paraHex(seletorPreenchimento.getValue());
            contexto.setCorPreenchimentoAtual(hex);
            aplicarNaSelecao(f -> f.setCorPreenchimentoHex(hex), true);
        });

        seletorBorda = new ColorPicker(Color.web(contexto.getCorBordaAtual()));
        seletorBorda.setMaxWidth(Double.MAX_VALUE);
        seletorBorda.setOnAction(e -> {
            if (atualizandoInterface) {
                return;
            }
            String hex = paraHex(seletorBorda.getValue());
            contexto.setCorBordaAtual(hex);
            aplicarNaSelecao(f -> f.setCorBordaHex(hex), true);
        });

        sliderEspessura = new Slider(1, 10, contexto.getEspessuraBordaAtual());
        sliderEspessura.setShowTickMarks(true);
        sliderEspessura.setShowTickLabels(true);
        sliderEspessura.setMajorTickUnit(9);
        sliderEspessura.setMinorTickCount(8);
        sliderEspessura.setSnapToTicks(true);
        // um único estado no histórico por arrasto do slider
        sliderEspessura.valueChangingProperty().addListener((obs, antes, mudando) -> {
            if (mudando && !atualizandoInterface && !contexto.getSelecionados().isEmpty()) {
                contexto.registrarEstado();
            }
        });
        sliderEspessura.valueProperty().addListener((obs, antes, valor) -> {
            if (atualizandoInterface) {
                return;
            }
            double espessura = valor.doubleValue();
            contexto.setEspessuraBordaAtual(espessura);
            aplicarNaSelecao(f -> f.setEspessuraBorda(espessura), !sliderEspessura.isValueChanging());
        });

        checkGrade = new CheckBox("Grade Magnética (Snapping)");
        checkGrade.setWrapText(true);
        checkGrade.setOnAction(e -> contexto.setGridSnappingHabilitado(checkGrade.isSelected()));

        conteudo.getChildren().addAll(
                rotulo("Cor Preenchimento:"), seletorPreenchimento,
                rotulo("Cor Borda:"), seletorBorda,
                rotulo("Espessura da Borda:"), sliderEspessura,
                checkGrade);
    }

    private void construirSecaoFerramentas() {
        conteudo.getChildren().add(titulo("--- FERRAMENTAS ---"));

        ToggleButton selecionar = botaoFerramenta("Selecionar", new FerramentaSelecionar(),
                "Clique para selecionar (SHIFT soma), arraste para mover ou desenhe uma caixa de seleção.");
        conteudo.getChildren().addAll(
                botaoFerramenta("✏ Linha Livre", new FerramentaDesenhar(),
                        "Clique para adicionar vértices; duplo clique, botão direito ou ENTER para finalizar."),
                selecionar,
                botaoFerramenta("Escalar", new FerramentaTransformar(ModoFerramenta.ESCALAR),
                        "Arraste afastando ou aproximando do centro da forma."),
                botaoFerramenta("Rotacionar", new FerramentaTransformar(ModoFerramenta.ROTACIONAR),
                        "Arraste em volta do centro da forma."),
                botaoFerramenta("Cisalhar", new FerramentaTransformar(ModoFerramenta.CISALHAR),
                        "Arraste na horizontal/vertical para cisalhar."),
                botaoFerramenta("Excluir", new FerramentaTransformar(ModoFerramenta.EXCLUIR),
                        "Clique em uma forma para excluí-la."),
                botaoFerramenta("Editar Vértice", new FerramentaEditarVertice(),
                        "Clique na forma e arraste um de seus vértices."),
                botao("Excluir Selecionados", contexto::excluirSelecionados));

        conteudo.getChildren().add(titulo("--- FORMAS ---"));
        conteudo.getChildren().addAll(
                botaoFerramenta("Inserir Quadrado",
                        new FerramentaInserirForma(FerramentaInserirForma.TipoForma.QUADRADO),
                        "Pressione no centro e arraste para definir o tamanho."),
                botaoFerramenta("Inserir Círculo",
                        new FerramentaInserirForma(FerramentaInserirForma.TipoForma.CIRCULO),
                        "Pressione no centro e arraste para definir o raio."),
                botaoFerramenta("⬡ Inserir Hexágono",
                        new FerramentaInserirForma(FerramentaInserirForma.TipoForma.HEXAGONO),
                        "Pressione no centro e arraste para definir o raio."));

        // sempre há uma ferramenta ativa
        grupoFerramentas.selectedToggleProperty().addListener((obs, antes, depois) -> {
            if (depois == null && antes != null) {
                grupoFerramentas.selectToggle(antes);
            }
        });
        selecionar.setSelected(true);
    }

    private void construirSecaoCamadas() {
        conteudo.getChildren().add(titulo("--- CAMADAS/GRUPO ---"));
        conteudo.getChildren().addAll(
                botao("⬆ Trazer p/ Frente", contexto::trazerParaFrente),
                botao("⬇ Enviar p/ Trás", contexto::enviarParaTras),
                botao("Agrupar", contexto::agruparSelecionados),
                botao("Desagrupar", contexto::desagruparSelecionados));

        conteudo.getChildren().add(titulo("--- SISTEMA ---"));
        conteudo.getChildren().addAll(
                botao("↶ Desfazer", contexto::desfazer),
                botao("↷ Refazer", contexto::refazer),
                botao("Salvar", acaoSalvar),
                botao("Carregar", acaoCarregar));
    }

    // ================================================================ Auxiliares

    private interface AlteracaoForma {
        void aplicar(FormaGeometrica forma);
    }

    private void aplicarNaSelecao(AlteracaoForma alteracao, boolean registrarHistorico) {
        List<FormaGeometrica> selecionados = contexto.getSelecionados();
        if (selecionados.isEmpty()) {
            return;
        }
        if (registrarHistorico) {
            contexto.registrarEstado();
        }
        for (FormaGeometrica f : selecionados) {
            alteracao.aplicar(f);
        }
        contexto.requestRedraw();
        contexto.requestStatsUpdate();
    }

    private ToggleButton botaoFerramenta(String texto, Ferramenta ferramenta, String dica) {
        ToggleButton botao = new ToggleButton(texto);
        botao.setToggleGroup(grupoFerramentas);
        botao.setMaxWidth(Double.MAX_VALUE);
        botao.setTooltip(new Tooltip(dica));
        botao.selectedProperty().addListener((obs, antes, selecionado) -> {
            if (selecionado) {
                contexto.setFerramentaAtual(ferramenta); // troca de estado (State Pattern)
                contexto.setMensagem(dica);
                contexto.requestStatsUpdate();
            }
        });
        return botao;
    }

    private Button botao(String texto, Runnable acao) {
        Button botao = new Button(texto);
        botao.setMaxWidth(Double.MAX_VALUE);
        botao.setOnAction(e -> acao.run());
        return botao;
    }

    private Label titulo(String texto) {
        Label label = new Label(texto);
        label.setStyle("-fx-font-size: 11px; -fx-text-fill: #555555;");
        VBox.setMargin(label, new Insets(8, 0, 2, 0));
        return label;
    }

    private Label rotulo(String texto) {
        Label label = new Label(texto);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setStyle("-fx-font-size: 11px;");
        return label;
    }

    /** Converte uma cor JavaFX para #RRGGBB (ou #RRGGBBAA quando há transparência). */
    static String paraHex(Color cor) {
        int r = (int) Math.round(cor.getRed() * 255);
        int g = (int) Math.round(cor.getGreen() * 255);
        int b = (int) Math.round(cor.getBlue() * 255);
        int a = (int) Math.round(cor.getOpacity() * 255);
        if (a < 255) {
            return String.format("#%02X%02X%02X%02X", r, g, b, a);
        }
        return String.format("#%02X%02X%02X", r, g, b);
    }
}
