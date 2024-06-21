package io.heraldprox.herald.sensor.payload.simple;

import androidx.annotation.NonNull;

import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.UInt16;
import io.heraldprox.herald.sensor.datatype.UInt8;
import io.heraldprox.herald.sensor.payload.extended.ConcreteExtendedDataV1;

/**
 * Represents a decoded Herald V1 payload. Minimal external metadata plus Extended internal data.
 * Only syntactically validates the data - doesn't claim to have valid Section content for their
 * semantics.
 */
public class ConcreteHeraldPayloadV1 {
    @NonNull
    protected UInt8 protocolVersion = UInt8.min;
    @NonNull
    protected UInt16 countryCode = UInt16.min;
    @NonNull
    protected UInt16 stateCode = UInt16.min;

    @NonNull
    protected ConcreteExtendedDataV1 extendedData = new ConcreteExtendedDataV1();

    public ConcreteHeraldPayloadV1() {
        // default ctor
    }

    public static boolean parse(@NonNull final PayloadData payload, @NonNull ConcreteHeraldPayloadV1 toUpdate) {
        // attempt to parse the given raw PayloadData class
        if (payload.length() >= 5) {
            // Could be a valid Herald payload
            toUpdate.protocolVersion = payload.uint8(0);
            toUpdate.countryCode = payload.uint16(1);
            toUpdate.stateCode = payload.uint16(3);

            // check currently valid
            if (!toUpdate.isValid()) {
                return false;
            }

            // Now try to parse extended data area
            PayloadData ext = new PayloadData();
            ext.append(payload.subdata(5));
            toUpdate.extendedData = new ConcreteExtendedDataV1(ext);
            return toUpdate.extendedData.isValid();
        }
        return false;
    }

    public boolean isValid() {
        // Determine if our data stored makes us a valid Herald V1 Payload
        return !protocolVersion.equals(UInt8.min) &&
                !countryCode.equals(UInt16.min) &&
                !stateCode.equals(UInt16.min) &&
                extendedData.isValid();
    }

    @NonNull
    public UInt8 getProtocolVersion() {
        return protocolVersion;
    }

    @NonNull
    public UInt16 getCountryCode() {
        return countryCode;
    }

    @NonNull
    public UInt16 getStateCode() {
        return stateCode;
    }

    @NonNull
    public ConcreteExtendedDataV1 getExtendedData() {
        return extendedData;
    }
}
