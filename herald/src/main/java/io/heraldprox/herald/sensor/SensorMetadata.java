package io.heraldprox.herald.sensor;

import io.heraldprox.herald.sensor.datatype.Data;

/**
 * Provides an arbitrary way to tag a device with a particular piece of metadata.
 * Particularly useful when it comes to classifying devices as a particular make/model or
 * with a particular capability (laptop, mobile device, beacon, etc.).
 *
 * Subclass this class to define your own metadata tagging. Have your code monitor the other
 * low-level Herald callbacks, like didDetect, didMeasure, didReceive, and then save a piece of
 * metadata on the Device class to have it advertised to all Herald app listeners.
 */
public abstract class SensorMetadata implements Comparable<SensorMetadata> {
    protected final Data data;
    public SensorMetadata(Data describing) {
        this.data = describing;
    }

    /**
     * Returns a Human readable English UK (en-GB) string specifying the description of this object
     * in terms of this metadata class.
     * @return
     */
    public String getDescription() {
        return "Unclassified: " + this.getClass().getName();
    }

    /**
     * Returns the class of this metadata. The same SensorMetadata Java class may support multiple
     * classes of object, so we've left this as an instance method.
     * @return
     */
    public abstract String getMetadataClass();

    /**
     * Returns this identify of this device instance, if one exists, WITHIN this metadata class.
     * Returns the empty string (not null!) if no instance ID is supported.
     * @return
     */
    public abstract String getInstanceId();

    /**
     * Method to add to identify the object as the same metadata class AND instance ID
     * @param other
     * @return
     */
    public boolean equals(SensorMetadata other) {
        return (other.getMetadataClass() == getMetadataClass()) && (other.getInstanceId() == getInstanceId());
    }

    /**
     * Compares this metadata object to another. Basic implements based on MetadataClass and InstanceID provided.
     * If this method returns 0 then equals(SensorMetadata) MUST return true. If this isn't the case for your
     * custom subclass, then you need to override both equals() and compareTo() in your subclass.
     *
     * @param o the object to be compared.
     * @return
     */
    @Override
    public int compareTo(SensorMetadata o) {
        int myClassCompare = getMetadataClass().compareTo(o.getMetadataClass());
        if (0 != myClassCompare) {
            return myClassCompare;
        }
        return getInstanceId().compareTo(o.getInstanceId());
    }

    /**
     * Returns any underlying data instance (E.g. manufacturer data area) that led to this determination.
     * @return
     */
    public Data getData() {
        return data;
    }
}
