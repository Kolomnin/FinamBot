package work.finamapibot;

import java.util.Arrays;

public class DataSet {
    private final double[] features;
    private final String target;

    public DataSet(double[] features, String target) {
        this.features = Arrays.copyOf(features, features.length);
        this.target = target;
    }

    public double[] getFeatures() {
        return Arrays.copyOf(features, features.length);
    }

    public String getTarget() {
        return target;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        DataSet dataSet = (DataSet) obj;
        return Arrays.equals(features, dataSet.features) &&
                target.equals(dataSet.target);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(features);
        result = 31 * result + target.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "DataSet{" +
                "features=" + Arrays.toString(features) +
                ", target='" + target + '\'' +
                '}';
    }
}