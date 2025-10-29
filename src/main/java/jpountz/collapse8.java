package jpountz;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import jdk.incubator.vector.*;
import org.openjdk.jmh.annotations.*;

import static jdk.incubator.vector.VectorOperators.LSHL;

@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1, jvmArgsPrepend = {"--add-modules=jdk.incubator.vector"})
@State(Scope.Benchmark)
public class collapse8 {

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
    public void testScalarCollapse8() {
        collapse8(arrScalar);
    }

    @Benchmark
    public void testVectorCollapse8() {
        collapse8Vector(arrVector);
    }


    static void collapse8(int[] arr) {
        for (int i = 0; i < 64; ++i) {
            arr[i] = (arr[i] << 24) |
                    (arr[64 + i] << 16) |
                    (arr[128 + i] << 8) |
                    arr[192 + i];
        }
    }


    static final VectorSpecies<Integer> INT_SPECIES = IntVector.SPECIES_PREFERRED; // 256-bit vectors, 8 ints per vector

    static void collapse8Vector(int[] arr) {
        for (int i = 0; i < 64; i += INT_SPECIES.length()) {
            IntVector b0 = IntVector.fromArray(INT_SPECIES, arr, i);          // 0..7
            IntVector b1 = IntVector.fromArray(INT_SPECIES, arr, 64 + i);     // 64..71
            IntVector b2 = IntVector.fromArray(INT_SPECIES, arr, 128 + i);    // 128..135
            IntVector b3 = IntVector.fromArray(INT_SPECIES, arr, 192 + i);    // 192..199

            IntVector res = b0.lanewise(LSHL, 24)
                    .or(b1.lanewise(LSHL, 16))
                    .or(b2.lanewise(LSHL, 8))
                    .or(b3);
            res.intoArray(arr, i);
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

        collapse8Vector(vectorResult);
        collapse8(scalarResult);

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
