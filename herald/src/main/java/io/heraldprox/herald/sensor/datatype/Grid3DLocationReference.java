package io.heraldprox.herald.sensor.datatype;

import androidx.annotation.NonNull;

public class Grid3DLocationReference implements LocationReference {
    protected double x = 0;
    protected double y = 0;
    protected double z = 0;

    public Grid3DLocationReference(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
;    }
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
}
