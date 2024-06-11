package io.heraldprox.herald.sensor.analysis.evaluation;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import io.heraldprox.herald.sensor.analysis.aggregates.Mean;
import io.heraldprox.herald.sensor.data.TextFile;
import io.heraldprox.herald.sensor.datatype.Data;
import io.heraldprox.herald.sensor.datatype.Date;
import io.heraldprox.herald.sensor.datatype.Grid3DLocationReference;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;
import io.heraldprox.herald.sensor.datatype.Tuple;

/**
 * Calculates a 3D position within a beacon field.
 *
 * Note: ALPHA QUALITY CODE UNTIL PROVEN IN EXPERIMENTS.
 */
public class BeaconFieldPositionCalculator {
    protected int interval = 30; // seconds
    protected Date lastRun = new Date();

    protected ArrayList<Tuple<Date,Grid3DLocationReference>> positionsOverTime = new ArrayList<>();

    public BeaconFieldPositionCalculator(int secondsPerAnalysis) {
        this.interval = secondsPerAnalysis;
    }

    public void update(Date nowDate, final Hashtable<TargetIdentifier,DeviceCalibrationRecord> records, final TextFile writeTo) {
        /* Specified nowDate to allow class testing */

        // Recalculate position if time has passed since last run
        long endTime = lastRun.secondsSinceUnixEpoch() + interval;
        if (endTime > nowDate.secondsSinceUnixEpoch()) {
            return;
        }
        Date endTimeDate = new Date(endTime);

        // TODO use spring physical model to calculate location within the beacon field
        int beaconCountWithData = 0;
        // Note: Need at least 3 beacons with enough data to ensure we can triangulate position
        // TODO determine if there's a better algorithm than using all readings. E.g. 3 strongest beacons or those with most data
        Frequencies<CorrectedRSSI> values = new Frequencies<CorrectedRSSI>();
        ArrayList<Tuple<Grid3DLocationReference,Double>> averageCorrectedRssiFrom = new ArrayList<Tuple<Grid3DLocationReference, Double>>();
        for (DeviceCalibrationRecord record : records.values()) {
            values.clear();
            record.addFromWindowIfBeacon(lastRun,endTimeDate, values);
            if (values.size() < 5) { // size() in this context is the number of unique CorrectedRSSI values, NOT number of readings (frequency)
                continue;
            }
            beaconCountWithData++;
            MeanCorrectedRSSICallback cb = new MeanCorrectedRSSICallback();
            values.getAll(cb);
//            double sumCorrectedRssi = 0.0;
//            for (Tuple<Double,Date> value : values) {
//                sumCorrectedRssi += value.a;
//            }
//            averageCorrectedRssiFrom.add(new Tuple<>(record.getPosition(),sumCorrectedRssi/values.size()));
            averageCorrectedRssiFrom.add(new Tuple<>(record.getPosition(),cb.mean()));
        }

        // TODO calculate actual spring / force directed position rather than hardcoding a value
//        positionsOverTime.add(new Tuple<>(endTimeDate,new Grid3DLocationReference(1,2,3))); // TODO CALCULATE THIS!!!

        // Algorithm: Instead of treating this as a 3D intersection of all spherical ranges,
        // consider it instead as an average of a pairwise of 3D positions calculated from the
        // centrepoint of the chords of intersection 2D ranges in each of 3 plans (xy,xz,yz)
        // For each pair then you can use simultaneous linear equations to calculate a centre point.
        // Now take the mean centrepoint across all of these pairwise calculations.
        Vector<Grid3DLocationReference> positions = new Vector<>();
        for (int i = 0;i < averageCorrectedRssiFrom.size() - 1;i++) {
            Tuple<Grid3DLocationReference,Double> firstRanging = averageCorrectedRssiFrom.get(i);
            Tuple<Grid3DLocationReference,Double> secondRanging = averageCorrectedRssiFrom.get(i + 1);

            // X,Y plane first
            double theta = Math.atan( (secondRanging.a.getY() - firstRanging.a.getY()) / (secondRanging.a.getX() - firstRanging.a.getX()));
            double c1x = firstRanging.a.getX() + (firstRanging.b * Math.cos(theta));
            double c1y = firstRanging.a.getY() + (firstRanging.b * Math.sin(theta));
            double c2x = secondRanging.a.getX() - (secondRanging.b * Math.cos(theta));
            double c2y = secondRanging.a.getY() - (secondRanging.b * Math.sin(theta));
            double cx = (c2x + c1x) / 2.0;
            double cy = (c2y + c1y) / 2.0;
            double cz = (firstRanging.a.getZ() + secondRanging.a.getZ()) / 2.0;
            positions.add(new Grid3DLocationReference(cx,cy,cz));

            // Y,Z plane
            theta = Math.atan( (secondRanging.a.getY() - firstRanging.a.getY()) / (secondRanging.a.getZ() - firstRanging.a.getZ()));
            c1y = firstRanging.a.getY() + (firstRanging.b * Math.cos(theta));
            double c1z = firstRanging.a.getZ() + (firstRanging.b * Math.sin(theta));
            c2y = secondRanging.a.getY() - (secondRanging.b * Math.cos(theta));
            double c2z = secondRanging.a.getZ() - (secondRanging.b * Math.sin(theta));
            cy = (c2y + c1y) / 2.0;
            cz = (c2z + c1z) / 2.0;
            cx = (firstRanging.a.getX() + secondRanging.a.getX()) / 2.0;
            positions.add(new Grid3DLocationReference(cx,cy,cz));

            // X,Z plane
            theta = Math.atan( (secondRanging.a.getX() - firstRanging.a.getX()) / (secondRanging.a.getZ() - firstRanging.a.getZ()));
            c1x = firstRanging.a.getX() + (firstRanging.b * Math.cos(theta));
            c1z = firstRanging.a.getZ() + (firstRanging.b * Math.sin(theta));
            c2x = secondRanging.a.getX() - (secondRanging.b * Math.cos(theta));
            c2z = secondRanging.a.getZ() - (secondRanging.b * Math.sin(theta));
            cx = (c2x + c1x) / 2.0;
            cz = (c2z + c1z) / 2.0;
            cy = (firstRanging.a.getY() + secondRanging.a.getY()) / 2.0;
            positions.add(new Grid3DLocationReference(cx,cy,cz));
        }

        // Don't forget the final pair - last and first elements of the arraylist
        if (averageCorrectedRssiFrom.size() >= 2) {
            Tuple<Grid3DLocationReference,Double> firstRanging = averageCorrectedRssiFrom.get(0);
            Tuple<Grid3DLocationReference,Double> secondRanging = averageCorrectedRssiFrom.get(averageCorrectedRssiFrom.size() - 1);

            // JUST COPY PASTE CODE FOR NOW - QUICKER

            // X,Y plane first
            double theta = Math.atan( (secondRanging.a.getY() - firstRanging.a.getY()) / (secondRanging.a.getX() - firstRanging.a.getX()));
            double c1x = firstRanging.a.getX() + (firstRanging.b * Math.cos(theta));
            double c1y = firstRanging.a.getY() + (firstRanging.b * Math.sin(theta));
            double c2x = secondRanging.a.getX() - (secondRanging.b * Math.cos(theta));
            double c2y = secondRanging.a.getY() - (secondRanging.b * Math.sin(theta));
            double cx = (c2x + c1x) / 2.0;
            double cy = (c2y + c1y) / 2.0;
            double cz = (firstRanging.a.getZ() + secondRanging.a.getZ()) / 2.0;
            positions.add(new Grid3DLocationReference(cx,cy,cz));

            // Y,Z plane
            theta = Math.atan( (secondRanging.a.getY() - firstRanging.a.getY()) / (secondRanging.a.getZ() - firstRanging.a.getZ()));
            c1y = firstRanging.a.getY() + (firstRanging.b * Math.cos(theta));
            double c1z = firstRanging.a.getZ() + (firstRanging.b * Math.sin(theta));
            c2y = secondRanging.a.getY() - (secondRanging.b * Math.cos(theta));
            double c2z = secondRanging.a.getZ() - (secondRanging.b * Math.sin(theta));
            cy = (c2y + c1y) / 2.0;
            cz = (c2z + c1z) / 2.0;
            cx = (firstRanging.a.getX() + secondRanging.a.getX()) / 2.0;
            positions.add(new Grid3DLocationReference(cx,cy,cz));

            // X,Z plane
            theta = Math.atan( (secondRanging.a.getX() - firstRanging.a.getX()) / (secondRanging.a.getZ() - firstRanging.a.getZ()));
            c1x = firstRanging.a.getX() + (firstRanging.b * Math.cos(theta));
            c1z = firstRanging.a.getZ() + (firstRanging.b * Math.sin(theta));
            c2x = secondRanging.a.getX() - (secondRanging.b * Math.cos(theta));
            c2z = secondRanging.a.getZ() - (secondRanging.b * Math.sin(theta));
            cx = (c2x + c1x) / 2.0;
            cz = (c2z + c1z) / 2.0;
            cy = (firstRanging.a.getY() + secondRanging.a.getY()) / 2.0;
            positions.add(new Grid3DLocationReference(cx,cy,cz));
        }

        // Now calculate average position over all positions
        // Note: Will be skewed 1/3 toward position between each beacon, so important we have good beacon spread
        double xTotal = 0;
        double yTotal = 0;
        double zTotal = 0;
        for (Grid3DLocationReference ref : positions) {
            xTotal += ref.getX();
            yTotal += ref.getY();
            zTotal += ref.getZ();
        }
        double xMean = xTotal / positions.size();
        double yMean = yTotal / positions.size();
        double zMean = zTotal / positions.size();

        double xDiffTotal = 0;
        double yDiffTotal = 0;
        double zDiffTotal = 0;

        for (Grid3DLocationReference ref : positions) {
            xDiffTotal += Math.pow(ref.getX() - xMean,2);
            yDiffTotal += Math.pow(ref.getY() - yMean,2);
            zDiffTotal += Math.pow(ref.getZ() - zMean,2);
        }
        // Sample standard deviation
        double xSD = Math.sqrt(xDiffTotal / (positions.size() - 1));
        double ySD = Math.sqrt(yDiffTotal / (positions.size() - 1));
        double zSD = Math.sqrt(zDiffTotal / (positions.size() - 1));

        Grid3DLocationReference estimatedPosition = new Grid3DLocationReference(
                xMean,
                yMean,
                zMean
        );

        // now record this estimate
        positionsOverTime.add(new Tuple<Date,Grid3DLocationReference>(endTimeDate,estimatedPosition));
        writeTo.write(endTimeDate + "," + estimatedPosition.getX() + "," + estimatedPosition.getY() + "," + estimatedPosition.getZ() +
                "," + xSD + "," + ySD + "," + zSD);

        // set new lastRun
        lastRun = endTimeDate;
    }

    public List<Tuple<Date,Grid3DLocationReference>> getPositionHistory() {
        return positionsOverTime;
    }

    public void clearPositionDateBefore(Date onOrBefore) {
        // remove old data to prevent memory starvation over long simulations
        ArrayList<Tuple<Date,Grid3DLocationReference>> newPositionsOverTime = new ArrayList<>();
        for (Tuple<Date,Grid3DLocationReference> position: positionsOverTime) {
            if (position.a.secondsSinceUnixEpoch() > onOrBefore.secondsSinceUnixEpoch()) {
                newPositionsOverTime.add(position);
            }
        }
        positionsOverTime = newPositionsOverTime;
    }
}
