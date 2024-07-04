package io.heraldprox.herald.sensor.analysis.evaluation;

import io.heraldprox.herald.sensor.datatype.Grid2DLocationReference;
import io.heraldprox.herald.sensor.datatype.Grid3DLocationReference;

public class Resolver2D {
//    static Grid2DLocationReference estimatePosition(Grid3DLocationReference b1Pos, Grid3DLocationReference b2Pos, CorrectedRSSI meanB1Rssi, CorrectedRSSI meanB2Rssi) {
//        double xp = b2Pos.getX() +
//                ((Math.pow(meanB2Rssi.doubleValue(),2) - Math.pow(b2Pos.getX(),2) - Math.pow(meanB1Rssi.doubleValue(),2))
//                    /
//                    (2 * b2Pos.getX()));
//        // TODO handle situation where values passed to sqrt are calculated as negative values
//        double yp = b1Pos.getY() + Math.sqrt(Math.pow(meanB1Rssi.doubleValue(),2) - Math.pow(xp - b1Pos.getX(),2));
//
//        return new Grid2DLocationReference(xp,yp);
//    }


    static Grid2DLocationReference estimatePosition(Grid3DLocationReference b1Pos, Grid3DLocationReference b2Pos, double dR1, double dR2) {

        // Determine chord length between B1 and B2 in terms of real coordinates
        double dX = b2Pos.getX() - b1Pos.getX();
        double dY = b2Pos.getY() - b2Pos.getY();
        double Lb1b2 = Math.sqrt(Math.pow(dX, 2) + Math.pow(dY,2));

        // Let x = Cos alpha (Angle of B2 to P), Let y = Cos theta (Angle of B1 to P)
        // Determine x and y in terms of dR1 and dR2 only
        double x = (Math.pow(Lb1b2,2) - Math.pow(dR1,2) + Math.pow(dR2,2)) /
                (2.0 * Lb1b2 * dR2);
        double alpha = Math.acos(x);
        double Lb2 = dR2 * x;

        double y = (Lb1b2 - (dR2 * x)) / dR1;
        double theta = Math.acos(y);
        double Lb1 = dR1 * y;

        // now determine angle in real coords between B1 and B2
        double tau = Math.atan(dX / dY);
        // Now determine angle from P to X plane
        double gamma = Math.PI - theta - tau;
        // Now determine difference in X and Y coords from B1pos
        double dPx = dR1 * Math.sin(gamma);
        double dPy = dR1 * Math.cos(gamma);

        // Now finally calculate P's X and Y coordinates
        return new Grid2DLocationReference(b1Pos.getX() + dPx,b1Pos.getY() + dPy);
    }

    static Grid2DLocationReference estimatePosition(Grid3DLocationReference b1Pos, Grid3DLocationReference b2Pos, CorrectedRSSI meanB1Rssi, CorrectedRSSI meanB2Rssi) {

        // Convert corrected mean RSSI to distance using Friis formula
        double c = 299702547; // ms in air
        double factor =  (c / (4 * Math.PI * 2436 * 1000000));
        double dR1 = Math.exp(0.05*meanB1Rssi.doubleValue()) / factor;
        double dR2 = Math.exp(0.05*meanB2Rssi.doubleValue()) / factor;

        System.out.println("dR1=" + dR1 + ", dR2=" + dR2);

        return estimatePosition(b1Pos,b2Pos,dR1,dR2);

    }
}
