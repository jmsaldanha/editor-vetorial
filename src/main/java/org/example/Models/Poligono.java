package org.example.Models;

/**
 * Polígono livre definido ponto a ponto (desenho por cliques).
 * Regra de negócio: um polígono válido possui no mínimo 3 pontos.
 */
public class Poligono extends FormaBase {

    /** Quantidade mínima de vértices de um polígono válido. */
    public static final int MINIMO_PONTOS = 3;

    public Poligono() {
        super();
    }

    @Override
    public FormaGeometrica clonar() {
        return copiarPara(new Poligono());
    }
}
