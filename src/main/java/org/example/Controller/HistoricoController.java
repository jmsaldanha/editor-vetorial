package org.example.Controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.example.Models.FormaGeometrica;

/**
 * Histórico de ações (Desfazer/Refazer) – padrões Memento e Command.
 * Cada estado guardado é uma cópia profunda (Prototype) da lista de formas.
 */
public class HistoricoController {

    private static final int LIMITE_ESTADOS = 100;

    private final Stack<List<FormaGeometrica>> undoStack = new Stack<>();
    private final Stack<List<FormaGeometrica>> redoStack = new Stack<>();

    /** Salva um memento (cópia profunda) do estado atual, antes de uma alteração. */
    public void registrarEstado(List<FormaGeometrica> formas) {
        undoStack.push(copiaProfunda(formas));
        if (undoStack.size() > LIMITE_ESTADOS) {
            undoStack.remove(0);
        }
        redoStack.clear();
    }

    /**
     * Volta ao estado anterior. O estado atual vai para a pilha de refazer.
     * Se não houver o que desfazer, devolve a própria lista recebida.
     */
    public List<FormaGeometrica> desfazer(List<FormaGeometrica> estadoAtual) {
        if (undoStack.isEmpty()) {
            return estadoAtual;
        }
        redoStack.push(copiaProfunda(estadoAtual));
        return undoStack.pop();
    }

    /**
     * Reaplica um estado desfeito. O estado atual volta para a pilha de desfazer.
     * Se não houver o que refazer, devolve a própria lista recebida.
     */
    public List<FormaGeometrica> refazer(List<FormaGeometrica> estadoAtual) {
        if (redoStack.isEmpty()) {
            return estadoAtual;
        }
        undoStack.push(copiaProfunda(estadoAtual));
        return redoStack.pop();
    }

    private List<FormaGeometrica> copiaProfunda(List<FormaGeometrica> formas) {
        List<FormaGeometrica> copia = new ArrayList<>(formas.size());
        for (FormaGeometrica f : formas) {
            copia.add(f.clonar());
        }
        return copia;
    }
}
