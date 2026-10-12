package org.example.Models;

/**
 * Quadrado regular: os 4 vértices são calculados automaticamente a partir
 * do centro e do raio (metade do lado).
 */
public class Quadrado extends Poligono {

    public Quadrado(Ponto centro, double raio) {
        super();
        if (centro == null) {
            throw new IllegalArgumentException("O centro do quadrado não pode ser nulo.");
        }
        if (!(raio > 0) || Double.isInfinite(raio)) {
            throw new IllegalArgumentException("O raio do quadrado deve ser estritamente positivo.");
        }
        double cx = centro.getX();
        double cy = centro.getY();
        pontos.add(new Ponto(cx - raio, cy - raio));
        pontos.add(new Ponto(cx + raio, cy - raio));
        pontos.add(new Ponto(cx + raio, cy + raio));
        pontos.add(new Ponto(cx - raio, cy + raio));
    }

    @Override
    public FormaGeometrica clonar() {
        return copiarPara(new Quadrado(new Ponto(0, 0), 1));
    }
}
