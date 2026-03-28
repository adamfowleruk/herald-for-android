package io.heraldprox.herald.sensor.analysis.evaluation;

import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Hashtable;
import java.util.List;
import java.util.Objects;

import io.heraldprox.herald.sensor.SensorDelegate;
import io.heraldprox.herald.sensor.SensorMetadata;
import io.heraldprox.herald.sensor.data.TextFile;
import io.heraldprox.herald.sensor.datatype.Callback;
import io.heraldprox.herald.sensor.datatype.Date;
import io.heraldprox.herald.sensor.datatype.Grid3DLocationReference;
import io.heraldprox.herald.sensor.datatype.ImmediateSendData;
import io.heraldprox.herald.sensor.datatype.Location;
import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.Proximity;
import io.heraldprox.herald.sensor.datatype.SensorState;
import io.heraldprox.herald.sensor.datatype.SensorType;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;
import io.heraldprox.herald.sensor.datatype.Tuple;
import io.heraldprox.herald.sensor.datatype.UInt16;
import io.heraldprox.herald.sensor.datatype.UInt8;
import io.heraldprox.herald.sensor.payload.extended.ConcreteExtendedDataSectionCodesV1;
import io.heraldprox.herald.sensor.payload.extended.ConcreteExtendedDataSectionV1;
import io.heraldprox.herald.sensor.payload.extended.ConcreteExtendedDataV1;
import io.heraldprox.herald.sensor.payload.simple.ConcreteHeraldPayloadV1;

/**
 * This class bypasses the analysis API because it only handles single double values - we need RSSI and TxPower for our analysis.
 * We also need to know when a device goes out of range so we can analyse the first seen and last seen times, and drop the device
 * from consideration if its too long (beacon not a mobile device) or too short (fleeting contact not useful for calibration).
 *
 * We also perform Proxemics self-calibration as well as beacon field 3D positional calibration.
 *
 * Note: ALPHA QUALITY CODE UNTIL PROVEN IN EXPERIMENTS.
 */
public class ProxemicsSelfCalibrationAnalyser implements SensorDelegate {
    protected Hashtable<TargetIdentifier, DeviceCalibrationRecord> records = new Hashtable<TargetIdentifier, DeviceCalibrationRecord>();
    protected BeaconFieldPositionCalculator positionCalculator = new BeaconFieldPositionCalculator(5 /* seconds */ );

    // TODO correctly handle the above data to be smoothed PRIOR to passing the (currently raw filtered) data to the polynomial calibration routines
    // TODO MODIFY FREQUENCY TO FREQUENCY PROPORTION BEFORE CURVE FITTING (so fitting is standardised across phones for relative frequency)
//    protected CorrectedRSSIFrequencyCalculator rssiCalculator = new CorrectedRSSIFrequencyCalculator(5, 30); /* 30 second sliding window every 5 seconds */

    // Calibration(s) supported:-
    protected PolynomialProxemicsCalibration polyCalibrator;

    protected long recalibrationDelay = 5 * 60; // seconds
    protected Date lastRecalibration = new Date();

    protected final TextFile beaconPositionFile;
    protected final TextFile rssiAggregateFile;
    protected final TextFile calibrationsFile;

    public ProxemicsSelfCalibrationAnalyser(final TextFile beaconPositionFile, final TextFile rssiAggregateFile, final TextFile calibrationsFile,
                                            final Callback<Tuple<Date, Grid3DLocationReference>> posCallback) {
        this.beaconPositionFile = beaconPositionFile;
        this.rssiAggregateFile = rssiAggregateFile;
        this.calibrationsFile = calibrationsFile;
        polyCalibrator = new PolynomialProxemicsCalibration(5, rssiAggregateFile, calibrationsFile);
        positionCalculator.setCallback(posCallback);
        // factor 5 = 3 turning points = 2 maxima (personal, social space)
    }

    /**
     * Applications need to call this function at least once every 5 seconds so as to correctly aggregate values.
     * This class guarantees the aggregation calculations will not run more often than they need to, and so it
     * is safe to call many times per second.
     * @param nowTime
     */
    public void update(final Date nowTime) {
        // Position calculator updates live (last 5 seconds) position when within a beacon field
        positionCalculator.update(nowTime, records, beaconPositionFile);
        // DeviceCalibrationRecord records and filters corrected RSSI values per detected device,
        //   and tracks that devices status. Data is not marked ready for analysis until the device
        //   is found to be valid (can be up to 25 minutes plus 30 seconds).
        // CorrectedRSSIFrequencyCalculator then aggregates this data up into 30 second windows
        //   per detected device every 30 seconds. (Into prefiltered (live) and postfiltered (validated) sets)
//        rssiCalculator.update(nowTime, records, rssiAggregateFile);
        // TODO clear old data out after max device deletion time + max analysis window
        // ProxemicsCalibration fitting routines are then ran every hour on all validated data up to this
        // point, and calibration values saved for this device across whole nearby device population
        if (nowTime.secondsSinceUnixEpoch() - lastRecalibration.secondsSinceUnixEpoch() > recalibrationDelay) {
            // Note: PolyCalibrator is performing the 30 second windowing currently NOT CorrectedRSSIFrequencyCalculator
            polyCalibrator.updateCalibration(lastRecalibration,nowTime,records);
            lastRecalibration = nowTime;
        }

        // Note: Live social mixing scoring is not yet calculated from this fitting data. We could do
        //       this in future if needs be after a certain minimum period of calibration. We're
        //       assuming for now we shall calculate this scoring after the fact from the raw
        //       stored contact data.
    }
    @Override
    public void sensor(@NonNull SensorType sensor, @NonNull TargetIdentifier didDetect) {
        // ignore - no data yet
    }

    @Override
    public void sensor(@NonNull SensorType sensor, boolean available, @NonNull TargetIdentifier didDeleteOrDetect) {
        // handle deletion
        if (!available) {
            DeviceCalibrationRecord detected = records.get(didDeleteOrDetect);
            if (null != detected) {
                detected.outOfRange(new Date());
            }
        }
    }
    @Override
    public void sensor(@NonNull SensorType sensor, @NonNull PayloadData didRead, @NonNull TargetIdentifier fromTarget) {
        Log.d(getClass().getName(),"sensor(didRead): Got payload: " + didRead.hexEncodedString());
        if (records.containsKey(fromTarget)) {
            Log.d(getClass().getName(),"sensor(didRead): Known target");
            // parse extended data area from payload, if available
            ConcreteHeraldPayloadV1 v1p = new ConcreteHeraldPayloadV1();
            if (ConcreteHeraldPayloadV1.parse(didRead,v1p)) {
                Log.d(getClass().getName(),"sensor(didRead): Parse succeeded.");
                // Check it's valid
                if (!v1p.isValid()) {
                    return;
                }
                // Check for beacon information
                ConcreteExtendedDataV1 extended = v1p.getExtendedData();
                List<ConcreteExtendedDataSectionV1> sections = extended.getSections();
                Log.d(getClass().getName(),"sensor(didRead): Parsed valid Herald V1 payload: " + v1p.toString());
                UInt8 posCode = ConcreteExtendedDataSectionV1.code(ConcreteExtendedDataSectionCodesV1.LocalGridPosition);
                for (ConcreteExtendedDataSectionV1 section : sections) {
                    if (section.code.equals(posCode)) {
                        Log.d(getClass().getName(),"sensor(didRead): Got geo section data: " + section.data + ", length: " + section.length);
                        // extract X, Y, Z position from UInt16 values
                        if (section.length.value == 6) {
                            // valid payload
                            UInt16 xu = section.data.uint16(0);
                            UInt16 yu = section.data.uint16(2);
                            UInt16 zu = section.data.uint16(4);
                            double x = xu.doubleValue();
                            double y = yu.doubleValue();
                            double z = zu.doubleValue();
                            Grid3DLocationReference ref = new Grid3DLocationReference(x, y, z);
                            Log.d(getClass().getName(),"sensor(didRead): Got parsed position: " + ref);
                            records.get(fromTarget).setPosition(ref);
                        }
                    } else {
                        Log.d(getClass().getName(),"sensor(didRead): Got other section: " + section.code);
                    }
                }
            } else {
                Log.d(getClass().getName(), "sensor(didRead): Parse failed");
            }
        }
    }

    @Override
    public void sensor(@NonNull SensorType sensor, @NonNull ImmediateSendData didReceive, @NonNull TargetIdentifier fromTarget) {
        // ignore
    }

    @Override
    public void sensor(@NonNull SensorType sensor, @NonNull List<PayloadData> didShare, @NonNull TargetIdentifier fromTarget) {
        // ignore
    }

    @Override
    public void sensor(@NonNull SensorType sensor, @NonNull Proximity didMeasure, @NonNull TargetIdentifier fromTarget) {
        if (!records.containsKey(fromTarget)) {
            records.put(fromTarget,new DeviceCalibrationRecord(fromTarget));
        }
        records.get(fromTarget).addRecord(didMeasure, new Date());
    }

    @Override
    public void sensor(@NonNull SensorType sensor, @NonNull Location didVisit) {
        // ignore
    }

    @Override
    public void sensor(@NonNull SensorType sensor, @NonNull Proximity didMeasure, @NonNull TargetIdentifier fromTarget, @NonNull PayloadData withPayload) {
        // ignore as didMeasure without payload is always called anyway (for ALL nearby devices, not just Herald ones)
    }

    @Override
    public void sensor(@NonNull SensorType sensor, @NonNull SensorState didUpdateState) {
        // ignore
    }

    @Override
    public void sensor(@NonNull SensorType sensor, @NonNull SensorMetadata didClassify, @NonNull TargetIdentifier aboutTarget) {
        if (records.containsKey(aboutTarget)) {
            records.get(aboutTarget).addSensorMetadata(didClassify);
        }
    }
}
