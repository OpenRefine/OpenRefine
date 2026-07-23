
package com.google.refine.sampling;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.math3.stat.inference.ChiSquareTest;
import org.testng.Assert;
import org.testng.annotations.Test;

public class BernoulliSamplerTest {

    private final BernoulliSampler sampler = new BernoulliSampler();

    @Test
    public void testBasicSampling() {
        // given
        int percentage = 10;
        List<Integer> list = createList(100);

        // when
        List<Integer> sample = sampler.sample(list, percentage);

        // then
        Assert.assertTrue(list.containsAll(sample)); // all elements in sample are taken from list
        Assert.assertEquals(sample, sample.stream().distinct().collect(Collectors.toList())); // no duplicates in sample
    }

    @Test
    public void testSamplingResultsInGivenProbabilityForEachElementToBeIncludedInSample() {
        // given
        int percentage = 30;
        List<Integer> list = createList(100);

        // Run bernoulli sampling multiple times and count occurrence of each element in sample
        int runs = 10000;
        long[] observedCounts = new long[list.size()];
        List<Integer> sample;
        for (int run = 0; run < runs; run++) {
            sample = sampler.sample(list, percentage);
            for (int sampledItem : sample) {
                observedCounts[sampledItem]++;
            }
        }

        // Expected count per item = number of runs * probability
        double expectedCount = runs * percentage / (double) 100;
        double[] expectedCounts = new double[list.size()];
        Arrays.fill(expectedCounts, expectedCount);

        // Perform chi-square test
        ChiSquareTest chiSquareTest = new ChiSquareTest();
        double pValue = chiSquareTest.chiSquareTest(expectedCounts, observedCounts);

        // Assert p-value is high enough (no significant deviation from probability)
        Assert.assertTrue(pValue > 0.05, "Bernoulli sampler deviates from expected probability: p-value=" + pValue);
    }

    // ------------------ edge cases ----------------
    @Test
    public void testSamplingWithPercentageZero() {
        // given
        int percentage = 0;
        List<Integer> list = createList(10);

        // when
        List<Integer> sample = sampler.sample(list, percentage);

        // then sample should be empty
        Assert.assertTrue(sample.isEmpty());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testSamplingWithNegativePercentage() {
        // given
        int percentage = -1;
        List<Integer> list = createList(10);

        // when
        sampler.sample(list, percentage);

        // then throw IllegalArgumentException (see test header)
    }

    @Test
    public void testSamplingWithPercentageHundred() {
        // given
        int percentage = 100;
        List<Integer> list = createList(100);

        // when
        List<Integer> sample = sampler.sample(list, percentage);

        // then
        Assert.assertEquals(sample, list);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testSamplingWithPercentageOverHundred() {
        // given
        int percentage = 500;
        List<Integer> list = createList(10);

        // when
        sampler.sample(list, percentage);

        // then throw IllegalArgumentException (see test header)
    }

    @Test
    public void testSamplingOnEmptyList() {
        // given empty list
        int percentage = 10;
        List<Integer> list = new ArrayList<>();

        // when
        List<Integer> sample = sampler.sample(list, percentage);

        // then sample should be empty
        Assert.assertTrue(sample.isEmpty());
    }

    // ----------- Utils --------------
    private List<Integer> createList(int range) {
        return IntStream.range(0, range)
                .boxed()
                .collect(Collectors.toList());
    }
}
