package org.example.Models;

/**
 * Círculo representado por vértices igualmente espaçados sobre a
 * circunferência, o que permite aplicar todas as transformações
 * (escala, rotação, cisalhamento e edição de vértices).
 * Regra de negócio: o raio deve ser estritamente positivo (r &gt; 0).
 */
public class Circulo extends FormaBase {

    private static final int SEGMENTOS = 72;

    public Circulo(Ponto centro, double raio) {
        if (centro == null) {
            throw new IllegalArgumentException("O centro do círculo não pode ser nulo.");
        }
        if (!(raio > 0) || Double.isInfinite(raio)) {
            throw new IllegalArgumentException("O raio do círculo deve ser estritamente positivo (r > 0).");
        }
        for (int i = 0; i < SEGMENTOS; i++) {
            double angulo = 2 * Math.PI * i / SEGMENTOS;
            pontos.add(new Ponto(centro.getX() + raio * Math.cos(angulo),
                                     centro.getY() + raio * Math.sin(angulo)));
        }
    }

    @Override
    public FormaGeometrica clonar() {
        return copiarPara(new Circulo(new Ponto(0, 0), 1));
    }
}
