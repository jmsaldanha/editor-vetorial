package org.example.Views;

import java.util.List;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.geometry.Orientation;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import org.example.Controller.EditorContext;
import org.example.Models.FormaGeometrica;

/**
 * Barra de status: coordenadas do cursor, total de formas e área/perímetro
 * em tempo real das formas selecionadas (ou da forma em construção).
 */
public class PainelStatusView extends HBox {

    private final EditorContext contexto;

    private final Label lblCoordenadas = new Label();
    private final Label lblTotal = new Label();
    private final Label lblSelecionadas = new Label();
    private final Label lblArea = new Label();
    private final Label lblPerimetro = new Label();
    private final Label lblMensagem = new Label();

    public PainelStatusView(EditorContext contexto) {
        super(10);
        this.contexto = contexto;
        setPadding(new Insets(4, 10, 4, 10));
        setAlignment(Pos.CENTER_LEFT);
        setStyle("-fx-background-color: #F0F0F0; -fx-border-color: #CCCCCC; -fx-border-width: 1 0 0 0;");

        lblCoordenadas.setMinWidth(130);
        lblMensagem.setStyle("-fx-text-fill: #444444;");
        HBox.setHgrow(lblMensagem, Priority.ALWAYS);

        getChildren().addAll(lblCoordenadas, separador(), lblTotal, separador(), lblSelecionadas,
                separador(), lblArea, separador(), lblPerimetro, separador(), lblMensagem);
        atualizar();
    }

    /** Recalcula as informações exibidas a partir do contexto do editor. */
    public void atualizar() {
        lblCoordenadas.setText(String.format("X: %.0f  Y: %.0f", contexto.getMouseX(), contexto.getMouseY()));
        lblTotal.setText("Formas: " + contexto.getFormas().size());

        List<FormaGeometrica> selecionados = contexto.getSelecionados();
        lblSelecionadas.setText("Selecionadas: " + selecionados.size());

        double area = 0;
        double perimetro = 0;
        if (!selecionados.isEmpty()) {
            for (FormaGeometrica f : selecionados) {
                area += f.calcularArea();
                perimetro += f.calcularPerimetro();
            }
        } else if (contexto.getFormaEmAndamento() != null) {
            area = contexto.getFormaEmAndamento().calcularArea();
            perimetro = contexto.getFormaEmAndamento().calcularPerimetro();
        }
        lblArea.setText(String.format("Área: %.2f", area));
        lblPerimetro.setText(String.format("Perímetro: %.2f", perimetro));

        String mensagem = contexto.getMensagem();
        if (contexto.isAlteracoesNaoSalvas()) {
            mensagem = "● " + mensagem;
        }
        lblMensagem.setText(mensagem);
    }

    private Separator separador() {
        return new Separator(Orientation.VERTICAL);
    }
}
