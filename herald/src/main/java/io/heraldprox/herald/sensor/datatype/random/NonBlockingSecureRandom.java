//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype.random;

import android.app.ActivityManager;

import androidx.annotation.NonNull;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import io.heraldprox.herald.sensor.data.ConcreteSensorLogger;
import io.heraldprox.herald.sensor.data.SensorLogger;
import io.heraldprox.herald.sensor.datatype.Data;
import io.heraldprox.herald.sensor.datatype.Int32;
import io.heraldprox.herald.sensor.datatype.Int64;
import io.heraldprox.herald.sensor.datatype.TimeInterval;
import io.heraldprox.herald.sensor.payload.simple.F;

/**
 * Non-blocking cryptographically secure random source based on a combination of a time-based
 * seed generator, reliable external entropy sources, and non-blocking cryptographic functions.
 * - Time-based seed generator uses nanosecond clock sampled at millisecond resolution according
 *   to thread sleep time which is unpredictable due to wider system processes. This is the same
 *   concept as the ThreadSeedGenerator random seed source used by SecureRandom.
 * - External entropy sources that are known to be plentiful in the target domain include BLE
 *   MAC address of other devices (usually generated by SecureRandom / UUID), detection time,
 *   and also call time of random function. All are truly unpredictable. Even when there are
 *   no other devices in the vicinity, the call time itself is unpredictable like the time-based
 *   seed generator.
 * - SHA-256 cryptographic hashing algorithm is known to generate hashes where the individual
 *   bits follow a uniform distribution. This function is used to advance the internal state of
 *   this random source, by hashing a combination of current state and external entropy.
 *
 * Algorithm and performance characteristics:
 * - 2048 bit seed entropy collected from thread scheduler timing of 1 millisecond at nanosecond resolution
 * - Up to 2048 bit external entropy collected from BLE MAC address of target devices and detection time
 * - 2048 bit internal state updated by SHA-256 of current state and available external entropy
 * - 2048 bit ephemeral state derived from SHA-256 of XOR(previous state, current state)
 * - Random bits derived from recursive SHA-256 of XOR(ephemeral state, random output)
 * - 6298ns/call to nextLong when the internal state is replaced every second.
 * - Distribution test shows solution offers similar characteristics as BlockingSecureRandomNIST
 *   - BlockingSecureRandomNIST, samples=1000000, sequenceError=0.011800755248335893, valueError=0.0119505
 *   - NonBlockingSecureRandom,  samples=1000000, sequenceError=0.011930763568868408, valueError=0.0124595
 *   - BlockingSecureRandom,     samples=1000000, sequenceError=0.01340685803891449,  valueError=0.0125805
 *
 * SecureRandom is blocking when it is used on an idle device as entropy is exhausted. The lack
 * of system activities mean entropy is not replenished at a required rate, thus causing other
 * subsystems that rely on this mechanism for random data to also block, e.g. BLE MAC address
 * generation, and halting the app until the device is active again. Experiments have shown even
 * a single instantiation of SecureRandom on an idle device is sufficient to cause blocking for
 * this app and wider BLE operations. SecureRandom may also block the app on start up, especially
 * when the phone was recently booted up. Following numerous investigations and implementations
 * of a cryptographically secure random source, the evidence suggests SecureRandom should be
 * avoided completely to prevent blocking.
 *
 * Please refer to the following paper for a review of entropy sources for SecureRandom
 *
 * Michaelis K., Meyer C., Schwenk J. (2013) Randomly Failed! The State of Randomness in Current
 * Java Implementations. In: Dawson E. (eds) Topics in Cryptology – CT-RSA 2013. CT-RSA 2013.
 * Lecture Notes in Computer Science, vol 7779. Springer, Berlin, Heidelberg.
 * https://doi.org/10.1007/978-3-642-36095-4_9
 *
 * This new solution addresses previous vulnerabilities by using
 * - 2048 bits as internal state
 * - SHA256 instead of SHA1 for hashing
 * - Single thread for entropy collection
 * - Initial entropy collection is blocking
 * - Reseeding is non-blocking, thus attack on system load cannot compromise internal state
 *
 * Note: Logging has been disabled by commenting to avoid information leakage, only fault
 * messages are retained.
 */
public class NonBlockingSecureRandom extends RandomSource {
    private final static SensorLogger logger = new ConcreteSensorLogger("Sensor", "Datatype.NonBlockingSecureRandom");
    // Internal state is replaced at regular intervals
    private final int seedBits;
    private final TimeInterval reseedInterval;
    private final AtomicBoolean reseedInProgress = new AtomicBoolean(false);
    private long nextReseedTimestamp = 0;
    // Internal state
    private final AtomicReference<Data> state = new AtomicReference<>(new Data());


    /**
     * Non-blocking cryptographically secure random source based on a combination of a time-based
     * seed generator, reliable external entropy sources, and non-blocking cryptographic functions.
     *
     * @param seedBits Number of random bits for seeding the internal state, and also mixing with current state on state update.
     * @param reseedInterval Time between complete internal state replacement.
     */
    public NonBlockingSecureRandom(final int seedBits, final TimeInterval reseedInterval) {
        this.seedBits = seedBits;
        this.reseedInterval = reseedInterval;
        // Initialise internal state, this is a short blocking operation
        // that should complete in about 1 second.
        reseed(seedBits, true);
    }

    /**
     * Non-blocking cryptographically secure random source based on recommended reseeding interval
     * parameters for pseudo device address generation. Internal state is fully replaced once
     * every 6 hours.
     */
    public NonBlockingSecureRandom() {
        this(2048, TimeInterval.hours(6));
    }

    /**
     * Replace state with truly random data by using unpredictable thread scheduling times as
     * entropy data. Thread scheduling is influenced by wider system processes which are inherently
     * unpredictable. The process should take about 1 second to complete for 1024 bit of random data.
     */
    private synchronized void reseed(final int bits, final boolean blocking) {
        // Ignore reseed requests if not reach next reseed timestamp
        if (System.currentTimeMillis() < nextReseedTimestamp) {
            return;
        }
        // Ignore reseed requests if one is already in progress or blocking
        if (!reseedInProgress.compareAndSet(false, true)) {
            return;
        }
        // Create a new thread for gathering entropy data and generating
        // a new seed. Do not use logger in this thread as it will cause
        // a deadlock.
        final Thread thread = new Thread() {
            @Override
            public void run() {
                // Generate random data as basis for hashing by SHA256
                final Data randomData = new Data(new byte[bits / 8]);
                // Generate one bit per millisecond
                long lastNanoTime = 0;
                for (int i=randomData.value.length * 8; i>0;) {
                    // Generate one random bit based on last bit of nano time
                    // Given nanoTime() is nanosecond precision, not resolution,
                    // consecutive calls may report the same value, thus this
                    // condition is checked and result discounted to prevent
                    // unintentional collection of repeated bits
                    final long nanoTime = System.nanoTime();
                    if (nanoTime != lastNanoTime) {
                        i--;
                        final byte randomBit = (byte) (nanoTime & 1);
                        // Set random bit in random data
                        final int byteIndex = i / 8;
                        randomData.value[byteIndex] = (byte) ((randomData.value[byteIndex] << 1) | randomBit);
                        lastNanoTime = nanoTime;
                    }
                    // Sleep for about one millisecond +/- random amount dependent on thread
                    // scheduling according to wider system processes
                    try {
                        sleep(1);
                    } catch (InterruptedException e) {
                        // Ignore exceptions
                    }
                }
                // Compute hash of random data as new internal state
                final Data newState = hash(randomData);
                // Replace internal state
                state.set(newState);
                // Reseed complete
                reseedInProgress.set(false);
                // Schedule next reseed time
                nextReseedTimestamp = System.currentTimeMillis() + reseedInterval.millis();
            }
        };
        thread.start();
        // Wait to reseeding is complete
        if (blocking) {
            try {
                thread.join();
            } catch (Throwable e) {
                reseedInProgress.set(false);
            }
        }
    }

    @Override
    public synchronized void nextBytes(@NonNull final byte[] bytes) {
        // Requirement 1 : Truly random seed
        // CSPRNG must be initialised with a truly random seed to be cryptographically secure.
        // This is achieved by using unpredictable thread scheduling time as entropy data, mixed
        // with unpredictable real-world events.
        // - 1A. Entropy from function call time is determined by state of recently encountered
        //       devices, and the proximity and state of devices in the user environment.
        //       Even if the device is in isolation, the call time is still unpredictable at nano
        //       time scale as the source time keeper is based on an infinite CPU loop that samples
        //       current time at millisecond scale, quantized to 500ms scale.
        addEntropy((byte) (System.nanoTime() & 0xFF));
        // - 1B. Entropy from external sources that are likely to have been derived from disparate
        //       SecureRandom instances. Using the sequence of detected BLE MAC addressed and time
        //       of detection as entropy material. This has been handled by addEntropy().
        // Requirement 2 : Uniformly distributed PRNG
        // CSPRNG must deliver uniformly distributed random values and a long period length, such
        // that knowledge of prior values offer negligible or no benefit in predicting future values.
        // - 2A. SHA256 cryptographic hash function is efficient and non-blocking. The output bits
        //       follow a uniform distribution and its characteristics are well understood. Applying
        //       SHA256 to current state and entropy data to generate the new state. Entropy data
        //       prevents future states from being predicted from current state if internal state
        //       has been compromised.
        final Data currentState = state.get();
        final Data newStateSourceMaterial = new Data();
        newStateSourceMaterial.append(currentState);
        useEntropy(seedBits, newStateSourceMaterial);
        final Data newState = hash(newStateSourceMaterial);
        state.set(newState);
        // - 2B. Internal state is replaced at regular intervals with completely new random material.
        reseed(seedBits, false);
        // - 2C. Using a combination of current and new states to derive an ephemeral random seed
        //       for generating the actual random data. The ephemeral seed is cryptographically
        //       separated from the underlying internal state via SHA256 and also an XOR function.
        //       The transformation "ephemeralSeed = sha256(xor(currentState, newState))" means
        //       the identification of current or new state will be cryptographically challenging
        //       given the ephemeral seed.
        final Data ephemeralRandomSeed = hash(xor(currentState, newState));
        // - 2D. Uniformly distributed random data is generated by recursive application of SHA256
        //       to hashes derived from the ephemeral random seed. SHA256 hashes are known to offer
        //       uniformly distributed bits, thus offering a sound basis as a pseudo random number
        //       generator. This recursive process is non-blocking but computationally expensive
        //       but the solution is primarily designed for generation of pseudo device addresses
        //       which is only 6 bytes long derived from truncation of 8 bytes of random data.
        Data randomByteBlock = hash(ephemeralRandomSeed);
        final Data ephemeralRandomSeedSuffix = ephemeralRandomSeed.subdata(1);
        for (int i=0; i<bytes.length; i++) {
            // Taking the first byte from the hash as random data
            bytes[i] = randomByteBlock.value[0];
            // Hashing the remaining bytes mixed with part of the ephemeral random seed to generate
            // the next block
            randomByteBlock = hash(xor(ephemeralRandomSeedSuffix, randomByteBlock.subdata(1)));
        }
    }

    /**
     * XOR function : Compute left xor right, assumes left and right are the same length
     * If the left and right data are of different lengths, the function will return
     * xor(left, right) up to minimum length.
     */
    @NonNull
    public final static Data xor(@NonNull final Data left, @NonNull final Data right) {
//        if (left.value.length != right.value.length) {
//            logger.fault("XOR being applied to data of different lengths (left={},right={})", left.value.length, right.value.length);
//        }
        final byte[] leftByteArray = left.value;
        final byte[] rightByteArray = right.value;
        final byte[] resultByteArray = new byte[Math.min(leftByteArray.length, rightByteArray.length)];
        for (int i=resultByteArray.length; i-->0;) {
            resultByteArray[i] = (byte) (leftByteArray[i] ^ rightByteArray[i]);
        }
        return new Data(resultByteArray);
    }

}
