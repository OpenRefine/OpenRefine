
package com.google.refine.sampling;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.testng.Assert;
import org.testng.annotations.Test;

public class SystematicSamplerTest {

    private final SystematicSampler sampler = new SystematicSampler();

    @Test
    public void testBasicSampling() {
        // given
        int step = 11;
        List<Integer> list = createList(100);

        // when
        List<Integer> sample = sampler.sample(list, step);

        // then
        List<Integer> expectedSample = List.of(0, 11, 22, 33, 44, 55, 66, 77, 88, 99);
        Assert.assertEquals(sample, expectedSample);
        Assert.assertTrue(list.containsAll(sample));
    }

    // ------------------ edge cases ----------------
    @Test
    public void testSamplingWithListSizeSmallerThenStepSize() {
        // given
        int step = 100;
        List<Integer> list = createList(10);

        // when
        List<Integer> sample = sampler.sample(list, step);

        // then only first element (index 0) is in sample
        Assert.assertEquals(sample.size(), 1);
        Assert.assertTrue(sample.contains(list.get(0)));
    }

    @Test
    public void testSamplingWithListSizeEqualToStepSize() {
        // given
        int step = 100;
        List<Integer> list = createList(100);

        // when
        List<Integer> sample = sampler.sample(list, step);

        // then only first element (index 0) is in sample
        Assert.assertEquals(sample.size(), 1);
        Assert.assertTrue(sample.contains(list.get(0)));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testSamplingWithStepSizeZero() {
        // given
        int step = 0;
        List<Integer> list = createList(10);

        // when
        sampler.sample(list, step);

        // then throw IllegalArgumentException (see test header)
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testSamplingWithNegativeStepSize() {
        // given
        int step = -1;
        List<Integer> list = createList(100);

        // when
        sampler.sample(list, step);

        // then throw IllegalArgumentException (see test header)
    }

    @Test
    public void testSamplingOnEmptyList() {
        // given
        int step = 10;
        List<Integer> list = new ArrayList<>();

        // when
        List<Integer> sample = sampler.sample(list, step);

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
