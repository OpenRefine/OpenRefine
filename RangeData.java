public class RangeData {
    private double min;
    private double max;
    private double step;
    private int[] bins;

    public RangeData(double min, double max, double step) {
        this.min = min;
        this.max = max;
        this.step = step;
    }

    public void updateMinMax(double value) {
        min = Math.min(min, value);
        max = Math.max(max, value);
    }

    public boolean isFiniteRange() {
        return !Double.isInfinite(min) && !Double.isInfinite(max);
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getStep() {
        return step;
    }

    public int[] getBins() {
        return bins;
    }

    // Additional methods for managing bins
    // ...
}