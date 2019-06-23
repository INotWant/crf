import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author iwant
 * @date 19-6-13 08:56
 */
public final class Utils {

    /**
     * 由特征模板解析出 unigram 特征
     */
    public static List<UnigramFeature> parseFeatureTemplate(String templateFile) {
        List<UnigramFeature> unigramFeatures = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(templateFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0 || line.charAt(0) == '#')
                    continue;
                int index = line.indexOf(':');
                if (index < 0)
                    continue;
                UnigramFeature unigramFeature = new UnigramFeature(line.substring(0, index));
                int s = -1;
                for (int i = index + 1; i < line.length(); i++) {
                    if (line.charAt(i) == '[')
                        s = i + 1;
                    else if (line.charAt(i) == ']') {
                        if (s >= 0 && s < i) {
                            String rcStr = line.substring(s, i);
                            String[] split = rcStr.split(",");
                            if (split != null && split.length == 2) {
                                unigramFeature.addColumn(Integer.parseInt(split[0]));
                                unigramFeature.addRow(Integer.parseInt(split[1]));
                            }
                        }
                    }
                }
                unigramFeatures.add(unigramFeature);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return unigramFeatures;
    }

    public static String[][] getWordsAndFlags(String[] sentence) {
        String[][] result = new String[2][];

        String[] words = new String[sentence.length];
        String[] flags = new String[sentence.length];
        result[0] = words;
        result[1] = flags;

        for (int i = 0; i < sentence.length; i++) {
            String line = sentence[i];
            String[] split = line.split("\t");
            if (split != null && split.length == 2) {
                words[i] = split[0];
                flags[i] = split[1];
            }
        }
        return result;
    }

    /**
     * 创建特征函数（句子）
     * <p>
     * 其他：
     * 特征函数集合 features
     * key： 特征函数标识；value：feature
     */
    public static void createFeaturesBySentence(Map<String, Feature> features, String[] sentence, UnigramFeature featureTemplate) {
        List<Integer> columns = featureTemplate.getColumns();
        String[][] wordsAndFlags = getWordsAndFlags(sentence);
        String[] words = wordsAndFlags[0];
        String[] flags = wordsAndFlags[1];

        String templateFlag = featureTemplate.getFlag();

        for (int i = 0; i < sentence.length; i++) {
            if ("".equals(words[i]) || "".equals(flags[i]))
                continue;
            StringBuilder key = new StringBuilder(templateFlag + ":");
            for (int j = 0; j < columns.size(); j++) {
                int row = columns.get(j) + i;
                if (row < 0)
                    key.append("_B-").append(Math.abs(row));
                else if (row >= sentence.length)
                    key.append("_B+").append(row - sentence.length + 1);
                else
                    key.append(words[row]);
                key.append("/");
            }
            key.deleteCharAt(key.length() - 1);
            Feature feature = features.get(key.toString());
            if (feature == null) {
                feature = new Feature(Feature.generateId(), key.toString());
                features.put(key.toString(), feature);
            }
            feature.addFreq(Utils.getFlagId(flags[i]));
        }
    }

    /**
     * 创建特征函数（整个训练集）
     */
    public static void createFeaturesByTrain(Map<String, Feature> features, String trainFile, List<UnigramFeature> unigramFeatures) {
        for (UnigramFeature unigramFeature : unigramFeatures)
            try (BufferedReader reader = new BufferedReader(new FileReader(trainFile))) {
                List<String> sentenceList = new ArrayList<>();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().length() == 0) {
                        if (sentenceList.size() >= 1) {
                            String[] sentence = new String[sentenceList.size()];
                            sentenceList.toArray(sentence);
                            createFeaturesBySentence(features, sentence, unigramFeature);
                        }
                        sentenceList.clear();
                    } else
                        sentenceList.add(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

    }

    /**
     * 保存所有的 特征函数
     */
    public static void saveFeaturesFile(LinkedHashMap<String, Feature> features, String featuresFile) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(featuresFile))) {
            for (Map.Entry<String, Feature> entry : features.entrySet()) {
                Feature feature = entry.getValue();
                writer.append(String.valueOf(feature.getId())).append("\t").
                        append(entry.getKey()).append("\t").
                        append(String.valueOf(feature.getTotalFreq()));
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static int getFlagId(String flag) {
        if ("B".equals(flag))
            return Config.B_FLAG;
        if ("M".equals(flag))
            return Config.M_FLAG;
        if ("E".equals(flag))
            return Config.E_FLAG;
        if ("S".equals(flag))
            return Config.S_FLAG;
        return 0;
    }

    public static String getFlagById(int flagId) {
        if (Config.B_FLAG == flagId)
            return "B";
        if (Config.M_FLAG == flagId)
            return "M";
        if (Config.E_FLAG == flagId)
            return "E";
        return "S";
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Feature> getModelFromFile(String modelFile) {
        Map<String, Feature> features;
        try (ObjectInputStream objIn = new ObjectInputStream(new BufferedInputStream(new FileInputStream(modelFile)))) {
            features = (Map<String, Feature>) objIn.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return features;
    }
}
