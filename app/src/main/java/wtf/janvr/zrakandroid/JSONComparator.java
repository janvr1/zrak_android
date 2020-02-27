package wtf.janvr.zrakandroid;

import org.json.JSONObject;

import java.util.Comparator;

class JSONComparator implements Comparator<JSONObject> {
    private final String key;
    private final Boolean asc;

    public JSONComparator(String key, Boolean ascending) {
        this.key = key;
        this.asc = ascending;
    }

    public int compare(JSONObject first,
                       JSONObject second) {
        try {
            String firstValue = first.get(key).toString();
            String secondValue = second.get(key).toString();
            assert firstValue != null;
            assert secondValue != null;
            if (asc) {
                return firstValue.compareToIgnoreCase(secondValue);
            } else {
                return secondValue.compareToIgnoreCase(firstValue);
            }
        } catch (Exception e) {
            return 0;
        }
    }
}
