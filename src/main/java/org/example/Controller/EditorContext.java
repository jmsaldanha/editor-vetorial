package org.example.Controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.example.Models.FormaGeometrica;
import org.example.Models.Ponto;

/**
 * Contexto do State Pattern: guarda o estado do editor (formas, seleção,
 * ferramenta ativa) e delega os eventos do mouse à {@link Ferramenta} atual.
 * Também implementa o comportamento Composite: formas com o mesmo groupId
 * são selecionadas e transformadas como uma única forma lógica.
 *
 * A comunicação com as Views é feita apenas por callbacks (requestRedraw /
 * requestStatsUpdate), mantendo o Controller desacoplado da interface.
 */
public class EditorContext {

    /** Espaçamento da grade magnética, em pixels. */
    public static final double TAMANHO_GRADE = 20.0;

    private List<FormaGeometrica> formas = new ArrayList<>();
    private final List<FormaGeometrica> selecionados = new ArrayList<>();
    private FormaGeometrica formaEmAndamento;
    private Ferramenta ferramentaAtual;
    private boolean gridSnappingHabilitado;

    private final HistoricoController historico;

    // Estilo aplicado às novas formas (definido na barra lateral)
    private String corPreenchimentoAtual = "#D3D3D3";
    private String corBordaAtual = "#000000";
    private double espessuraBordaAtual = 1.0;

    // Informações exibidas na barra de status
    private double mouseX;
    private double mouseY;
    private String mensagem = "";
    private boolean alteracoesNaoSalvas;

    // Callbacks registrados pela camada de View
    private Runnable ouvinteRedesenho;
    private Runnable ouvinteEstatisticas;
    private Runnable ouvinteSelecao;

    public EditorContext(HistoricoController historico) {
        this.historico = Objects.requireNonNull(historico);
        this.ferramentaAtual = new FerramentaSelecionar();
    }

    // ================================================================ Diagrama

    /** Solicita à View que redesenhe o canvas. */
    public void requestRedraw() {
        if (ouvinteRedesenho != null) {
            ouvinteRedesenho.run();
        }
    }

    /** Solicita à View que atualize a barra de status. */
    public void requestStatsUpdate() {
        if (ouvinteEstatisticas != null) {
            ouvinteEstatisticas.run();
        }
    }

    /** Composite: une as formas selecionadas sob um mesmo groupId. */
    public void agruparSelecionados() {
        if (selecionados.size() < 2) {
            setMensagem("Selecione ao menos 2 formas para agrupar.");
            return;
        }
        registrarEstado();
        String novoGrupo = UUID.randomUUID().toString();
        for (FormaGeometrica f : selecionados) {
            f.setGroupId(novoGrupo);
        }
        setMensagem(selecionados.size() + " formas agrupadas.");
        atualizarTudo();
    }

    /** Remove o groupId das formas selecionadas. */
    public void desagruparSelecionados() {
        boolean possuiGrupo = selecionados.stream().anyMatch(f -> f.getGroupId() != null);
        if (!possuiGrupo) {
            setMensagem("Nenhum grupo selecionado.");
            return;
        }
        registrarEstado();
        for (FormaGeometrica f : selecionados) {
            f.setGroupId(null);
        }
        setMensagem("Grupo desfeito.");
        atualizarTudo();
    }

    /** Camadas: move as formas selecionadas para o topo da pilha de desenho. */
    public void trazerParaFrente() {
        if (selecionados.isEmpty()) {
            return;
        }
        registrarEstado();
        List<FormaGeometrica> ordem = selecionadosNaOrdemDeDesenho();
        formas.removeAll(ordem);
        formas.addAll(ordem);
        atualizarTudo();
    }

    /** Camadas: move as formas selecionadas para o fundo da pilha de desenho. */
    public void enviarParaTras() {
        if (selecionados.isEmpty()) {
            return;
        }
        registrarEstado();
        List<FormaGeometrica> ordem = selecionadosNaOrdemDeDesenho();
        formas.removeAll(ordem);
        formas.addAll(0, ordem);
        atualizarTudo();
    }

    /** Snap to Grid: arredonda a coordenada para o ponto mais próximo da grade. */
    public double aplicarSnap(double valor) {
        if (!gridSnappingHabilitado) {
            return valor;
        }
        return Math.round(valor / TAMANHO_GRADE) * TAMANHO_GRADE;
    }

    // ================================================================ Histórico

    /** Registra um memento do estado atual antes de uma modificação. */
    public void registrarEstado() {
        historico.registrarEstado(formas);
        alteracoesNaoSalvas = true;
    }

    public void desfazer() {
        List<FormaGeometrica> anterior = historico.desfazer(formas);
        if (anterior == formas) {
            setMensagem("Nada para desfazer.");
        } else {
            substituirFormas(anterior);
            alteracoesNaoSalvas = true;
            setMensagem("Ação desfeita.");
        }
        atualizarTudo();
    }

    public void refazer() {
        List<FormaGeometrica> proximo = historico.refazer(formas);
        if (proximo == formas) {
            setMensagem("Nada para refazer.");
        } else {
            substituirFormas(proximo);
            alteracoesNaoSalvas = true;
            setMensagem("Ação refeita.");
        }
        atualizarTudo();
    }

    // ================================================================ Formas

    public void adicionarForma(FormaGeometrica forma) {
        formas.add(Objects.requireNonNull(forma));
    }

    /** Substitui todas as formas (usado ao carregar arquivo e no undo/redo). */
    public void substituirFormas(List<FormaGeometrica> novasFormas) {
        formas = new ArrayList<>(novasFormas);
        selecionados.clear();
        formaEmAndamento = null;
        notificarSelecao();
    }

    /** Forma mais ao topo que contém o ponto informado (ou null). */
    public FormaGeometrica formaNoPonto(double x, double y) {
        for (int i = formas.size() - 1; i >= 0; i--) {
            if (formas.get(i).contemPonto(x, y)) {
                return formas.get(i);
            }
        }
        return null;
    }

    /** Remove a forma e todos os membros do seu grupo. */
    public void excluirForma(FormaGeometrica forma) {
        List<FormaGeometrica> remover = membrosDoGrupo(forma);
        formas.removeAll(remover);
        selecionados.removeAll(remover);
        notificarSelecao();
    }

    public void excluirSelecionados() {
        if (selecionados.isEmpty()) {
            setMensagem("Nenhuma forma selecionada.");
            atualizarTudo();
            return;
        }
        registrarEstado();
        int qtd = selecionados.size();
        formas.removeAll(selecionados);
        selecionados.clear();
        notificarSelecao();
        setMensagem(qtd + " forma(s) excluída(s).");
        atualizarTudo();
    }

    // ================================================================ Seleção (Composite)

    /** Seleciona a forma e todos os membros do seu grupo. */
    public void selecionar(FormaGeometrica forma, boolean adicionarASelecao) {
        if (!adicionarASelecao) {
            selecionados.clear();
        }
        for (FormaGeometrica f : membrosDoGrupo(forma)) {
            if (!selecionados.contains(f)) {
                selecionados.add(f);
            }
        }
        notificarSelecao();
    }

    public void desselecionar(FormaGeometrica forma) {
        selecionados.removeAll(membrosDoGrupo(forma));
        notificarSelecao();
    }

    public void limparSelecao() {
        if (!selecionados.isEmpty()) {
            selecionados.clear();
            notificarSelecao();
        }
    }

    public boolean isSelecionada(FormaGeometrica forma) {
        return selecionados.contains(forma);
    }

    /** Todas as formas que compartilham o groupId da forma informada. */
    public List<FormaGeometrica> membrosDoGrupo(FormaGeometrica forma) {
        List<FormaGeometrica> membros = new ArrayList<>();
        if (forma == null) {
            return membros;
        }
        String grupo = forma.getGroupId();
        if (grupo == null) {
            membros.add(forma);
            return membros;
        }
        for (FormaGeometrica f : formas) {
            if (grupo.equals(f.getGroupId())) {
                membros.add(f);
            }
        }
        if (!membros.contains(forma)) {
            membros.add(forma);
        }
        return membros;
    }

    /** Centro lógico da seleção (média dos centroides das formas selecionadas). */
    public Ponto getCentroSelecao() {
        if (selecionados.isEmpty()) {
            return null;
        }
        double sx = 0;
        double sy = 0;
        for (FormaGeometrica f : selecionados) {
            Ponto c = f.getCentroide();
            sx += c.getX();
            sy += c.getY();
        }
        return new Ponto(sx / selecionados.size(), sy / selecionados.size());
    }

    // ================================================================ Transformações da seleção
    // Cada forma é transformada em torno do seu próprio centroide e depois
    // deslocada, de modo que o conjunto se comporte como uma única forma
    // transformada em torno do pivô (centro do grupo).

    public void transladarSelecionados(double dx, double dy) {
        for (FormaGeometrica f : selecionados) {
            f.transladar(dx, dy);
        }
    }

    public void escalarSelecionados(double fator, Ponto pivo) {
        for (FormaGeometrica f : selecionados) {
            Ponto c = f.getCentroide();
            f.escalar(fator);
            f.transladar((pivo.getX() - c.getX()) * (1 - fator),
                         (pivo.getY() - c.getY()) * (1 - fator));
        }
    }

    public void rotacionarSelecionados(double anguloRadianos, Ponto pivo) {
        double cos = Math.cos(anguloRadianos);
        double sin = Math.sin(anguloRadianos);
        for (FormaGeometrica f : selecionados) {
            Ponto c = f.getCentroide();
            f.rotacionar(anguloRadianos);
            double vx = c.getX() - pivo.getX();
            double vy = c.getY() - pivo.getY();
            double novoX = pivo.getX() + vx * cos - vy * sin;
            double novoY = pivo.getY() + vx * sin + vy * cos;
            f.transladar(novoX - c.getX(), novoY - c.getY());
        }
    }

    public void cisalharSelecionados(double shx, double shy, Ponto pivo) {
        for (FormaGeometrica f : selecionados) {
            Ponto c = f.getCentroide();
            f.cisalhar(shx, shy);
            double vx = c.getX() - pivo.getX();
            double vy = c.getY() - pivo.getY();
            double novoX = pivo.getX() + vx + shx * vy;
            double novoY = pivo.getY() + shy * vx + vy;
            f.transladar(novoX - c.getX(), novoY - c.getY());
        }
    }

    // ================================================================ Auxiliares

    private List<FormaGeometrica> selecionadosNaOrdemDeDesenho() {
        List<FormaGeometrica> ordem = new ArrayList<>();
        for (FormaGeometrica f : formas) {
            if (selecionados.contains(f)) {
                ordem.add(f);
            }
        }
        return ordem;
    }

    private void notificarSelecao() {
        if (ouvinteSelecao != null) {
            ouvinteSelecao.run();
        }
    }

    private void atualizarTudo() {
        requestRedraw();
        requestStatsUpdate();
    }

    // ================================================================ Getters / Setters

    public List<FormaGeometrica> getFormas() {
        return Collections.unmodifiableList(formas);
    }

    public List<FormaGeometrica> getSelecionados() {
        return Collections.unmodifiableList(selecionados);
    }

    public FormaGeometrica getFormaEmAndamento() {
        return formaEmAndamento;
    }

    public void setFormaEmAndamento(FormaGeometrica formaEmAndamento) {
        this.formaEmAndamento = formaEmAndamento;
    }

    public Ferramenta getFerramentaAtual() {
        return ferramentaAtual;
    }

    /** Troca o estado (ferramenta) ativo do editor. */
    public void setFerramentaAtual(Ferramenta ferramentaAtual) {
        this.ferramentaAtual = Objects.requireNonNull(ferramentaAtual);
        this.formaEmAndamento = null;
        atualizarTudo();
    }

    public boolean isGridSnappingHabilitado() {
        return gridSnappingHabilitado;
    }

    public void setGridSnappingHabilitado(boolean gridSnappingHabilitado) {
        this.gridSnappingHabilitado = gridSnappingHabilitado;
        requestRedraw();
    }

    public String getCorPreenchimentoAtual() {
        return corPreenchimentoAtual;
    }

    public void setCorPreenchimentoAtual(String corPreenchimentoAtual) {
        this.corPreenchimentoAtual = corPreenchimentoAtual;
    }

    public String getCorBordaAtual() {
        return corBordaAtual;
    }

    public void setCorBordaAtual(String corBordaAtual) {
        this.corBordaAtual = corBordaAtual;
    }

    public double getEspessuraBordaAtual() {
        return espessuraBordaAtual;
    }

    public void setEspessuraBordaAtual(double espessuraBordaAtual) {
        this.espessuraBordaAtual = espessuraBordaAtual;
    }

    /** Aplica o estilo atual da barra lateral a uma forma nova. */
    public void aplicarEstiloAtual(FormaGeometrica forma) {
        forma.setCorPreenchimentoHex(corPreenchimentoAtual);
        forma.setCorBordaHex(corBordaAtual);
        forma.setEspessuraBorda(espessuraBordaAtual);
    }

    public double getMouseX() {
        return mouseX;
    }

    public double getMouseY() {
        return mouseY;
    }

    public void setPosicaoMouse(double x, double y) {
        this.mouseX = x;
        this.mouseY = y;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem == null ? "" : mensagem;
    }

    public boolean isAlteracoesNaoSalvas() {
        return alteracoesNaoSalvas;
    }

    public void setAlteracoesNaoSalvas(boolean alteracoesNaoSalvas) {
        this.alteracoesNaoSalvas = alteracoesNaoSalvas;
    }

    public void setOuvinteRedesenho(Runnable ouvinte) {
        this.ouvinteRedesenho = ouvinte;
    }

    public void setOuvinteEstatisticas(Runnable ouvinte) {
        this.ouvinteEstatisticas = ouvinte;
    }

    public void setOuvinteSelecao(Runnable ouvinte) {
        this.ouvinteSelecao = ouvinte;
    }
}
