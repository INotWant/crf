/**
 * @author iwant
 * @date 19-6-13 09:38
 * @desc 一些配置
 */
public final class Config {

    public static final int B_FLAG = 0;
    public static final int M_FLAG = 1;
    public static final int E_FLAG = 2;
    public static final int S_FLAG = 3;

    public static final String B2B = "BB";
    public static final String B2M = "BM";
    public static final String B2E = "BE";
    public static final String B2S = "BS";
    public static final String M2B = "MB";
    public static final String M2M = "MM";
    public static final String M2E = "ME";
    public static final String M2S = "MS";
    public static final String E2B = "EB";
    public static final String E2M = "EM";
    public static final String E2E = "EE";
    public static final String E2S = "ES";
    public static final String S2B = "SB";
    public static final String S2M = "SM";
    public static final String S2E = "SE";
    public static final String S2S = "SS";

    public static final int PROCESSOR_NUM = 4;

    public static final String TRAIN_FILE = "data/save/train.data";
    public static final String TEST_FILE = "data/save/test.data";
    public static final String VERIFICATION_FILE = "data/save/verification.data";
    public static final String MODEL_FILE = "data/model";

    public static final String TEMPLATE_FILE = "data/template";

    // 特征频率最小值
    // 如果低于该频率则清除此特征
    public static final int FEATURE_FREQ_MIN = 3;

    // 如果在验证集上错误率增长超过 1% 则停止训练
    public static final double MAX_ERROR_CHANGE = 0.01;

    // 最大训练轮数
    public static final int MAX_TRAIN_COUNTER = 150;

    // 每 batch 的大小
    public static final int BATCH_SIZE = 500;

    // 每隔 N batch 验证一次
    public static final int N_BATCH_VERIFICATION = 100;

    // 超过 UNUPDATE_COUNTER 次数未得到训练便会停止
    public static final int UNUPDATE_COUNTER = 5;

    // 标准句子长度，如果超过该长度则进行断句
    // 用于解决，长句子容易造成 Z: Infinity
    public static final int STANDARD_SENTENCE_LEN = 30;
}
