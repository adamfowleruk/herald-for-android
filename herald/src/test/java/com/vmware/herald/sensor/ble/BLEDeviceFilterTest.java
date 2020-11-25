//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package com.vmware.herald.sensor.ble;

import com.vmware.herald.sensor.ble.BLEDeviceFilter;
import com.vmware.herald.sensor.datatype.Data;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class BLEDeviceFilterTest {

    @Test
    public void testHexTransform() throws Exception {
        final Random random = new Random(0);
        for (int i = 0; i < 1000; i++) {
            final byte[] expected = new byte[i];
            random.nextBytes(expected);
            final String hex = new Data(expected).hexEncodedString();
            final byte[] actual = Data.fromHexEncodedString(hex).value;
            assertArrayEquals(expected, actual);
        }
    }


    @Test
    public void testManufacturerData() throws Exception {
        {
            final Data raw = Data.fromHexEncodedString("02011A020A0C0BFF4C001006071EA3DD89E014FF4C0001000000000000000000002000000000000000000000000000000000000000000000000000000000");
            final List<Data> segments = BLEDeviceFilter.extractManufacturerData(raw);
            assertEquals(2, segments.size());
            assertEquals("1006071EA3DD89E0", segments.get(0).hexEncodedString());
            assertEquals("0100000000000000000000200000000000", segments.get(1).hexEncodedString());
        }
        {
            final Data raw = Data.fromHexEncodedString("02011A14FF4C0001000000000000000000002000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
            final List<Data> segments = BLEDeviceFilter.extractManufacturerData(raw);
            assertEquals(1, segments.size());
            assertEquals("0100000000000000000000200000000000", segments.get(0).hexEncodedString());
        }
        {
            final Data raw = Data.fromHexEncodedString("02011A020A0C0BFF4C0010060C1E4FDE4DF714FF4C0001000000000000000000002000000000000000000000000000000000000000000000000000000000");
            final List<Data> segments = BLEDeviceFilter.extractManufacturerData(raw);
            assertEquals(2, segments.size());
            assertEquals("10060C1E4FDE4DF7", segments.get(0).hexEncodedString());
            assertEquals("0100000000000000000000200000000000", segments.get(1).hexEncodedString());
        }
        {
            final Data raw = Data.fromHexEncodedString("0201060AFF4C001005421C1E616A000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
            final List<Data> segments = BLEDeviceFilter.extractManufacturerData(raw);
            assertEquals(1, segments.size());
            assertEquals("1005421C1E616A", segments.get(0).hexEncodedString());
        }
        {
            // iPhoneSE 1st gen w/ Herald
            final Data raw = Data.fromHexEncodedString("02011a020a0c11079bfd5bd672451e80d3424647af328142");
            final List<Data> segments = BLEDeviceFilter.extractManufacturerData(raw);
            assertEquals(0, segments.size());
        }
        {
            // iPhoneSE 1st gen background
            final Data raw = Data.fromHexEncodedString("02011a020a0c14ff4c000100000000000000000000200000000000");
            final List<Data> segments = BLEDeviceFilter.extractManufacturerData(raw);
            assertEquals(1, segments.size());
            assertEquals("0100000000000000000000200000000000", segments.get(0).hexEncodedString());
        }
        {
            // iPhoneX
            final Data raw = Data.fromHexEncodedString("1eff4c001219006d17255505df2aec6ef580be0ddeba8bb034c996de5b0200");
            final List<Data> segments = BLEDeviceFilter.extractManufacturerData(raw);
            assertEquals(1, segments.size());
            assertEquals("1219006d17255505df2aec6ef580be0ddeba8bb034c996de5b0200".toUpperCase(), segments.get(0).hexEncodedString());
        }
        {
            // iPhone7
            final Data raw = Data.fromHexEncodedString("0bff4c001006061a396363ce");
            final List<Data> segments = BLEDeviceFilter.extractManufacturerData(raw);
            assertEquals(1, segments.size());
            assertEquals("1006061a396363ce".toUpperCase(), segments.get(0).hexEncodedString());
        } // AFC Adding multiple test cases like this means you only ever see the first error reported. Best to re-write as separate test functions
        {
            // nRFConnect app running on iPhone - a Valid device
            final Data raw = Data.fromHexEncodedString("1bff4c000c0e00c857ac085510515d52cf3862211006551eee51497a");
            final List<Data> segments = BLEDeviceFilter.extractManufacturerData(raw);
            assertEquals(2, segments.size()); // AFC This returns 1 if parsed incorrectly
            assertEquals("0c0e00c857ac085510515d52cf386221".toUpperCase(), segments.get(0).hexEncodedString()); // AFC too long - Current implementation incorrectly ignores the apple segment length, 0e
            assertEquals("1006551eee51497a".toUpperCase(), segments.get(1).hexEncodedString()); // AFC This is not returned as a separate segment
        }
    }

    @Test
    public void testReceivedManufacturerDataAppleTV() throws Exception {
        final Data raw = Data.fromHexEncodedString("02011a020a0c0aff4c00100508141bba69");
        final List<Data> messages = BLEDeviceFilter.extractMessageData(BLEDeviceFilter.extractManufacturerData(raw));
        assertEquals(1, messages.size());
        assertEquals("100508141BBA69", messages.get(0).hexEncodedString());
    }

    @Test
    public void testReceivedManufacturerDataCoincidence() throws Exception {
        final Data raw = Data.fromHexEncodedString("02011a020a0c0aff4c0010050814ff4c00"); // AFC step through this to see lengthOfManufacturerData = 10
        final List<Data> messages = BLEDeviceFilter.extractMessageData(BLEDeviceFilter.extractManufacturerData(raw));
        assertEquals(1, messages.size());
        assertEquals("10050814FF4C00", messages.get(0).hexEncodedString());
    }

    @Test
    public void testReceivedManufacturerDataMultipleAppleSegments() throws Exception {
        final Data raw = Data.fromHexEncodedString("02011a0aff4c0010050814123456100101"); // AFC step through this to see lengthOfManufacturerData = 10
        final List<Data> messages = BLEDeviceFilter.extractMessageData(BLEDeviceFilter.extractManufacturerData(raw));
        assertEquals(2, messages.size());
        assertEquals("10050814123456", messages.get(0).hexEncodedString());
        assertEquals("100101", messages.get(1).hexEncodedString());
    }

    @Test
    public void testReceivedManufacturerDataLegacyZeroOne() throws Exception {
        final Data raw = Data.fromHexEncodedString("02011a020a0c0aff4c001005031c8ba89d14ff4c000100200000000000000000000000000000000000000000000000000000000000000000000000000000");
        final List<Data> messages = BLEDeviceFilter.extractMessageData(BLEDeviceFilter.extractManufacturerData(raw));
        assertEquals(2, messages.size());
        assertEquals("1005031c8ba89d".toUpperCase(), messages.get(0).hexEncodedString());
        assertEquals("0100200000000000000000000000000000", messages.get(1).hexEncodedString());
    }

    @Test
    public void testReceivedManufacturerDataMacbookPro() throws Exception {
        final Data raw = Data.fromHexEncodedString("02011a0aff4c001005031c0b4cac");
        final List<Data> messages = BLEDeviceFilter.extractMessageData(BLEDeviceFilter.extractManufacturerData(raw));
        assertEquals(1, messages.size());
        assertEquals("1005031C0B4CAC", messages.get(0).hexEncodedString());
    }

    @Test
    public void testReceivedManufacturerDataMacbookProUnderflow() throws Exception {
        final Data raw = Data.fromHexEncodedString("02011a0aff4c001005031c0b4c");
        final List<Data> messages = BLEDeviceFilter.extractMessageData(BLEDeviceFilter.extractManufacturerData(raw));
        assertEquals(0, messages.size());
    }

    @Test
    public void testReceivedManufacturerDataMacbookProOverflow() throws Exception {
        final Data raw = Data.fromHexEncodedString("02011a0aff4c001005031c0b4cac02011a0aff4c00100503");
        final List<Data> messages = BLEDeviceFilter.extractMessageData(BLEDeviceFilter.extractManufacturerData(raw));
        assertEquals(1, messages.size());
        assertEquals("1005031C0B4CAC", messages.get(0).hexEncodedString());
    }

    @Test
    public void testMessageData() throws Exception { // AFC these hex strings are actually incorrectly formatted and won't be passed from scan record - our hex() functions are generating extraneous additional data
        {
            final Data raw = Data.fromHexEncodedString("02011A020A0C0BFF4C001006071EA3DD89E014FF4C0001000000000000000000002000000000000000000000000000000000000000000000000000000000");
            final List<Data> messages = BLEDeviceFilter.extractMessageData(BLEDeviceFilter.extractManufacturerData(raw));
            assertEquals(2, messages.size());
            assertEquals("1006071EA3DD89E0", messages.get(0).hexEncodedString());
            assertEquals("0100000000000000000000200000000000", messages.get(1).hexEncodedString());
        }
        {
            final Data raw = Data.fromHexEncodedString("02011A14FF4C0001000000000000000000002000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
            final List<Data> messages = BLEDeviceFilter.extractMessageData(BLEDeviceFilter.extractManufacturerData(raw));
            assertEquals(1, messages.size());
            assertEquals("0100000000000000000000200000000000", messages.get(0).hexEncodedString());
        }
        {
            final Data raw = Data.fromHexEncodedString("02011A020A0C0BFF4C0010060C1E4FDE4DF714FF4C0001000000000000000000002000000000000000000000000000000000000000000000000000000000");
            final List<Data> messages = BLEDeviceFilter.extractMessageData(BLEDeviceFilter.extractManufacturerData(raw));
            assertEquals(2, messages.size());
            assertEquals("10060C1E4FDE4DF7", messages.get(0).hexEncodedString());
            assertEquals("0100000000000000000000200000000000", messages.get(1).hexEncodedString());
        }
        {
            final Data raw = Data.fromHexEncodedString("0201060AFF4C001005421C1E616A000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
            final List<Data> messages = BLEDeviceFilter.extractMessageData(BLEDeviceFilter.extractManufacturerData(raw));
            assertEquals(1, messages.size());
            assertEquals("1005421C1E616A", messages.get(0).hexEncodedString());
        }
        {
            final List<Data> segments = new ArrayList<>(0);
            final Data segment = new Data();
            segment.append(Data.fromHexEncodedString("10060C1E4FDE4DF7"));
            segment.append(Data.fromHexEncodedString("1005421C1E616A"));
            segment.append(Data.fromHexEncodedString("1006071EA3DD89E0"));
            segments.add(segment);
            final List<Data> messages = BLEDeviceFilter.extractMessageData(segments);
            assertEquals(3, messages.size());
            assertEquals("10060C1E4FDE4DF7", messages.get(0).hexEncodedString());
            assertEquals("1005421C1E616A", messages.get(1).hexEncodedString());
            assertEquals("1006071EA3DD89E0", messages.get(2).hexEncodedString());
        }
    }

    @Test
    public void testCompilePatterns() throws Exception {
        final List<BLEDeviceFilter.FilterPattern> filterPatterns = BLEDeviceFilter.compilePatterns(new String[]{"^10....04", "^10....14"});
        assertEquals(2, filterPatterns.size());
        assertNotNull(BLEDeviceFilter.match(filterPatterns, "10060C044FDE4DF7"));
        assertNotNull(BLEDeviceFilter.match(filterPatterns, "10060C144FDE4DF7"));

        // Ignoring dots
        assertNotNull(BLEDeviceFilter.match(filterPatterns, "10XXXX044FDE4DF7"));
        assertNotNull(BLEDeviceFilter.match(filterPatterns, "10XXXX144FDE4DF7"));

        // Not correct values
        assertNull(BLEDeviceFilter.match(filterPatterns, "10060C054FDE4DF7"));
        assertNull(BLEDeviceFilter.match(filterPatterns, "10060C154FDE4DF7"));

        // Not start of pattern
        assertNull(BLEDeviceFilter.match(filterPatterns, "010060C054FDE4DF7"));
        assertNull(BLEDeviceFilter.match(filterPatterns, "010060C154FDE4DF7"));
    }
}