package ambulance;

import java.util.*;

// represents a directed graph of roads
public class Graph {

    public static final int MAX_NODES = 50;
    public static final int MAX_EDGES = 200;

    // functional interface to decide how edge weight is calculated
    @FunctionalInterface
    public interface EdgeWeighter {
        // returns weight of an edge (can return INF to ignore it)
        double weight(Edge edge, int edgeIndex);
    }

    private final int numNodes;
    private final List<Edge> edges=new ArrayList<>();

    public Graph(int numNodes) {
        if (numNodes<=0 || numNodes>MAX_NODES)
            throw new IllegalArgumentException("numNodes must be 1.." + MAX_NODES);

        this.numNodes = numNodes;
    }

    // basic getters
    public int getNumNodes() {
        return numNodes;
    }

    public List<Edge> getEdges() {
        return Collections.unmodifiableList(edges);
    }

    public int getEdgeCount() {
        return edges.size();
    }

    // adds a new edge to the graph
    public void addEdge(int from, int to, double weight) {
        if (edges.size()>=MAX_EDGES)
            throw new IllegalStateException("Edge limit reached");

        if (from>=numNodes || to>=numNodes)
            throw new IllegalArgumentException("Node out of range");

        edges.add(new Edge(from, to, weight));
    }

    // core dijkstra logic (used for both normal and traffic cases)
    public DijkstraResult dijkstra(int start, int end, EdgeWeighter weighter) {

        double[] dist=new double[numNodes];
        int[] parent=new int[numNodes];
        boolean[] visited = new boolean[numNodes];

        Arrays.fill(dist, Edge.INF);
        Arrays.fill(parent, -1);

        dist[start]=0.0;

        for (int iter = 0; iter<numNodes-1; iter++) {

            int u=-1;

            // pick closest unvisited node
            for (int v=0; v<numNodes; v++)
                if (!visited[v] && (u==-1 || dist[v]<dist[u])) u=v;

            if (u==-1 || dist[u]>=Edge.INF) break;

            visited[u]=true;

            // relax edges
            for (int ei=0; ei<edges.size(); ei++) {
                Edge e = edges.get(ei);

                if (e.getFrom()!=u) continue;

                double w=weighter.weight(e, ei);
                if (w>=Edge.INF) continue;

                if (!visited[e.getTo()] && dist[u]+w < dist[e.getTo()]) {
                    dist[e.getTo()] = dist[u]+w;
                    parent[e.getTo()]=u;
                }
            }
        }

        return new DijkstraResult(start, end, dist[end], parent, numNodes);
    }

    // shortcut when ignoring traffic
    public DijkstraResult dijkstraBase(int start, int end) {
        return dijkstra(start, end, (e, i) -> e.getSimpleWeight());
    }

    // prints edges in a simple table format
    public String getEdgeTable() {
        StringBuilder sb=new StringBuilder();

        sb.append(String.format("Nodes: %d  Edges: %d%n", numNodes, edges.size()));
        sb.append(String.format("%-4s %-5s %-5s %-10s %-8s %-8s%n",
                "ID", "From", "To", "BaseWt(s)", "Traffic", "Blocked"));

        sb.append("-".repeat(46)).append("\n");

        for (int i=0;i<edges.size();i++) {
            Edge e = edges.get(i);

            sb.append(String.format("%-4d %-5d %-5d %-10.1f %-8d %-8s%n",
                    i, e.getFrom(), e.getTo(), e.getBaseWeight(),
                    e.getTrafficLevel(), e.isBlocked() ? "YES" : "No"));
        }

        return sb.toString();
    }

    // result object for dijkstra
    public static class DijkstraResult {

        public final int start;
        public final int end;
        public final double cost;
        public final int[] path;

        DijkstraResult(int start, int end, double cost, int[] parent, int n) {
            this.start=start;
            this.end = end;
            this.cost = cost;

            if (cost>=Edge.INF) {
                this.path=new int[0];
            } else {
                List<Integer> rev = new ArrayList<>();

                for (int cur=end; cur!=-1; cur=parent[cur])
                    rev.add(cur);

                Collections.reverse(rev);
                this.path = rev.stream().mapToInt(x -> x).toArray();
            }
        }

        public boolean reachable() {
            return path.length>0;
        }

        public String pathString() {
            if (!reachable()) return "(no path)";

            StringBuilder sb = new StringBuilder();

            for (int i=0;i<path.length;i++) {
                if (i>0) sb.append(" -> ");
                sb.append(path[i]);
            }

            return sb.toString();
        }
    }
}