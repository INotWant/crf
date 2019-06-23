import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author iwant
 * @date 19-6-15 14:49
 * @desc 评测
 */
public final class Evaluation {

    public static void main(String[] args) {
        Run.init();

        String resultFile = "data/save/result.data";

        System.out.println("[INFO] 评测使用的文件：" + Config.TEST_FILE + " !");
        System.out.println("[INFO] 结果将会保存在：" + resultFile + " !");

        if (!new File(resultFile).exists()) {
            System.out.println("[INFO] 分词进行中...");
            Predict.predictFile(Run.getFeatures(), Run.getUnigramFeatures(), Config.TEST_FILE, resultFile);
        }
        System.out.println("[INFO] 分词已结束，正在评估准确率...");

        int wError = 0, wTotalNum = 0;
        int sError = 0, sTotalNum = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(resultFile))) {
            List<String> sentenceList = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().length() == 0) {
                    if (sentenceList.size() >= 1) {
                        String[] sentence = new String[sentenceList.size()];
                        sentenceList.toArray(sentence);

                        wTotalNum += sentence.length;
                        ++sTotalNum;

                        int tWordError = findErrorInSentence(sentence);
                        wError += tWordError;

                        if (tWordError != 0)
                            ++sError;

                    }
                    sentenceList.clear();
                } else
                    sentenceList.add(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println("[INFO] 评测结果如下：");
        System.out.println("\twTotal: " + wTotalNum);
        System.out.println("\tsTotal: " + sTotalNum);
        System.out.println("\twError: " + wError + ", " + String.format("%.2f%%", (wError * 100. / wTotalNum)));
        System.out.println("\tsError: " + sError + ", " + String.format("%.2f%%", (sError * 100. / sTotalNum)));

    }

    private static int findErrorInSentence(String[] sentence) {
        int wError = 0;
        for (String word : sentence) {
            String[] split = word.split("\t");
            if (!split[1].equals(split[2]))
                ++wError;
        }
        return wError;
    }

}
