
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
public class collapse16 {

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
    public void testScalarCollapse16() {
        collapse16(arrScalar);
    }

    @Benchmark
    public void testVectorCollapse16() {
        collapse16Vector(arrVector);
    }


    static void collapse16(int[] arr) {
        for (int i = 0; i < 128; ++i) {
            arr[i] = (arr[i] << 16) | arr[128 + i];
        }
    }

    static final VectorSpecies<Integer> INT_SPECIES = IntVector.SPECIES_PREFERRED; // 256-bit vectors, 8 ints per vector
    static void collapse16Vector(int[] arr) {
        for (int i = 0; i < 128; i += INT_SPECIES.length()) {
            IntVector hi = IntVector.fromArray(INT_SPECIES, arr, i);        // upper 16 bits
            IntVector lo = IntVector.fromArray(INT_SPECIES, arr, 128 + i);  // lower 16 bits

            IntVector res = hi.lanewise(LSHL, 16)   // shift high half up
                    .or(lo);              // merge with low half
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

        collapse16Vector(vectorResult);
        collapse16(scalarResult);

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
