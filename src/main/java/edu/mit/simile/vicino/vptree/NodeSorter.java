package edu.mit.simile.vicino.vptree;

public class NodeSorter {

    /**
     * Sorts and array of objects.
     */
    public void sort(Node nodes[]) {
        NodeSorter.sort(nodes, 0, nodes.length - 1);
    }
    
    /**
     * Sort array of Objects using the QuickSort algorithm.
     * 
     * @param s
     *            An Object[].
     * @param lo
     *            The current lower bound.
     * @param hi
     *            The current upper bound.
     */
    public static void sort(Node nodes[], int lo, int hi) {
        if (lo >= hi) {
            return;
        }

        /*
         * Use median-of-three(lo, mid, hi) to pick a partition. Also swap them
         * into relative order while we are at it.
         */
        int mid = (lo + hi) / 2;

        if (nodes[lo].getDistance() > nodes[mid].getDistance()) {
            // Swap.
            Node tmp = nodes[lo];
            nodes[lo] = nodes[mid];
            nodes[mid] = tmp;
        }

        if (nodes[mid].getDistance() > nodes[hi].getDistance()) {
            // Swap .
            Node tmp = nodes[mid];
            nodes[mid] = nodes[hi];
            nodes[hi] = tmp;

            if (nodes[lo].getDistance() > nodes[mid].getDistance()) {
                // Swap.
                Node tmp2 = nodes[lo];
                nodes[lo] = nodes[mid];
                nodes[mid] = tmp2;
            }
        }

        // Start one past lo since already handled lo.

        int left = lo + 1;

        // Similarly, end one before hi since already handled hi.

        int right = hi - 1;

        // If there are three or fewer elements, we are done.

        if (left >= right) {
            return;
        }

        Node partition = nodes[mid];

        while (true) {
            while (nodes[right].getDistance() > partition.getDistance()) {
                --right;
            }

            while (left < right && nodes[left].getDistance() <= partition.getDistance()) {
                ++left;
            }

            if (left < right) {
                // Swap.
                Node tmp = nodes[left];
                nodes[left] = nodes[right];
                nodes[right] = tmp;

                --right;
            } else {
                break;
            }
        }

        sort(nodes, lo, left);
        sort(nodes, left + 1, hi);
    }
}
