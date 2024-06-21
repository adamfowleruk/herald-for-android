package io.heraldprox.herald.sensor.payload.extended;

import static io.heraldprox.herald.sensor.payload.extended.ConcreteExtendedDataSectionCodesV1.LocalGridPosition;

import androidx.annotation.NonNull;

import io.heraldprox.herald.sensor.datatype.Data;
import io.heraldprox.herald.sensor.datatype.UInt8;

public class ConcreteExtendedDataSectionV1 {
    @NonNull
    public final UInt8 code;
    @NonNull
    public final UInt8 length;
    @NonNull
    public final Data data;

    public ConcreteExtendedDataSectionV1(@NonNull final UInt8 code, @NonNull final UInt8 length, @NonNull final Data data) {
        this.code = code;
        this.length = length;
        this.data = data;
    }

    /**
     * Fetches the code number for the given Extended Code section Java Enum.
     *
     * See https://heraldprox.io/specs/payload-extended
     *
     * @since v2.3
     * @param v1c
     * @return The UInt8 code of the section. Set to Invalid (0xfe) if unsupported
     */
    @NonNull
    public static UInt8 code(ConcreteExtendedDataSectionCodesV1 v1c) {
        switch (v1c) {
            case Version:
                return new UInt8(0x00);
            // TODO other cases here for current valid spec
            // See https://heraldprox.io/specs/payload-extended
            case LocalGridPosition:
                return new UInt8(0x09);
            case VenueName:
                return new UInt8(0x10);
            case VenueArea:
                return new UInt8(0x11);
            case VenueDisambiguation:
                return new UInt8(0x12);
            default: return new UInt8(0xfe); // Invalid
        }
    }
}