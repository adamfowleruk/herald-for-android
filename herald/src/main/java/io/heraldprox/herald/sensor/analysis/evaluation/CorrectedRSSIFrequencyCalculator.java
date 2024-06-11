package io.heraldprox.herald.sensor.analysis.evaluation;

import java.util.Hashtable;

import io.heraldprox.herald.sensor.data.TextFile;
import io.heraldprox.herald.sensor.datatype.Date;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;

/**
 * Calculates relative frequencies of corrected RSSI values over all valid nearby devices.
 *
 * Note: ALPHA QUALITY CODE UNTIL PROVEN IN EXPERIMENTS.
 *
 * @deprecated Not to be used - PolyCalibrator performs the windowing itself on the raw data
 */
public class CorrectedRSSIFrequencyCalculator {

    // note that our endDateInclusive time will be the TimeSeries index time
    TimeSeries<Frequencies<CorrectedRSSI>> frequencies = new TimeSeries<>();
    protected long interval = 5;
    protected long window = 30;

    protected boolean confirmedValidOnly = false;

    public CorrectedRSSIFrequencyCalculator(boolean confirmedValidOnly) {
        this.confirmedValidOnly = confirmedValidOnly;
    }

    public CorrectedRSSIFrequencyCalculator(long updateEverySeconds, long windowSizeSeconds) {
        this.interval = updateEverySeconds;
        this.window = windowSizeSeconds;
    }

    public void update(Date nowTime, final Hashtable<TargetIdentifier,DeviceCalibrationRecord> records, final TextFile writeTo) {
        // TODO modify DeviceCalibrationRecord to use TimeSeries indexed data
        // TODO implement this method based on confirmedValidOnly

        // TODO perform update calculations and save RSSI histogram data
        // call DeviceCalibrationRecord.addFromWindowIfValid() and aggregate data up
    }
}
