package io.heraldprox.herald.sensor.analysis.evaluation;

import java.util.SortedMap;
import java.util.TreeMap;

import io.heraldprox.herald.sensor.datatype.Callback;
import io.heraldprox.herald.sensor.datatype.Date;
import io.heraldprox.herald.sensor.datatype.Tuple;

/**
 * Provides a time efficient collection for Time Series indexed data with arbitrary stored data.
 * Provides functions for ranges too that are also efficient.
 *
 * Note: ALPHA QUALITY CODE UNTIL PROVEN IN EXPERIMENTS.
 *
 * @param <E> The type of data that is indexed efficiently by time
 */
public class TimeSeries<E> { // TODO implement Collection<E>?
    protected TreeMap<Date,E> data = new TreeMap<Date, E>();

    public TimeSeries() {
        // empty constructor
    }

    public void put(Date key, E value) {
        data.put(key,value);
    }

    public void add(Tuple<Date,E> value) {
        data.put(value.a,value.b);
    }

    public E get(Date key) {
        return data.get(key);
    }

    public void getRange(Date fromExclusive,Date toInclusive, Callback<Tuple<Date,E>> cb) {
        final SortedMap<Date,E> submap = data.subMap(new Date(fromExclusive.secondsSinceUnixEpoch() + 1), toInclusive);
        for (Date key: submap.keySet()) {
            cb.accept(new Tuple<>(key,submap.get(key)));
        }
    }

    public void getAll(Callback<Tuple<Date,E>> cb) {
        for (Date key: data.keySet()) {
            cb.accept(new Tuple<>(key,data.get(key)));
        }
    }

    public void clear() {
        data.clear();
    }

    public void clearBetween(Date fromExclusive,Date toInclusive) {
        final SortedMap<Date,E> submap = data.subMap(new Date(fromExclusive.secondsSinceUnixEpoch() + 1), toInclusive);
        for (Date key: submap.keySet()) {
            data.remove(key);
        }
    }

    public int size() {
        return data.size();
    }
}
