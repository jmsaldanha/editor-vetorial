package org.example.Models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Implementações base comuns a todas as formas. Toda forma é descrita por uma
 * lista de vértices ({@link Ponto}) e por seus atributos de estilo.
 */
public abstract class FormaBase implements FormaGeometrica {

    private static final Pattern HEX_COR = Pattern.compile("^#([0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$");
    private static final double EPSILON = 1e-9;

    protected List<Ponto> pontos = new ArrayList<>();
    protected String corPreenchimentoHex = "#D3D3D3";
    protected String corBordaHex = "#000000";
    protected double espessuraBorda = 1.0;
    protected String groupId;

    // ------------------------------------------------------------ Vértices

    @Override
    public void adicionarPonto(Ponto p) {
        if (p == null) {
            throw new IllegalArgumentException("Não é possível adicionar um ponto nulo.");
        }
        pontos.add(p);
    }

    /** Lista somente leitura dos vértices (os pontos em si continuam editáveis). */
    @Override
    public List<Ponto> getPontos() {
        return Collections.unmodifiableList(pontos);
    }

    @Override
    public int getQuantidadePontos() {
        return pontos.size();
    }

    // ------------------------------------------------------------ Estilo

    @Override
    public String getCorPreenchimentoHex() {
        return corPreenchimentoHex;
    }

    @Override
    public void setCorPreenchimentoHex(String hex) {
        this.corPreenchimentoHex = validarCor(hex);
    }

    @Override
    public String getCorBordaHex() {
        return corBordaHex;
    }

    @Override
    public void setCorBordaHex(String hex) {
        this.corBordaHex = validarCor(hex);
    }

    @Override
    public double getEspessuraBorda() {
        return espessuraBorda;
    }

    @Override
    public void setEspessuraBorda(double espessura) {
        if (!(espessura > 0) || Double.isInfinite(espessura)) {
            throw new IllegalArgumentException("A espessura da borda deve ser positiva.");
        }
        this.espessuraBorda = espessura;
    }

    @Override
    public String getGroupId() {
        return groupId;
    }

    @Override
    public void setGroupId(String groupId) {
        this.groupId = (groupId == null || groupId.isBlank()) ? null : groupId;
    }

    private static String validarCor(String hex) {
        if (hex == null || !HEX_COR.matcher(hex).matches()) {
            throw new IllegalArgumentException("Cor inválida: " + hex + " (use o formato #RRGGBB).");
        }
        return hex.toUpperCase();
    }

    // ------------------------------------------------------------ Medidas

    /** Área pela fórmula de Gauss (fórmula do cadarço). */
    @Override
    public double calcularArea() {
        int n = pontos.size();
        if (n < 3) {
            return 0.0;
        }
        double soma = 0.0;
        for (int i = 0; i < n; i++) {
            Ponto a = pontos.get(i);
            Ponto b = pontos.get((i + 1) % n); // o último termo fecha (xn*y1 - x1*yn)
            soma += a.getX() * b.getY() - b.getX() * a.getY();
        }
        return Math.abs(soma) / 2.0;
    }

    /** Perímetro: soma das distâncias euclidianas entre vértices consecutivos. */
    @Override
    public double calcularPerimetro() {
        int n = pontos.size();
        if (n < 2) {
            return 0.0;
        }
        double perimetro = 0.0;
        for (int i = 0; i < n - 1; i++) {
            perimetro += pontos.get(i).distancia(pontos.get(i + 1));
        }
        if (n >= 3) {
            perimetro += pontos.get(n - 1).distancia(pontos.get(0));
        }
        return perimetro;
    }

    /** Centroide geométrico do polígono (média dos vértices para formas degeneradas). */
    @Override
    public Ponto getCentroide() {
        int n = pontos.size();
        if (n == 0) {
            return new Ponto(0, 0);
        }
        double areaSinal = 0.0;
        double cx = 0.0;
        double cy = 0.0;
        if (n >= 3) {
            for (int i = 0; i < n; i++) {
                Ponto a = pontos.get(i);
                Ponto b = pontos.get((i + 1) % n);
                double cruz = a.getX() * b.getY() - b.getX() * a.getY();
                areaSinal += cruz;
                cx += (a.getX() + b.getX()) * cruz;
                cy += (a.getY() + b.getY()) * cruz;
            }
            areaSinal /= 2.0;
        }
        if (Math.abs(areaSinal) < EPSILON) {
            double sx = 0;
            double sy = 0;
            for (Ponto p : pontos) {
                sx += p.getX();
                sy += p.getY();
            }
            return new Ponto(sx / n, sy / n);
        }
        return new Ponto(cx / (6.0 * areaSinal), cy / (6.0 * areaSinal));
    }

    /** Teste ponto-em-polígono (ray casting). */
    @Override
    public boolean contemPonto(double px, double py) {
        int n = pontos.size();
        if (n < 3) {
            return false;
        }
        boolean dentro = false;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double xi = pontos.get(i).getX();
            double yi = pontos.get(i).getY();
            double xj = pontos.get(j).getX();
            double yj = pontos.get(j).getY();
            boolean cruza = (yi > py) != (yj > py)
                    && px < (xj - xi) * (py - yi) / (yj - yi) + xi;
            if (cruza) {
                dentro = !dentro;
            }
        }
        return dentro;
    }

    // ------------------------------------------------------------ Transformações

    /** Translação: soma dx e dy a cada ponto. */
    @Override
    public void transladar(double dx, double dy) {
        for (Ponto p : pontos) {
            p.mover(dx, dy);
        }
    }

    /** Escala: multiplica a distância de cada ponto ao centroide pelo fator. */
    @Override
    public void escalar(double fator) {
        if (!(fator > 0) || Double.isInfinite(fator)) {
            throw new IllegalArgumentException("O fator de escala deve ser positivo.");
        }
        Ponto c = getCentroide();
        for (Ponto p : pontos) {
            p.setX(c.getX() + (p.getX() - c.getX()) * fator);
            p.setY(c.getY() + (p.getY() - c.getY()) * fator);
        }
    }

    /** Rotação dos pontos em torno do centroide (ângulo em radianos). */
    @Override
    public void rotacionar(double anguloRadianos) {
        Ponto c = getCentroide();
        double cos = Math.cos(anguloRadianos);
        double sin = Math.sin(anguloRadianos);
        for (Ponto p : pontos) {
            double vx = p.getX() - c.getX();
            double vy = p.getY() - c.getY();
            p.setX(c.getX() + vx * cos - vy * sin);
            p.setY(c.getY() + vx * sin + vy * cos);
        }
    }

    /** Cisalhamento em torno do centroide: x' = x + shx*y ; y' = y + shy*x. */
    @Override
    public void cisalhar(double shx, double shy) {
        Ponto c = getCentroide();
        for (Ponto p : pontos) {
            double vx = p.getX() - c.getX();
            double vy = p.getY() - c.getY();
            p.setX(c.getX() + vx + shx * vy);
            p.setY(c.getY() + shy * vx + vy);
        }
    }

    // ------------------------------------------------------------ Prototype

    @Override
    public abstract FormaGeometrica clonar();

    /**
     * Copia (em profundidade) vértices e estilo desta forma para o destino.
     * Usado pelas subclasses na implementação de {@link #clonar()}.
     */
    protected <T extends FormaBase> T copiarPara(T destino) {
        destino.pontos = new ArrayList<>();
        for (Ponto p : this.pontos) {
            destino.pontos.add(p.clonar());
        }
        destino.corPreenchimentoHex = this.corPreenchimentoHex;
        destino.corBordaHex = this.corBordaHex;
        destino.espessuraBorda = this.espessuraBorda;
        destino.groupId = this.groupId;
        return destino;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + pontos.size() + " pontos]";
    }
}
