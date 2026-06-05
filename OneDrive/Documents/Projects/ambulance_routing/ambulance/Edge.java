package ambulance;

public class Edge {

    // basic info about the edge (kept private for safety)
    private final int from;
    private final int to;
    private final double baseWeight;

    // these can change depending on traffic conditions
    private int trafficLevel;
    private boolean blocked;

    // using a large value instead of MAX to avoid overflow issues in calculations
    public static final double INF = Double.MAX_VALUE / 2.0;

    // constructor to initialize an edge
    public Edge(int from, int to, double baseWeight) {
        if (from<0 || to<0)
            throw new IllegalArgumentException("Node IDs must be non-negative");

        if (baseWeight<=0)
            throw new IllegalArgumentException("Base weight must be positive");

        this.from=from;
        this.to = to;
        this.baseWeight=baseWeight;
        this.trafficLevel = 0;
        this.blocked=false;
    }

    // simple getters
    public int getFrom() {
        return from;
    }

    public int getTo() {
        return to;
    }

    public double getBaseWeight() {
        return baseWeight;
    }

    public int getTrafficLevel() {
        return trafficLevel;
    }

    public boolean isBlocked() {
        return blocked;
    }

    // updates traffic level (kept between 0 and 10)
    public void setTrafficLevel(int level) {
        this.trafficLevel=Math.max(0, Math.min(10, level));
        this.blocked = (this.trafficLevel>=10);
    }

    // basic weight (used when ignoring congestion)
    public double getSimpleWeight() {
        return blocked ? INF : baseWeight;
    }

    // weight adjusted with traffic
    public double getCongestionWeight() {
        if (blocked) return INF;
        return baseWeight * (1.0 + trafficLevel/10.0);
    }

    // for printing/debugging
    public String toString() {
        return String.format("Edge(%d->%d base=%.1f traffic=%d%s)",
                from, to, baseWeight, trafficLevel, blocked ? " BLOCKED" : "");
    }
}