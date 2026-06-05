package ambulance;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.List;

// panel to draw and visualize the road graph
public class GraphPanel extends JPanel {

    // colors used for the dark theme
    private static final Color BG_DARK=new Color(0x10, 0x16, 0x20);
    private static final Color NODE_FILL = new Color(0x1C, 0x22, 0x2E);
    private static final Color NODE_BORDER=new Color(0x3A, 0x9B, 0xDC);
    private static final Color NODE_TEXT = new Color(0xE8, 0xED, 0xF2);
    private static final Color EDGE_LABEL=new Color(0x55, 0x66, 0x77);
    private static final Color AMBER = new Color(0xF5, 0xA6, 0x23); // route highlight
    private static final Color BLUE=new Color(0x2E, 0x86, 0xFF); // start node

    // fixed positions for demo (10 nodes)
    private static final int[][] DEMO_POS={
        {100,250},{240,145},{390,145},{500,245},{500,375},
        {100,375},{300,295},{390,375},{390,480},{240,480}
    };

    private static final int NODE_R=18;
    private static final int CANVAS_W = 640;
    private static final int CANVAS_H=560;

    private final Graph graph;
    private final TrafficController tc;

    private int[] highlightPath=new int[0]; // last route
    private int startNode=-1; // ambulance start
    private int[][] nodePositions;

    public GraphPanel(Graph graph, TrafficController tc) {
        this.graph=graph;
        this.tc = tc;
        setPreferredSize(new Dimension(CANVAS_W, CANVAS_H));
        setBackground(BG_DARK);
        recomputePositions();
    }

    public void refresh() {
        recomputePositions();
        repaint();
    }

    public void highlightRoute(int[] path, int src) {
        this.highlightPath = (path==null) ? new int[0] : path.clone();
        this.startNode=src;
        recomputePositions();
        repaint();
    }

    public void clearHighlight() {
        highlightPath=new int[0];
        startNode = -1;
        repaint();
    }

    // decides where nodes should be placed
    private void recomputePositions() {
        int n=graph.getNumNodes();

        if (n==10) {
            nodePositions=DEMO_POS;
        } else {
            nodePositions = circleLayout(n, CANVAS_W/2, CANVAS_H/2,
                    Math.min(CANVAS_W, CANVAS_H)/2 - 50);
        }
    }

    // basic circular layout for nodes
    private int[][] circleLayout(int n, int cx, int cy, int r) {
        int[][] pos=new int[n][2];

        for (int i=0;i<n;i++) {
            double angle = 2*Math.PI*i/n - Math.PI/2;
            pos[i][0]=(int)(cx + r*Math.cos(angle));
            pos[i][1]=(int)(cy + r*Math.sin(angle));
        }
        return pos;
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);

        Graphics2D g=(Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawGrid(g);
        drawEdges(g);
        drawHighlightedRoute(g);
        drawNodes(g);
        drawLegend(g);
    }

    // draws background grid
    private void drawGrid(Graphics2D g) {
        g.setColor(new Color(0x1A, 0x22, 0x30));
        g.setStroke(new BasicStroke(0.5f));

        for (int x=0;x<getWidth();x+=40) g.drawLine(x,0,x,getHeight());
        for (int y=0;y<getHeight();y+=40) g.drawLine(0,y,getWidth(),y);
    }

    // draws all edges with traffic coloring
    private void drawEdges(Graphics2D g) {
        List<Edge> edges=graph.getEdges();

        for (int i=0;i<edges.size();i++) {
            Edge e = edges.get(i);

            if (e.getFrom()>=nodePositions.length || e.getTo()>=nodePositions.length) continue;

            int[] fp=nodePositions[e.getFrom()];
            int[] tp=nodePositions[e.getTo()];

            Color col=trafficColor(e.getTrafficLevel(), e.isBlocked());
            float thick=1.5f + (e.getTrafficLevel()/10.0f)*3.5f;

            Stroke str = e.isBlocked()
                ? new BasicStroke(thick, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,10f,new float[]{8f,6f},0f)
                : new BasicStroke(thick, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

            g.setColor(col);
            g.setStroke(str);
            drawArrow(g, fp[0],fp[1], tp[0],tp[1]);

            // small label on edge
            g.setStroke(new BasicStroke(1));
            g.setColor(EDGE_LABEL);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 9));

            int mx=(fp[0]+tp[0])/2 + 4;
            int my=(fp[1]+tp[1])/2 - 4;

            g.drawString(String.format("%.0fs T=%d", e.getBaseWeight(), e.getTrafficLevel()), mx, my);
        }
    }

    // highlights the chosen route
    private void drawHighlightedRoute(Graphics2D g) {
        if (highlightPath.length<2) return;

        g.setColor(AMBER);
        g.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        for (int i=0;i<highlightPath.length-1;i++) {
            int a=highlightPath[i], b=highlightPath[i+1];

            if (a>=nodePositions.length || b>=nodePositions.length) continue;

            drawArrow(g, nodePositions[a][0],nodePositions[a][1],
                         nodePositions[b][0],nodePositions[b][1]);
        }

        int mid = highlightPath[highlightPath.length/2];
        if (mid<nodePositions.length) {
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
            g.drawString("ACTIVE ROUTE", nodePositions[mid][0]+6, nodePositions[mid][1]-12);
        }
    }

    // draws nodes and highlights
    private void drawNodes(Graphics2D g) {
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));

        int n=Math.min(graph.getNumNodes(), nodePositions.length);

        for (int i=0;i<n;i++) {
            int x=nodePositions[i][0], y=nodePositions[i][1];

            boolean onRoute=isOnRoute(i);
            boolean isStart=(i==startNode);

            if (onRoute || isStart) {
                Color glow = isStart
                        ? new Color(BLUE.getRed(), BLUE.getGreen(), BLUE.getBlue(),45)
                        : new Color(AMBER.getRed(), AMBER.getGreen(), AMBER.getBlue(),40);

                g.setColor(glow);
                g.fillOval(x-NODE_R-6, y-NODE_R-6, (NODE_R+6)*2, (NODE_R+6)*2);
            }

            g.setColor(isStart ? new Color(0x0A,0x2A,0x5A) : NODE_FILL);
            g.fillOval(x-NODE_R, y-NODE_R, NODE_R*2, NODE_R*2);

            Color border = isStart ? BLUE : (onRoute ? AMBER : NODE_BORDER);
            g.setColor(border);
            g.setStroke(new BasicStroke(isStart||onRoute ? 2.5f : 1.5f));
            g.drawOval(x-NODE_R, y-NODE_R, NODE_R*2, NODE_R*2);

            g.setColor(isStart ? BLUE : (onRoute ? AMBER : NODE_TEXT));
            FontMetrics fm=g.getFontMetrics();
            String lbl=String.valueOf(i);

            g.drawString(lbl, x-fm.stringWidth(lbl)/2, y+fm.getAscent()/2 - 1);

            if (isStart) {
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 9));
                g.setColor(BLUE);

                FontMetrics sfm=g.getFontMetrics();
                g.drawString("START", x-sfm.stringWidth("START")/2, y+NODE_R+11);

                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
            }
        }
    }

    // legend at bottom
    private void drawLegend(Graphics2D g) {
        int lx=8, ly=getHeight()-120;

        g.setColor(new Color(0x10,0x16,0x20,210));
        g.fillRoundRect(lx-4, ly-14, 155,125,6,6);

        g.setColor(new Color(0x2E,0x3A,0x50));
        g.setStroke(new BasicStroke(1));
        g.drawRoundRect(lx-4, ly-14, 155,125,6,6);

        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 9));
        g.setColor(AMBER);
        g.drawString("LEGEND", lx, ly);

        String[] labels={"Clear (0–3)","Mild (4–5)","Moderate (6–7)","Severe (8–9)","Blocked (10)"};
        int[] levels={0,4,6,8,10};

        for (int i=0;i<labels.length;i++) {
            int ry=ly+14 + i*15;

            g.setColor(trafficColor(levels[i], levels[i]>=10));
            g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(lx, ry, lx+20, ry);

            g.setColor(new Color(0xB0,0xBC,0xCC));
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
            g.drawString(labels[i], lx+26, ry+4);
        }

        int sy=ly+14 + 5*15 + 4;

        g.setColor(BLUE);
        g.fillOval(lx+3, sy-7, 14,14);

        g.setColor(new Color(0xB0,0xBC,0xCC));
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        g.drawString("Ambulance start", lx+26, sy+4);
    }

    // draws arrow between nodes
    private void drawArrow(Graphics2D g, int x1,int y1,int x2,int y2) {
        double dx=x2-x1, dy=y2-y1;
        double len=Math.sqrt(dx*dx + dy*dy);

        if (len==0) return;

        double ux=dx/len, uy=dy/len;

        int sx=(int)(x1 + ux*NODE_R), sy=(int)(y1 + uy*NODE_R);
        int ex=(int)(x2 - ux*NODE_R), ey=(int)(y2 - uy*NODE_R);

        g.drawLine(sx,sy,ex,ey);

        double angle=Math.atan2(ey-sy, ex-sx), aa=Math.toRadians(22);
        int al=9;

        Stroke saved=g.getStroke();
        g.setStroke(new BasicStroke(1.5f));

        g.drawLine(ex,ey,(int)(ex - al*Math.cos(angle-aa)),(int)(ey - al*Math.sin(angle-aa)));
        g.drawLine(ex,ey,(int)(ex - al*Math.cos(angle+aa)),(int)(ey - al*Math.sin(angle+aa)));

        g.setStroke(saved);
    }

    private boolean isOnRoute(int id) {
        for (int n : highlightPath) if (n==id) return true;
        return false;
    }

    // decides color based on traffic level
    private Color trafficColor(int level, boolean blocked) {
        if (blocked || level>=10) return new Color(0xE7,0x4C,0x3C);
        if (level>=8) return new Color(0xE0,0x6C,0x1A);
        if (level>=6) return new Color(0xF0,0xB8,0x15);
        if (level>=4) return new Color(0xA8,0xD4,0x20);
        return new Color(0x2E,0xCC,0x71);
    }
}