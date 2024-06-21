package io.heraldprox.herald.sensor.metadata;

import io.heraldprox.herald.sensor.SensorMetadata;
import io.heraldprox.herald.sensor.datatype.Data;

public class BeaconMetadata extends SensorMetadata {

    protected boolean heraldBeacon = false;
    public BeaconMetadata(Data describes, boolean isHeraldBeacon) {
        super(describes);
        this.heraldBeacon = isHeraldBeacon;
    }
    @Override
    public String getDescription() {
        return super.getDescription();
    }

    @Override
    public String getMetadataClass() {
        return "Beacon";
    }

    @Override
    public String getInstanceId() {
        return data.toString(); // use full beacon metadata field
    }

    public boolean isHeraldBeacon() {
        return heraldBeacon;
    }
}
