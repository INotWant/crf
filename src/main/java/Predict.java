import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author iwant
 * @date 19-6-14 16:01
 * @desc 使用已经学习的模型进行预测
 */
public final class Predict {

    /**
     * 使用 维特比 算法进行预测
     *
     * @param sentence        句子
     * @param features        所有特征函数
     * @param unigramFeatures 特征（unigram）模板
     * @return 句子的标注
     */
    @SuppressWarnings("unchecked")
    public static String[] predict(String[] sentence, Map<String, Feature> features, List<UnigramFeature> unigramFeatures) {
        if (sentence == null || sentence.length == 0)
            return null;

        // 返回结果
        String[] flags = new String[sentence.length];
        // 提取词
        String[] words = new String[sentence.length];
        for (int i = 0; i < sentence.length; i++) {
            String str = sentence[i];
            int index = str.indexOf("\t");
            if (index >= 0)
                words[i] = str.substring(0, index);
            else
                words[i] = str;
        }

        // 记录某位置有哪些状态特征
        // 下标从 0 开始
        List[] posAndFeature = new List[words.length];
        for (int i = 0; i < posAndFeature.length; i++)
            posAndFeature[i] = new ArrayList<String>();
        // 针对 状态特征
        for (UnigramFeature unigramFeature : unigramFeatures) {
            String templateFlag = unigramFeature.getFlag();
            List<Integer> columns = unigramFeature.getColumns();
            for (int i = 0; i < posAndFeature.length; i++) {
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
                posAndFeature[i].add(key);
            }
        }

        double[] weights = new double[4];
        int[][] path = new int[sentence.length - 1][4];

        // start 的状态
        String lastFlag = "S", currFlag;
        weights[0] = features.get(lastFlag + "B").getWeight(0);
        weights[1] = features.get(lastFlag + "M").getWeight(0);
        weights[2] = features.get(lastFlag + "E").getWeight(0);
        weights[3] = features.get(lastFlag + "S").getWeight(0);

        for (int i = 0; i < sentence.length; i++) {
            List<String> featureIds = posAndFeature[i];
            double[] tWeights = new double[4];

            for (int j = 0; j < 4; j++) {
                double max = Double.MIN_VALUE;
                currFlag = Utils.getFlagById(j);

                // 状态特征
                for (String featureId : featureIds) {
                    Feature feature = features.get(featureId);
                    if (feature != null)
                        tWeights[j] += feature.getWeight(Utils.getFlagId(currFlag));
                }

                for (int k = 0; k < 4; k++) {
                    lastFlag = Utils.getFlagById(k);
                    // 转移特征
                    double weight = features.get(lastFlag + currFlag).getWeight(0);
                    weight += weights[k] + tWeights[j];
                    if (weight > max) {
                        max = weight;
                        if (i > 0)
                            path[i - 1][j] = k;
                    }
                }
                tWeights[j] = max;
            }
            weights = tWeights;
        }

        // 找路
        int pos = 0;
        double max = weights[0];
        for (int i = 1; i < 4; i++)
            if (weights[i] > max) {
                max = weights[i];
                pos = i;
            }
        flags[sentence.length - 1] = Utils.getFlagById(pos);
        for (int i = sentence.length - 2; i >= 0; i--) {
            flags[i] = Utils.getFlagById(path[i][pos]);
            pos = path[i][pos];
        }
        return flags;
    }

    /**
     * 预测整个文件
     * <p>
     * 注：要求每个字占一行，并且句子之间有空行相隔
     */
    public static void predictFile(Map<String, Feature> features, List<UnigramFeature> unigramFeatures, String testFile, String saveFile) {
        try (BufferedReader reader = new BufferedReader(new FileReader(testFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(saveFile))) {
            List<String> sentenceList = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().length() == 0) {
                    if (sentenceList.size() >= 1) {
                        String[] sentence = new String[sentenceList.size()];
                        sentenceList.toArray(sentence);

                        String[] flags = predict(sentence, features, unigramFeatures);

                        for (int i = 0; i < sentence.length; i++) {
                            writer.write(sentence[i]);
                            assert flags != null;
                            writer.write("\t" + flags[i]);
                            writer.newLine();
                        }
                        writer.newLine();
                    }
                    sentenceList.clear();
                } else
                    sentenceList.add(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}