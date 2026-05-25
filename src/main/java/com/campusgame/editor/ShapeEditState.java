package com.campusgame.editor;

import com.campusgame.map.data.BuildingData;

public class ShapeEditState {

    public enum ShapePreset { SQUARE, RECTANGLE, CIRCLE_8, CIRCLE_16, POLYGON }

    public static final int HANDLE_RADIUS = 9;

    private int[]        verticesX;
    private int[]        verticesZ;
    private boolean      polygonMode;
    private int          hoveredVertex  = -1;
    private int          selectedVertex = -1;
    private boolean      dragging       = false;
    private BuildingData source         = null;
    private boolean      active         = false;
    private boolean appendMode = false;
    public boolean isAppendMode()          { return appendMode; }
    public void    setAppendMode(boolean v){ appendMode = v; }

    public void beginEdit(BuildingData b) {
        source = b; active = true; hoveredVertex = -1; selectedVertex = -1; dragging = false;
        if (b.isPolygon()) {
            verticesX = b.polygonX.clone(); verticesZ = b.polygonZ.clone(); polygonMode = true;
        } else {
            int x1=(int)b.x, z1=(int)b.z, x2=(int)(b.x+b.width), z2=(int)(b.z+b.depth);
            verticesX = new int[]{x1,x2,x2,x1};
            verticesZ = new int[]{z1,z1,z2,z2};
            polygonMode = false;
        }
    }

    public void endEdit() {
        active=false; source=null; verticesX=null; verticesZ=null;
        hoveredVertex=-1; selectedVertex=-1; dragging=false;
        appendMode = false;
    }

    public void applyPreset(ShapePreset preset, BuildingData current) {
        float cx, cz, hw, hd;
        if (current.isPolygon()) {
            cx = centroid(current.polygonX); cz = centroid(current.polygonZ); hw = hd = 80f;
        } else {
            cx = current.x + current.width/2f; cz = current.z + current.depth/2f;
            hw = current.width/2f; hd = current.depth/2f;
        }
        switch (preset) {
            case SQUARE -> {
                float s = Math.max(hw, hd);
                verticesX = new int[]{(int)(cx-s),(int)(cx+s),(int)(cx+s),(int)(cx-s)};
                verticesZ = new int[]{(int)(cz-s),(int)(cz-s),(int)(cz+s),(int)(cz+s)};
                polygonMode = false;
            }
            case RECTANGLE -> {
                verticesX = new int[]{(int)(cx-hw),(int)(cx+hw),(int)(cx+hw),(int)(cx-hw)};
                verticesZ = new int[]{(int)(cz-hd),(int)(cz-hd),(int)(cz+hd),(int)(cz+hd)};
                polygonMode = false;
            }
            case CIRCLE_8  -> buildCircle(cx, cz, Math.max(hw, hd), 8);
            case CIRCLE_16 -> buildCircle(cx, cz, Math.max(hw, hd), 16);
            case POLYGON -> {
                verticesX = new int[]{(int)cx,(int)(cx+hw),(int)(cx-hw)};
                verticesZ = new int[]{(int)(cz-hd),(int)(cz+hd),(int)(cz+hd)};
                polygonMode = true;
            }
        }
    }

    private void buildCircle(float cx, float cz, float r, int n) {
        verticesX = new int[n]; verticesZ = new int[n];
        for (int i = 0; i < n; i++) {
            double a = 2*Math.PI*i/n - Math.PI/2;
            verticesX[i] = (int)(cx + r*Math.cos(a));
            verticesZ[i] = (int)(cz + r*Math.sin(a));
        }
        polygonMode = true;
    }

    private float centroid(int[] arr) { float s=0; for(int v:arr) s+=v; return s/arr.length; }

    public int findNearestVertex(int sx, int sy, int[] svx, int[] svz) {
        int best=-1; double best2=(double)HANDLE_RADIUS*HANDLE_RADIUS;
        for (int i=0; i<svx.length; i++) {
            double dx=svx[i]-sx, dz=svz[i]-sy, d2=dx*dx+dz*dz;
            if (d2 < best2) { best2=d2; best=i; }
        }
        return best;
    }

    public int findNearestEdgeMidpoint(int sx, int sy, int[] svx, int[] svz) {
        int best=-1; double best2=(double)HANDLE_RADIUS*HANDLE_RADIUS; int n=svx.length;
        for (int i=0; i<n; i++) {
            int j=(i+1)%n, mx=(svx[i]+svx[j])/2, mz=(svz[i]+svz[j])/2;
            double dx=mx-sx, dz=mz-sy, d2=dx*dx+dz*dz;
            if (d2 < best2) { best2=d2; best=i; }
        }
        return best;
    }

    public void insertVertexAfterEdge(int edgeIndex) {
        int n=verticesX.length, i=edgeIndex, j=(edgeIndex+1)%n;
        int mx=(verticesX[i]+verticesX[j])/2, mz=(verticesZ[i]+verticesZ[j])/2;
        int[] nx=new int[n+1], nz=new int[n+1];
        System.arraycopy(verticesX,0,nx,0,i+1); nx[i+1]=mx;
        System.arraycopy(verticesX,i+1,nx,i+2,n-i-1);
        System.arraycopy(verticesZ,0,nz,0,i+1); nz[i+1]=mz;
        System.arraycopy(verticesZ,i+1,nz,i+2,n-i-1);
        verticesX=nx; verticesZ=nz; polygonMode=true;
    }

    public void deleteVertex(int idx) {
        if (verticesX==null || verticesX.length<=3) return;
        int n=verticesX.length;
        int[] nx=new int[n-1], nz=new int[n-1];
        for (int i=0,k=0; i<n; i++) { if(i==idx) continue; nx[k]=verticesX[i]; nz[k++]=verticesZ[i]; }
        verticesX=nx; verticesZ=nz;
        if (selectedVertex==idx || selectedVertex>=verticesX.length) selectedVertex=-1;
        if (hoveredVertex ==idx || hoveredVertex >=verticesX.length) hoveredVertex=-1;
    }

    public void moveVertex(int idx, int wx, int wz) {
        if (idx<0 || verticesX==null || idx>=verticesX.length) return;
        verticesX[idx]=wx; verticesZ[idx]=wz;
    }

    public boolean      isActive()               { return active; }
    public BuildingData getSource()              { return source; }
    public int[]        getVerticesX()           { return verticesX; }
    public int[]        getVerticesZ()           { return verticesZ; }
    public boolean      isPolygonMode()          { return polygonMode; }
    public int          getHoveredVertex()       { return hoveredVertex; }
    public void         setHoveredVertex(int i)  { hoveredVertex=i; }
    public int          getSelectedVertex()      { return selectedVertex; }
    public void         setSelectedVertex(int i) { selectedVertex=i; }
    public boolean      isDragging()             { return dragging; }
    public void         setDragging(boolean v)   { dragging=v; }
}