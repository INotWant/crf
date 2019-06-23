import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author iwant
 * @date 19-6-14 17:09
 * @desc 训练
 */
public class TrainC {

    public static ExecutorService executorService = Executors.newScheduledThreadPool(Config.PROCESSOR_NUM + 1);

    private static Random random = new Random();

    private static List<UnigramFeature> unigramFeatures;
    private static Map<String, Feature> features = new LinkedHashMap<>();

    private static void init() {
        System.out.println("[INFO] 开始初始化！");
        System.out.println("[INFO] 开始解析特征模板...");
        unigramFeatures = Utils.parseFeatureTemplate(Config.TEMPLATE_FILE);
        System.out.println("[INFO] 特征模板解析完毕！");

        System.out.println("[INFO] 开始创建特征函数...");
        features.put(Config.B2B, new Feature(0, Config.B2B));
        features.put(Config.B2M, new Feature(1, Config.B2M));
        features.put(Config.B2E, new Feature(2, Config.B2E));
        features.put(Config.B2S, new Feature(3, Config.B2S));
        features.put(Config.M2B, new Feature(4, Config.M2B));
        features.put(Config.M2M, new Feature(5, Config.M2M));
        features.put(Config.M2E, new Feature(6, Config.M2E));
        features.put(Config.M2S, new Feature(7, Config.M2S));
        features.put(Config.E2B, new Feature(8, Config.E2B));
        features.put(Config.E2M, new Feature(9, Config.E2M));
        features.put(Config.E2E, new Feature(10, Config.E2E));
        features.put(Config.E2S, new Feature(11, Config.E2S));
        features.put(Config.S2B, new Feature(12, Config.S2B));
        features.put(Config.S2M, new Feature(13, Config.S2M));
        features.put(Config.S2E, new Feature(14, Config.S2E));
        features.put(Config.S2S, new Feature(15, Config.S2S));
        Utils.createFeaturesByTrain(features, Config.TRAIN_FILE, unigramFeatures);
        System.out.println("[INFO] 一种创建 " + ((features.size() - 16) * 4 + 16) + " 个特征！");

        if (Config.FEATURE_FREQ_MIN > 0) {
            System.out.println("[INFO] 开始清理特征...");
            Iterator<Map.Entry<String, Feature>> iterator = features.entrySet().iterator();
            while (iterator.hasNext()) {
                Feature feature = iterator.next().getValue();
                if (feature.getTotalFreq() < Config.FEATURE_FREQ_MIN && feature.getId() >= 16)
                    iterator.remove();
            }
            System.out.println("[INFO] 剩余 " + ((features.size() - 16) * 4 + 16) + " 个特征！");
        }
        System.out.println("[INFO] 初始化完毕！");
    }

    private static int compareFlags(String[] predictFlags, String[] flags) {
        int sumError = 0;
        for (int i = 0; i < flags.length; i++)
            if (!predictFlags[i].equals(flags[i]))
                ++sumError;
        return sumError;
    }

    private static boolean isCommonPunctuation(char c) {
        switch (c) {
            case '，':
            case '。':
            case '？':
            case '“':
            case '”':
            case '：':
            case '、':
            case '；':
            case '!':
                return true;
            default:
                return false;
        }
    }

    private static String[][] getSentences(String fileName) {
        List<String[]> sentencesList = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            List<String> sentenceList = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().length() == 0) {
                    if (sentenceList.size() >= 1) {
                        String[] sentence = new String[sentenceList.size()];
                        sentenceList.toArray(sentence);
                        sentencesList.add(sentence);
                        sentenceList.clear();
                    }
                } else if (sentenceList.size() >= Config.STANDARD_SENTENCE_LEN) {
                    sentenceList.add(line);
                    if (isCommonPunctuation(line.charAt(0))) {
                        String[] sentence = new String[sentenceList.size()];
                        sentenceList.toArray(sentence);
                        sentencesList.add(sentence);
                        sentenceList.clear();
                    }
                } else
                    sentenceList.add(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String[][] sentences = new String[sentencesList.size()][];
        sentencesList.toArray(sentences);
        return sentences;
    }

    private static void saveModel(Map<String, Feature> features, String modelFile) {
        try (ObjectOutputStream objOut = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(modelFile)))) {
            objOut.writeObject(features);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws InterruptedException {
        // 初始化
        init();

        System.out.println("[INFO] 开始加载训练集...");
        String[][] trainSentences = getSentences(Config.TRAIN_FILE);
        System.out.println("[INFO] 训练集加载完成！一共 " + trainSentences.length + " 个句子！");

        System.out.println("[INFO] 开始加载验证集...");
        String[][] vSentences = getSentences(Config.VERIFICATION_FILE);
        System.out.println("[INFO] 验证集加载完成！一共 " + vSentences.length + " 个句子！");

        System.out.println("[INFO] 开始训练！");

        // 训练迭代次数
        int trainCounter = 0, unupdateCounter = 0;
        double minErrorRate = 1.0, errorRate = 1.0;
        Map<String, Feature> modelFeatures = null;

        exit:
        do {
            // shuffle
            String[] tempSentence;
            for (int i = 0; i < trainSentences.length; i++) {
                int r = random.nextInt(trainSentences.length);
                tempSentence = trainSentences[r];
                trainSentences[r] = trainSentences[i];
                trainSentences[i] = tempSentence;
            }

            String[][] batchSentences = new String[Config.BATCH_SIZE][];
            for (int i = 0; i < trainSentences.length / Config.BATCH_SIZE; i++) {
                System.arraycopy(trainSentences, i * Config.BATCH_SIZE, batchSentences, 0, Config.BATCH_SIZE);
                double delta = LearnC.learnSentences(batchSentences, features, unigramFeatures);

                // 在验证集上测试
                if (i % Config.N_BATCH_VERIFICATION == 0) {
                    System.out.println("[INFO] delta " + delta);

                    int totalFlagNum = 0;
                    int errorFlagNum = 0;

                    for (int j = 0; j < vSentences.length; j++) {
                        tempSentence = vSentences[j];
                        String[] flags = Utils.getWordsAndFlags(tempSentence)[1];
                        String[] predictFlags = Predict.predict(tempSentence, features, unigramFeatures);
                        errorFlagNum += compareFlags(predictFlags, flags);
                        totalFlagNum += flags.length;
                    }
                    errorRate = (errorFlagNum + 0.0) / totalFlagNum;
                    System.out.println("[INFO] total: " + totalFlagNum + ", error: " + errorFlagNum + ", " +
                            String.format("%.2f%%", errorRate * 100));

                    if (errorRate < minErrorRate) {
                        unupdateCounter = 0;
                        minErrorRate = errorRate;
                        // 深拷贝
                        modelFeatures = (Map<String, Feature>) ((LinkedHashMap<String, Feature>) features).clone();
                    } else
                        ++unupdateCounter;
                    if (unupdateCounter > Config.UNUPDATE_COUNTER)
                        break exit;
                }
            }

            ++trainCounter;
        } while (errorRate - minErrorRate < Config.MAX_ERROR_CHANGE &&
                trainCounter < Config.MAX_TRAIN_COUNTER);

        System.out.println("[INFO] 训练结束！");

        System.out.println("[INFO] 开始保存模型！");
        saveModel(modelFeatures, Config.MODEL_FILE);
        System.out.println("[INFO] 模型所在位置：" + Config.MODEL_FILE);

        executorService.shutdown();
    }

}
