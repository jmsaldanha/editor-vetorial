package org.example.Models;

/**
 * Representa um ponto (vértice) no plano cartesiano 2D.
 */
public class Ponto {

    private double x;
    private double y;

    public Ponto(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    /** Desloca o ponto pelos fatores dx e dy. */
    public void mover(double dx, double dy) {
        this.x += dx;
        this.y += dy;
    }

    /** Distância euclidiana até outro ponto. */
    public double distancia(Ponto outro) {
        if (outro == null) {
            throw new IllegalArgumentException("O ponto de referência não pode ser nulo.");
        }
        return Math.hypot(this.x - outro.x, this.y - outro.y);
    }

    /** Prototype: cópia independente do ponto. */
    public Ponto clonar() {
        return new Ponto(x, y);
    }

    @Override
    public String toString() {
        return String.format("(%.2f, %.2f)", x, y);
    }
}
