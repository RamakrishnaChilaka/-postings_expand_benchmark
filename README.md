See https://en.algorithmica.org/hpc/algorithms/prefix/ for a good reference on computing prefix sums using SIMD. SIMD is supposed to make prefix sums multiple times faster, but this isn't the case currently with Java's Panama vector API.

To run benchmarks:

$ mvn clean package

$ java -jar target/benchmarks.jar

To see the produced assembly:

$ java -jar target/benchmarks.jar -prof perfasm

## Benchmark Results on 256 bit vectors

| Benchmark                     | Mode | Cnt |   Score   |  Error  | Units |
|-------------------------------|------|-----|-----------|---------|-------|
| expand16 (Scalar)             | thrpt|  5  | 112.842   | ± 0.221 | ops/us |
| expand16 (Vector)             | thrpt|  5  | 105.594   | ± 1.307 | ops/us |
| expand8 (Scalar)              | thrpt|  5  |  66.726   | ± 0.452 | ops/us |
| expand8 (Vector)              | thrpt|  5  | 105.821   | ± 0.272 | ops/us |

### Key Takeaways
- **expand8**: Vectorized version is ~59% faster than scalar (66.7 → 105.8 ops/us).
- **expand16**: Scalar slightly outperforms vector (112.8 vs 105.6 ops/us).
- Vectorization is highly beneficial for 8-bit extraction, but less so for 16-bit where scalar loop is already very efficient.

## Benchmark Results on 128 bit vectors
```
Benchmark                       Mode  Cnt    Score   Error   Units
j.expand16.testScalarExpand16  thrpt    5  112.636 ± 1.064  ops/us
j.expand16.testVectorExpand16  thrpt    5   79.319 ± 0.367  ops/us
j.expand8.testScalarExpand8    thrpt    5   66.782 ± 0.483  ops/us
j.expand8.testVectorExpand8    thrpt    5   79.338 ± 0.676  ops/us
```
