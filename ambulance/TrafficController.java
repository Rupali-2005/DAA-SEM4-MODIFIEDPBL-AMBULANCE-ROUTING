package ambulance;

import java.util.Random;

// handles all traffic stuff (basically glue of multiple C modules)
public class TrafficController {
    // constants 
    public static final int MAX_TRAFFIC=10;
    public static final double BASE_WEIGHT_NORM = 100.0;
    public static final double REROUTE_THRESHOLD_PCT=20.0;
    public enum Severity { CLEAR, MILD, MODERATE, SEVERE }
    // small struct-ish holder
    public static class PriorityScore {
        public double rawScore;
        public double normalized;
        public Severity severity;
    }
    private final Graph graph;
    private final PriorityScore[] scores; // aligned with edges
    private double priorityFactor = 1.0;
    private int networkSeverityOrdinal=0;
    private final Random rng = new Random();
    public TrafficController(Graph graph){
        this.graph=graph;
        this.scores = new PriorityScore[Graph.MAX_EDGES];
        for(int i=0;i<scores.length;i++){
            scores[i]=new PriorityScore();
            scores[i].severity = Severity.CLEAR;
        }
    }

    public void setInitialTraffic(int edgeId,int level){
        if(!validEdge(edgeId)) return;

        graph.getEdges().get(edgeId).setTrafficLevel(level);
        rebuildScores(); // always keep in sync
    }
    public String updateTraffic(int edgeId,int newLevel){

        if(!validEdge(edgeId))
            return "[UPDATE] ERROR: edge "+edgeId+" out of range (max "+(graph.getEdgeCount()-1)+").";

        Edge e = graph.getEdges().get(edgeId);
        int oldLevel = e.getTrafficLevel();
        double oldEff = computeEffectiveWeight(e,edgeId);
        e.setTrafficLevel(newLevel);
        rebuildScores(); 
        double newEff=computeEffectiveWeight(e,edgeId);
        StringBuilder log=new StringBuilder();
        log.append(String.format(
                "Edge %d (%d→%d): level %d → %d%s%n",
                edgeId,e.getFrom(),e.getTo(),
                oldLevel,e.getTrafficLevel(),
                e.isBlocked() ? "  BLOCKED" : ""
        ));
        if(newEff>=Edge.INF){
            log.append(String.format("Edge %d impassable — re-route required.%n",edgeId));
        }
        else if(oldEff>0 && oldEff<Edge.INF){
            double pct=((newEff-oldEff)/oldEff)*100.0;
            if(pct>REROUTE_THRESHOLD_PCT){
                log.append(String.format(
                        "Effective weight +%.1f%% (%.1f → %.1f s).%n",
                        pct,oldEff,newEff
                ));
            }
        }
        return log.toString();
    }
    
    public String simulateRandomUpdate(){

        StringBuilder log = new StringBuilder("[SIM] ══ Random traffic simulation ══\n");
        int updates = Math.max(1, graph.getEdgeCount()/3);
        for(int i=0;i<updates;i++){
            int edgeId = rng.nextInt(graph.getEdgeCount());
            int cur = graph.getEdges().get(edgeId).getTrafficLevel();
            int next = Math.max(
                    0,
                    Math.min(MAX_TRAFFIC, cur + rng.nextInt(6)-2)
            );
            log.append(updateTraffic(edgeId,next));
        }
        log.append("[SIM] Done.\n");
        return log.toString();
    }

    public double computeEffectiveWeight(Edge e,int edgeIdx){
        if(e.isBlocked()) return Edge.INF;
        double congestion = 1.0 + (double)e.getTrafficLevel()/MAX_TRAFFIC;
        double priority = 1.0 + scores[edgeIdx].normalized;
        return e.getBaseWeight() * congestion * priorityFactor * priority;
    }

    //used by dijkstra
    public Graph.EdgeWeighter effectiveWeighter(){
        return (edge,idx) -> computeEffectiveWeight(edge,idx);
    }
    public Graph.DijkstraResult triggerReroute(int src,int dst,double refCost,StringBuilder log){
        log.append(String.format("Re-routing: node %d → node %d%n",src,dst));
        Graph.DijkstraResult result =
                graph.dijkstra(src,dst,effectiveWeighter());

        if(!result.reachable()){
            log.append("No route found.\n");
            return result;
        }
        log.append(String.format("Path: %s%n",result.pathString()));
        log.append(String.format("ETA : %.1f s%n",result.cost));
        if(refCost < Edge.INF){
            double pct = ((result.cost-refCost)/refCost)*100.0;
            if(pct > REROUTE_THRESHOLD_PCT){
                log.append(String.format(
                        "Cost +%.1f%% vs previous — new route adopted.%n",pct
                ));
            }
            else if(pct<0){
                log.append(String.format("V Cost improved by %.1f%%.%n",-pct));
            }
        }
        return result;
    }

    public void rebuildScores(){
        int worst=0;
        for(int i=0;i<graph.getEdgeCount();i++){
            Edge e = graph.getEdges().get(i);
            Severity sev = classifySeverity(e.getTrafficLevel());
            scores[i]=calculateScore(e,sev);
            if(sev.ordinal()>worst)
                worst=sev.ordinal();
        }
        networkSeverityOrdinal=worst;

        priorityFactor = switch(Severity.values()[worst]){
            case SEVERE -> 2.5;
            case MODERATE -> 1.5;
            default -> 1.0;
        };
    }

    public String getPriorityReport(){
        StringBuilder sb = new StringBuilder();
        sb.append("[PRIORITY] ══ Priority Score Report ══\n");
        sb.append(String.format(
                "  Network severity: %s  |  Priority factor: %.1f%n",
                Severity.values()[networkSeverityOrdinal],priorityFactor
        ));
        sb.append(String.format(
                "  %-5s %-5s %-5s %-8s %-10s %-8s %-8s %-8s%n",
                "Edge","From","To","Traffic","Severity","Raw","Norm","Blocked"
        ));
        sb.append("  " + "─".repeat(62) + "\n");
        for(int i=0;i<graph.getEdgeCount();i++){
            Edge e = graph.getEdges().get(i);
            PriorityScore ps = scores[i];
            sb.append(String.format(
                    "  %-5d %-5d %-5d %-8d %-10s %-8.3f %-8.3f %s%n",
                    i,e.getFrom(),e.getTo(),
                    e.getTrafficLevel(),
                    ps.severity,ps.rawScore,ps.normalized,
                    e.isBlocked() ? "YES":"No"
            ));
        }
        return sb.toString();
    }
    public String getEffectiveWeightTable(){
        StringBuilder sb=new StringBuilder();
        sb.append(String.format(
                "  %-5s %-5s %-5s %-12s %-8s %-12s %s%n",
                "Edge","From","To","Base(s)","Traffic","Eff(s)","Blocked"
        ));
        sb.append("  " + "─".repeat(58) + "\n");
        for(int i=0;i<graph.getEdgeCount();i++){

            Edge e = graph.getEdges().get(i);
            double eff = computeEffectiveWeight(e,i);
            sb.append(String.format(
                    "  %-5d %-5d %-5d %-12.1f %-8d %-12.2f %s%n",
                    i,e.getFrom(),e.getTo(),
                    e.getBaseWeight(),
                    e.getTrafficLevel(),
                    eff>=Edge.INF ? 0.0 : eff,
                    e.isBlocked() ? "YES":"No"
            ));
        }
        return sb.toString();
    }

    private Severity classifySeverity(int level){

        if(level>=8) return Severity.SEVERE;
        if(level>=6) return Severity.MODERATE;
        if(level>=4) return Severity.MILD;
        return Severity.CLEAR;
    }

    private PriorityScore calculateScore(Edge e,Severity sev){
        PriorityScore ps=new PriorityScore();
        ps.severity=sev;
        ps.rawScore =
                ((double)e.getTrafficLevel()/MAX_TRAFFIC)*0.5
              + (e.getBaseWeight()/BASE_WEIGHT_NORM)*0.3
              + ((double)sev.ordinal()/3.0)*0.2;

        if(e.isBlocked()) 
            ps.rawScore*=3.0;
        ps.normalized = Math.min(ps.rawScore,1.0);
        return ps;
    }
    private boolean validEdge(int id){
        return id>=0 && id<graph.getEdgeCount();
    }
}