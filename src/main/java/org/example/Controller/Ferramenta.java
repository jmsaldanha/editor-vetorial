package org.example.Controller;

import javafx.scene.input.MouseEvent;

/**
 * Interface de Estado (State Pattern): cada ferramenta define como a tela de
 * desenho reage aos eventos do mouse.
 */
public interface Ferramenta {

    void aoPressionarMouse(MouseEvent e, EditorContext contexto);

    void aoArrastarMouse(MouseEvent e, EditorContext contexto);

    void aoSoltarMouse(MouseEvent e, EditorContext contexto);

    void aoClicar(MouseEvent e, EditorContext contexto);
}
