//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.ble;

public interface BLETimerDelegate {

    void bleTimer(final long currentTimeMillis);
}
