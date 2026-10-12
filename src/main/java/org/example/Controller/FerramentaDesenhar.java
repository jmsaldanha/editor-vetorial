package org.example.Controller;

import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import org.example.Models.FormaGeometrica;
import org.example.Models.Poligono;
import org.example.Models.Ponto;

/**
 * Estado "Linha Livre": desenha polígonos por cliques.
 * <ul>
 *   <li>Clique esquerdo: adiciona um vértice;</li>
 *   <li>Duplo clique, clique direito ou ENTER: finaliza o polígono;</li>
 *   <li>ESC: cancela o polígono em andamento.</li>
 * </ul>
 * Polígonos com menos de 3 pontos são descartados (regra de negócio).
 */
public class FerramentaDesenhar implements Ferramenta {

    @Override
    public void aoPressionarMouse(MouseEvent e, EditorContext contexto) {
        // O desenho é feito por cliques.
    }

    @Override
    public void aoArrastarMouse(MouseEvent e, EditorContext contexto) {
        // O desenho é feito por cliques.
    }

    @Override
    public void aoSoltarMouse(MouseEvent e, EditorContext contexto) {
        // O desenho é feito por cliques.
    }

    @Override
    public void aoClicar(MouseEvent e, EditorContext contexto) {
        if (!e.isStillSincePress()) {
            return;
        }
        if (e.getButton() == MouseButton.SECONDARY || e.getClickCount() >= 2) {
            finalizarPoligono(contexto);
            return;
        }
        if (e.getButton() != MouseButton.PRIMARY) {
            return;
        }

        double x = contexto.aplicarSnap(e.getX());
        double y = contexto.aplicarSnap(e.getY());

        FormaGeometrica emAndamento = contexto.getFormaEmAndamento();
        if (emAndamento == null) {
            emAndamento = new Poligono();
            contexto.aplicarEstiloAtual(emAndamento);
            contexto.setFormaEmAndamento(emAndamento);
        }

        // evita vértices repetidos no mesmo lugar (comum com a grade ativa)
        int n = emAndamento.getQuantidadePontos();
        if (n > 0) {
            Ponto ultimo = emAndamento.getPontos().get(n - 1);
            if (ultimo.getX() == x && ultimo.getY() == y) {
                return;
            }
        }

        emAndamento.adicionarPonto(new Ponto(x, y));
        contexto.setMensagem("Vértices: " + emAndamento.getQuantidadePontos()
                + "  (duplo clique, botão direito ou ENTER para finalizar)");
        contexto.requestRedraw();
        contexto.requestStatsUpdate();
    }

    /** Conclui o polígono em andamento, validando o mínimo de 3 pontos. */
    public void finalizarPoligono(EditorContext contexto) {
        FormaGeometrica forma = contexto.getFormaEmAndamento();
        contexto.setFormaEmAndamento(null);
        if (forma == null) {
            return;
        }
        if (forma.getQuantidadePontos() < Poligono.MINIMO_PONTOS) {
            contexto.setMensagem("Polígono descartado: são necessários no mínimo "
                    + Poligono.MINIMO_PONTOS + " pontos.");
        } else {
            contexto.registrarEstado();
            contexto.adicionarForma(forma);
            contexto.setMensagem("Polígono criado com " + forma.getQuantidadePontos() + " vértices.");
        }
        contexto.requestRedraw();
        contexto.requestStatsUpdate();
    }

    /** Descarta o polígono em andamento. */
    public void cancelar(EditorContext contexto) {
        if (contexto.getFormaEmAndamento() != null) {
            contexto.setFormaEmAndamento(null);
            contexto.setMensagem("Desenho cancelado.");
            contexto.requestRedraw();
            contexto.requestStatsUpdate();
        }
    }
}
