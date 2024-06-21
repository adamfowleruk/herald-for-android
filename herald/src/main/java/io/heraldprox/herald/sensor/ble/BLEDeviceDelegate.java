//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.ble;

import androidx.annotation.NonNull;

import io.heraldprox.herald.sensor.SensorMetadata;

public interface BLEDeviceDelegate {

    void device(@NonNull final BLEDevice device, @NonNull final BLEDeviceAttribute didUpdate);

    /**
     * Sends a device metadata update about this device.
     * Forms the foundation of the device make/model and Beacon identification code.
     *
     * @since v2.3
     * @param device
     * @param metaUpdatedOrAdded
     */
    void device(@NonNull final BLEDevice device, @NonNull final SensorMetadata metaUpdatedOrAdded);
}

