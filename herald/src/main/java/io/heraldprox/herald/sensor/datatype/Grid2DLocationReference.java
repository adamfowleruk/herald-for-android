package io.heraldprox.herald.sensor.datatype;

import androidx.annotation.NonNull;

public class Grid2DLocationReference implements LocationReference {
    protected double x = 0;
    protected double y = 0;

    protected double xSd = -1;
    protected double ySd = -1;

    public Grid2DLocationReference(double x, double y) {
        this.x = x;
        this.y = y;
    }
    public Grid2DLocationReference(double x, double y, double xSd, double ySd) {
        this.x = x;
        this.y = y;
        this.xSd = xSd;
        this.ySd = ySd;
    }
    @NonNull
    @Override
    public String description() {
        return "Grid2D, x: " + x + ", y: " + y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getXSd() { return xSd; }

    public double getYSd() {
        return ySd;
    }

}
