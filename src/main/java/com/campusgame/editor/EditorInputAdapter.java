package com.campusgame.editor;

import com.campusgame.renderer.Camera;

import java.awt.event.*;

public class EditorInputAdapter extends MouseAdapter implements MouseMotionListener {

    private final EditorMode editor;

    public EditorInputAdapter(EditorMode editor) {
        this.editor = editor;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        editor.onMouseMoved(e.getX(), e.getY());
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        // Treat drag same as move so ghost follows cursor during drag
        editor.onMouseMoved(e.getX(), e.getY());
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            editor.onLeftClick(e.getX(), e.getY());
        }
    }
}