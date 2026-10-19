
package com.google.refine.sampling;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.math3.stat.inference.ChiSquareTest;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ReservoirSamplerTest {

    private final ReservoirSampler sampler = new ReservoirSampler();

    @Test
    public void testBasicSampling() {
        // given
        int reservoirSize = 10;
        List<Integer> list = createList(100);

        // when
        List<Integer> sample = sampler.sample(list, reservoirSize);

        // then
        Assert.assertEquals(sample.size(), reservoirSize); // sample should always be size of reservoir
        Assert.assertTrue(list.containsAll(sample)); // all elements in sample are taken from list
        Assert.assertEquals(sample, sample.stream().distinct().collect(Collectors.toList())); // no duplicates in sample
    }

    @Test
    public void testSamplingResultsInEqualProbabilityForEachElementToBeIncludedInSample() {
        // given
        int reservoirSize = 10;
        List<Integer> list = createList(100);

        // Run reservoir sampling multiple times and count occurrence of each element in sample
        int runs = 10000;
        long[] observedCounts = new long[list.size()];
        List<Integer> sample;
        for (int run = 0; run < runs; run++) {
            sample = sampler.sample(list, reservoirSize);
            for (int sampledItem : sample) {
                observedCounts[sampledItem]++;
            }
        }

        // Expected count per item assuming uniform distribution
        double expectedCount = (runs * reservoirSize) / (double) list.size();
        double[] expectedCounts = new double[list.size()];
        Arrays.fill(expectedCounts, expectedCount);

        // Perform chi-square test
        ChiSquareTest chiSquareTest = new ChiSquareTest();
        double pValue = chiSquareTest.chiSquareTest(expectedCounts, observedCounts);

        // Assert p-value is high enough (no significant deviation from uniformity)
        Assert.assertTrue(pValue > 0.05, "Reservoir sampling failed uniformity test: p-value=" + pValue);
    }

    // ------------------ edge cases ----------------
    @Test
    public void testSamplingWithListSizeSmallerThenReservoirSize() {
        // given
        int reservoirSize = 100;
        List<Integer> list = createList(10);

        // when
        List<Integer> sample = sampler.sample(list, reservoirSize);

        // then reservoir = original list
        Assert.assertEquals(sample.size(), list.size());
        Assert.assertEquals(sample, list);
    }

    @Test
    public void testSamplingWithListSizeEqualToReservoirSize() {
        // given
        int reservoirSize = 10;
        List<Integer> list = createList(reservoirSize);

        // when
        List<Integer> sample = sampler.sample(list, reservoirSize);

        // then reservoir = original list
        Assert.assertEquals(sample.size(), reservoirSize);
        Assert.assertEquals(sample, list);
    }

    @Test
    public void testSamplingWithReservoirSizeZero() {
        // given
        int reservoirSize = 0;
        List<Integer> list = createList(10);

        // when
        List<Integer> sample = sampler.sample(list, reservoirSize);

        // then reservoir should be empty
        Assert.assertTrue(sample.isEmpty());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testSamplingWithNegativeReservoirSize() {
        // given
        int reservoirSize = -1;
        List<Integer> list = createList(10);

        // when
        sampler.sample(list, reservoirSize);

        // then throw IllegalArgumentException (see test header)
    }

    @Test
    public void testSamplingOnEmptyList() {
        // given
        int reservoirSize = 10;
        List<Integer> list = new ArrayList<>();

        // when
        List<Integer> sample = sampler.sample(list, reservoirSize);

        // then reservoir should be empty
        Assert.assertTrue(sample.isEmpty());
    }

    // ----------- Utils --------------
    private List<Integer> createList(int range) {
        return IntStream.range(0, range)
                .boxed()
                .collect(Collectors.toList());
    }
}
