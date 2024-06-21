package io.heraldprox.herald.sensor.analysis.evaluation;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.TreeSet;

import io.heraldprox.herald.sensor.Device;
import io.heraldprox.herald.sensor.SensorMetadata;
import io.heraldprox.herald.sensor.datatype.Callback;
import io.heraldprox.herald.sensor.datatype.Date;
import io.heraldprox.herald.sensor.datatype.Grid3DLocationReference;
import io.heraldprox.herald.sensor.datatype.Proximity;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;
import io.heraldprox.herald.sensor.datatype.Tuple;
import io.heraldprox.herald.sensor.metadata.BeaconMetadata;

/**
 * This class manages an individual detected devices corrected RSSI readings over time.
 * This includes validating the device is a valid mobile device (not a beacon), has enough
 * data, and smoothing the data prior to analysis either individually or as a population of devices.
 *
 * Note: ALPHA QUALITY CODE UNTIL PROVEN IN EXPERIMENTS.
 */
public class DeviceCalibrationRecord {
    protected TargetIdentifier target;
    protected Date firstSeen = null;
    protected Date lastSeen = null;

    protected boolean hasFinishedEvaluation = false;
    protected boolean isTooLongContact = false;
    protected boolean isTooShortContact = false;
    protected boolean hasTooLittleData = false;


    protected boolean deleted = false;
    protected boolean beacon = false;
    protected boolean heraldBeacon = false;
    protected Grid3DLocationReference position = null;

    // Use a datatype that orders by Date, ascending
    protected TimeSeries<CorrectedRSSI> data = new TimeSeries<>();
//    protected ArrayList<Tuple<Double,Date>> data = new ArrayList<>();

    TreeSet<SensorMetadata> metadata = new TreeSet<>();

    public DeviceCalibrationRecord(@NonNull TargetIdentifier tgt) {
        this.target = tgt;
    }

    public void addRecord(@NonNull Proximity record, @NonNull Date seen) {
        // save proximity record if, and only if, it has TxPower included
        // TxPower check
        if (null == record.calibration) {
            return;
        }
        // filter for sensible RSSI in the first place (i.e. >= -99)
        if (record.value < -99.0d) {
            return;
        }
        if (null == firstSeen) {
            firstSeen = seen;
        }
        lastSeen = seen;
        // Record value in ordered set as corrected Rssi
        data.put(seen,new CorrectedRSSI(record.calibration.value - record.value));
        // Note: Inverted such that we are a positive number as Corrected RSSI
    }

    public void outOfRange(@NonNull Date whenDeleted) {
        // evaluate first and last seen and device if we're valid or not
        checkIsTooShortContact();
        checkIsTooLittleData();

        hasFinishedEvaluation = true;
        deleted = true;
    }

    private boolean checkIsTooShortContact() {
        if (null == firstSeen) {
            isTooShortContact = true; // shouldn't be possible, but just in case
        }
        if (null == lastSeen) {
            isTooShortContact = true;
        }
        if (!isTooShortContact) { // prevent NPE
            long duration = lastSeen.secondsSinceUnixEpoch() - firstSeen.secondsSinceUnixEpoch();
            if (duration < 60) {
                isTooShortContact = true;
            } else if (duration > 25 * 60) { //25 minutes
                isTooLongContact = true;
            }
        }
        return isTooShortContact;
    }

    private boolean checkIsTooLittleData() {
        if (data.size() < 60) { // TODO validate this boundary against previous practical testing
            // Note that most devices advertise 5 times a second, meaning this is at least a 12 second contact
            // TODO evaluate if it should be 30 seconds or higher
            hasTooLittleData = true;
        }
        // TODO evaluate if we should have more than a certain number of unique values (I.e. not just one value repeated)
        return hasTooLittleData;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public boolean isValidMobileDeviceForCalibration() {
        return hasFinishedEvaluation &&
                !isTooLongContact && !isTooShortContact && !hasTooLittleData &&
                (null==position);
    }
    public boolean isValidMobileDeviceForCalibrationInFlight() {
        return (null==position) && !checkIsTooLittleData() && !checkIsTooShortContact();
    }

    public void addFromWindowIfFinallyValid(@NonNull Date minExclusive,@NonNull Date maxInclusive, Frequencies<CorrectedRSSI> addTo) {
        if (!isValidMobileDeviceForCalibration()) { // includes !hasFinishedEvaluation
            return;
        }
        // TODO smooth to 30 second windows every 5 seconds here first (i.e. per advertising target)
        data.getRange(minExclusive, maxInclusive, new Callback<Tuple<Date, CorrectedRSSI>>() {
            @Override
            public void accept(Tuple<Date, CorrectedRSSI> value) {
                addTo.increment(value.b);
            }
        });
//        for (Tuple<Date> value : data) {
//            if (value.b.secondsSinceUnixEpoch() > minExclusive.secondsSinceUnixEpoch() &&
//                    value.b.secondsSinceUnixEpoch() <= maxInclusive.secondsSinceUnixEpoch()) {
//                addTo.add(value);
//            }
//        }
    }
    public void addFromWindowIfCurrentlyValid(@NonNull Date minExclusive,@NonNull Date maxInclusive, Frequencies<CorrectedRSSI> addTo) {
        if (!isValidMobileDeviceForCalibrationInFlight()) { // not including !hasFinishedEvaluation
            return;
        }
        // TODO smooth to 30 second windows every 5 seconds here first (i.e. per advertising target)
        data.getRange(minExclusive, maxInclusive, new Callback<Tuple<Date, CorrectedRSSI>>() {
            @Override
            public void accept(Tuple<Date, CorrectedRSSI> value) {
                addTo.increment(value.b);
            }
        });
//        for (Tuple<Double,Date> value : data) {
//            if (value.b.secondsSinceUnixEpoch() > minExclusive.secondsSinceUnixEpoch() &&
//                    value.b.secondsSinceUnixEpoch() <= maxInclusive.secondsSinceUnixEpoch()) {
//                addTo.add(value);
//            }
//        }
    }


    public void addFromWindowIfBeacon(@NonNull Date minExclusive,@NonNull Date maxInclusive,  Frequencies<CorrectedRSSI> addTo) {
        if (!isPositionBeacon()) { // allows beacon positions still in range (I.e. live position calculations)
            return;
        }
//        for (Tuple<Double,Date> value : data) {
//            if (value.b.secondsSinceUnixEpoch() > minExclusive.secondsSinceUnixEpoch() &&
//                    value.b.secondsSinceUnixEpoch() <= maxInclusive.secondsSinceUnixEpoch()) {
//                addTo.add(value);
//            }
//        }z
        data.getRange(minExclusive, maxInclusive, new Callback<Tuple<Date, CorrectedRSSI>>() {
            @Override
            public void accept(Tuple<Date, CorrectedRSSI> value) {
                addTo.increment(value.b);
            }
        });
    }

    public void setPosition(Grid3DLocationReference pos) {
        this.position = pos;
    }

    public boolean isPositionBeacon() {
        return beacon && heraldBeacon && (null != this.position);
    }

    public Grid3DLocationReference getPosition() {
        return position;
    }

    public void addSensorMetadata(SensorMetadata meta) {
        this.metadata.add(meta);
        if (meta instanceof BeaconMetadata) {
            beacon = true;
            BeaconMetadata bm = (BeaconMetadata)meta;
            heraldBeacon = bm.isHeraldBeacon();
        }
    }

}
