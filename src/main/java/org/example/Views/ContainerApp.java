package org.example.Views;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import org.example.Controller.EditorContext;
import org.example.Controller.FerramentaDesenhar;
import org.example.Controller.FerramentaEditarVertice;
import org.example.Controller.HistoricoController;
import org.example.Models.Circulo;
import org.example.Models.FormaGeometrica;
import org.example.Models.Hexagono;
import org.example.Models.Poligono;
import org.example.Models.Ponto;
import org.example.Models.Quadrado;

/**
 * Janela principal do editor: monta o layout (barra lateral, canvas e barra de
 * status), encaminha os eventos do mouse à ferramenta ativa, desenha as formas
 * e cuida da persistência em JSON.
 */
public class ContainerApp extends Application {

    private static final String VERSAO_FORMATO = "1.0";
    private static final Color COR_SELECAO = Color.web("#00A000");
    private static final Color COR_VERTICE = Color.web("#FF0000");

    private Canvas areaDeDesenho;
    private EditorContext contexto;
    private BarraLateralView barraLateral;
    private PainelStatusView painelStatus;

    private Stage janela;
    private File arquivoAtual;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        this.janela = stage;

        HistoricoController historico = new HistoricoController();
        contexto = new EditorContext(historico);

        areaDeDesenho = new Canvas(1000, 700);
        Pane painelCentral = new Pane(areaDeDesenho);
        painelCentral.setStyle("-fx-background-color: white;");
        painelCentral.setMinSize(0, 0);
        areaDeDesenho.widthProperty().bind(painelCentral.widthProperty());
        areaDeDesenho.heightProperty().bind(painelCentral.heightProperty());
        areaDeDesenho.widthProperty().addListener(o -> redesenharCanvas());
        areaDeDesenho.heightProperty().addListener(o -> redesenharCanvas());

        barraLateral = new BarraLateralView(contexto, this::salvarArquivo, this::carregarArquivo);
        painelStatus = new PainelStatusView(contexto);

        // Views observam o contexto (baixo acoplamento Controller -> View)
        contexto.setOuvinteRedesenho(this::redesenharCanvas);
        contexto.setOuvinteEstatisticas(() -> {
            painelStatus.atualizar();
            atualizarTitulo();
        });
        contexto.setOuvinteSelecao(barraLateral::atualizarCoresInterface);

        BorderPane raiz = new BorderPane();
        raiz.setLeft(barraLateral);
        raiz.setCenter(painelCentral);
        raiz.setBottom(painelStatus);

        Scene cena = new Scene(raiz, 1200, 800);
        stage.setScene(cena);
        stage.setMinWidth(700);
        stage.setMinHeight(500);

        inicializarEventos();
        atualizarTitulo();
        redesenharCanvas();
        stage.show();
    }

    // ================================================================ Eventos

    /** Liga os eventos do mouse/teclado à ferramenta ativa (State Pattern). */
    public void inicializarEventos() {
        areaDeDesenho.setOnMousePressed(e -> {
            areaDeDesenho.requestFocus();
            contexto.getFerramentaAtual().aoPressionarMouse(e, contexto);
        });
        areaDeDesenho.setOnMouseDragged(e -> {
            contexto.setPosicaoMouse(e.getX(), e.getY());
            contexto.getFerramentaAtual().aoArrastarMouse(e, contexto);
            painelStatus.atualizar();
        });
        areaDeDesenho.setOnMouseReleased(e -> contexto.getFerramentaAtual().aoSoltarMouse(e, contexto));
        areaDeDesenho.setOnMouseClicked(e -> contexto.getFerramentaAtual().aoClicar(e, contexto));
        areaDeDesenho.setOnMouseMoved(e -> {
            contexto.setPosicaoMouse(e.getX(), e.getY());
            if (contexto.getFormaEmAndamento() != null
                    && contexto.getFerramentaAtual() instanceof FerramentaDesenhar) {
                redesenharCanvas(); // linha-guia até o cursor
            }
            painelStatus.atualizar();
        });

        janela.getScene().addEventFilter(KeyEvent.KEY_PRESSED, this::tratarTeclado);
        janela.setOnCloseRequest(this::confirmarFechamento);
    }

    private void tratarTeclado(KeyEvent e) {
        if (e.isShortcutDown()) {
            if (e.getCode() == KeyCode.Z && e.isShiftDown() || e.getCode() == KeyCode.Y) {
                contexto.refazer();
                e.consume();
            } else if (e.getCode() == KeyCode.Z) {
                contexto.desfazer();
                e.consume();
            } else if (e.getCode() == KeyCode.S) {
                salvarArquivo();
                e.consume();
            } else if (e.getCode() == KeyCode.O) {
                carregarArquivo();
                e.consume();
            }
            return;
        }
        boolean desenhando = contexto.getFerramentaAtual() instanceof FerramentaDesenhar;
        switch (e.getCode()) {
            case DELETE, BACK_SPACE -> {
                contexto.excluirSelecionados();
                e.consume();
            }
            case ENTER -> {
                if (desenhando) {
                    ((FerramentaDesenhar) contexto.getFerramentaAtual()).finalizarPoligono(contexto);
                    e.consume();
                }
            }
            case ESCAPE -> {
                if (desenhando) {
                    ((FerramentaDesenhar) contexto.getFerramentaAtual()).cancelar(contexto);
                }
                contexto.limparSelecao();
                contexto.requestRedraw();
                contexto.requestStatsUpdate();
                e.consume();
            }
            default -> {
                // demais teclas são ignoradas
            }
        }
    }

    // ================================================================ Renderização

    private void redesenharCanvas() {
        GraphicsContext gc = areaDeDesenho.getGraphicsContext2D();
        double largura = areaDeDesenho.getWidth();
        double altura = areaDeDesenho.getHeight();

        gc.setLineDashes((double[]) null);
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, largura, altura);

        if (contexto.isGridSnappingHabilitado()) {
            gc.setStroke(Color.web("#E4E4E4"));
            gc.setLineWidth(1);
            for (double x = 0; x <= largura; x += EditorContext.TAMANHO_GRADE) {
                gc.strokeLine(x + 0.5, 0, x + 0.5, altura);
            }
            for (double y = 0; y <= altura; y += EditorContext.TAMANHO_GRADE) {
                gc.strokeLine(0, y + 0.5, largura, y + 0.5);
            }
        }

        for (FormaGeometrica forma : contexto.getFormas()) {
            desenharForma(forma, contexto.isSelecionada(forma));
        }

        FormaGeometrica emAndamento = contexto.getFormaEmAndamento();
        if (emAndamento != null) {
            desenharFormaEmAndamento(gc, emAndamento);
        }
    }

    private void desenharForma(FormaGeometrica forma, boolean selecionada) {
        GraphicsContext gc = areaDeDesenho.getGraphicsContext2D();
        int n = forma.getQuantidadePontos();
        if (n < 2) {
            return;
        }
        double[] xs = new double[n];
        double[] ys = new double[n];
        List<Ponto> pontos = forma.getPontos();
        for (int i = 0; i < n; i++) {
            xs[i] = pontos.get(i).getX();
            ys[i] = pontos.get(i).getY();
        }

        gc.setLineJoin(StrokeLineJoin.ROUND);
        gc.setLineCap(StrokeLineCap.ROUND);
        if (n >= 3) {
            gc.setFill(Color.web(forma.getCorPreenchimentoHex()));
            gc.fillPolygon(xs, ys, n);
        }
        gc.setStroke(selecionada ? COR_SELECAO : Color.web(forma.getCorBordaHex()));
        gc.setLineWidth(selecionada ? Math.max(2, forma.getEspessuraBorda()) : forma.getEspessuraBorda());
        if (n >= 3) {
            gc.strokePolygon(xs, ys, n);
        } else {
            gc.strokeLine(xs[0], ys[0], xs[1], ys[1]);
        }

        // vértices visíveis na ferramenta Editar Vértice
        if (selecionada && contexto.getFerramentaAtual() instanceof FerramentaEditarVertice) {
            gc.setFill(COR_VERTICE);
            for (int i = 0; i < n; i++) {
                gc.fillOval(xs[i] - 3, ys[i] - 3, 6, 6);
            }
        }
    }

    private void desenharFormaEmAndamento(GraphicsContext gc, FormaGeometrica forma) {
        int n = forma.getQuantidadePontos();
        if (n == 0) {
            return;
        }
        double[] xs = new double[n];
        double[] ys = new double[n];
        for (int i = 0; i < n; i++) {
            xs[i] = forma.getPontos().get(i).getX();
            ys[i] = forma.getPontos().get(i).getY();
        }

        if (contexto.getFerramentaAtual() instanceof FerramentaDesenhar) {
            // polígono sendo construído: linha aberta + vértices
            if (n >= 3) {
                gc.setFill(Color.web(forma.getCorPreenchimentoHex()).deriveColor(0, 1, 1, 0.5));
                gc.fillPolygon(xs, ys, n);
            }
            gc.setStroke(Color.web(forma.getCorBordaHex()));
            gc.setLineWidth(forma.getEspessuraBorda());
            gc.strokePolyline(xs, ys, n);
            gc.setLineDashes(5, 5);
            gc.setLineWidth(1);
            gc.strokeLine(xs[n - 1], ys[n - 1], contexto.getMouseX(), contexto.getMouseY());
            gc.setLineDashes((double[]) null);
            gc.setFill(COR_VERTICE);
            for (int i = 0; i < n; i++) {
                gc.fillOval(xs[i] - 3, ys[i] - 3, 6, 6);
            }
        } else {
            // prévia de inserção ou caixa de seleção (tracejada)
            if (n >= 3) {
                gc.setFill(Color.web(forma.getCorPreenchimentoHex()));
                gc.fillPolygon(xs, ys, n);
            }
            gc.setStroke(Color.web(forma.getCorBordaHex()));
            gc.setLineWidth(Math.max(1, forma.getEspessuraBorda()));
            gc.setLineDashes(6, 4);
            gc.strokePolygon(xs, ys, n);
            gc.setLineDashes((double[]) null);
        }
    }

    // ================================================================ Persistência (JSON)

    private void salvarArquivo() {
        FileChooser seletor = criarSeletorArquivo("Salvar desenho");
        if (arquivoAtual != null) {
            seletor.setInitialDirectory(arquivoAtual.getParentFile());
            seletor.setInitialFileName(arquivoAtual.getName());
        } else {
            seletor.setInitialFileName("desenho.json");
        }
        File arquivo = seletor.showSaveDialog(janela);
        if (arquivo == null) {
            return;
        }
        if (!arquivo.getName().toLowerCase().endsWith(".json")) {
            arquivo = new File(arquivo.getParentFile(), arquivo.getName() + ".json");
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (Writer escritor = Files.newBufferedWriter(arquivo.toPath(), StandardCharsets.UTF_8)) {
            gson.toJson(paraJson(), escritor);
            arquivoAtual = arquivo;
            contexto.setAlteracoesNaoSalvas(false);
            contexto.setMensagem("Desenho salvo em " + arquivo.getName());
        } catch (IOException | RuntimeException ex) {
            mostrarErro("Erro ao salvar",
                    "Não foi possível salvar o arquivo \"" + arquivo.getName() + "\".",
                    ex.getMessage());
        }
        contexto.requestStatsUpdate();
    }

    private void carregarArquivo() {
        if (!confirmarDescarteDeAlteracoes()) {
            return;
        }
        FileChooser seletor = criarSeletorArquivo("Carregar desenho");
        if (arquivoAtual != null) {
            seletor.setInitialDirectory(arquivoAtual.getParentFile());
        }
        File arquivo = seletor.showOpenDialog(janela);
        if (arquivo == null) {
            return;
        }

        try (Reader leitor = Files.newBufferedReader(arquivo.toPath(), StandardCharsets.UTF_8)) {
            JsonElement raiz = JsonParser.parseReader(leitor);
            if (raiz == null || !raiz.isJsonObject()) {
                throw new JsonParseException("O conteúdo não é um objeto JSON.");
            }
            JsonObject objeto = raiz.getAsJsonObject();
            List<FormaGeometrica> formasCarregadas = formasDoJson(objeto);
            boolean grade = lerCanvasGrade(objeto);

            // só substitui o desenho atual se o arquivo inteiro for válido
            contexto.registrarEstado();
            contexto.substituirFormas(formasCarregadas);
            contexto.setGridSnappingHabilitado(grade);
            barraLateral.atualizarCoresInterface();
            arquivoAtual = arquivo;
            contexto.setAlteracoesNaoSalvas(false);
            contexto.setMensagem(formasCarregadas.size() + " forma(s) carregada(s) de " + arquivo.getName());
        } catch (IOException ex) {
            mostrarErro("Erro de leitura",
                    "Não foi possível ler o arquivo \"" + arquivo.getName() + "\".",
                    ex.getMessage());
        } catch (RuntimeException ex) {
            // JsonParseException, IllegalStateException, IllegalArgumentException, NumberFormatException...
            mostrarErro("Arquivo corrompido",
                    "O arquivo \"" + arquivo.getName() + "\" está corrompido ou não é um desenho válido.\n"
                            + "O desenho atual foi mantido.",
                    ex.getMessage());
        }
        contexto.requestRedraw();
        contexto.requestStatsUpdate();
    }

    private JsonObject paraJson() {
        JsonObject raiz = new JsonObject();
        raiz.addProperty("versao", VERSAO_FORMATO);

        JsonObject canvas = new JsonObject();
        canvas.addProperty("largura", areaDeDesenho.getWidth());
        canvas.addProperty("altura", areaDeDesenho.getHeight());
        canvas.addProperty("corFundo", "#FFFFFF");
        canvas.addProperty("gridSnapping", contexto.isGridSnappingHabilitado());
        canvas.addProperty("tamanhoGrade", EditorContext.TAMANHO_GRADE);
        raiz.add("canvas", canvas);

        JsonArray formas = new JsonArray();
        for (FormaGeometrica forma : contexto.getFormas()) {
            JsonObject f = new JsonObject();
            f.addProperty("tipo", forma.getClass().getSimpleName());
            f.addProperty("corPreenchimento", forma.getCorPreenchimentoHex());
            f.addProperty("corBorda", forma.getCorBordaHex());
            f.addProperty("espessuraBorda", forma.getEspessuraBorda());
            if (forma.getGroupId() != null) {
                f.addProperty("groupId", forma.getGroupId());
            }
            JsonArray pontos = new JsonArray();
            for (Ponto p : forma.getPontos()) {
                JsonObject ponto = new JsonObject();
                ponto.addProperty("x", p.getX());
                ponto.addProperty("y", p.getY());
                pontos.add(ponto);
            }
            f.add("pontos", pontos);
            formas.add(f);
        }
        raiz.add("formas", formas);
        return raiz;
    }

    private boolean lerCanvasGrade(JsonObject raiz) {
        JsonObject canvas = raiz.has("canvas") ? raiz.getAsJsonObject("canvas") : null;
        if (canvas != null && canvas.has("gridSnapping")) {
            return canvas.get("gridSnapping").getAsBoolean();
        }
        return false;
    }

    private List<FormaGeometrica> formasDoJson(JsonObject raiz) {
        JsonArray array = obrigatorio(raiz, "formas").getAsJsonArray();
        List<FormaGeometrica> formas = new ArrayList<>();
        int indice = 0;
        for (JsonElement elemento : array) {
            indice++;
            try {
                formas.add(formaDoJson(elemento.getAsJsonObject()));
            } catch (RuntimeException ex) {
                throw new JsonParseException("Forma nº " + indice + ": " + ex.getMessage(), ex);
            }
        }
        return formas;
    }

    private FormaGeometrica formaDoJson(JsonObject json) {
        String tipo = obrigatorio(json, "tipo").getAsString();
        JsonArray arrayPontos = obrigatorio(json, "pontos").getAsJsonArray();
        List<Ponto> pontos = new ArrayList<>();
        for (JsonElement e : arrayPontos) {
            JsonObject p = e.getAsJsonObject();
            double x = obrigatorio(p, "x").getAsDouble();
            double y = obrigatorio(p, "y").getAsDouble();
            if (!Double.isFinite(x) || !Double.isFinite(y)) {
                throw new IllegalArgumentException("coordenada inválida.");
            }
            pontos.add(new Ponto(x, y));
        }

        FormaGeometrica forma;
        switch (tipo) {
            case "Poligono" -> {
                if (pontos.size() < Poligono.MINIMO_PONTOS) {
                    throw new IllegalArgumentException("um polígono precisa de no mínimo "
                            + Poligono.MINIMO_PONTOS + " pontos.");
                }
                forma = new Poligono();
                for (Ponto p : pontos) {
                    forma.adicionarPonto(p);
                }
            }
            case "Circulo" -> forma = new Circulo(new Ponto(0, 0), 1);
            case "Quadrado" -> forma = new Quadrado(new Ponto(0, 0), 1);
            case "Hexagono" -> forma = new Hexagono(new Ponto(0, 0), 1);
            default -> throw new IllegalArgumentException("tipo de forma desconhecido: " + tipo);
        }

        // formas regulares: os vértices gerados recebem as coordenadas salvas
        if (!"Poligono".equals(tipo)) {
            if (pontos.size() != forma.getQuantidadePontos()) {
                throw new IllegalArgumentException("quantidade de pontos inválida para " + tipo + ".");
            }
            for (int i = 0; i < pontos.size(); i++) {
                forma.getPontos().get(i).setX(pontos.get(i).getX());
                forma.getPontos().get(i).setY(pontos.get(i).getY());
            }
        }

        forma.setCorPreenchimentoHex(obrigatorio(json, "corPreenchimento").getAsString());
        forma.setCorBordaHex(obrigatorio(json, "corBorda").getAsString());
        forma.setEspessuraBorda(obrigatorio(json, "espessuraBorda").getAsDouble());
        if (json.has("groupId") && !json.get("groupId").isJsonNull()) {
            forma.setGroupId(json.get("groupId").getAsString());
        }
        return forma;
    }

    private JsonElement obrigatorio(JsonObject objeto, String campo) {
        JsonElement valor = objeto.get(campo);
        if (valor == null || valor.isJsonNull()) {
            throw new JsonParseException("campo obrigatório ausente: \"" + campo + "\".");
        }
        return valor;
    }

    private FileChooser criarSeletorArquivo(String titulo) {
        FileChooser seletor = new FileChooser();
        seletor.setTitle(titulo);
        seletor.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Desenho vetorial (*.json)", "*.json"),
                new FileChooser.ExtensionFilter("Todos os arquivos", "*.*"));
        return seletor;
    }

    // ================================================================ Diálogos

    private void confirmarFechamento(WindowEvent evento) {
        if (!confirmarDescarteDeAlteracoes()) {
            evento.consume();
        }
    }

    /**
     * Alerta sobre alterações não salvas.
     * @return true se pode continuar (salvou ou descartou), false se cancelou.
     */
    private boolean confirmarDescarteDeAlteracoes() {
        if (!contexto.isAlteracoesNaoSalvas()) {
            return true;
        }
        ButtonType salvar = new ButtonType("Salvar", ButtonBar.ButtonData.YES);
        ButtonType naoSalvar = new ButtonType("Não salvar", ButtonBar.ButtonData.NO);
        ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert alerta = new Alert(Alert.AlertType.WARNING, "", salvar, naoSalvar, cancelar);
        alerta.initOwner(janela);
        alerta.setTitle("Alterações não salvas");
        alerta.setHeaderText("O desenho possui alterações que não foram salvas.");
        alerta.setContentText("Deseja salvar antes de continuar?");

        Optional<ButtonType> resposta = alerta.showAndWait();
        if (resposta.isEmpty() || resposta.get() == cancelar) {
            return false;
        }
        if (resposta.get() == salvar) {
            salvarArquivo();
            return !contexto.isAlteracoesNaoSalvas(); // cancelou ou falhou ao salvar
        }
        return true;
    }

    private void mostrarErro(String titulo, String cabecalho, String detalhe) {
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.initOwner(janela);
        alerta.setTitle(titulo);
        alerta.setHeaderText(cabecalho);
        alerta.setContentText("Detalhe: " + (detalhe == null ? "erro desconhecido" : detalhe));
        alerta.showAndWait();
    }

    private void atualizarTitulo() {
        String nome = arquivoAtual == null ? "Sem título" : arquivoAtual.getName();
        String marcador = contexto.isAlteracoesNaoSalvas() ? "* " : "";
        janela.setTitle(marcador + nome + " - Editor de Desenho Vetorial");
    }
}
