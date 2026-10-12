package org.example.Models;

import java.util.List;

/**
 * Contrato para formas geométricas (Listing 1 da especificação).
 */
public interface FormaGeometrica {

    void adicionarPonto(Ponto p);

    List<Ponto> getPontos();

    int getQuantidadePontos();

    String getCorPreenchimentoHex();

    void setCorPreenchimentoHex(String hex);

    String getCorBordaHex();

    void setCorBordaHex(String hex);

    double getEspessuraBorda();

    void setEspessuraBorda(double espessura);

    String getGroupId();

    void setGroupId(String groupId);

    double calcularArea();

    double calcularPerimetro();

    Ponto getCentroide();

    boolean contemPonto(double px, double py);

    void transladar(double dx, double dy);

    void escalar(double fator);

    void rotacionar(double anguloRadianos);

    void cisalhar(double shx, double shy);

    /** Prototype: cópia profunda, sem compartilhamento de referências. */
    FormaGeometrica clonar();
}
