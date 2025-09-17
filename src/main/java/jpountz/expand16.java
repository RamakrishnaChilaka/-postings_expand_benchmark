package jpountz;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import jdk.incubator.vector.*;
import org.openjdk.jmh.annotations.*;

@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1, jvmArgsPrepend = {"--add-modules=jdk.incubator.vector"})
@State(Scope.Benchmark)
public class expand16 {

    // See this good resource on using SIMD for prefix sums: https://en.algorithmica.org/hpc/algorithms/prefix/
    private int[] arrScalar;
    private int[] arrVector;


    @Setup(Level.Trial)
    public void setup() {
        arrScalar = new int[256];
        arrVector = new int[256];
        Random rand = new Random(42);

        for (int i = 0; i < 256; i++) {
            arrScalar[i] = rand.nextInt();
            arrVector[i] = arrScalar[i];
        }
    }

    @Benchmark
    public void testScalarExpand16() {
        expand16(arrScalar);
    }

    @Benchmark
    public void testVectorExpand16() {
        expand16Vector(arrVector);
    }


    static void expand16(int[] arr) {
        for (int i = 0; i < 128; ++i) {
            int l = arr[i];
            arr[i] = (l >>> 16) & 0xFFFF;
            arr[128 + i] = l & 0xFFFF;
        }
    }


    static final VectorSpecies<Integer> SPECIES = IntVector.SPECIES_256; // 256-bit vectors, 8 ints per vector

    static void expand16Vector(int[] arr) {
        for (int i = 0; i < 128; i += SPECIES.length()) {
            IntVector v = IntVector.fromArray(SPECIES, arr, i);

            // Extract upper and lower 16 bits
            IntVector hi = v.lanewise(VectorOperators.LSHR, 16).and(0xFFFF);
            IntVector lo = v.and(0xFFFF);

            // Store results into their positions
            hi.intoArray(arr, i);           // first 128 elements
            lo.intoArray(arr, 128 + i);     // next 128 elements
        }
    }
}
