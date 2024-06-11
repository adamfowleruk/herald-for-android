package io.heraldprox.herald.sensor.analysis.evaluation;

import java.util.SortedMap;
import java.util.TreeMap;

import io.heraldprox.herald.sensor.datatype.Callback;
import io.heraldprox.herald.sensor.datatype.Tuple;

/**
 * Provides efficient indexing of a value by frequencies. Guarantees to return a 0 frequency on
 * get rather than through an exception. Utility functions for ranges and incrementing frequencies.
 *
 * Note: Unlike the TimeSeries class, this classes from range parameter is inclusive not
 * exclusive. This is because this class does not know the semantics of type E and so cannot
 * managed that boundary value itself.
 *
 * Note: ALPHA QUALITY CODE UNTIL PROVEN IN EXPERIMENTS.
 *
 * @param <E> The type for which Frequency data will be indexed
 */
public class Frequencies<E> { // TODO implement Collection<E>? OR SORTEDMAP<E,LONG>?
    TreeMap<E,Long> frequencies = new TreeMap<>();

    public Frequencies() {
        // default constructor
    }

    public void put(E value,Long frequency) {
        frequencies.put(value,frequency);
    }

    public void increment(E key) {
        increase(key,Long.valueOf(1));
    }

    public void increase(E key, Long amountBy) {
        if (!frequencies.containsKey(key)) {
            frequencies.put(key,Long.valueOf(amountBy));
        }
        frequencies.put(key,Long.valueOf(frequencies.get(key).longValue() + amountBy));
    }

    public void getRange(E fromInclusive, E toInclusive, Callback<Tuple<E,Long>> cb) {
        final SortedMap<E,Long> submap = frequencies.subMap(fromInclusive, toInclusive);
        for (E key: submap.keySet()) {
            cb.accept(new Tuple<E,Long>(key,submap.get(key)));
        }
    }

    public Long get(E key) {
        if (!frequencies.containsKey(key)) {
            return Long.valueOf(0);
        }
        return frequencies.get(key);
    }

    public void getAll(Callback<Tuple<E,Long>> cb) {
        for (E key: frequencies.keySet()) {
            cb.accept(new Tuple<>(key,frequencies.get(key)));
        }
    }

    public void clear() {
        frequencies.clear();
    }

    public void clearBetween(E fromInclusive,E toInclusive) {
        final SortedMap<E,Long> submap = frequencies.subMap(fromInclusive, toInclusive);
        for (E key: submap.keySet()) {
            frequencies.remove(key);
        }
    }

    /**
     * WARNING: Returns number of unique values for which there is a frequency, not total number of readings (totalFrequency).
     * @see totalFrequencies()
     * @return
     */
    public int size() {
        return frequencies.keySet().size();
    }

    public long totalFrequencies() {
        final long[] total = {0};
        getAll(new Callback<Tuple<E, Long>>() {
            @Override
            public void accept(Tuple<E, Long> value) {
                total[0] += value.b;
            }
        });

        return total[0];
    }

    public Tuple<E,Long> mostFrequent() {
        if (size() == 0) {
            return null; // cannot instantiate E!
        }
        E mostFrequent = null;
        Long highestFrequency = Long.valueOf(0);
        for (E k : frequencies.keySet()) {
            Long freq = frequencies.get(k);
            if (freq > highestFrequency) {
                mostFrequent = k;
                highestFrequency = freq;
            }
        }
        return new Tuple<E,Long>(mostFrequent,highestFrequency);
    }
}
