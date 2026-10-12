package org.example.Models;

/**
 * Hexágono regular: os 6 vértices são calculados automaticamente a partir
 * do centro e do raio (distância do centro a cada vértice).
 */
public class Hexagono extends FormaBase {

    public Hexagono(Ponto centro, double raio) {
        if (centro == null) {
            throw new IllegalArgumentException("O centro do hexágono não pode ser nulo.");
        }
        if (!(raio > 0) || Double.isInfinite(raio)) {
            throw new IllegalArgumentException("O raio do hexágono deve ser estritamente positivo.");
        }
        for (int i = 0; i < 6; i++) {
            double angulo = Math.toRadians(60.0 * i);
            pontos.add(new Ponto(centro.getX() + raio * Math.cos(angulo),
                                     centro.getY() + raio * Math.sin(angulo)));
        }
    }

    @Override
    public FormaGeometrica clonar() {
        return copiarPara(new Hexagono(new Ponto(0, 0), 1));
    }
}
