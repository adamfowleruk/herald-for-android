package io.heraldprox.herald.sensor.analysis.evaluation;

/**
 * Tagging class that represents a Corrected RSSI PRIOR to calibration. (Hence Long and not double type).
 *
 * Note: ALPHA QUALITY CODE UNTIL PROVEN IN EXPERIMENTS.
 */
public class CorrectedRSSI implements Comparable<CorrectedRSSI> {
    protected Long cr;

    public CorrectedRSSI(Long value) {
        this.cr = value;
    }

    public CorrectedRSSI(double toApproximate) {
        this.cr = Long.valueOf((long)toApproximate);
    }

    public long longValue() {
        return cr.longValue();
    }

    public double doubleValue() {
        return cr.doubleValue();
    }

    public Long getValue() {
        return cr;
    }

    @Override
    public int compareTo(CorrectedRSSI o) {
        return cr.compareTo(o.cr);
    }

    public String toString() {
        return cr.toString();
    }
}
