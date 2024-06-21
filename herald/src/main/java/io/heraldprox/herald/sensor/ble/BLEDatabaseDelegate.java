//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.ble;

import androidx.annotation.NonNull;

import io.heraldprox.herald.sensor.SensorMetadata;

/**
 * Delegate for receiving registry create/update/delete events.
 */
public interface BLEDatabaseDelegate {

    void bleDatabaseDidCreate(@NonNull final BLEDevice device);

    void bleDatabaseDidUpdate(@NonNull final BLEDevice device, @NonNull final BLEDeviceAttribute attribute);

    /**
     * Updates metadata about a remote target
     *
     * @since v2.3
     *
     * @param device
     * @param metaUpdatedOrAdded
     */
    void bleDatabaseDidUpdateMetadata(@NonNull BLEDevice device, @NonNull SensorMetadata metaUpdatedOrAdded);

    void bleDatabaseDidDelete(@NonNull final BLEDevice device);
}
