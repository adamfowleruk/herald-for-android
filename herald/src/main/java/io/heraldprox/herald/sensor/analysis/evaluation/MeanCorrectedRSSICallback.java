package io.heraldprox.herald.sensor.analysis.evaluation;

import io.heraldprox.herald.sensor.datatype.Callback;
import io.heraldprox.herald.sensor.datatype.Tuple;

public class MeanCorrectedRSSICallback implements Callback<Tuple<CorrectedRSSI,Long>> {
        long total = 0;
        long count = 0;
        @Override
        public void accept(Tuple<CorrectedRSSI, Long> value) {
            total += value.a.longValue()*value.b.longValue();
            count += value.b;
        }

        public double mean() {
            if (0 == count) {
                return 0;
            }
            return total / count;
        }
}
