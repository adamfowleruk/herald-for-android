//  Copyright 2020-2024 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.protocol;

import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.UInt8;
import io.heraldprox.herald.sensor.payload.extended.ExtendedData;
import io.heraldprox.herald.sensor.payload.heraldv2wire.HeraldV2WireData;

/**
 * Wire protocol helper class for the Herald Protocol V2.
 *
 * @since v2.3 July 2024
 */
public class HeraldProtocolV2 {
    public static final UInt8 PROTOCOL_VERSION_HERALD_V2_0 = new UInt8(0x01);

    public static final UInt8 ACTION_SINGLE_PAYLOAD_WRITE_ARBITRARY = new UInt8(0x00);
    public static final UInt8 ACTION_SINGLE_PAYLOAD_WRITE_EXTENDED = new UInt8(0x01);

    // TODO Add secured payload and MESH on-the-wire actions here (includes packet segmentation and encryption)

    /**
     * This approach is basically the V1 payload (including custom payloads) implemented over the V2 characteristic.
     * Ideally avoid using this for one of the other protocol payload options in this standard.
     *
     * @param arbitraryPayload
     * @return the encoded payload data
     */
    public static HeraldV2WireData singlePayloadWrite(PayloadData arbitraryPayload) {
        HeraldV2WireData pd = new HeraldV2WireData(PROTOCOL_VERSION_HERALD_V2_0,ACTION_SINGLE_PAYLOAD_WRITE_ARBITRARY,arbitraryPayload);

        return pd;
    }

    /**
     * Writes an Extended Data (Probably V1.1 July 2004) packet unencrypted on the wire.
     * Ideally avoid using this for one of the other protocol payload options in this standard.
     *
     * @param extendedData
     * @return the encoded payload data
     */
    public static HeraldV2WireData singlePayloadWrite(ExtendedData extendedData) {
        HeraldV2WireData pd = new HeraldV2WireData(PROTOCOL_VERSION_HERALD_V2_0,ACTION_SINGLE_PAYLOAD_WRITE_EXTENDED,extendedData.payload());

        return pd;
    }

    public static PayloadData extractPayloadData(PayloadData rawHeraldV2Payload) {
        // check length is at least 3 bytes (version + action + some data)
        if (rawHeraldV2Payload.size() < 3) {
            return null;
        }
        // Check first value
        if (PROTOCOL_VERSION_HERALD_V2_0.value!=rawHeraldV2Payload.uint8(0).value) {
            return null;
        }
        if (ACTION_SINGLE_PAYLOAD_WRITE_ARBITRARY.value==rawHeraldV2Payload.uint8(1).value) {
            PayloadData pd = new PayloadData();
            pd.append(rawHeraldV2Payload.subdata(2));
            return pd;
        }
        // TODO support ExtendedData payload... might just be same as above for now
//        if (ACTION_SINGLE_PAYLOAD_WRITE_EXTENDED.equals(rawHeraldV2Payload.uint8(1)) {
//            PayloadData pd = new PayloadData();
//            pd.append(rawHeraldV2Payload.subdata(2));
//            return pd;
//        }
        return null;
    }
}
