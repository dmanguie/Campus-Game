package com.campusgame.editor;

import com.campusgame.map.data.PathData;

public class PathEditState {

    public static final int HANDLE_RADIUS = 9;

    private float[]  wx;
    private float[]  wz;
    private PathData source;
    private boolean  active        = false;
    private int      hoveredPoint  = -1;
    private int      selectedPoint = -1;
    private boolean  dragging      = false;

    public void beginEdit(PathData path) {
        source=path; active=true; hoveredPoint=-1; selectedPoint=-1; dragging=false;
        copyFrom(path);
    }

    public void endEdit() {
        active=false; source=null; wx=wz=null;
        hoveredPoint=selectedPoint=-1; dragging=false;
    }

    private void copyFrom(PathData path) {
        int n=path.points.size(); wx=new float[n]; wz=new float[n];
        for (int i=0; i<n; i++) { wx[i]=path.points.get(i)[0]; wz[i]=path.points.get(i)[1]; }
    }

    public void appendPoint(float x, float z)      { wx=append(wx,x); wz=append(wz,z); }

    public void insertPointAfter(int edgeIndex) {
        int n=wx.length, j=(edgeIndex+1)%n;
        float mx=(wx[edgeIndex]+wx[j])/2f, mz=(wz[edgeIndex]+wz[j])/2f;
        wx=insertAt(wx,edgeIndex+1,mx); wz=insertAt(wz,edgeIndex+1,mz);
    }

    public void deletePoint(int idx) {
        if (wx==null || wx.length<=2) return;
        wx=removeAt(wx,idx); wz=removeAt(wz,idx);
        if (selectedPoint==idx || selectedPoint>=wx.length) selectedPoint=-1;
        if (hoveredPoint ==idx || hoveredPoint >=wx.length) hoveredPoint=-1;
    }

    public void movePoint(int idx, float x, float z) {
        if (idx<0||wx==null||idx>=wx.length) return; wx[idx]=x; wz[idx]=z;
    }

    public int findNearestPoint(int sx, int sy, int[] svx, int[] svz) {
        int best=-1; double best2=(double)HANDLE_RADIUS*HANDLE_RADIUS;
        for (int i=0; i<svx.length; i++) {
            double dx=svx[i]-sx,dz=svz[i]-sy,d2=dx*dx+dz*dz;
            if (d2<best2) { best2=d2; best=i; }
        }
        return best;
    }

    public int findNearestEdgeMidpoint(int sx, int sy, int[] svx, int[] svz) {
        int best=-1; double best2=(double)HANDLE_RADIUS*HANDLE_RADIUS;
        for (int i=0; i<svx.length-1; i++) {
            int mx=(svx[i]+svx[i+1])/2, mz=(svz[i]+svz[i+1])/2;
            double dx=mx-sx,dz=mz-sy,d2=dx*dx+dz*dz;
            if (d2<best2) { best2=d2; best=i; }
        }
        return best;
    }

    public void applyTo(PathData path) {
        path.points.clear();
        for (int i=0; i<wx.length; i++) path.addPoint(wx[i],wz[i]);
    }

    public boolean  isActive()              { return active; }
    public PathData getSource()             { return source; }
    public float[]  getWx()                 { return wx; }
    public float[]  getWz()                 { return wz; }
    public int      getHoveredPoint()       { return hoveredPoint; }
    public void     setHoveredPoint(int i)  { hoveredPoint=i; }
    public int      getSelectedPoint()      { return selectedPoint; }
    public void     setSelectedPoint(int i) { selectedPoint=i; }
    public boolean  isDragging()            { return dragging; }
    public void     setDragging(boolean v)  { dragging=v; }

    private float[] append(float[] arr, float v) {
        float[] n=new float[arr.length+1]; System.arraycopy(arr,0,n,0,arr.length); n[arr.length]=v; return n;
    }
    private float[] insertAt(float[] arr, int pos, float v) {
        float[] n=new float[arr.length+1];
        System.arraycopy(arr,0,n,0,pos); n[pos]=v; System.arraycopy(arr,pos,n,pos+1,arr.length-pos); return n;
    }
    private float[] removeAt(float[] arr, int idx) {
        float[] n=new float[arr.length-1];
        for (int i=0,k=0; i<arr.length; i++) { if(i!=idx) n[k++]=arr[i]; } return n;
    }
}