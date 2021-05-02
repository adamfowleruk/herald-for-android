//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.analysis.views;

import com.vmware.herald.sensor.analysis.sampling.Filter;
import com.vmware.herald.sensor.analysis.sampling.Sample;
import com.vmware.herald.sensor.datatype.DoubleValue;

public class InRange<T extends DoubleValue> implements Filter<T> {
    private final double min, max;

    public InRange(final double min, final double max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public boolean test(Sample<T> item) {
        return item.value().doubleValue() >= min && item.value().doubleValue() <= max;
    }
}