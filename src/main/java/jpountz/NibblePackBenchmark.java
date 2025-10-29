package jpountz;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import jdk.incubator.vector.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1, jvmArgsPrepend = {"--add-modules=jdk.incubator.vector"})
@State(Scope.Benchmark)
public class NibblePackBenchmark {
    // --- 1. Use @Param for dynamic BITS_PER_VALUE ---
    // The benchmark will run once for each value in this array.
    // 16 and above causes slow-down
    @Param({"2", "3", "4", "5", "6", "7", "8", "9", "10", "16", "18", "20", "22", "24", "26", "28", "30", "32"})
    public int bitsPerValue;

    private static final int PRIMITIVE_SIZE  = 32;

    // These values will be calculated in setup()
    private int intsPerShift;
    private int vectorLen;

    private int[] src;
    private int[] dstScalar;
    private int[] dstVector;

    @Setup(Level.Trial)
    public void setup() {
        // --- 2. Dynamic calculations based on @Param value ---
        this.intsPerShift = bitsPerValue * 8;
        this.vectorLen = this.intsPerShift; // Length of the destination array

        // Calculate source array size: PRIMITIVE_SIZE / bitsPerValue = number of values
        // that fit into one 32-bit int. We need this many groups of vectorLen.
        int srcLen = this.vectorLen * (PRIMITIVE_SIZE / bitsPerValue);

        Random rnd = new Random(42);
        src        = new int[srcLen];
        dstScalar  = new int[this.vectorLen];
        dstVector  = new int[this.vectorLen];

        // Ensure generated values fit within the current bitsPerValue
        for (int i = 0; i < src.length; i++) {
            src[i] = rnd.nextInt(1 << bitsPerValue);
        }

        sanityCheck();
    }

    /* ---------- scalar reference ---------- */
    @Benchmark
    public void scalarPack(Blackhole bh) {
        packNibblesScalar(bitsPerValue, vectorLen, src, dstScalar);
        bh.consume(dstScalar);
    }

    /* ---------- vectorised version ---------- */
    @Benchmark
    public void vectorPack(Blackhole bh) {
        packNibblesVector(bitsPerValue, vectorLen, src, dstVector);
        bh.consume(dstVector);
    }

    /* scalar code - UPDATED to take dynamic parameters */
    private static void packNibblesScalar(int bpv, int vectorLen, int[] src, int[] dst) {
        final int numIntsPerShift = vectorLen;
        int srcIdx = 0;
        int shift = PRIMITIVE_SIZE - bpv; // Use bpv (bitsPerValue)

        for (int i = 0; i < numIntsPerShift; ++i) {
            dst[i] = src[srcIdx++] << shift;
        }

        for (shift = shift - bpv; shift >= 0; shift -= bpv) {
            for (int i = 0; i < numIntsPerShift; ++i) {
                dst[i] |= src[srcIdx++] << shift;
            }
        }
    }

    // NOTE: The VectorSpecies must remain constant (IntVector.SPECIES_256)
    // for this single benchmark class, as it's a constant field.
    private static final VectorSpecies<Integer> SPECIES = IntVector.SPECIES_256;

    /* vectorised equivalent - UPDATED to take dynamic parameters */
    private static void packNibblesVector(int bpv, int vectorLen, int[] src, int[] dst) {
        final int lanes = SPECIES.length(); // 8
        int srcIdx = 0;

        for (int shift = PRIMITIVE_SIZE - bpv; shift >= 0; shift -= bpv) { // Use bpv
            for (int i = 0; i < vectorLen; i += lanes) { // Use vectorLen
                IntVector v = IntVector.fromArray(SPECIES, src, srcIdx)
                        .lanewise(VectorOperators.LSHL, shift);

                if (shift == PRIMITIVE_SIZE - bpv) {
                    v.intoArray(dst, i);
                } else {
                    v.or(IntVector.fromArray(SPECIES, dst, i))
                            .intoArray(dst, i);
                }
                srcIdx += lanes;
            }
        }
    }

    /* The sanity Check method - UPDATED to use dynamic parameters */
    public void sanityCheck() {
        int[] golden = dstScalar.clone();
        int[] vector = dstVector.clone();

        packNibblesScalar(bitsPerValue, vectorLen, src, golden);
        packNibblesVector(bitsPerValue, vectorLen, src, vector);

        for (int i = 0; i < vectorLen; i++) {
            if (golden[i] != vector[i])
                throw new AssertionError("Mismatch for BPV=" + bitsPerValue + " at " + i
                        + ": scalar=" + golden[i] + ", vector=" + vector[i]);
        }
        System.out.println("Sanity check passed for BPV=" + bitsPerValue + " during setup.");
    }
}