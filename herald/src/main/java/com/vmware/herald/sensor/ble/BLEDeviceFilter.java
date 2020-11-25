//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package com.vmware.herald.sensor.ble;

import android.bluetooth.le.ScanRecord;
import android.content.Context;

import com.vmware.herald.sensor.ble.BLEDevice;
import com.vmware.herald.sensor.ble.BLESensorConfiguration;
import com.vmware.herald.sensor.data.ConcreteSensorLogger;
import com.vmware.herald.sensor.data.SensorLogger;
import com.vmware.herald.sensor.data.TextFile;
import com.vmware.herald.sensor.datatype.Data;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// Device filter for avoiding connection to devices that definitely cannot
/// host sensor services.
public class BLEDeviceFilter {
    private final static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final SensorLogger logger = new ConcreteSensorLogger("Sensor", "BLE.BLEDeviceFilter");
    private final List<FilterPattern> filterPatterns;
    private final TextFile textFile;
    private final Map<Data, ShouldIgnore> samples = new HashMap<>();

    // Counter for training samples
    private final static class ShouldIgnore {
        public long yes = 0;
        public long no = 0;
    }

    // Pattern for filtering device based on message content
    public final static class FilterPattern {
        public final String regularExpression;
        public final Pattern pattern;
        public FilterPattern(final String regularExpression, final Pattern pattern) {
            this.regularExpression = regularExpression;
            this.pattern = pattern;
        }
    }

    // Match of a filter pattern
    public final static class MatchingPattern {
        public final FilterPattern filterPattern;
        public final String message;
        public MatchingPattern(FilterPattern filterPattern, String message) {
            this.filterPattern = filterPattern;
            this.message = message;
        }
    }

    /// BLE device filter for matching devices against filters defined
    /// in BLESensorConfiguration.deviceFilterFeaturePatterns.
    public BLEDeviceFilter() {
        this(null, null, BLESensorConfiguration.deviceFilterFeaturePatterns);
    }

    /// BLE device filter for matching devices against BLESensorConfiguration.deviceFilterFeaturePatterns
    /// and writing advert data to file for analysis.
    public BLEDeviceFilter(final Context context, final String file) {
        this(context, file, BLESensorConfiguration.deviceFilterFeaturePatterns);
    }

    /// BLE device filter for matching devices against the given set of patterns
    /// and writing advert data to file for analysis.
    public BLEDeviceFilter(final Context context, final String file, final String[] patterns) {
        if (context == null || file == null) {
            textFile = null;
        } else {
            textFile = new TextFile(context, file);
            if (textFile.empty()) {
                textFile.write("time,ignore,featureData,scanRecordRawData,identifier,rssi,deviceModel,deviceName");
            }
        }
        if (BLESensorConfiguration.deviceFilterTrainingEnabled || patterns == null || patterns.length == 0) {
            filterPatterns = null;
        } else {
            filterPatterns = compilePatterns(patterns);
        }
    }

    // MARK:- Pattern matching functions
    // Using regular expression over hex representation of feature data for maximum flexibility and usability

    /// Match message against all patterns in sequential order, returns matching pattern or null
    protected static FilterPattern match(final List<FilterPattern> filterPatterns, final String message) {
        final SensorLogger logger = new ConcreteSensorLogger("Sensor", "BLE.BLEDeviceFilter");
        if (message == null) {
            return null;
        }
        for (final FilterPattern filterPattern : filterPatterns) {
            try {
                final Matcher matcher = filterPattern.pattern.matcher(message);
                if (matcher.find()) {
                    return filterPattern;
                }
            } catch (Throwable e) {
            }
        }
        return null;
    }

    /// Compile regular expressions into patterns.
    protected static List<FilterPattern> compilePatterns(final String[] regularExpressions) {
        final SensorLogger logger = new ConcreteSensorLogger("Sensor", "BLE.BLEDeviceFilter");
        final List<FilterPattern> filterPatterns = new ArrayList<>(regularExpressions.length);
        for (final String regularExpression : regularExpressions) {
            try {
                final Pattern pattern = Pattern.compile(regularExpression, Pattern.CASE_INSENSITIVE);
                final FilterPattern filterPattern = new FilterPattern(regularExpression, pattern);
                filterPatterns.add(filterPattern);
            } catch (Throwable e) {
                logger.fault("compilePatterns, invalid filter pattern (regularExpression={})", regularExpression);
            }
        }
        return filterPatterns;
    }

    /// Extract all manufacturer data segments from raw scan record data
    protected final static List<Data> extractManufacturerData(final Data raw) { // AFC Duplication - please replace with equivalent AdvertParser method
        final List<Data> segments = new ArrayList<>(1);
        // Scan raw data to search for "FF4C00" and use preceding 1-byte to establish length of segment
        for (int i=0; i<raw.value.length-2; i++) { // AFC this needs to be moved along by consumed data
            try {
                // Search for "FF4C00" // AFC No need to search, each segment is preceded by a data count. What if FF4c00 happened to exist as part of another data area?
                if (raw.value[i] == (byte) 0xFF && raw.value[i+1] == (byte) 0x4C && raw.value[i+2] == (byte) 0x00) {
                    // Extract segment based on 1-byte length value preceding "FF4C00"
                    final int lengthOfManufacturerDataSegment = raw.value[i-1] & 0xFF;
                    final Data segment = raw.subdata(i+3, lengthOfManufacturerDataSegment - 3);
                    segments.add(segment); // AFC very lucky that add null does not throw an exception - last 3 bytes could be 'FF4C00' as they are encrypted bytes. This results in subdata returning null.
                }
            } catch (Throwable e) {
                // Errors are expected due to parsing errors and corrupted data
            }
        }
        return segments;
    }

    /// Extract all messages from manufacturer data segments
    protected final static List<Data> extractMessageData(final List<Data> manufacturerData) { // AFC Duplication - please replace with equivalent AdvertParser method
        final List<Data> messages = new ArrayList<>();
        for (Data segment : manufacturerData) {
            try {
                // "01" marks legacy service UUID encoding
                if (segment.value[0] == (byte) 0x01) {
                    messages.add(segment);
                }
                // Assume all other prefixes mark new messages "Type:Length:Data"
                else {
                    final byte[] raw = segment.value;
                    for (int i=0; i<raw.length - 1; i++) {
                        // Type (1-byte), Length (1-byte), Data
                        final int lengthOfMessage = raw[i+1] & 0xFF;
                        final Data message = segment.subdata(i, lengthOfMessage + 2);
                        if (message != null) {
                            messages.add(message);
                        }
                        i += (lengthOfMessage + 1);
                    }
                }
            } catch (Throwable e) {
                // Errors are expected due to parsing errors and corrupted data
            }
        }
        return messages;
    }

    // MARK:- Filtering functions

    /// Extract feature data from scan record
    private List<Data> extractFeatures(final ScanRecord scanRecord) { // AFC Duplication - please replace with equivalent AdvertParser method. Also cannot be tested independently of Bluetooth stack - tightly coupled
        if (scanRecord == null) {
            return null;
        }
        // Get message data
        final List<Data> featureList = new ArrayList<>();
        final byte[] rawData = scanRecord.getBytes();
        if (rawData != null) {
            final List<Data> manufacturerData = extractManufacturerData(new Data(rawData));
            final List<Data> messageData = extractMessageData(manufacturerData);
            featureList.addAll(messageData);
        }
        return featureList;
    }

    /// Add training example to adaptive filter.
    public synchronized void train(final BLEDevice device, final boolean ignore) {
        final ScanRecord scanRecord = device.scanRecord();
        // Get feature data from scan record
        if (scanRecord == null) {
            return;
        }
        final Data scanRecordData = (scanRecord.getBytes() == null ? null : new Data(scanRecord.getBytes()));
        if (scanRecordData == null) {
            return;
        }
        final List<Data> featureList = extractFeatures(scanRecord);
        if (featureList == null) {
            return;
        }
        // Update ignore yes/no counts for feature data
        for (Data featureData : featureList) {
            ShouldIgnore shouldIgnore = samples.get(featureData);
            if (shouldIgnore == null) {
                shouldIgnore = new ShouldIgnore();
                samples.put(featureData, shouldIgnore);
            }
            if (ignore) {
                shouldIgnore.yes++;
            } else {
                shouldIgnore.no++;
            }
            logger.debug("train (ignore={},feature={},scanRecord={},device={})", (ignore ? "Y" : "N"), featureData.hexEncodedString(), scanRecordData.hexEncodedString(), device.description());
            // Write sample to text file for analysis
            if (textFile == null) {
                return;
            }
            final StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append('"');
            stringBuilder.append(dateFormatter.format(new Date()));
            stringBuilder.append('"');
            stringBuilder.append(',');
            stringBuilder.append(ignore ? 'Y' : 'N');
            stringBuilder.append(',');
            stringBuilder.append(featureData.hexEncodedString());
            stringBuilder.append(',');
            stringBuilder.append(scanRecordData.hexEncodedString());
            stringBuilder.append(',');
            stringBuilder.append(device.identifier.value);
            stringBuilder.append(',');
            if (device.rssi() != null) {
                stringBuilder.append(device.rssi().value);
            }
            stringBuilder.append(',');
            if (device.model() != null) {
                stringBuilder.append('"');
                stringBuilder.append(device.model());
                stringBuilder.append('"');
            }
            stringBuilder.append(',');
            if (device.deviceName() != null) {
                stringBuilder.append('"');
                stringBuilder.append(device.deviceName());
                stringBuilder.append('"');
            }
            textFile.write(stringBuilder.toString());
        }
    }

    /// Match scan record messages against all registered patterns, returns matching pattern or null.
    public MatchingPattern match(final BLEDevice device) { // AFC tightly coupled to ScanRecord, consider revising
        // No pattern to match against
        if (filterPatterns == null || filterPatterns.isEmpty()) {
            return null;
        }
        final ScanRecord scanRecord = device.scanRecord();
        // Cannot match device without any scan record data
        if (scanRecord == null) {
            return null;
        }
        // Extract feature data from scan record
        // Cannot match device without any feature data
        final List<Data> featureList = extractFeatures(scanRecord);
        if (featureList == null || featureList.isEmpty()) {
            return null;
        }
        for (Data featureData : featureList) {
            final String message = featureData.hexEncodedString();
            final FilterPattern filterPattern = match(filterPatterns, message);
            if (filterPattern != null) {
                return new MatchingPattern(filterPattern, message);
            }
        }
        return null;
    }

    /// Should the device be ignored based on scan record data?
    private boolean ignoreBasedOnStatistics(final BLEDevice device) { // AFC tightly coupled to ScanRecord, consider revising
        final ScanRecord scanRecord = device.scanRecord();
        // Do not ignore device without any scan record data
        if (scanRecord == null) {
            return false;
        }
        // Extract feature data from scan record
        // Do not ignore device without any feature data
        final List<Data> featureList = extractFeatures(scanRecord);
        if (featureList == null) {
            return false;
        }
        for (Data featureData : featureList) {
            // Get training example statistics
            final ShouldIgnore shouldIgnore = samples.get(featureData);
            // Do not ignore device based on unknown feature data
            if (shouldIgnore == null) {
                return false;
            }
            // Do not ignore device if there is even one example of it being legitimate
            if (shouldIgnore.no > 0) { // AFC This logic could fail if data to result in 'yes' was spotted before data for 'no'
                return false;
            }
            // Ignore device if the signature has been registered for ignore more than twice
            if (shouldIgnore.yes > 2) {
                return true;
            }
        }
        // Do not ignore device if no decision is reached based on existing rules
        return false;
    }
}
