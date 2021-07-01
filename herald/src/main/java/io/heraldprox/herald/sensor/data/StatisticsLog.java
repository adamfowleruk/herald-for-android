//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.data;

import android.content.Context;

import androidx.annotation.NonNull;

import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.Proximity;
import io.heraldprox.herald.sensor.datatype.Distribution;
import io.heraldprox.herald.sensor.datatype.SensorType;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CSV statistics log for post event analysis and visualisation
 */
public class StatisticsLog extends SensorDelegateLogger {
    @NonNull
    private final PayloadData payloadData;
    private final Map<TargetIdentifier, String> identifierToPayload = new ConcurrentHashMap<>();
    private final Map<String, Date> payloadToTime = new ConcurrentHashMap<>();
    private final Map<String, Distribution> payloadToSample = new ConcurrentHashMap<>();

    public StatisticsLog(@NonNull final Context context, @NonNull final String filename, @NonNull final PayloadData payloadData) {
        super(context, filename);
        this.payloadData = payloadData;
    }

    private void add(@NonNull final TargetIdentifier identifier) {
        final String payload = identifierToPayload.get(identifier);
        if (null == payload) {
            return;
        }
        add(payload);
    }

    private void add(@NonNull final String payload) {
        final Date time = payloadToTime.get(payload);
        final Distribution distribution = payloadToSample.get(payload);
        if (null == time || null == distribution) {
            payloadToTime.put(payload, new Date());
            payloadToSample.put(payload, new Distribution());
            return;
        }
        final Date now = new Date();
        payloadToTime.put(payload, now);
        distribution.add((now.getTime() - time.getTime()) / 1000d);
        write();
    }

    private void write() {
        final StringBuilder content = new StringBuilder("payload,count,mean,sd,min,max\n");
        final List<String> payloadList = new ArrayList<>();
        for (final String payload : payloadToSample.keySet()) {
            if (payload.equals(payloadData.shortName())) {
                continue;
            }
            payloadList.add(payload);
        }
        Collections.sort(payloadList);
        for (final String payload : payloadList) {
            final Distribution distribution = payloadToSample.get(payload);
            if (null == distribution) {
                continue;
            }
            if (null == distribution.mean() || null == distribution.standardDeviation() || null == distribution.min() || null == distribution.max()) {
                continue;
            }
            content.append(csv(payload));
            content.append(',');
            content.append(distribution.count());
            content.append(',');
            content.append(distribution.mean());
            content.append(',');
            content.append(distribution.standardDeviation());
            content.append(',');
            content.append(distribution.min());
            content.append(',');
            content.append(distribution.max());
            content.append('\n');
        }
        overwrite(content.toString());
    }


    // MARK:- SensorDelegate

    @Override
    public void sensor(@NonNull final SensorType sensor, @NonNull final PayloadData didRead, @NonNull final TargetIdentifier fromTarget) {
        identifierToPayload.put(fromTarget, didRead.shortName());
        add(fromTarget);
    }

    @Override
    public void sensor(@NonNull final SensorType sensor, @NonNull final Proximity didMeasure, @NonNull final TargetIdentifier fromTarget) {
        add(fromTarget);
    }

    @Override
    public void sensor(@NonNull final SensorType sensor, @NonNull final List<PayloadData> didShare, @NonNull final TargetIdentifier fromTarget) {
        for (PayloadData payload : didShare) {
            add(payload.shortName());
        }
    }
}
