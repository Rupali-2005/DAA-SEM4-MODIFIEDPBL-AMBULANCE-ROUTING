// handles reading and writing hospital + patient data
package ambulance;

import java.io.*;
import java.util.*;

public class Database {

    private static final String HOSPITAL_FILE="hospitals.txt";
    private static final String PATIENT_FILE = "patients.txt";

    // reads hospital data from file and turns it into objects
    public static List<Hospital> loadHospitals() {
        List<Hospital> list=new ArrayList<>();
        File f = new File(HOSPITAL_FILE);
        if (!f.exists()) return list;

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            int lineNo=0;

            while ((line = br.readLine()) != null) {
                lineNo++;
                line=line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                try {
                    Hospital h=parseHospital(line);
                    if (h!=null) list.add(h);
                } catch (Exception e) {
                    System.out.println("Hospital file line " + lineNo + " skipped: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("Cannot read " + HOSPITAL_FILE + ": " + e.getMessage());
        }
        return list;
    }

    // takes one line and builds a Hospital object from it
    private static Hospital parseHospital(String line) {
        String[] tok=line.split(";");
        if (tok.length<4) return null;

        Hospital h = new Hospital();
        h.setId(Integer.parseInt(tok[0].trim()));
        h.setName(tok[1].trim());

        int specCount = Integer.parseInt(tok[2].trim());
        int idx=3;

        for (int i=0;i<specCount && idx<tok.length;i++,idx++) {
            h.addSpecialization(tok[idx]);
        }

        if (idx+2>=tok.length) return null;

        h.setBeds(Integer.parseInt(tok[idx++].trim()));
        h.setIcu(Integer.parseInt(tok[idx++].trim()));
        h.setLocNode(Integer.parseInt(tok[idx].trim()));

        return h;
    }

    // saves hospital list back into the file
    public static void saveHospitals(List<Hospital> list) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(HOSPITAL_FILE))) {
            pw.println("# AmbulanceOS hospital data");

            for (Hospital h : list) {
                StringBuilder sb=new StringBuilder();

                sb.append(h.getId()).append(";").append(h.getName()).append(";")
                  .append(h.getSpecializations().size()).append(";");

                for (String s : h.getSpecializations()) {
                    sb.append(s).append(";");
                }

                sb.append(h.getBeds()).append(";")
                  .append(h.getIcu()).append(";")
                  .append(h.getLocNode());

                pw.println(sb);
            }
        }
    }

    // reads patient data and creates Patient objects
    public static List<Patient> loadPatients() {
        List<Patient> list = new ArrayList<>();
        File f=new File(PATIENT_FILE);

        if (!f.exists()) return list;

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            int lineNo = 0;

            while ((line = br.readLine()) != null) {
                lineNo++;
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) continue;

                try {
                    Patient p = parsePatient(line);
                    if (p!=null) list.add(p);
                } catch (Exception e) {
                    System.out.println("Patient file line " + lineNo + " skipped: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("Cannot read " + PATIENT_FILE + ": " + e.getMessage());
        }

        return list;
    }

    // builds a Patient object from one line
    private static Patient parsePatient(String line) {
        String[] tok = line.split(";");

        if (tok.length<6) return null;

        Patient p=new Patient();

        p.setId(Integer.parseInt(tok[0].trim()));
        p.setName(tok[1].trim());
        p.setPriority(Integer.parseInt(tok[2].trim()));
        p.setSpecialization(tok[3].trim());
        p.setHospitalId(Integer.parseInt(tok[4].trim()));
        p.setLocNode(Integer.parseInt(tok[5].trim()));

        return p;
    }

    // saves patient list into file
    public static void savePatients(List<Patient> list) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(PATIENT_FILE))) {

            pw.println("# AmbulanceOS patient data");

            for (Patient p : list) {
                pw.printf("%d;%s;%d;%s;%d;%d%n",
                        p.getId(), p.getName(), p.getPriority(),
                        p.getSpecialization(), p.getHospitalId(), p.getLocNode());
            }
        }
    }
}