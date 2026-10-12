package org.example.Controller;

import java.util.Objects;

import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import org.example.Models.Circulo;
import org.example.Models.FormaGeometrica;
import org.example.Models.Hexagono;
import org.example.Models.Ponto;
import org.example.Models.Quadrado;

/**
 * Estado de inserção de formas regulares (Quadrado, Círculo e Hexágono).
 * Pressione no ponto que será o centro e arraste para definir o raio;
 * um clique simples insere a forma com o raio padrão.
 */
public class FerramentaInserirForma implements Ferramenta {

    /** Tipos de forma regular que podem ser inseridos. */
    public enum TipoForma { QUADRADO, CIRCULO, HEXAGONO }

    private static final double RAIO_PADRAO = 50.0;
    private static final double RAIO_MINIMO_ARRASTO = 3.0;

    private Ponto centroInsercao;
    private final TipoForma tipo;

    public FerramentaInserirForma(TipoForma tipo) {
        this.tipo = Objects.requireNonNull(tipo);
    }

    public TipoForma getTipo() {
        return tipo;
    }

    @Override
    public void aoPressionarMouse(MouseEvent e, EditorContext contexto) {
        if (e.getButton() != MouseButton.PRIMARY) {
            return;
        }
        centroInsercao = new Ponto(contexto.aplicarSnap(e.getX()), contexto.aplicarSnap(e.getY()));
    }

    @Override
    public void aoArrastarMouse(MouseEvent e, EditorContext contexto) {
        if (centroInsercao == null) {
            return;
        }
        double raio = calcularRaio(e, contexto);
        if (raio >= RAIO_MINIMO_ARRASTO) {
            FormaGeometrica previa = criarForma(raio);
            contexto.aplicarEstiloAtual(previa);
            contexto.setFormaEmAndamento(previa);
            contexto.setMensagem(String.format("Raio: %.1f", raio));
            contexto.requestRedraw();
            contexto.requestStatsUpdate();
        }
    }

    @Override
    public void aoSoltarMouse(MouseEvent e, EditorContext contexto) {
        if (centroInsercao == null) {
            return;
        }
        double raio = calcularRaio(e, contexto);
        if (raio < RAIO_MINIMO_ARRASTO) {
            raio = RAIO_PADRAO;
        }
        try {
            FormaGeometrica forma = criarForma(raio);
            contexto.aplicarEstiloAtual(forma);
            contexto.registrarEstado();
            contexto.adicionarForma(forma);
            contexto.setMensagem(nomeDoTipo() + " inserido.");
        } catch (IllegalArgumentException ex) {
            contexto.setMensagem(ex.getMessage());
        }
        contexto.setFormaEmAndamento(null);
        centroInsercao = null;
        contexto.requestRedraw();
        contexto.requestStatsUpdate();
    }

    @Override
    public void aoClicar(MouseEvent e, EditorContext contexto) {
        // A inserção é concluída ao soltar o mouse.
    }

    private double calcularRaio(MouseEvent e, EditorContext contexto) {
        Ponto atual = new Ponto(contexto.aplicarSnap(e.getX()), contexto.aplicarSnap(e.getY()));
        return centroInsercao.distancia(atual);
    }

    /** As formas regulares calculam seus vértices automaticamente. */
    private FormaGeometrica criarForma(double raio) {
        return switch (tipo) {
            case QUADRADO -> new Quadrado(centroInsercao.clonar(), raio);
            case CIRCULO -> new Circulo(centroInsercao.clonar(), raio);
            case HEXAGONO -> new Hexagono(centroInsercao.clonar(), raio);
        };
    }

    private String nomeDoTipo() {
        return switch (tipo) {
            case QUADRADO -> "Quadrado";
            case CIRCULO -> "Círculo";
            case HEXAGONO -> "Hexágono";
        };
    }
}
