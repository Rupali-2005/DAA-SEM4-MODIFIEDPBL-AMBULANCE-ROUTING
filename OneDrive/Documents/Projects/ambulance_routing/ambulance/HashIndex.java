package ambulance;

import java.util.*;
// simple index: specialization → list of hospital indices
public class HashIndex {
    private final Map<String, List<Integer>> table=new HashMap<>();
    public void clear() {
        table.clear();
    }
    // adds a hospital index under a specialization
    public void insert(String specialization, int hospitalIndex) {
        if (specialization==null || specialization.isBlank()) return;

        table.computeIfAbsent(normalise(specialization), k -> new ArrayList<>())
             .add(hospitalIndex);
    }
    // returns matching indices (empty list if nothing found)
    public List<Integer> lookup(String specialization) {
        if (specialization == null) return Collections.emptyList();

        return table.getOrDefault(normalise(specialization), Collections.emptyList());
    }
    public boolean contains(String specialization) {
        return !lookup(specialization).isEmpty();
    }
    // just trims and lowers the string for consistency
    private static String normalise(String s) {
        return s.trim().toLowerCase(Locale.ROOT);
    }
}