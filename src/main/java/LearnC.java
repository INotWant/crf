import org.apache.commons.math3.linear.Array2DRowRealMatrix;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author iwant
 * @date 19-6-13 14:44
 * @desc 学习过程
 */
public final class LearnC {


    private static int[] sArr;
    private static AtomicInteger spAtomic = new AtomicInteger();

    /**
     * 使用批量数据训练 CRF
     * <p>
     * 算法：改进的迭代尺度法（<统计机器学习,李航> P203-204，算法S）
     *
     * @param batchSentences  批量训练数据
     * @param features        特征集
     * @param unigramFeatures 特征模板
     * @return 本次训练的总下降
     * @throws InterruptedException
     */
    public static double learnSentences(String[][] batchSentences, Map<String, Feature> features, List<UnigramFeature> unigramFeatures) throws InterruptedException {
        /*
         * 1）训练前处理
         * 主要思路是先统计该批量数据的所有特征和计算它们的期望值；
         * 然后才进行训练。
         */

        // localFeatures --> key: 特征标记, value: Feature
        // localFeatures 是针对该批量数据的特征统计
        Map<String, Feature> localFeatures = new ConcurrentHashMap<>();

        // featureAndExp --> key 特征（名称:状态）, value 期望
        Map<String, Double> featureAndExp = new ConcurrentHashMap<>();

        CountDownLatch countDownLatch = new CountDownLatch(batchSentences.length);

        // 用于求最大特征总数
        sArr = new int[batchSentences.length];
        spAtomic.set(0);

        for (int i = 0; i < batchSentences.length; i++) {
            String[] sentence = batchSentences[i];
            // 多线程
            TrainC.executorService.execute(() -> {
                getConditionExps(featureAndExp, sentence, unigramFeatures, features, localFeatures);
                countDownLatch.countDown();
            });
        }

        // 等待所有子线程完成
        countDownLatch.await();

        // 计算 S, 最大特征总数
        int S = Integer.MIN_VALUE;
        for (int s : sArr)
            if (s > S)
                S = s;

        // 总下降
        double totalDelta = 0;

        /*
         * 2）开始训练
         * 每次迭代训练一个特征
         */
        for (Map.Entry<String, Double> entry : featureAndExp.entrySet()) {
            String key = entry.getKey();
            // 确定具体特征：主要针对状态特征，在 key 中含有对应的状态，如此此处是由 Feature 类的定义决定的
            int flagId = 0;
            int index = key.indexOf(":");
            if (index >= 0) {
                flagId = Utils.getFlagId(String.valueOf(key.charAt(key.length() - 1)));
                key = key.substring(0, key.length() - 2);
            }

            // expM --> 模型(条件分布)得到的特征函数的期望
            double expM = entry.getValue();

            // expE --> “经验”得到的特征关于联合分布的期望值
            int expE = 0;
            if (localFeatures.get(key) != null)
                expE = localFeatures.get(key).getFreq(flagId);
            if (expE == 0)
                continue;

            // 下降
            double delta = Math.log(expE / expM) / S;
            if (Double.isNaN(delta) || Double.isInfinite(delta)) {
                System.err.println("[WARN] expE: " + expE + ", expM: " + expM + ", S: " + S);
                continue;
            }
            features.get(key).updateWeight(flagId, delta);
            totalDelta += Math.abs(delta);
        }

        return totalDelta;
    }

    /**
     * 求指定句子中所有特征关于条件分布的期望值
     *
     * @param featureAndExp   保存计算的期望值
     * @param sentence        句子
     * @param unigramFeatures 特征模板
     * @param features        特征集
     * @param localFeatures   保存统计的局部特征
     */
    @SuppressWarnings("unchecked")
    private static void getConditionExps(Map<String, Double> featureAndExp, String[] sentence, List<UnigramFeature> unigramFeatures, Map<String, Feature> features, Map<String, Feature> localFeatures) {
        if (sentence == null || sentence.length == 0)
            return;

        String[][] wordsAndFlags = Utils.getWordsAndFlags(sentence);
        String[] words = wordsAndFlags[0];
        String[] flags = wordsAndFlags[1];

        int len = sentence.length;

        // featureAndPos --> key: 特征标签, value: 记录特征出现的位置
        Map<String, List<Integer>> featureAndPos = new HashMap<>();

        // 针对 状态特征
        for (UnigramFeature unigramFeature : unigramFeatures) {
            String templateFlag = unigramFeature.getFlag();
            List<Integer> columns = unigramFeature.getColumns();
            for (int i = 0; i < sentence.length; i++) {
                if ("".equals(words[i]) || "".equals(flags[i]))
                    continue;
                StringBuilder keySb = new StringBuilder(templateFlag + ":");
                for (int j = 0; j < columns.size(); j++) {
                    int row = columns.get(j) + i;
                    if (row < 0)
                        keySb.append("_B-").append(Math.abs(row));
                    else if (row >= sentence.length)
                        keySb.append("_B+").append(row - sentence.length + 1);
                    else
                        keySb.append(words[row]);
                    keySb.append("/");
                }
                keySb.deleteCharAt(keySb.length() - 1);
                String key = keySb.toString();
                if (features.containsKey(key)) {
                    List<Integer> value = featureAndPos.computeIfAbsent(key, k -> new ArrayList<>());
                    value.add(i + 1);

                    Feature feature = new Feature(key);
                    Feature oldFeature = localFeatures.putIfAbsent(key, feature);
                    if (oldFeature != null)
                        feature = oldFeature;
                    feature.addFreq(Utils.getFlagId(flags[i]));
                }
            }
        }

        // 针对 转移特征
        // start 假设为 “S”
        String lastF = "S";
        String currF = flags[0];
        Feature feature = new Feature(lastF + currF);
        Feature oldFeature = localFeatures.putIfAbsent(lastF + currF, feature);
        if (oldFeature != null)
            feature = oldFeature;
        feature.addFreq(0);
        for (int i = 1; i < flags.length; i++) {
            lastF = flags[i - 1];
            currF = flags[i];

            feature = new Feature(lastF + currF);
            oldFeature = localFeatures.putIfAbsent(lastF + currF, feature);
            if (oldFeature != null)
                feature = oldFeature;
            feature.addFreq(0);
        }
        // end 假设为 “S”
        feature = new Feature(currF + "S");
        oldFeature = localFeatures.putIfAbsent(currF + "S", feature);
        if (oldFeature != null)
            feature = oldFeature;
        feature.addFreq(0);

        // 特征总数
        int s = 0;

        // 记录某位置有哪些"状态"特征
        // 下标从 1 开始
        List[] posAndFeature = new List[len + 2];
        for (int i = 0; i < posAndFeature.length; i++)
            posAndFeature[i] = new ArrayList<String>();
        posAndFeature[0] = null;
        for (Map.Entry<String, List<Integer>> entry : featureAndPos.entrySet()) {
            String key = entry.getKey();
            List<Integer> posList = entry.getValue();
            if (posList != null) {
                s += posList.size();
                if (posList != null)
                    for (int i = 0; i < posList.size(); i++)
                        posAndFeature[posList.get(i)].add(key);
            }
        }

        // 再加上转移特征的
        s += (len + 1);
        sArr[spAtomic.getAndIncrement()] = s;

        // M 矩阵
        // 注下标从 1 开始
        Array2DRowRealMatrix[] M = new Array2DRowRealMatrix[len + 2];
        computeM(M, posAndFeature, features);

        // 计算 alpha
        Array2DRowRealMatrix[] alpha = new Array2DRowRealMatrix[len + 2];
        computeAlpha(alpha, M);

        // 计算 beta
        Array2DRowRealMatrix[] beta = new Array2DRowRealMatrix[len + 2];
        computeBeta(beta, M);

        // 计算 Z(X)
        double z = 0;
        double[] row = alpha[alpha.length - 2].getRow(0);
        for (double d : row)
            z += d;

        // 处理异常
        if (Double.isNaN(z) || Double.isInfinite(z) || z == 0.0) {
            System.err.println("[WARN] Z: " + z + ", sentenceLen: " + sentence.length);
            return;
        }

        // 添加所有的转移特征
        {
            featureAndPos.put(Config.B2B, null);
            featureAndPos.put(Config.B2M, null);
            featureAndPos.put(Config.B2E, null);
            featureAndPos.put(Config.B2S, null);
            featureAndPos.put(Config.M2B, null);
            featureAndPos.put(Config.M2M, null);
            featureAndPos.put(Config.M2E, null);
            featureAndPos.put(Config.M2S, null);
            featureAndPos.put(Config.E2B, null);
            featureAndPos.put(Config.E2M, null);
            featureAndPos.put(Config.E2E, null);
            featureAndPos.put(Config.E2S, null);
            featureAndPos.put(Config.S2B, null);
            featureAndPos.put(Config.S2M, null);
            featureAndPos.put(Config.S2E, null);
        }

        // 获取该句子内所有特征函数的期望
        for (Map.Entry<String, List<Integer>> entry : featureAndPos.entrySet()) {
            String key = entry.getKey();
            feature = features.get(key);

            // 特征函数对应的条件分布数学期望
            double cExp = 0;
            if (feature.getId() <= 15) {
                // 转移特征
                int lFlagID = Utils.getFlagId(String.valueOf(key.charAt(0)));
                int cFlagId = Utils.getFlagId(String.valueOf(key.charAt(1)));
                for (int i = 1; i <= len + 1; i++) {
                    cExp += (alpha[i - 1].getRow(0)[lFlagID] *
                            M[i].getRow(lFlagID)[cFlagId] *
                            beta[i].getRow(cFlagId)[0]);
                }
                cExp /= z;

                Double hExpDouble = featureAndExp.putIfAbsent(key, cExp);
                if (hExpDouble != null) {
                    double finalCExp = cExp;
                    featureAndExp.computeIfPresent(key, (s1, aDouble) -> aDouble + finalCExp);
                }
            } else {
                // 状态特征 --> 含四个
                for (int i = 0; i < 4; i++) {       // yi 一次循环计算一个特征的条件期望（针对状态 i ）
                    cExp = 0;
                    for (int pos : entry.getValue())
                        cExp += (alpha[pos].getRow(0)[i] *
                                beta[pos].getRow(i)[0]);

                    cExp /= z;
                    if (Double.isNaN(cExp)) {
                        System.err.println("[WARN] Z: " + z + ", cExp: " + cExp + ", sentence: " + Arrays.toString(sentence));
                        continue;
                    }

                    String newKey = key + ":" + Utils.getFlagById(i);
                    Double hExpDouble = featureAndExp.putIfAbsent(newKey, cExp);
                    if (hExpDouble != null) {
                        double finalCExp = cExp;
                        featureAndExp.computeIfPresent(newKey, (s1, aDouble) -> aDouble + finalCExp);
                    }
                }
            }
        }
    }

    private static void computeM(Array2DRowRealMatrix[] M, List[] posAndFeature, Map<String, Feature> features) {
        // 转移概率的权重都是固定的
        double[][] transferArray = new double[4][4];
        for (int i = 0; i < 4; i++)
            for (int j = 0; j < 4; j++)
                transferArray[i][j] = features.get(Utils.getFlagById(i) + Utils.getFlagById(j)).getWeight(0);

        for (int i = 1; i < M.length; i++) {
            double[][] array = new double[4][4];
            for (int j = 0; j < 4; j++)
                System.arraycopy(transferArray[j], 0, array[j], 0, array[j].length);

            List featureList = posAndFeature[i];
            for (Object obj : featureList) {
                String featureId = (String) obj;
                Feature feature = features.get(featureId);

                if (feature.getId() > 15) {
                    // 状态特征
                    for (int j = 0; j < 4; j++) {
                        array[0][j] += feature.getWeight(j);
                        array[1][j] += feature.getWeight(j);
                        array[2][j] += feature.getWeight(j);
                        array[3][j] += feature.getWeight(j);
                    }
                }
            }

            for (int j = 0; j < 4; j++)
                for (int k = 0; k < 4; k++)
                    array[j][k] = Math.exp(array[j][k]);

            M[i] = new Array2DRowRealMatrix(array);
        }
    }

    private static void computeAlpha(Array2DRowRealMatrix[] alpha, Array2DRowRealMatrix[] M) {
        double[][] array = new double[1][4];
        array[0][Utils.getFlagId("S")] = 1;
        alpha[0] = new Array2DRowRealMatrix(array);

        for (int i = 1; i < alpha.length; i++)
            alpha[i] = alpha[i - 1].multiply(M[i]);
    }

    private static void computeBeta(Array2DRowRealMatrix[] beta, Array2DRowRealMatrix[] M) {
        double[][] array = new double[4][1];
        array[Utils.getFlagId("S")][0] = 1;
        beta[beta.length - 1] = new Array2DRowRealMatrix(array);

        for (int i = beta.length - 2; i >= 0; --i)
            beta[i] = M[i + 1].multiply(beta[i + 1]);
    }
}
