//  Copyright 2020-2024 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.payload.heraldv2wire;

import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.UInt8;
import io.heraldprox.herald.sensor.protocol.HeraldProtocolV2;

import androidx.annotation.NonNull;

public class HeraldV2WireData extends PayloadData {

    public HeraldV2WireData(@NonNull UInt8 protocolVersion, @NonNull UInt8 action, @NonNull PayloadData wrapped) {
        super();
        super.append(protocolVersion);
        super.append(action);
        super.append(wrapped);
    }
}
