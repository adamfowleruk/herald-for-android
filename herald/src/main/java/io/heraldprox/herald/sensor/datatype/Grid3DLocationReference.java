package io.heraldprox.herald.sensor.datatype;

import androidx.annotation.NonNull;

public class Grid3DLocationReference implements LocationReference {
    protected double x = 0;
    protected double y = 0;
    protected double z = 0;

    protected double xSd = -1;
    protected double ySd = -1;
    protected double zSd = -1;

    public Grid3DLocationReference(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    public Grid3DLocationReference(double x, double y, double z, double xSd, double ySd, double zSd) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.xSd = xSd;
        this.ySd = ySd;
        this.zSd = zSd;
    }
    @NonNull
    @Override
    public String description() {
        return "Grid3D, x: " + x + ", y: " + y + ", z: " + z;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public double getXSd() { return xSd; }

    public double getYSd() {
        return ySd;
    }

    public double getZSd() {
        return zSd;
    }
}
