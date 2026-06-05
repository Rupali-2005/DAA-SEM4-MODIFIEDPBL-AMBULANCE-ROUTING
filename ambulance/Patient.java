package ambulance;
// basic patient model (priority 1 = chill, 5 = critical)
public class Patient {
    private int id;
    private String name;
    private int priority; // 1-5
    private String specialization;
    private int hospitalId; // -1 means not assigned yet
    private int locNode;
    private boolean usedIcu; // track this so discharge logic is easier later
    public Patient(){
        this.hospitalId=-1;
    }

    public Patient(int id,String name,int priority,String specialization,int locNode){
        this.id=id;
        this.name = name;
        this.priority=Math.max(1, Math.min(5,priority));
        this.specialization = specialization;
        this.locNode=locNode;
        this.hospitalId = -1;
    }

    // getters (keeping them simple)
    public int getId(){ return id; }
    public String getName() { return name; }
    public int getPriority(){ return priority; }
    public String getSpecialization(){ return specialization; }
    public int getHospitalId(){ return hospitalId; }
    public int getLocNode(){ return locNode; }
    public boolean isUsingIcu(){ return usedIcu; }

    public boolean needsIcu(){
        return priority>=4;
    }

    // setters
    public void setId(int id){ this.id=id; }
    public void setName(String name){ this.name=name; }

    public void setPriority(int p){
        this.priority = Math.max(1,Math.min(5,p));
    }

    public void setSpecialization(String s){ this.specialization=s; }
    public void setHospitalId(int hid){ this.hospitalId=hid; }
    public void setLocNode(int node){ this.locNode=node; }
    public void setUsedIcu(boolean u){ this.usedIcu=u; }

    // convert numeric priority to readable label
    public String priorityLabel(){
        switch(priority){
            case 1: return "Routine";
            case 2: return "Low";
            case 3: return "Moderate";
            case 4: return "Urgent";
            case 5: return "Critical";
            default: return "Unknown";
        }
    }
    public String toString(){
        return String.format(
                "ID=%-2d | %-18s | %-8s | %-15s | Node=%-2d | Hospital=%s",
                id,name,priorityLabel(),specialization,locNode,
                hospitalId<0 ? "Unassigned" : String.valueOf(hospitalId)
        );
    }
}