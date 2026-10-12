package org.example.Controller;

import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import org.example.Models.FormaGeometrica;
import org.example.Models.Poligono;
import org.example.Models.Ponto;

/**
 * Estado "Selecionar":
 * <ul>
 *   <li>Clique numa forma: seleciona (SHIFT adiciona/remove da seleção);</li>
 *   <li>Arrastar uma forma selecionada: translada a seleção;</li>
 *   <li>Arrastar no vazio: caixa de seleção.</li>
 * </ul>
 */
public class FerramentaSelecionar implements Ferramenta {

    private boolean arrastandoCaixa;
    private double selecaoStartX;
    private double selecaoStartY;
    private boolean estadoRegistrado;

    @Override
    public void aoPressionarMouse(MouseEvent e, EditorContext contexto) {
        if (e.getButton() != MouseButton.PRIMARY) {
            return;
        }
        selecaoStartX = e.getX();
        selecaoStartY = e.getY();
        estadoRegistrado = false;

        FormaGeometrica alvo = contexto.formaNoPonto(e.getX(), e.getY());
        if (alvo != null) {
            arrastandoCaixa = false;
            if (e.isShiftDown() && contexto.isSelecionada(alvo)) {
                contexto.desselecionar(alvo);
            } else if (!contexto.isSelecionada(alvo)) {
                contexto.selecionar(alvo, e.isShiftDown());
            }
        } else {
            arrastandoCaixa = true;
            if (!e.isShiftDown()) {
                contexto.limparSelecao();
            }
        }
        contexto.requestRedraw();
        contexto.requestStatsUpdate();
    }

    @Override
    public void aoArrastarMouse(MouseEvent e, EditorContext contexto) {
        if (!e.isPrimaryButtonDown()) {
            return;
        }
        if (arrastandoCaixa) {
            contexto.setFormaEmAndamento(criarRetanguloSelecao(e.getX(), e.getY()));
            contexto.requestRedraw();
            return;
        }
        if (contexto.getSelecionados().isEmpty()) {
            return;
        }
        // deslocamento (respeita a grade magnética quando habilitada)
        double dx = contexto.aplicarSnap(e.getX() - selecaoStartX);
        double dy = contexto.aplicarSnap(e.getY() - selecaoStartY);
        if (dx == 0 && dy == 0) {
            return;
        }
        if (!estadoRegistrado) {
            contexto.registrarEstado();
            estadoRegistrado = true;
        }
        contexto.transladarSelecionados(dx, dy);
        selecaoStartX += dx;
        selecaoStartY += dy;
        contexto.requestRedraw();
        contexto.requestStatsUpdate();
    }

    @Override
    public void aoSoltarMouse(MouseEvent e, EditorContext contexto) {
        if (arrastandoCaixa) {
            double minX = Math.min(selecaoStartX, e.getX());
            double maxX = Math.max(selecaoStartX, e.getX());
            double minY = Math.min(selecaoStartY, e.getY());
            double maxY = Math.max(selecaoStartY, e.getY());
            if (maxX - minX > 2 || maxY - minY > 2) {
                for (FormaGeometrica f : contexto.getFormas()) {
                    if (estaDentro(f, minX, minY, maxX, maxY)) {
                        contexto.selecionar(f, true);
                    }
                }
            }
            contexto.setFormaEmAndamento(null);
            contexto.setMensagem(contexto.getSelecionados().size() + " forma(s) selecionada(s).");
        }
        arrastandoCaixa = false;
        estadoRegistrado = false;
        contexto.requestRedraw();
        contexto.requestStatsUpdate();
    }

    @Override
    public void aoClicar(MouseEvent e, EditorContext contexto) {
        // A seleção é tratada ao pressionar o mouse.
    }

    private boolean estaDentro(FormaGeometrica f, double minX, double minY, double maxX, double maxY) {
        if (f.getQuantidadePontos() == 0) {
            return false;
        }
        for (Ponto p : f.getPontos()) {
            if (p.getX() < minX || p.getX() > maxX || p.getY() < minY || p.getY() > maxY) {
                return false;
            }
        }
        return true;
    }

    /** Retângulo temporário exibido como "forma em andamento" durante a caixa de seleção. */
    private FormaGeometrica criarRetanguloSelecao(double x, double y) {
        Poligono caixa = new Poligono();
        caixa.adicionarPonto(new Ponto(selecaoStartX, selecaoStartY));
        caixa.adicionarPonto(new Ponto(x, selecaoStartY));
        caixa.adicionarPonto(new Ponto(x, y));
        caixa.adicionarPonto(new Ponto(selecaoStartX, y));
        caixa.setCorPreenchimentoHex("#1E90FF22");
        caixa.setCorBordaHex("#1E90FF");
        caixa.setEspessuraBorda(1);
        return caixa;
    }
}
