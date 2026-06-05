package ambulance;

import java.io.IOException;
import java.util.*;

public class AmbulanceSystem {

    public static final int MAX_HOSPITALS = 20;
    public static final int MAX_PATIENTS = 50;

    private final List<Hospital> hospitals = new ArrayList<>();
    private final List<Patient> patients = new ArrayList<>();
    private final HashIndex hashIndex = new HashIndex();
    private Graph graph;
    private TrafficController tc;

    // Used for showing last route in UI
    private int[] lastRoute = new int[0];
    private int lastDispatchNode = -1;

    public AmbulanceSystem() {
        graph = new Graph(10);
        setupDemoGraph();
        tc = new TrafficController(graph);
        setupDemoTraffic();

        hospitals.addAll(Database.loadHospitals());
        patients.addAll(Database.loadPatients());
        buildHashIndex();
    }

    private void setupDemoGraph() {
        graph.addEdge(0, 1, 30); graph.addEdge(1, 2, 20);
        graph.addEdge(2, 3, 25); graph.addEdge(3, 4, 15);
        graph.addEdge(0, 5, 40); graph.addEdge(5, 6, 20);
        graph.addEdge(6, 3, 30); graph.addEdge(1, 6, 35);
        graph.addEdge(4, 7, 20); graph.addEdge(7, 8, 25);
        graph.addEdge(8, 9, 15); graph.addEdge(6, 9, 45);
        graph.addEdge(2, 7, 30); graph.addEdge(5, 9, 60);
    }

    private void setupDemoTraffic() {
        tc.setInitialTraffic(0, 3);
        tc.setInitialTraffic(2, 6);
        tc.setInitialTraffic(7, 8);
        tc.setInitialTraffic(11, 5);
        tc.rebuildScores();
    }

    public void buildHashIndex() {
        hashIndex.clear();
        for (int i = 0; i < hospitals.size(); i++)
            for (String s : hospitals.get(i).getSpecializations())
                hashIndex.insert(s, i);
    }

    public List<Hospital> getHospitals() { return Collections.unmodifiableList(hospitals); }
    public List<Patient> getPatients() { return Collections.unmodifiableList(patients); }
    public Graph getGraph() { return graph; }
    public TrafficController getTc() { return tc; }
    public int[] getLastRoute() { return lastRoute.clone(); }
    public int getLastDispatchNode() { return lastDispatchNode; }

    // Resets graph with new node count
    public String resetGraph(int nodeCount) {
        if (nodeCount < 2 || nodeCount > Graph.MAX_NODES)
            return "Node count must be between 2 and " + Graph.MAX_NODES + ".";
        graph = new Graph(nodeCount);
        tc = new TrafficController(graph);
        lastRoute = new int[0];
        lastDispatchNode = -1;
        return "Graph reset: " + nodeCount + " nodes, 0 edges. Add edges below.";
    }

    public String addGraphEdge(int from, int to, double weight) {
        if (from < 0 || from >= graph.getNumNodes() ||
            to < 0 || to >= graph.getNumNodes())
            return String.format("Nodes must be in range 0–%d.", graph.getNumNodes() - 1);
        if (from == to)
            return "Self-loops are not allowed.";
        if (weight <= 0)
            return "Weight must be greater than zero.";
        try {
            graph.addEdge(from, to, weight);
            tc.rebuildScores();
            return String.format("Edge added: %d -> %d (%.1f s) [Edge ID = %d]",
                    from, to, weight, graph.getEdgeCount() - 1);
        } catch (IllegalStateException e) {
            return "Edge limit reached (max " + Graph.MAX_EDGES + ").";
        }
    }

    public String loadDemoGraph() {
        graph = new Graph(10);
        setupDemoGraph();
        tc = new TrafficController(graph);
        setupDemoTraffic();
        lastRoute = new int[0];
        lastDispatchNode = -1;
        return "Demo graph loaded: 10 nodes, 14 edges with preset traffic.";
    }

    public String getDashboardSummary() {
        int totalBeds = hospitals.stream().mapToInt(Hospital::getBeds).sum();
        int totalIcu = hospitals.stream().mapToInt(Hospital::getIcu).sum();
        int assigned = (int) patients.stream().filter(p -> p.getHospitalId() >= 0).count();

        return String.format(
            "Hospitals : %d registered | Total beds: %d | ICU units: %d%n" +
            "Patients  : %d total | %d dispatched%n" +
            "Road graph: %d nodes | %d edges%n",
            hospitals.size(), totalBeds, totalIcu,
            patients.size(), assigned,
            graph.getNumNodes(), graph.getEdgeCount());
    }

    public String addHospital(Hospital h) {
        if (hospitals.size() >= MAX_HOSPITALS) return "Hospital database is full (max " + MAX_HOSPITALS + ").";
        h.setId(hospitals.size() + 1);
        hospitals.add(h);
        buildHashIndex();
        return "Hospital '" + h.getName() + "' added (ID=" + h.getId() + ").";
    }

    public String updateHospital(int id, int beds, int icu) {
        for (Hospital h : hospitals) {
            if (h.getId() == id) {
                h.setBeds(beds); h.setIcu(icu);
                return "Hospital ID=" + id + " updated.";
            }
        }
        return "Hospital ID=" + id + " not found.";
    }

    public String deleteHospital(int id) {
        boolean removed = hospitals.removeIf(h -> h.getId() == id);
        if (removed) { buildHashIndex(); return "Hospital ID=" + id + " deleted."; }
        return "Hospital ID=" + id + " not found.";
    }

    public String getAllHospitals() {
        if (hospitals.isEmpty()) return "No hospitals registered.";
        StringBuilder sb = new StringBuilder();
        hospitals.forEach(h -> sb.append(h).append("\n"));
        return sb.toString();
    }

    public String addPatientAndDispatch(Patient p) {
        if (patients.size() >= MAX_PATIENTS) return "Patient list full (max " + MAX_PATIENTS + ").";
        p.setId(patients.size() + 1);
        patients.add(p);
        return "Patient registered (ID=" + p.getId() + ").\n\n" + dispatchAmbulance(p);
    }

    public String getAllPatients() {
        if (patients.isEmpty()) return "No patients on record.";
        StringBuilder sb = new StringBuilder();
        patients.forEach(p -> sb.append(p).append("\n"));
        return sb.toString();
    }

    public String dischargePatient(int patientId) {
        for (Patient p : patients) {
            if (p.getId() == patientId) {
                if (p.getHospitalId() < 0) return "Patient " + patientId + " not yet assigned to a hospital.";
                for (Hospital h : hospitals) {
                    if (h.getId() == p.getHospitalId()) {
                        h.dischargePatient(p.isUsingIcu());
                        p.setHospitalId(-1);
                        return String.format("Patient %s discharged from %s. Bed/ICU released.",
                                p.getName(), h.getName());
                    }
                }
                return "Assigned hospital not found.";
            }
        }
        return "Patient ID=" + patientId + " not found.";
    }

    public String dispatchForExisting(int patientId) {
        return patients.stream()
                .filter(p -> p.getId() == patientId)
                .findFirst()
                .map(this::dispatchAmbulance)
                .orElse("Patient ID=" + patientId + " not found.");
    }

    // Core dispatch logic
    public String dispatchAmbulance(Patient p) {
        StringBuilder log = new StringBuilder();
        log.append("DISPATCH: ").append(p.getName())
           .append(" [").append(p.priorityLabel()).append("]\n\n");

        log.append("Step 1: Filter hospitals\n");
        Filter.FilterResult fr = Filter.filterHospitals(hashIndex, hospitals, p);
        log.append(fr.report).append("\n");
        if (!fr.hasCandidates()) return log.toString();

        log.append("Step 2: Routing\n");
        tc.rebuildScores();

        int bestIdx = -1;
        double bestCost = Edge.INF;
        Graph.DijkstraResult bestResult = null;

        for (int idx : fr.indices) {
            Hospital h = hospitals.get(idx);
            StringBuilder routeLog = new StringBuilder();
            Graph.DijkstraResult res = tc.triggerReroute(p.getLocNode(), h.getLocNode(), Edge.INF, routeLog);
            log.append("  -> ").append(h.getName()).append(": ");
            if (res.reachable()) {
                log.append(String.format("ETA %.1f s path: %s\n", res.cost, res.pathString()));
                if (res.cost < bestCost) { bestCost = res.cost; bestIdx = idx; bestResult = res; }
            } else {
                log.append("unreachable\n");
            }
        }

        if (bestIdx == -1) {
            log.append("\nNo reachable hospital found.\n");
            return log.toString();
        }

        Hospital best = hospitals.get(bestIdx);
        log.append("\nStep 3: Dispatch\n");
        log.append(String.format("  Best hospital : %s (node %d)\n", best.getName(), best.getLocNode()));
        log.append(String.format("  Route : %s\n", bestResult.pathString()));
        log.append(String.format("  ETA : %.1f s\n", bestCost));

        boolean usedIcu = p.needsIcu() && best.hasAvailableIcu();
        boolean admitted = best.admitPatient(p.needsIcu());
        if (!admitted) {
            log.append("Capacity changed during routing - dispatch failed.\n");
            return log.toString();
        }

        p.setHospitalId(best.getId());
        p.setUsedIcu(usedIcu);

        log.append(String.format("  Admitted to : %s (%s)\n", best.getName(), usedIcu ? "ICU" : "General"));
        log.append(String.format("  Remaining : %d beds, %d ICU\n", best.getBeds(), best.getIcu()));
        log.append("\nAmbulance dispatched.\n");

        lastRoute = bestResult.path;
        lastDispatchNode = p.getLocNode();
        return log.toString();
    }

    public String updateTraffic(int edgeId, int level) { return tc.updateTraffic(edgeId, level); }
    public String simulateTraffic() { return tc.simulateRandomUpdate(); }
    public String getPriorityReport() { return tc.getPriorityReport(); }

    public String getRoadGraph() {
        return "BASE GRAPH\n" + graph.getEdgeTable()
             + "\nTRAFFIC WEIGHTS\n" + tc.getEffectiveWeightTable();
    }

    public String saveAll() {
        try {
            Database.saveHospitals(hospitals);
            Database.savePatients(patients);
            return "Data saved.";
        } catch (IOException e) {
            return "Save failed: " + e.getMessage();
        }
    }
}