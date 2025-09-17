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
public class expand8 {

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


    static final VectorSpecies<Integer> SPECIES = IntVector.SPECIES_256; // 256-bit vectors, 8 ints per vector

    static void expand8Vector(int[] arr) {
        // 256 elements, processed 8 ints at a time
        for (int i = 0; i < 64; i += SPECIES.length()) {
            IntVector v = IntVector.fromArray(SPECIES, arr, i);

            // extract 4 bytes per int in parallel
            IntVector b0 = v.lanewise(VectorOperators.LSHR, 24).and(0xFF);
            IntVector b1 = v.lanewise(VectorOperators.LSHR, 16).and(0xFF);
            IntVector b2 = v.lanewise(VectorOperators.LSHR, 8).and(0xFF);
            IntVector b3 = v.and(0xFF);

            b0.intoArray(arr, i);          // first 64 elements
            b1.intoArray(arr, 64 + i);     // next 64
            b2.intoArray(arr, 128 + i);    // next 64
            b3.intoArray(arr, 192 + i);    // last 64
        }
    }
}
