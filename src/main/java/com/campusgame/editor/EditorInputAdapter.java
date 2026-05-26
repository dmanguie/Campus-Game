package com.campusgame.editor;

import java.awt.event.*;

public class EditorInputAdapter extends MouseAdapter implements MouseMotionListener {

    private final EditorMode editor;

    public EditorInputAdapter(EditorMode editor) { this.editor = editor; }

    @Override public void mouseMoved(MouseEvent e)   { editor.onMouseMoved(e.getX(), e.getY()); }
    @Override public void mouseDragged(MouseEvent e) { editor.onMouseDragged(e.getX(), e.getY()); }

    @Override
    public void mousePressed(MouseEvent e) {
        int x = e.getX(), y = e.getY();
        switch (e.getButton()) {
            case MouseEvent.BUTTON1 -> editor.onLeftClick(x, y);
            case MouseEvent.BUTTON2 -> editor.onMiddleClick(x, y);
            case MouseEvent.BUTTON3 -> editor.onRightClick(x, y);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) editor.onLeftRelease(e.getX(), e.getY());
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        editor.onMouseWheel(e.getX(), e.getY(), e.getWheelRotation());
    }
}