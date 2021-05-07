//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.datatype;

import java.util.Objects;

/// Unsigned integer (8 bits)
public class UInt8 implements DoubleValue {
    public final static int bitWidth = 8;
    public final static UInt8 min = new UInt8(0);
    public final static UInt8 max = new UInt8(255);
    public final int value;

    public UInt8(int value) {
        this.value = (value < 0 ? 0 : (value > 255 ? 255 : value));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UInt8 uInt8 = (UInt8) o;
        return value == uInt8.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }

    @Override
    public double doubleValue() {
        return value;
    }
}
