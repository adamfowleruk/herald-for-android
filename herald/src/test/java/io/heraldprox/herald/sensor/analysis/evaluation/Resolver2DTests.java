package io.heraldprox.herald.sensor.analysis.evaluation;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

import io.heraldprox.herald.sensor.datatype.Grid2DLocationReference;
import io.heraldprox.herald.sensor.datatype.Grid3DLocationReference;

public class Resolver2DTests {

    @Test
    public void testB1AtZeroB2SameY() throws Exception {
        Grid3DLocationReference b1Pos = new Grid3DLocationReference(0,0,0,0,0,0);
        Grid3DLocationReference b2Pos = new Grid3DLocationReference(2,0,0,0,0,0);

        double expectedXp = 1.5;
        double expectedYp = 0.5;

        CorrectedRSSI b1MeanRssi = new CorrectedRSSI(-33.38);
        CorrectedRSSI b2MeanRssi = new CorrectedRSSI(-26.85);

//        Grid2DLocationReference calc = Resolver2D.estimatePosition(b1Pos,b2Pos,b1MeanRssi,b2MeanRssi);
        Grid2DLocationReference calc = Resolver2D.estimatePosition(b1Pos,b2Pos,1.58114,0.707);

        double diffX = calc.getX() - expectedXp;
        double diffY = calc.getY() - expectedYp;
        System.out.println("X=" + calc.getX() + ", Y=" + calc.getY() + ", diffX=" + diffX + ", diffY=" + diffY);
        assertTrue("X out of range by " + diffX + " at x=" + calc.getX(),Math.abs(diffX) <= 0.01);
        assertTrue("Y out of range by " + diffY + " at y=" + calc.getY(),Math.abs(diffY) <= 0.01);
    }


}
