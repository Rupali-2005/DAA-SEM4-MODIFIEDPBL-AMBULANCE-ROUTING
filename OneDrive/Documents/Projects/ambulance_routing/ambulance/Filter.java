package ambulance;
import java.util.*;

public class Filter {

    // holds the result of filtering (matching indices + a small report)
    public static class FilterResult {
        public final List<Integer> indices; // indices of matching hospitals
        public final String report;

        FilterResult(List<Integer> indices, String report) {
            this.indices = indices;
            this.report=report;
        }

        public boolean hasCandidates() {
            return !indices.isEmpty();
        }
    }

    // filters hospitals based on patient needs
    public static FilterResult filterHospitals(HashIndex ht, List<Hospital> hospitals, Patient p) {

        // using StringBuilder since we keep appending messages
        StringBuilder sb=new StringBuilder();
        List<Integer> results = new ArrayList<>();

        List<Integer> candidates=ht.lookup(p.getSpecialization());

        if (candidates.isEmpty()) {
            sb.append("  No hospital registered for specialization: ")
              .append(p.getSpecialization());
            return new FilterResult(results, sb.toString());
        }

        for (int idx : candidates) {
            if (idx<0 || idx>=hospitals.size()) continue;

            Hospital h = hospitals.get(idx);

            if (!h.hasSpecialization(p.getSpecialization()))
                continue;

            if (!h.hasAvailableBed()) {
                sb.append(" Failed ").append(h.getName()).append(" — no capacity\n");
                continue;
            }

            if (p.needsIcu() && !h.hasAvailableIcu()) {
                sb.append(" Failed ").append(h.getName()).append(" — needs ICU, none available\n");
                continue;
            }

            sb.append("  Success ").append(h.getName())
              .append(" (beds=").append(h.getBeds())
              .append(", icu=").append(h.getIcu()).append(")\n");

            results.add(idx);
        }

        if (results.isEmpty())
            sb.append("No suitable hospital found for ").append(p.getName()).append(".");

        return new FilterResult(results, sb.toString());
    }
}