package io.heraldprox.herald.sensor.analysis.evaluation;

import java.util.Hashtable;

import io.heraldprox.herald.sensor.data.TextFile;
import io.heraldprox.herald.sensor.datatype.Date;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;
import io.heraldprox.herald.sensor.datatype.Tuple;

/**
 * Interface defining a class which provides calibrated values based on prior data analysis.
 *
 * Note: ALPHA QUALITY CODE UNTIL PROVEN IN EXPERIMENTS.
 */
public interface Calibrator {

    /**
     * Update, or create from scratch, internal calibration values based on a new data window.
     *
     * @param fromExclusive
     * @param toInclusive
     * @param records
     */
    public void updateCalibration(Date fromExclusive, Date toInclusive, final Hashtable<TargetIdentifier,DeviceCalibrationRecord> records);

    /**
     * Apply the current calibration to the specified value, producing a 'corrected RSSI' output, Cr.
     *
     * @param recorded
     * @param toCorrect
     * @return
     */
    public Double calibrateValue(Date recorded, Tuple<Double, Date> toCorrect);
}
