package org.example.Controller;

import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import org.example.Models.FormaGeometrica;
import org.example.Models.Ponto;

/**
 * Estado "Editar Vértice": clique numa forma para exibir seus vértices
 * e arraste um vértice para alterar a geometria da forma.
 */
public class FerramentaEditarVertice implements Ferramenta {

    private static final double TOLERANCIA = 8.0;

    private Ponto verticeSelecionado;
    private boolean estadoRegistrado;

    @Override
    public void aoPressionarMouse(MouseEvent e, EditorContext contexto) {
        if (e.getButton() != MouseButton.PRIMARY) {
            return;
        }
        estadoRegistrado = false;
        verticeSelecionado = null;
        Ponto clique = new Ponto(e.getX(), e.getY());

        // 1) vértices das formas já selecionadas têm prioridade
        verticeSelecionado = procurarVertice(contexto, clique, true);
        // 2) depois, vértices de qualquer forma (da mais ao topo para a do fundo)
        if (verticeSelecionado == null) {
            verticeSelecionado = procurarVertice(contexto, clique, false);
        }
        // 3) sem vértice próximo: clique dentro da forma apenas a seleciona
        if (verticeSelecionado == null) {
            FormaGeometrica alvo = contexto.formaNoPonto(e.getX(), e.getY());
            if (alvo != null) {
                contexto.selecionar(alvo, false);
            } else {
                contexto.limparSelecao();
            }
        }
        contexto.requestRedraw();
        contexto.requestStatsUpdate();
    }

    @Override
    public void aoArrastarMouse(MouseEvent e, EditorContext contexto) {
        if (verticeSelecionado == null || !e.isPrimaryButtonDown()) {
            return;
        }
        if (!estadoRegistrado) {
            contexto.registrarEstado();
            estadoRegistrado = true;
        }
        verticeSelecionado.setX(contexto.aplicarSnap(e.getX()));
        verticeSelecionado.setY(contexto.aplicarSnap(e.getY()));
        contexto.requestRedraw();
        contexto.requestStatsUpdate();
    }

    @Override
    public void aoSoltarMouse(MouseEvent e, EditorContext contexto) {
        verticeSelecionado = null;
        estadoRegistrado = false;
    }

    @Override
    public void aoClicar(MouseEvent e, EditorContext contexto) {
        // A edição é feita por arrasto.
    }

    private Ponto procurarVertice(EditorContext contexto, Ponto clique, boolean somenteSelecionadas) {
        var formas = contexto.getFormas();
        for (int i = formas.size() - 1; i >= 0; i--) {
            FormaGeometrica f = formas.get(i);
            if (somenteSelecionadas && !contexto.isSelecionada(f)) {
                continue;
            }
            for (Ponto p : f.getPontos()) {
                if (p.distancia(clique) <= TOLERANCIA) {
                    if (!contexto.isSelecionada(f)) {
                        contexto.selecionar(f, false);
                    }
                    return p;
                }
            }
        }
        return null;
    }
}
