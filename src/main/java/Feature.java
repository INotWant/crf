import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author iwant
 * @date 19-6-13 09:31
 * @desc 特征函数
 */
public class Feature implements Serializable, Cloneable {

    // 特征函数数字标识
    private int id;
    // 特征函数标识（含特征函数的内容）
    private String featureId;
    // 出现频率
    private AtomicInteger[] freqs = new AtomicInteger[4];

    // 权重
    // 注：对应四个标记（BMES）
    private double[] weights = new double[4];

    public Feature(int id, String featureId) {
        this.id = id;
        this.featureId = featureId;
        for (int i = 0; i < weights.length; i++)
            this.freqs[i] = new AtomicInteger();
    }

    public Feature(String featureId) {
        this(-1, featureId);
    }

    public int getId() {
        return id;
    }

    public String getFeatureId() {
        return featureId;
    }

    public void addFreq(int flagId) {
        this.freqs[flagId].incrementAndGet();
    }

    public int getFreq(int flagId) {
        return freqs[flagId].get();
    }

    public int getTotalFreq() {
        int sum = 0;
        for (int i = 0; i < freqs.length; i++)
            sum += freqs[i].get();
        return sum;
    }

    public double[] getWeights() {
        return weights;
    }

    public double getBWeight() {
        return weights[Config.B_FLAG];
    }

    public double getMWeight() {
        return weights[Config.M_FLAG];
    }

    public double getEWeight() {
        return weights[Config.E_FLAG];
    }

    public double getSWeight() {
        return weights[Config.S_FLAG];
    }

    public double getWeightSum() {
        double sum = 0;
        for (int i = 0; i < weights.length; i++)
            sum += weights[i];
        return sum;
    }

    private static int generateId = 16;

    /**
     * 自动产生特征函数数字标识
     */
    public static int generateId() {
        return generateId++;
    }

    public double getWeight(int flagId) {
        return weights[flagId];
    }

    public void updateWeight(int flagId, double delta) {
        this.weights[flagId] += delta;
    }

    @Override
    public Feature clone() throws CloneNotSupportedException {
        Feature feature = (Feature) super.clone();

        feature.freqs = new AtomicInteger[4];
        feature.freqs[0] = new AtomicInteger(this.freqs[0].get());
        feature.freqs[1] = new AtomicInteger(this.freqs[1].get());
        feature.freqs[2] = new AtomicInteger(this.freqs[2].get());
        feature.freqs[3] = new AtomicInteger(this.freqs[3].get());

        feature.weights = weights.clone();
        return feature;
    }
}
