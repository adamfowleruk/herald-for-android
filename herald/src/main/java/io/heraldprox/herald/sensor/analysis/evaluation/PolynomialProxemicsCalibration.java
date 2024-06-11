package io.heraldprox.herald.sensor.analysis.evaluation;

import java.util.ArrayList;
import java.util.Hashtable;

import io.heraldprox.herald.sensor.data.TextFile;
import io.heraldprox.herald.sensor.datatype.Callback;
import io.heraldprox.herald.sensor.datatype.Date;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;
import io.heraldprox.herald.sensor.datatype.Tuple;

import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.solvers.LaguerreSolver;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;

/**
 * The polynomial calibrator will try to find the two proxemic RSSI-frequency peaks for
 * 'Personal space' and 'Public space' as defined by Hall 1966. It does this by using a factor
 * 5 polynomial curve fitting routine.
 *
 * Once the locations of the first two peaks are found,
 * it then performs a linear calibration of a given smoothed RSSI value based on these peaks.
 *
 * Note: ALPHA QUALITY CODE UNTIL PROVEN IN EXPERIMENTS.
 */
public class PolynomialProxemicsCalibration implements Calibrator {
    protected final int factor;
    protected final PolynomialCurveFitter fitter;

    protected Frequencies<CorrectedRSSI> rssiFrequencies = new Frequencies<>();
//    Hashtable<Long,Long> rssiFrequencies = new Hashtable<>();

    protected double[] coefficients = new double[]{1,1,1,1,1};

    protected final WeightedObservedPoints obs = new WeightedObservedPoints();

    protected PolynomialFunction calibration = new PolynomialFunction(coefficients);

    protected double[] maximaPeakXValues = new double[]{};

    // Linear calibration values:-
//    double dY = 1;
//    double dX = 1;
//    double m = 1;
//    double c = 0;
    double linearOffsetX = 0;
    double linearScaleX = 1;
    protected TextFile freqFile;
    protected TextFile calibrationsFile;

    public PolynomialProxemicsCalibration(int polyFactor, TextFile freqFile, TextFile calibrationsFile) {
        this.factor = polyFactor;
        this.fitter = PolynomialCurveFitter.create(factor);
        this.freqFile = freqFile;
        this.calibrationsFile = calibrationsFile;
    }

    @Override
    public void updateCalibration(Date fromExclusive, Date toInclusive, Hashtable<TargetIdentifier, DeviceCalibrationRecord> records) {
        Frequencies<CorrectedRSSI> values = new Frequencies<>();

//        Frequencies<CorrectedRSSI> smoothedFrequencies = new Frequencies<>();
        // A. ADD ALL NEW DATA AND RECALCULATE TOTAL FREQUENCIES
        // For all records, get the ones we know are valid for use in calibration
        //  - Append these to the existing rssiFrequencies totals
        // TODO take into account when they approach LONG_MAX (half their frequencies and continue, save the number of halvings)
        // Smooth over smoothing period
        long to = toInclusive.secondsSinceUnixEpoch();
        for (long startExclusive = fromExclusive.secondsSinceUnixEpoch() - 3; startExclusive <= to + 2; startExclusive += 5) {
            for (DeviceCalibrationRecord record: records.values()) {
                // Get a 30 second smoothed value for this device within EACH 5 second time window
                // Note: This eliminates 'chatty devices' issues.
                values.clear();
                record.addFromWindowIfFinallyValid(new Date(startExclusive - 15), new Date(startExclusive + 15), values);

                MeanCorrectedRSSICallback cb = new MeanCorrectedRSSICallback();
                // now get a smooth average for a SINGLE RSSI at the end time for this period
                values.getAll(cb);

                rssiFrequencies.increment(new CorrectedRSSI((long)cb.mean()));
            }
        }
//        for (DeviceCalibrationRecord record: records.values()) {
//            values.clear();
//            record.addFromWindowIfFinallyValid(fromExclusive,toInclusive,values);
////            for (Tuple<Double,Date> v: values) {
////                Long key = Long.valueOf(v.a.longValue());
////                if (!rssiFrequencies.containsKey(key)) {
////                    rssiFrequencies.put(key,Long.valueOf(1));
////                } else {
////                    rssiFrequencies.put(key,Long.valueOf(rssiFrequencies.get(key).longValue() + 1));
////                }
////            }
//        }
        // now use these frequencies as observed points' Y coordinate, with Cr as the X coordinate
//        double mostFrequentRssi = 1;
//        double highestFrequency = 0;

        // sanity check for no data for calibration
        if (rssiFrequencies.size() == 0) {
            return;
        }

        double frequencyFraction = 1.0 / rssiFrequencies.totalFrequencies();
        obs.clear();
        final String[] freqLine = {toInclusive.toString()};
        rssiFrequencies.getAll(new Callback<Tuple<CorrectedRSSI, Long>>() {
            @Override
            public void accept(Tuple<CorrectedRSSI, Long> value) {
                freqLine[0] += "," + value.a + ":" + value.b;
                obs.add(value.a.getValue().doubleValue(),value.b.doubleValue() * frequencyFraction); // normalise frequency between 0 and 1
            }
        });
        Tuple<CorrectedRSSI,Long> mostFrequent = rssiFrequencies.mostFrequent();
//        for (Long key : rssiFrequencies.keySet()) {
//            Long value = rssiFrequencies.get(key);
//            freqLine[0] += "," + key + ":" + value;
//            double rssi = key.doubleValue();
//            double freq = value.doubleValue();
//            obs.add(rssi,freq);
//            if (freq > highestFrequency) {
//                highestFrequency = freq;
//                mostFrequentRssi = rssi;
//            }
//        }
        freqFile.write(freqLine[0]);

        // B. CREATE OUR POLYNOMIAL CURVE FOR Cr TO FREQUENCY, AND FIND PEAKS AND THEIR X VALUES
        // Now perform polynomial fitting to curve y = f(x), with y being frequency and x being corrected RSSI (from 0 to ~120)
        coefficients = fitter.fit(obs.toList());

        // Now find turning points for peak/trough where f'(x) = 0
        // Ensure we pick the first two where f''(x) < 0 (i.e. local maxima, not minima)
        //  - these are the points for Personal and Public space
        calibration = new PolynomialFunction(coefficients); // f(x)
        PolynomialFunction derivative = calibration.polynomialDerivative(); // f'(x)

        // Use the point with maximum frequency as the initial point
        // Then solve for all complex roots
        LaguerreSolver solver = new LaguerreSolver();
        Complex[] allRoots = solver.solveAllComplex(derivative.getCoefficients(),mostFrequent.a.doubleValue());

        // Just take the approximate real value as the turning points

        // Now take the second derivative and determine the gradient (f''(x)) at these locations
        PolynomialFunction secondDerivative = derivative.polynomialDerivative(); // f''(x)
        double firstMaximaX = -1;
        double secondMaximaX = -1;
        double thirdMaximaX = -1;
        double firstMaximaXi = -1;
        double secondMaximaXi = -1;
        double thirdMaximaXi = -1;
        int numMaxima = 0;
        // Note: allRoots guaranteed to be in ascending order of X by Commons Math library
        for (Complex root: allRoots) {
            double turningPointAspect = secondDerivative.value(root.getReal());
            if (turningPointAspect < 0) { // is a local MAXIMA (i.e. peak) not minima (i.e. trough)
                // local Maxima - one of our peaks!
                // Note: We store their real part only, as the closest estimate to X (i.e. Cr) possible
                if (firstMaximaX < 0) {
                    firstMaximaX = root.getReal();
                    firstMaximaXi = root.getImaginary();
                    numMaxima = 1;
                } else if (secondMaximaX < 0) {
                    secondMaximaX = root.getReal();
                    secondMaximaXi = root.getImaginary();
                    numMaxima = 2;
                } else if (thirdMaximaX < 0) {
                    thirdMaximaX = root.getReal();
                    thirdMaximaXi = root.getImaginary();
                    numMaxima = 3;
                }
            }
        }
        System.out.print("We have " + numMaxima + " local maxima peaks");
        if (numMaxima < 2) {
            System.out.println("Not enough peaks in calibration. Skipping.");
            return;
        }
        maximaPeakXValues = new double[numMaxima];
        // Yes there is a better way to do this - but I'm tired, so am prioritising accuracy and ease of reading over generalisability
        for (int i = 0;i < numMaxima;i++) {
            if (0 == i) {
                maximaPeakXValues[i] = firstMaximaX;
            } else if (1 == i) {
                maximaPeakXValues[i] = secondMaximaX;
            } else if (2 == i) {
                maximaPeakXValues[i] = thirdMaximaX;
            }
        }

        // Now calculate the linear scaling values
        double firstY = calibration.value(firstMaximaX);
        double secondY = calibration.value(secondMaximaX);
        double thirdY = calibration.value(thirdMaximaX);
        double dY = secondY - firstY;
        double dX = secondMaximaX - firstMaximaX;
        double m = dY/dX;
        double c = firstY - (firstMaximaX * m);

        double xForYEqualToZero = -c / m;

        // C. USE PEAKS' X VALUES TO CREATE A LINEAR SCALE TO MODIFY RSSI CORRECTED FOR TxPower TO EXPECTED SIGNAL STRENGTH VALUES

        // We need the reference values for expected RSSI at these proxemic distances
        // Let us assume for now those distances are Centred from Hall (likely less than these):-
        double dIntimate = 0.23;
        double dPersonal = 0.84;
        double dSocial = 2.46;
        double dPublic = 5.65;

        // What should the expected corrected receiver gain be for these distances?
        double bluetoothMeanAdvertisingFrequency = Math.pow(10,6) * (2402 + 2426 + 2480) / 3.0; // MHz
        double cInAir = 299702547; // m/s
        // Assume a receiver gain of 0 dBm
        double constant = 4.0 * Math.PI * cInAir;
        // 0.64681038792082243787704755070941 = freq / constant
        // dPers expected Cr with no receiver gain = 2.2700459980820801481720385561334
        // dSocial expected Cr with no receiver gain = 11.603162418912029519841172489154
        // Note: Above are both positive and increasing because we've inverted the scale axis, unlike negative RSSI values
        double expCrPersonal = -20*Math.log(bluetoothMeanAdvertisingFrequency / (constant * dPersonal));
        double expCrSocial = -20*Math.log(bluetoothMeanAdvertisingFrequency / (constant * dSocial));

        double offsetX = expCrPersonal - firstMaximaX;
        double scaleX = (expCrSocial - expCrPersonal) / (secondMaximaX - firstMaximaX);
        if (scaleX > 0) {
            // sanity check
            linearOffsetX = offsetX;
            linearScaleX = scaleX;
        }
        // Note: Whilst scaleX should be 1.0, the attenuation of a phone position may have non linear effects. (i.e. 10% loss of signal strength rather than fixed value)

        // E. CALCULATE ESTIMATE FOR RECEIVER GAIN BASED ON THIS TOO (ALTHOUGH NOT USED IN CALIBRATION)
        // This is the value of xForYEqualToZero, scaled into the real signal strength cartesian realm
        double receiverGainEstimate = xForYEqualToZero * scaleX;

        // TODO Now report calibration values to the text file
        calibrationsFile.write(toInclusive.toString() + ",polynomial," +
                "firstMaximaX=" + firstMaximaX + ":firstMaximaXi=" + firstMaximaXi +
                ":secondMaximaX=" + secondMaximaX + ":secondMaximaXi=" + secondMaximaXi +
                ":thirdMaximaX=" + thirdMaximaX + ":thirdMaximaXi=" + thirdMaximaXi +
                ":firstMaximaY=" + firstY +
                ":secondMaximaY=" + secondY +
                ":thirdMaximaY=" + thirdY +
                ":offsetX=" + offsetX + ":scaleX=" + scaleX
        );

    }

    @Override
    public Double calibrateValue(Date recorded, Tuple<Double, Date> toCorrect) {
        if (0 == maximaPeakXValues.length) {
            return toCorrect.a;
        }
        return ((toCorrect.a.doubleValue() - linearOffsetX) * linearScaleX) + linearOffsetX;
    }
}
