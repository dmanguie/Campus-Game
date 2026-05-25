package com.campusgame.editor.panel;

import com.campusgame.map.data.BuildingData;

/**
 * PROPERTY PANEL (editor/panel/PropertyPanel.java)
 * --------------------------------------------------
 * Data model for the building property panel shown when a building is selected.
 *
 * Stores editable fields the user can change:
 *   name, floors, tag, colorARGB, collisionEnabled
 *
 * The actual UI drawing is in PropertyPanelRenderer (Swing/2D).
 * This class is pure data — no rendering.
 *
 * Future: hook into a real Swing JPanel or text-input system for Phase 5.
 *
 * Phase 4 workflow:
 *   1. User selects a building (S tool)
 *   2. PropertyPanel.load(building) copies its fields in
 *   3. PropertyPanelRenderer draws the panel on screen
 *   4. User presses Tab to cycle fields, Enter to commit
 *   5. PropertyPanel.buildUpdated() returns a new BuildingData
 *   6. EditorMode.commitProperty() replaces the old building
 */
public class PropertyPanel {

    // ── Panel state ───────────────────────────────────────────────────
    private boolean visible      = false;
    private boolean editingField = false;

    // ── Editable fields ───────────────────────────────────────────────
    public String  name             = "";
    public int     floors           = 1;
    public String  tag              = "building";
    public int     colorARGB        = 0xFF888888;
    public boolean collisionEnabled = true;
    public float   rotationDegrees  = 0f;

    // ── Field focus (which field is being edited) ─────────────────────
    public enum Field { NAME, FLOORS, TAG, COLOR, COLLISION, ROTATION }
    private Field focusedField = Field.NAME;

    // ── Source building (for comparison / cancel) ─────────────────────
    private BuildingData source = null;

    // ── Load from a BuildingData ──────────────────────────────────────
    public void load(BuildingData b) {
        this.source           = b;
        this.name             = b.name;
        this.floors           = b.floors;
        this.tag              = b.tag != null ? b.tag : "building";
        this.colorARGB        = b.colorARGB;
        this.collisionEnabled = b.collisionEnabled;
        this.rotationDegrees  = b.rotationDegrees;
        this.focusedField     = Field.NAME;
        this.visible          = true;
    }

    public void hide() { visible = false; source = null; }

    // ── Build a new BuildingData from the edited fields ───────────────
    public BuildingData buildUpdated() {
        if (source == null) return null;
        if (source.isPolygon()) {
            return new BuildingData(name, source.polygonX, source.polygonZ,
                    floors, colorARGB, rotationDegrees, collisionEnabled, tag);
        }
        return new BuildingData(name, source.x, source.z,
                source.width, source.depth, floors,
                colorARGB, rotationDegrees, collisionEnabled, tag);
    }

    // ── Field navigation ──────────────────────────────────────────────
    public void nextField() {
        Field[] fields = Field.values();
        focusedField = fields[(focusedField.ordinal() + 1) % fields.length];
    }

    public void prevField() {
        Field[] fields = Field.values();
        focusedField = fields[(focusedField.ordinal() + fields.length - 1) % fields.length];
    }

    /** Apply a key character to the currently focused field. */
    public void typeChar(char c) {
        switch (focusedField) {
            case NAME -> {
                if (c == '\b') { if (!name.isEmpty()) name = name.substring(0, name.length()-1); }
                else if (c >= 32) name += c;
            }
            case FLOORS -> {
                if (c >= '1' && c <= '9') floors = c - '0';
            }
            case TAG -> {
                if (c == '\b') { if (!tag.isEmpty()) tag = tag.substring(0, tag.length()-1); }
                else if (c >= 32) tag += c;
            }
            case COLLISION -> {
                if (c == 't' || c == 'T') collisionEnabled = true;
                if (c == 'f' || c == 'F') collisionEnabled = false;
            }
            default -> {}
        }
    }

    // ── Getters ───────────────────────────────────────────────────────
    public boolean   isVisible()      { return visible; }
    public boolean   isEditingField() { return editingField; }
    public Field     getFocusedField(){ return focusedField; }
    public BuildingData getSource()   { return source; }

    public void setEditingField(boolean v) { editingField = v; }
    public void setColor(int argb)         { colorARGB = argb; }
    public void setFocusedField(Field f)   { focusedField = f; }
}