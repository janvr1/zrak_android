package wtf.janvr.zrakandroid;

import java.util.Comparator;
import java.util.Map;

class MapComparator implements Comparator<Map<String, String>> {
    private final String key;
    private final Boolean asc;

    public MapComparator(String key, Boolean ascending) {
        this.key = key;
        this.asc = ascending;
    }

    public int compare(Map<String, String> first,
                       Map<String, String> second) {
        String firstValue = first.get(key);
        String secondValue = second.get(key);
        assert firstValue != null;
        assert secondValue != null;
        if (asc) {
            return firstValue.compareToIgnoreCase(secondValue);
        } else {
            return secondValue.compareToIgnoreCase(firstValue);
        }

    }
}
