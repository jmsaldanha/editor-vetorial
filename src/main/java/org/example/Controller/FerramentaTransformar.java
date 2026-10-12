package org.example.Controller;

import java.util.Objects;

import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import org.example.Models.FormaGeometrica;
import org.example.Models.Ponto;

/**
 * Estado de transformação geométrica. O {@link ModoFerramenta} define a operação:
 * <ul>
 *   <li>ESCALAR: arrastar afastando/aproximando do centro da forma;</li>
 *   <li>ROTACIONAR: arrastar em volta do centro da forma;</li>
 *   <li>CISALHAR: arrastar na horizontal (shx) e/ou vertical (shy);</li>
 *   <li>EXCLUIR: clicar na forma para removê-la.</li>
 * </ul>
 * As transformações atuam na seleção; formas agrupadas se comportam como uma
 * única forma lógica (Composite).
 */
public class FerramentaTransformar implements Ferramenta {

    private static final double SENSIBILIDADE_CISALHAMENTO = 0.01;
    private static final double DISTANCIA_MINIMA = 4.0;

    private final ModoFerramenta modo;
    private double ultimoX;
    private double ultimoY;
    private boolean estadoRegistrado;

    public FerramentaTransformar(ModoFerramenta modo) {
        this.modo = Objects.requireNonNull(modo);
    }

    public ModoFerramenta getModo() {
        return modo;
    }

    @Override
    public void aoPressionarMouse(MouseEvent e, EditorContext contexto) {
        if (e.getButton() != MouseButton.PRIMARY) {
            return;
        }
        FormaGeometrica alvo = contexto.formaNoPonto(e.getX(), e.getY());

        if (modo == ModoFerramenta.EXCLUIR) {
            if (alvo != null) {
                contexto.registrarEstado();
                contexto.excluirForma(alvo);
                contexto.setMensagem("Forma excluída.");
                contexto.requestRedraw();
                contexto.requestStatsUpdate();
            }
            return;
        }

        if (alvo != null && !contexto.isSelecionada(alvo)) {
            contexto.selecionar(alvo, false);
        }
        if (contexto.getSelecionados().isEmpty()) {
            contexto.setMensagem("Clique em uma forma para transformá-la.");
        }
        ultimoX = e.getX();
        ultimoY = e.getY();
        estadoRegistrado = false;
        contexto.requestRedraw();
        contexto.requestStatsUpdate();
    }

    @Override
    public void aoArrastarMouse(MouseEvent e, EditorContext contexto) {
        if (modo == ModoFerramenta.EXCLUIR || !e.isPrimaryButtonDown()
                || contexto.getSelecionados().isEmpty()) {
            return;
        }
        Ponto centro = contexto.getCentroSelecao();
        double x = e.getX();
        double y = e.getY();

        switch (modo) {
            case ESCALAR -> {
                double antes = Math.hypot(ultimoX - centro.getX(), ultimoY - centro.getY());
                double depois = Math.hypot(x - centro.getX(), y - centro.getY());
                if (antes < DISTANCIA_MINIMA || depois < DISTANCIA_MINIMA) {
                    return;
                }
                registrarUmaVez(contexto);
                contexto.escalarSelecionados(depois / antes, centro);
            }
            case ROTACIONAR -> {
                double anguloAntes = Math.atan2(ultimoY - centro.getY(), ultimoX - centro.getX());
                double anguloDepois = Math.atan2(y - centro.getY(), x - centro.getX());
                registrarUmaVez(contexto);
                contexto.rotacionarSelecionados(anguloDepois - anguloAntes, centro);
            }
            case CISALHAR -> {
                double shx = (x - ultimoX) * SENSIBILIDADE_CISALHAMENTO;
                double shy = (y - ultimoY) * SENSIBILIDADE_CISALHAMENTO;
                registrarUmaVez(contexto);
                contexto.cisalharSelecionados(shx, shy, centro);
            }
            default -> {
                return;
            }
        }
        ultimoX = x;
        ultimoY = y;
        contexto.requestRedraw();
        contexto.requestStatsUpdate();
    }

    @Override
    public void aoSoltarMouse(MouseEvent e, EditorContext contexto) {
        estadoRegistrado = false;
    }

    @Override
    public void aoClicar(MouseEvent e, EditorContext contexto) {
        // As ações acontecem ao pressionar/arrastar.
    }

    /** Guarda um único memento por gesto de arrasto. */
    private void registrarUmaVez(EditorContext contexto) {
        if (!estadoRegistrado) {
            contexto.registrarEstado();
            estadoRegistrado = true;
        }
    }
}
