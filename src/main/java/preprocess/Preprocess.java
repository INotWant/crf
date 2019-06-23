package preprocess;

import java.io.*;

/**
 * @author iwant
 * @date 19-6-12 19:22
 */
public final class Preprocess {

    /**
     * 预处理
     */
    public static void preprocess(String dir, String saveFile) {
        File dirFile = new File(dir);
        File[] files = dirFile.listFiles();
        if (files != null) {
            for (File file : files) {
                String line;
                if (file.isFile())
                    try (BufferedReader reader = new BufferedReader(new FileReader(file));
                         BufferedWriter writer = new BufferedWriter(new FileWriter(saveFile, true))) {
                        while ((line = reader.readLine()) != null) {
                            String[] split = line.split(" ");
                            for (int i = 0; i < split.length; i++) {
                                String word = split[i];
                                if (word.length() == 0)
                                    continue;
                                if (word.charAt(0) == '。') {
                                    writer.write("。\tS\n\n");
                                    continue;
                                } else {
                                    for (int j = 0; j < word.length(); j++) {
                                        if (j == 0) {
                                            if (word.length() == 1)
                                                continue;
                                            if (word.charAt(j + 1) == '/') {
                                                writer.write(word.charAt(0) + "\tS\n");
                                                break;
                                            } else
                                                writer.write(word.charAt(0) + "\tB\n");
                                        } else if (j + 1 < word.length()) {
                                            if (word.charAt(j + 1) == '/') {
                                                writer.write(word.charAt(j) + "\tE\n");
                                                break;
                                            } else
                                                writer.write(word.charAt(j) + "\tM\n");
                                        }
                                    }
                                }
                                if (i == split.length - 1)
                                    writer.write("\n");
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            }
        }
    }

    public static void main(String[] args) {

        String dir = "data/人民日报语料库2014/2014/012";
        for (int i = 2; i <= 3; i++)
            preprocess(dir + i, "data/save/train.data");

        preprocess("data/人民日报语料库2014/2014/0106", "data/save/test.data");

        String saveFile = "data/save/verification.data";
        preprocess("data/人民日报语料库2014/2014/0104", saveFile);

    }


}


