package com.campusgame.editor;

import com.campusgame.map.Building;
import com.campusgame.map.CampusMap;
import com.campusgame.map.data.BuildingData;
import com.campusgame.map.data.EntranceData;
import com.campusgame.renderer.Camera;

import java.awt.*;

public class EditorOverlayRenderer {

    private final EditorState state;
    private final CampusMap   campusMap;
    private final Camera      camera;

    private static final Color GHOST_FILL      = new Color(150,150,150,100);
    private static final Color GHOST_BORDER    = new Color(255,255,255,180);
    private static final Color SELECT_COLOR    = new Color(255,230,  0,200);
    private static final Color BANNER_BG       = new Color(  0,  0,  0,170);
    private static final Color STATUS_BG       = new Color( 20, 20, 20,200);
    private static final Color SHAPE_OUTLINE   = new Color( 80,200,255,220);
    private static final Color VERTEX_NORMAL   = new Color( 80,200,255,220);
    private static final Color VERTEX_HOVERED  = new Color(255,255,100,240);
    private static final Color VERTEX_SELECTED = new Color(255,100, 80,240);
    private static final Color MIDPOINT_HINT   = new Color( 80,255,160,160);
    private static final Color LEGEND_BG       = new Color(  0,  0,  0,160);

    private static final Color ENTRANCE_RING   = new Color( 80,220,120,220);
    private static final Color ENTRANCE_FILL   = new Color( 80,220,120, 55);
    private static final Color ENTRANCE_SEL    = new Color(255,220, 50,240);
    private static final Color ENTRANCE_GHOST  = new Color(120,220,160,120);
    private static final Color ENTRANCE_LABEL  = new Color( 60,200,100,220);
    private final javax.swing.JComponent parentComponent;

    public EditorOverlayRenderer(EditorState state, CampusMap campusMap, Camera camera,
                                 javax.swing.JComponent parentComponent) {
        this.state = state; this.campusMap = campusMap; this.camera = camera;
        this.parentComponent = parentComponent;
    }
    public EditorState getState() { return state; }

    public void draw(Graphics2D g, int screenW, int screenH) {
        if (!state.isActive()) return;
        drawBanner(g, screenW);
        drawGhost(g);
        drawSelection(g);
        drawEntrances(g);
        drawShapeEdit(g, screenW);
        drawPathEdit(g, screenW)    ;
        drawEntranceLegend(g, screenW);
        drawScenePicker(g);
        drawToolHUD(g, screenW, screenH);  // ← was missing
        drawStatus(g, screenW, screenH);
    }

    // ── Banner ────────────────────────────────────────────────────────
    private void drawBanner(Graphics2D g, int screenW) {
        g.setColor(BANNER_BG); g.fillRect(0,0,screenW,28);
        g.setFont(new Font("Consolas",Font.BOLD,13));
        g.setColor(new Color(255,220,50)); g.drawString("✏  EDITOR MODE",10,19);

        String toolLabel = switch (state.getCurrentTool()) {
            case PLACE      -> "[ P ] PLACE";
            case SELECT     -> "[ Q ] SELECT";
            case MOVE       -> "[ W ] MOVE";
            case RESIZE     -> "[ E ] RESIZE";
            case ROTATE     -> "[ R ] ROTATE";
            case PATH_EDIT  -> "[ T ] PATH";
            case DELETE     -> "[ X ] DELETE";
            case SHAPE_EDIT -> "[ V ] SHAPE EDIT";
            case ENTRANCE   -> "[ N ] ENTRANCE";
        };



        g.setColor(Color.WHITE);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(toolLabel, screenW/2 - fm.stringWidth(toolLabel)/2, 19);

        g.setColor(new Color(180,180,180));
        g.setFont(new Font("SansSerif",Font.PLAIN,11));
        String hint = "F1=Exit  P=Place  S=Select  X=Delete  V=Shape  R=Path  N=Entrance  Ctrl+S=Save";
        g.drawString(hint, screenW - g.getFontMetrics().stringWidth(hint) - 10, 19);
    }

    // ── Ghost building ────────────────────────────────────────────────
    private void drawGhost(Graphics2D g) {
        BuildingData ghost = state.getGhostBuilding();
        if (ghost==null || state.getCurrentTool()!=EditorState.Tool.PLACE) return;
        int sx=camera.worldToScreenX(ghost.x), sy=camera.worldToScreenY(ghost.z);
        int sw=(int)ghost.width, sh=(int)ghost.depth;
        g.setColor(GHOST_FILL); g.fillRect(sx,sy,sw,sh);
        g.setColor(GHOST_BORDER);
        g.setStroke(new BasicStroke(1.5f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND,
                1f,new float[]{6f,4f},0f));
        g.drawRect(sx,sy,sw,sh); g.setStroke(new BasicStroke(1f));
        g.setColor(Color.WHITE); g.setFont(new Font("SansSerif",Font.BOLD,9));
        g.drawString(ghost.name, sx+4, sy+12);
    }

    // ── Building selection ────────────────────────────────────────────
    private void drawSelection(Graphics2D g) {
        if (state.getCurrentTool()==EditorState.Tool.SHAPE_EDIT) return;
        BuildingData sel = state.getSelectedBuilding();
        if (sel==null) return;
        for (Building b : campusMap.getBuildings()) {
            if (b.getData()!=sel) continue;
            g.setColor(SELECT_COLOR); g.setStroke(new BasicStroke(3f));
            if (sel.isPolygon()) {
                int n=sel.polygonX.length; int[] px=new int[n],py=new int[n];
                for (int i=0;i<n;i++){
                    px[i]=camera.worldToScreenX(sel.polygonX[i]);
                    py[i]=camera.worldToScreenY(sel.polygonZ[i]);
                }
                g.drawPolygon(px,py,n);
            } else {
                g.drawRect(camera.worldToScreenX(b.getX())-2,
                        camera.worldToScreenY(b.getY())-2,
                        b.getWidth()+4, b.getDepth()+4);
            }
            g.setStroke(new BasicStroke(1f));
            int tx = sel.isPolygon()
                    ? camera.worldToScreenX(sel.polygonX[0])
                    : camera.worldToScreenX(b.getX());
            int ty = sel.isPolygon()
                    ? camera.worldToScreenY(sel.polygonZ[0])
                    : camera.worldToScreenY(b.getY());
            g.setFont(new Font("SansSerif",Font.BOLD,10));
            int tw = g.getFontMetrics().stringWidth(sel.name);
            g.setColor(new Color(0,0,0,160)); g.fillRoundRect(tx,ty-18,tw+8,16,4,4);
            g.setColor(SELECT_COLOR); g.drawString(sel.name,tx+4,ty-6);
            break;
        }
    }

    // ── Entrance markers ──────────────────────────────────────────────
    private void drawEntrances(Graphics2D g) {
        EditorState.EntranceEditState ee = state.getEntranceEdit();
        if (state.getCurrentTool() == EditorState.Tool.ENTRANCE && ee.ghost != null) {
            drawEntranceMarker(g, ee.ghost, ENTRANCE_GHOST, false);
        }
        for (EntranceData e : campusMap.getEntrances()) {
            boolean selected = (e == ee.selected);
            drawEntranceMarker(g, e, selected ? ENTRANCE_SEL : ENTRANCE_RING, selected);
        }
    }

    private void drawEntranceMarker(Graphics2D g, EntranceData e, Color ring, boolean selected) {
        int sx = camera.worldToScreenX(e.worldX);
        int sz = camera.worldToScreenY(e.worldZ);
        int r  = Math.max(8, (int)(e.triggerRadius * 0.5f));

        g.setColor(selected ? new Color(255,220,50,40) : ENTRANCE_FILL);
        g.fillOval(sx - r, sz - r, r*2, r*2);

        g.setColor(ring);
        g.setStroke(new BasicStroke(selected ? 2.5f : 1.8f));
        g.drawOval(sx - r, sz - r, r*2, r*2);
        g.setStroke(new BasicStroke(1f));

        g.setColor(ring);
        g.fillRect(sx - 7, sz - 10, 14, 18);
        g.setColor(selected ? Color.BLACK : new Color(0,0,0,120));
        g.drawRect(sx - 7, sz - 10, 14, 18);

        g.setColor(selected ? Color.BLACK : new Color(0,0,0,160));
        g.fillOval(sx + 2, sz - 2, 4, 4);

        if (e.id != null) {
            g.setFont(new Font("Consolas", Font.BOLD, 9));
            String top = e.id;
            String bot = e.interiorSceneId != null && !e.interiorSceneId.isEmpty()
                    ? "→ " + e.interiorSceneId : "→ (no scene — press C)";
            int lw = Math.max(g.getFontMetrics().stringWidth(top),
                    g.getFontMetrics().stringWidth(bot));
            int lx = sx - lw / 2 - 3;
            int ly = sz + r + 4;
            g.setColor(new Color(0,0,0,150));
            g.fillRoundRect(lx, ly, lw + 6, 26, 4, 4);
            g.setColor(selected ? ENTRANCE_SEL : ENTRANCE_LABEL);
            g.drawString(top, lx + 3, ly + 10);
            g.setColor(new Color(160,220,160,200));
            g.drawString(bot, lx + 3, ly + 21);
        }
    }

    // ── Scene picker ──────────────────────────────────────────────────
    private void drawScenePicker(Graphics2D g) {
        EditorState.EntranceEditState ee = state.getEntranceEdit();
        if (!ee.pickerOpen || ee.sceneIds.isEmpty()) return;

        int px = 20, py = 60;
        int rowH = 22, boxW = 260;
        int boxH = ee.sceneIds.size() * rowH + 28;

        g.setColor(new Color(10, 10, 20, 220));
        g.fillRoundRect(px - 4, py - 24, boxW + 8, boxH, 8, 8);
        g.setColor(new Color(255, 220, 50, 180));
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(px - 4, py - 24, boxW + 8, boxH, 8, 8);
        g.setStroke(new BasicStroke(1f));

        g.setFont(new Font("Consolas", Font.BOLD, 12));
        g.setColor(new Color(255, 220, 50));
        g.drawString("Assign scene to: " + ee.selected.id, px, py - 8);

        g.setFont(new Font("Consolas", Font.PLAIN, 12));
        for (int i = 0; i < ee.sceneIds.size(); i++) {
            int ry = py + i * rowH;
            boolean hovered = (i == ee.pickerHover);
            g.setColor(hovered ? new Color(80,180,100,200) : new Color(30,30,50,180));
            g.fillRect(px, ry, boxW, rowH - 2);
            g.setColor(hovered ? Color.BLACK : new Color(140,220,160));
            g.drawString(ee.sceneIds.get(i), px + 8, ry + 15);
            if (ee.sceneIds.get(i).equals(ee.selected.interiorSceneId)) {
                g.setColor(hovered ? Color.BLACK : new Color(80,220,120));
                g.drawString("✓", px + boxW - 20, ry + 15);
            }
        }

        g.setFont(new Font("Consolas", Font.PLAIN, 10));
        g.setColor(new Color(150, 150, 150));
        g.drawString("Click to assign  •  Esc to cancel",
                px, py + ee.sceneIds.size() * rowH + 14);
    }

    // ── Entrance legend ───────────────────────────────────────────────
    private void drawEntranceLegend(Graphics2D g, int screenW) {
        if (state.getCurrentTool() != EditorState.Tool.ENTRANCE) return;
        drawLegend(g, screenW, new String[]{
                "ENTRANCE TOOL",
                "Click empty = place door",
                "Click marker = select",
                "C = assign scene",
                "Drag = move door",
                "Del = delete selected",
                "Esc = deselect"
        });
    }

    // ── Shape edit ────────────────────────────────────────────────────
    private void drawShapeEdit(Graphics2D g, int screenW) {
        if (state.getCurrentTool()!=EditorState.Tool.SHAPE_EDIT) return;
        ShapeEditState se = state.getShapeEdit();
        if (!se.isActive()) return;
        int[] vx=se.getVerticesX(), vz=se.getVerticesZ();
        int n=vx.length; int[] sx=new int[n],sy=new int[n];
        for (int i=0;i<n;i++){sx[i]=camera.worldToScreenX(vx[i]);sy[i]=camera.worldToScreenY(vz[i]);}

        g.setColor(new Color(80,200,255,30)); g.fillPolygon(sx,sy,n);
        g.setColor(SHAPE_OUTLINE);
        g.setStroke(new BasicStroke(1.8f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND,
                1f,new float[]{6f,3f},0f));
        g.drawPolygon(sx,sy,n); g.setStroke(new BasicStroke(1f));

        g.setColor(MIDPOINT_HINT);
        for (int i=0;i<n;i++){int j=(i+1)%n;drawCross(g,(sx[i]+sx[j])/2,(sy[i]+sy[j])/2,5);}

        int hovered=se.getHoveredVertex(), selected=se.getSelectedVertex();
        for (int i=0;i<n;i++) {
            Color fill; int r;
            if(i==selected){fill=VERTEX_SELECTED;r=7;}
            else if(i==hovered){fill=VERTEX_HOVERED;r=7;}
            else{fill=VERTEX_NORMAL;r=5;}
            g.setColor(fill); g.fillOval(sx[i]-r,sy[i]-r,r*2,r*2);
            g.setColor(Color.BLACK); g.drawOval(sx[i]-r,sy[i]-r,r*2,r*2);
            g.setColor(new Color(255,255,255,160));
            g.setFont(new Font("Consolas",Font.BOLD,9));
            g.drawString(String.valueOf(i),sx[i]+r+2,sy[i]-r+8);
        }
        drawLegend(g, screenW, new String[]{"SHAPE PRESETS","1=Square","2=Rectangle",
                "3=Circle 8pt","4=Circle 16pt","5=New Polygon","",
                "L-drag=move vert","Mid=add vert","R-click=del vert","",
                "Enter=commit","Esc=cancel"});
    }

    // ── Path edit ─────────────────────────────────────────────────────
    private void drawPathEdit(Graphics2D g, int screenW) {
        if (state.getCurrentTool()!=EditorState.Tool.PATH_EDIT) return;
        PathEditState pe = state.getPathEdit();
        if (!pe.isActive()) return;
        float[] wx=pe.getWx(), wz=pe.getWz(); int n=wx.length;
        int[] sx=new int[n],sy=new int[n];
        for (int i=0;i<n;i++){sx[i]=camera.worldToScreenX(wx[i]);sy[i]=camera.worldToScreenY(wz[i]);}

        g.setColor(new Color(220,215,200,160));
        g.setStroke(new BasicStroke(8f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        for (int i=0;i<n-1;i++) g.drawLine(sx[i],sy[i],sx[i+1],sy[i+1]);
        g.setStroke(new BasicStroke(1f));

        g.setColor(MIDPOINT_HINT);
        for (int i=0;i<n-1;i++) drawCross(g,(sx[i]+sx[i+1])/2,(sy[i]+sy[i+1])/2,5);

        int hov=pe.getHoveredPoint(), sel=pe.getSelectedPoint();
        for (int i=0;i<n;i++) {
            Color fill; int r;
            if(i==sel){fill=VERTEX_SELECTED;r=7;}
            else if(i==hov){fill=VERTEX_HOVERED;r=7;}
            else{fill=new Color(220,180,80,220);r=5;}
            g.setColor(fill); g.fillOval(sx[i]-r,sy[i]-r,r*2,r*2);
            g.setColor(Color.BLACK); g.drawOval(sx[i]-r,sy[i]-r,r*2,r*2);
            g.setColor(new Color(255,255,255,160));
            g.setFont(new Font("Consolas",Font.BOLD,9));
            g.drawString(String.valueOf(i),sx[i]+r+2,sy[i]-r+8);
        }
        drawLegend(g, screenW, new String[]{"PATH EDIT","Click=add point","L-drag=move point",
                "Mid=insert point","R-click=del point","","Enter=finish","Esc=cancel"});
    }

    // ── Shared helpers ────────────────────────────────────────────────
    private void drawLegend(Graphics2D g, int screenW, String[] lines) {
        g.setFont(new Font("Consolas",Font.PLAIN,11));
        FontMetrics fm=g.getFontMetrics();
        int lineH=fm.getHeight()+1, boxW=190, boxH=lines.length*lineH+10;
        int boxX=screenW-boxW-10, boxY=38;
        g.setColor(LEGEND_BG); g.fillRoundRect(boxX,boxY,boxW,boxH,6,6);
        int ty=boxY+lineH;
        for (String line : lines) {
            boolean isHeader = line.equals("SHAPE PRESETS")
                    || line.equals("PATH EDIT")
                    || line.equals("ENTRANCE TOOL");
            if (isHeader) {
                g.setColor(new Color(255,230,80));
                g.setFont(new Font("Consolas",Font.BOLD,11));
            } else if (line.isEmpty()) {
                ty+=4; g.setFont(new Font("Consolas",Font.PLAIN,11));
                g.setColor(new Color(140,220,255));
            } else {
                g.setFont(new Font("Consolas",Font.PLAIN,11));
                g.setColor(new Color(140,220,255));
            }
            g.drawString(line, boxX+8, ty); ty+=lineH;
        }
    }

    private void drawCross(Graphics2D g, int cx, int cy, int arm) {
        g.drawLine(cx-arm,cy,cx+arm,cy); g.drawLine(cx,cy-arm,cx,cy+arm);
    }

    private void drawStatus(Graphics2D g, int screenW, int screenH) {
        String msg=state.getStatusMessage(); if(msg.isEmpty()) return;
        g.setFont(new Font("Consolas",Font.BOLD,13));
        FontMetrics fm=g.getFontMetrics(); int tw=fm.stringWidth(msg);
        int tx=screenW/2-tw/2, ty=screenH-20;
        g.setColor(STATUS_BG); g.fillRoundRect(tx-8,ty-16,tw+16,22,6,6);
        g.setColor(new Color(100,255,140)); g.drawString(msg,tx,ty);
    }

    // ── Tool HUD ──────────────────────────────────────────────────────
    private void drawToolHUD(Graphics2D g, int screenW, int screenH) {
        if (state.getCurrentTool() == EditorState.Tool.SHAPE_EDIT
                || state.getCurrentTool() == EditorState.Tool.PATH_EDIT
                || state.getCurrentTool() == EditorState.Tool.ENTRANCE) return;
        EditorState.Tool cur = state.getCurrentTool();
        String[] tools = {"[Q] Select","[W] Move","[E] Resize","[R] Rotate","[T] Path"};
        EditorState.Tool[] toolVals = {
                EditorState.Tool.SELECT, EditorState.Tool.MOVE,
                EditorState.Tool.RESIZE, EditorState.Tool.ROTATE,
                EditorState.Tool.PATH_EDIT
        };
        g.setFont(new Font("Consolas", Font.BOLD, 11));
        FontMetrics fm = g.getFontMetrics();
        int pad = 8, gap = 6;
        int startX = 10;
        int startY = screenH - 38;
        for (int i = 0; i < tools.length; i++) {
            int w = fm.stringWidth(tools[i]) + pad * 2;
            boolean active = (cur == toolVals[i]);
            g.setColor(active ? new Color(255, 220, 50, 220) : new Color(20, 20, 20, 190));
            g.fillRoundRect(startX, startY, w, 22, 5, 5);
            g.setColor(active ? new Color(50, 40, 0) : new Color(180, 180, 180));
            g.drawRoundRect(startX, startY, w, 22, 5, 5);
            g.setColor(active ? new Color(30, 20, 0) : new Color(200, 200, 200));
            g.drawString(tools[i], startX + pad, startY + 15);
            startX += w + gap;
        }
    }

}