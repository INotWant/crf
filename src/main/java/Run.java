import java.util.*;

/**
 * @author iwant
 * @date 19-6-15 14:45
 * @desc 演示程序
 */
public final class Run {

    private static List<UnigramFeature> unigramFeatures;
    private static Map<String, Feature> features;

    public static void init() {
        System.out.println("[INFO] 开始初始化！");
        System.out.println("[INFO] 开始解析特征模板...");
        unigramFeatures = Utils.parseFeatureTemplate(Config.TEMPLATE_FILE);
        System.out.println("[INFO] 特征模板解析完毕！");

        System.out.println("[INFO] 开始加载模型...");
        features = Utils.getModelFromFile(Config.MODEL_FILE);
        System.out.println("[INFO] 一共加载了 " + ((features.size() - 16) * 4 + 16) + " 个特征！");

        System.out.println("[INFO] 初始化完毕！");
    }

    public static void main(String[] args) {
        init();

        System.out.println("[INFO] 请输入...");

        Scanner scanner = new Scanner(System.in);
        String line;

        List<String> sentenceList = new ArrayList<>();
        for (; ; ) {
            System.out.print("> ");
            if ((line = scanner.nextLine()) != null) {
                line = line.trim();
                if ("exit".equals(line))
                    break;
                for (int i = 0; i < line.length(); i++) {
                    if (!Character.isWhitespace(line.charAt(i)))
                        sentenceList.add(String.valueOf(line.charAt(i)));
                }
                String[] sentence = new String[sentenceList.size()];
                sentenceList.toArray(sentence);
                String[] flags = Predict.predict(sentence, features, unigramFeatures);
                System.out.print("\t");
                for (int i = 0; i < sentence.length; i++) {
                    assert flags != null;
                    System.out.print(sentence[i] + "/" + flags[i] + " ");
                }
                System.out.println();
                System.out.print("\t");
                for (int i = 0; i < sentence.length; i++) {
                    if (flags[i].equals("B") || flags[i].equals("M")) {
                        if (i + 1 < sentence.length &&
                                (flags[i + 1].equals("M") || flags[i + 1].equals("E")))
                            System.out.print(sentence[i]);
                        else
                            System.out.print(sentence[i] + " ");
                    } else
                        System.out.print(sentence[i] + " ");
                }
                System.out.println();
            } else
                break;
            sentenceList.clear();
        }
    }

    public static List<UnigramFeature> getUnigramFeatures() {
        return unigramFeatures;
    }

    public static Map<String, Feature> getFeatures() {
        return features;
    }
}
