import java.util.ArrayList;
import java.util.List;

/**
 * @author iwant
 * @date 19-6-13 09:07
 * @desc 特征函数模板
 */
public final class UnigramFeature {

    private String flag;

    // 特征相对于当前行的偏移
    private List<Integer> columns = new ArrayList<>();
    // 暂时不用留作扩展
    private List<Integer> rows = new ArrayList<>();


    public UnigramFeature(String flag) {
        this.flag = flag;
    }

    public List<Integer> getColumns() {
        return columns;
    }

    public List<Integer> getRows() {
        return rows;
    }

    public void addRow(int row) {
        this.rows.add(row);
    }

    public void addColumn(int column) {
        this.columns.add(column);
    }

    public String getFlag() {
        return flag;
    }
}
