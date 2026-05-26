package com.campusgame.editor;

import java.awt.event.*;

/**
 * INTERIOR EDITOR INPUT ADAPTER  (editor/InteriorEditorInputAdapter.java)
 *
 * Mirrors EditorInputAdapter but forwards to InteriorEditorMode.
 * Registered on the Renderer panel alongside the existing EditorInputAdapter.
 * Events are only forwarded when InteriorEditorMode.isActive() is true,
 * so there is no conflict with the exterior editor.
 */
public class InteriorEditorInputAdapter extends MouseAdapter
        implements MouseMotionListener {

    private final InteriorEditorMode editor;

    public InteriorEditorInputAdapter(InteriorEditorMode editor) {
        this.editor = editor;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (editor.isActive()) editor.onMouseMoved(e.getX(), e.getY());
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (editor.isActive()) editor.onMouseDragged(e.getX(), e.getY());
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (!editor.isActive()) return;
        if (e.getButton() == MouseEvent.BUTTON1)
            editor.onLeftClick(e.getX(), e.getY());
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (!editor.isActive()) return;
        if (e.getButton() == MouseEvent.BUTTON1)
            editor.onLeftRelease(e.getX(), e.getY());
    }
}