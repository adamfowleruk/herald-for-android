//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.ble.filter;

import io.heraldprox.herald.sensor.datatype.Data;

public class BLEAdvertAppleManufacturerSegment {
    public final int type;
    public final int reportedLength;
    public final byte[] data; // BIG ENDIAN (network order) AT THIS POINT
    public final Data raw;

    public BLEAdvertAppleManufacturerSegment(int type, int reportedLength, byte[] dataBigEndian, Data raw) {
        this.type = type;
        this.reportedLength = reportedLength;
        this.data = dataBigEndian;
        this.raw = raw;
    }

    @Override
    public String toString() {
        return raw.hexEncodedString();
    }
}
