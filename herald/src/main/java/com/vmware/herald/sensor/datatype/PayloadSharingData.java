//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.datatype;

public class PayloadSharingData {
    public final RSSI rssi;
    public final Data data;

    /**
     * Payload sharing data
     *
     * @param rssi RSSI between self and peer.
     * @param data Payload data of devices being shared by self to peer.
     */
    public PayloadSharingData(final RSSI rssi, final Data data) {
        this.rssi = rssi;
        this.data = data;
    }
}
