package com.campusgame.renderer.projection;

public class RenderObject implements Comparable<RenderObject> {
    public final float     sortZ;
    public final Runnable  drawCall;

    public RenderObject(float sortZ, Runnable drawCall) {
        this.sortZ    = sortZ;
        this.drawCall = drawCall;
    }

    @Override public int compareTo(RenderObject o) {
        return Float.compare(this.sortZ, o.sortZ);
    }
}