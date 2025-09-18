package jpountz;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import jdk.incubator.vector.*;
import org.openjdk.jmh.annotations.*;

import static jdk.incubator.vector.VectorOperators.LSHR;

@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1, jvmArgsPrepend = {"--add-modules=jdk.incubator.vector"})
@State(Scope.Benchmark)
public class expand8 {

    // See this good resource on using SIMD for prefix sums: https://en.algorithmica.org/hpc/algorithms/prefix/
    private int[] arrScalar;
    private int[] arrVector;


    @Setup(Level.Trial)
    public void setup() {
        sanityCheck();
        arrScalar = new int[256];
        arrVector = new int[256];
        Random rand = new Random(42);

        for (int i = 0; i < 256; i++) {
            arrScalar[i] = rand.nextInt();
            arrVector[i] = arrScalar[i];
        }
    }

    @Benchmark
    public void testScalarExpand8() {
        expand8(arrScalar);
    }

    @Benchmark
    public void testVectorExpand8() {
        expand8Vector(arrVector);
    }


    static void expand8(int[] arr) {
        for (int i = 0; i < 64; ++i) {
            int l = arr[i];
            arr[i] = (l >>> 24) & 0xFF;
            arr[64 + i] = (l >>> 16) & 0xFF;
            arr[128 + i] = (l >>> 8) & 0xFF;
            arr[192 + i] = l & 0xFF;
        }
    }


    static final VectorSpecies<Integer> INT_SPECIES = IntVector.SPECIES_256; // 256-bit vectors, 8 ints per vector

    static void expand8Vector(int[] arr) {
        // BLOCK_SIZE is 256
        for (int i = 0; i < 64; i += INT_SPECIES.length()) {
            IntVector v = IntVector.fromArray(INT_SPECIES, arr, i);

            v.lanewise(LSHR, 24).intoArray(arr, i);
            v.lanewise(LSHR, 16).and(0xFF).intoArray(arr, 64 + i);
            v.lanewise(LSHR, 8).and(0xFF).intoArray(arr, 128 + i);
            v.and(0xFF).intoArray(arr, 192 + i);
        }
    }

    static void sanityCheck() {
        Random rnd = new Random(42);
        int[] input = new int[256];
        for (int i = 0; i < input.length; i++) {
            input[i] = rnd.nextInt();
        }

        int[] vectorResult = input.clone();
        int[] scalarResult = input.clone();

        expand8Vector(vectorResult);
        expand8(scalarResult);

        for (int i = 0; i < 256; i++) {
            if (vectorResult[i] != scalarResult[i]) {
                throw new AssertionError(
                        "Mismatch at index " + i + ": vector=" + vectorResult[i] +
                                ", scalar=" + scalarResult[i]);
            }
        }

        System.out.println("Sanity check passed: vectorized result matches scalar.");
    }
}
