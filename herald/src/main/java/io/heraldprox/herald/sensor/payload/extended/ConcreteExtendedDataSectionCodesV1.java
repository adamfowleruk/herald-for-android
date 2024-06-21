package io.heraldprox.herald.sensor.payload.extended;

/**
 * Extended data area code enums. See ConcreteExtendedDataSectionV1.code() for fetching values.
 *
 * THESE ENUMS ARE ONLY TO BE USED TRANSIENTLY AS A HELPER TO CODING, NEVER AS ACTUAL VALUES
 * STORED OVER TIME THEMSELVES. VALUES MAY CHANGE OR SWAP WITHOUT NOTICE. USE code(Enum) FOR
 * A LONG TERM PERSISTENT VALUE INSTEAD.
 *
 * See https://heraldprox.io/specs/payload-extended
 *
 * @since v2.3
 */
public enum ConcreteExtendedDataSectionCodesV1 {

    Version,
    RSSIDescription,
    RSSIErrorBound,
    SonarRangeEstimate,
    SonarErrorBound,
    ApproxGeoLocationText,
    ApproxGeoLocationGPS,
    ApproxGeoLocationWebMercator,
    ApproxGeoLocationITRF2014,
    LocalGridPosition, // changed from ReservedGeo09 in Herald v2.3
    ReservedGeo0a,
    ReservedGeo0b,
    ReservedGeo0c,
    ReservedGeo0d,
    ReservedGeo0e,
    ReservedGeo0f,
    VenueName,
    VenueArea,
    VenueDisambiguation,
    VenueURL,
    VenueAddress,
    EntrySource,
    EntryVersion,
    LocationCountryID,
    LocationGS1ID,
    Reserved19,
    ContactEventStartTime,
    ContactEventEndTime,
    EncryptedDataLocal,
    EncryptedDataAuthority,
    YourLastRSSI,
    YourLastTxPower,

    ExtendedCodeLength,

    Invalid,
}
