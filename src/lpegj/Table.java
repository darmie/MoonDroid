package lpegj;

import java.util.Map;

public class Table {
    public Map<String, Object> hash;
    public Object[] array;

    Table(Map<String, Object> hash, Object[] array) {
        this.hash = hash;
        this.array = array;
    }
}
