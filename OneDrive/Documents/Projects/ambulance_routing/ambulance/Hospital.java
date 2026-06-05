package ambulance;
import java.util.*;

// represents a hospital with its capacity and specializations
public class Hospital {

    private int id;
    private String name;
    private final List<String> specializations;

    private int beds;
    private int icu;
    private int locNode;

    public Hospital() {
        specializations=new ArrayList<>();
    }

    public Hospital(int id, String name, List<String> specializations,
                    int beds, int icu, int locNode) {

        this.id=id;
        this.name = name;
        this.specializations=new ArrayList<>(specializations);

        this.beds = beds;
        this.icu=icu;
        this.locNode=locNode;
    }

    // getters
    public int getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public List<String> getSpecializations() {
        return Collections.unmodifiableList(specializations);
    }

    public int getBeds() {
        return beds;
    }
    public int getIcu() {
        return icu;
    }
    public int getLocNode() {
        return locNode;
    }

    // setters (mainly used while loading data or updating state)
    public void setId(int id) {
        this.id = id;
    }
    public void setName(String name) {
        this.name=name;
    }
    public void setBeds(int beds) {
        this.beds = Math.max(0, beds);
    }
    public void setIcu(int icu) {
        this.icu=Math.max(0, icu);
    }
    public void setLocNode(int node) {
        this.locNode = node;
    }

    // adds a specialization if valid
    public void addSpecialization(String s) {
        if (s!=null && !s.isBlank())
            specializations.add(s.trim());
    }

    // basic availability checks
    public boolean hasAvailableBed() {
        return beds>0;
    }

    public boolean hasAvailableIcu() {
        return icu>0;
    }

    // tries to admit a patient based on requirement
    public boolean admitPatient(boolean needsIcu) {
        if (needsIcu && icu>0) {
            icu--;
            return true;
        }

        if (!needsIcu && beds>0) {
            beds--;
            return true;
        }

        return false;
    }
    // frees up a slot when patient leaves
    public void dischargePatient(boolean wasIcu) {
        if (wasIcu) icu++;
        else beds++;
    }
    // checks if hospital supports a specialization
    public boolean hasSpecialization(String spec) {
        return specializations.stream()
                .anyMatch(s -> s.equalsIgnoreCase(spec));
    }
    public String toString() {
        return String.format("ID=%-2d | %-22s | Beds=%-3d | ICU=%-3d | Node=%d | %s",
                id, name, beds, icu, locNode, specializations);
    }
}