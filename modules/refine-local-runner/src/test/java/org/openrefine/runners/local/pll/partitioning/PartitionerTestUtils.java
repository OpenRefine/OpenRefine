
package org.openrefine.runners.local.pll.partitioning;

import org.testng.Assert;

import org.openrefine.runners.local.pll.PairPLL;
import org.openrefine.runners.local.pll.Partition;

public class PartitionerTestUtils {

    public static <K, V> void checkPartitionerAdequacy(Partitioner<K> partitioner, PairPLL<K, V> pll) {
        Assert.assertEquals(partitioner.numPartitions(), pll.numPartitions());
        for (Partition partition : pll.getPartitions()) {
            pll.iterate(partition).forEach(
                    tuple -> Assert.assertEquals(partitioner.getPartition(tuple.getKey()), partition.getIndex(),
                            "Partitioner is inconsistent with PLL"));
        }
    }
}
