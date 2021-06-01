//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.analysis.sampling;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.heraldprox.herald.sensor.datatype.DoubleValue;

public class ConcreteAnalysisDelegate<T extends DoubleValue> implements AnalysisDelegate<T> {
    private final Class<T> inputType;
    @NonNull
    private final ListManager<T> listManager;
    @NonNull
    private final SampleList<T> sampleList;

    public ConcreteAnalysisDelegate(final Class<T> inputType, final int listSize) {
        this.inputType = inputType;
        this.listManager = new ListManager<>(listSize);
        this.sampleList = new SampleList<>(listSize);
    }

    @Override
    public Class<T> inputType() {
        return inputType;
    }

    @Override
    public void reset() {
        listManager.clear();
    }

    @NonNull
    @Override
    public SampleList<T> samples() {
        return sampleList;
    }

    @Nullable
    public SampleList<T> samples(final SampledID sampledID) {
        return listManager.list(sampledID);
    }

    @Override
    public void newSample(SampledID sampled, Sample<T> item) {
        listManager.push(sampled, item);
        sampleList.push(item);
    }
}