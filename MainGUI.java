package ambulance;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

public class MainGUI extends JFrame {
    static final Color BG = new Color(0x13, 0x17, 0x1E);
    static final Color PANEL = new Color(0x1C, 0x22, 0x2E);
    static final Color PANEL_LITE = new Color(0x23, 0x2B, 0x3A);
    static final Color BORDER_C=new Color(0x2E, 0x3A, 0x50);
    static final Color ACCENT=new Color(0xF5, 0xA6, 0x23);
    static final Color ACCENT_DIM=new Color(0x8A, 0x5D, 0x10);
    static final Color TEXT_PRI=new Color(0xE8, 0xED, 0xF2);
    static final Color TEXT_SEC=new Color(0x72, 0x85, 0x9A);
    static final Color TEXT_DIM=new Color(0x44, 0x55, 0x66);
    static final Color GREEN=new Color(0x2E, 0xCC, 0x71);
    static final Color RED=new Color(0xE7, 0x4C, 0x3C);
    static final Color BLUE=new Color(0x3A, 0x9B, 0xDC);
    static final Color CONSOLE_BG=new Color(0x0C, 0x10, 0x14);
    static final Font F_LABEL=new Font("SansSerif", Font.PLAIN, 12);
    static final Font F_BOLD=new Font("SansSerif", Font.BOLD, 12);
    static final Font F_SMALL=new Font("SansSerif", Font.PLAIN, 11);
    static final Font F_MONO=new Font(Font.MONOSPACED, Font.PLAIN, 12);
    static final Font F_ERR=new Font("SansSerif", Font.ITALIC, 11);

    private final AmbulanceSystem sys=new AmbulanceSystem();
    private JTextArea console;
    private GraphPanel graphPanel;
    private JLabel statusLabel;
    private JLabel statHospitals, statBeds, statIcu, statPatients, statDispatched;
    private DefaultTableModel hospitalModel, patientModel;

    public MainGUI() {
        super("AmbulanceOS · Emergency Routing & Dispatch");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1150, 780));
        graphPanel = new GraphPanel(sys.getGraph(), sys.getTc());

        JPanel root= new JPanel(new BorderLayout(0, 0));
        root.setBackground(BG);
        root.add(buildTopBar(), BorderLayout.NORTH);
        root.add(buildCenter(), BorderLayout.CENTER);
        root.add(buildStatusBar(), BorderLayout.SOUTH);
        setContentPane(root);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        refreshStats();
        log("AmbulanceOS ready. Graph: " + sys.getGraph().getNumNodes() + " nodes · " + sys.getGraph().getEdgeCount() + " edges.");
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout()) {
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0;
                g.setColor(PANEL);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(ACCENT);
                g.fillRect(0, getHeight() - 2, getWidth(), 2);
            }
        };
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(12, 20, 12, 20));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);
        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("AmbulanceOS");
        title.setFont(new Font("SansSerif", Font.BOLD, 17));
        title.setForeground(TEXT_PRI);
        JLabel sub= new JLabel("EMERGENCY ROUTING & DISPATCH SYSTEM");
        sub.setFont(new Font("SansSerif", Font.BOLD, 9));
        sub.setForeground(ACCENT);
        stack.add(title);
        stack.add(sub);
        left.add(Box.createHorizontalStrut(10));
        left.add(stack);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        right.add(pill("LIVE", GREEN));
        right.add(pill("10 NODES", BLUE));
        right.add(pill("14 EDGES", TEXT_SEC));
        JButton save = accentBtn("Save & Exit");
        save.addActionListener(e -> { log(sys.saveAll()); System.exit(0); });
        right.add(save);

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JSplitPane buildCenter() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.BOLD, 12));
        tabs.setBackground(PANEL);
        tabs.addTab("  Dashboard  ", buildDashboardTab());
        tabs.addTab("  Hospitals  ", buildHospitalTab());
        tabs.addTab("  Dispatch   ", buildDispatchTab());
        tabs.addTab("  Traffic    ", buildTrafficTab());
        tabs.addTab("  Graph Setup", buildGraphSetupTab());
        tabs.addTab("  Graph View ", buildGraphViewTab());
        tabs.addTab("  Reports    ", buildReportTab());

        console = new JTextArea();
        console.setEditable(false);
        console.setFont(F_MONO);
        console.setBackground(CONSOLE_BG);
        console.setForeground(new Color(0x7E, 0xC8, 0x8A));
        console.setCaretColor(ACCENT);
        console.setBorder(new EmptyBorder(8, 10, 8, 10));

        JScrollPane cs = new JScrollPane(console);
        cs.setBackground(CONSOLE_BG);
        cs.getViewport().setBackground(CONSOLE_BG);
        cs.setBorder(BorderFactory.createMatteBorder(2, 0, 0, 0, BORDER_C));

        JPanel cw = new JPanel(new BorderLayout());
        cw.setBackground(CONSOLE_BG);
        JLabel ch=new JLabel("SYSTEM CONSOLE");
        ch.setFont(new Font("SansSerif", Font.BOLD, 10));
        ch.setForeground(TEXT_DIM);
        ch.setBackground(new Color(0x10, 0x14, 0x1A));
        ch.setOpaque(true);
        ch.setBorder(new EmptyBorder(4, 8, 4, 8));
        cw.add(ch, BorderLayout.NORTH);
        cw.add(cs, BorderLayout.CENTER);

        JSplitPane sp=new JSplitPane(JSplitPane.VERTICAL_SPLIT, tabs, cw);
        sp.setDividerLocation(470);
        sp.setDividerSize(4);
        sp.setBorder(null);
        return sp;
    }

    private JPanel buildDashboardTab() {
        JPanel root= new JPanel(new BorderLayout(16, 16));
        root.setBackground(BG);
        root.setBorder(new EmptyBorder(16, 16, 16, 16));

        JPanel cards= new JPanel(new GridLayout(1, 5, 12, 0));
        cards.setOpaque(false);
        statHospitals=statCard(cards, "HOSPITALS", "0", BLUE);
        statBeds=statCard(cards, "TOTAL BEDS", "0", GREEN);
        statIcu=statCard(cards, "ICU UNITS", "0", new Color(0xA0, 0x60, 0xF0));
        statPatients=statCard(cards, "PATIENTS", "0", TEXT_SEC);
        statDispatched=statCard(cards, "DISPATCHED", "0", ACCENT);

        JPanel sumCard=card("SYSTEM STATUS");
        sumCard.setLayout(new BorderLayout());
        JTextArea sumText=new JTextArea(sys.getDashboardSummary());
        sumText.setEditable(false);
        sumText.setFont(F_MONO);
        sumText.setBackground(PANEL);
        sumText.setForeground(TEXT_PRI);
        sumText.setBorder(new EmptyBorder(4, 4, 4, 4));
        sumCard.add(sumText, BorderLayout.CENTER);

        JButton refreshBtn=secBtn("Refresh Stats");
        refreshBtn.addActionListener(e -> { refreshStats(); sumText.setText(sys.getDashboardSummary()); });
        JPanel south=new JPanel(new FlowLayout(FlowLayout.LEFT));
        south.setOpaque(false);
        south.add(refreshBtn);

        root.add(cards, BorderLayout.NORTH);
        root.add(sumCard, BorderLayout.CENTER);
        root.add(south, BorderLayout.SOUTH);
        return root;
    }

    private JPanel buildHospitalTab() {
        JPanel root=new JPanel(new BorderLayout(12, 12));
        root.setBackground(BG);
        root.setBorder(new EmptyBorder(14, 14, 14, 14));

        String[] cols={"ID", "Name", "Specializations", "Beds", "ICU", "Node"};
        hospitalModel=new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table=styledTable(hospitalModel);
        JScrollPane tsp = styledScroll(table);

        JPanel formCard= card("ADD HOSPITAL");
        formCard.setLayout(new GridBagLayout());
        GridBagConstraints gc = baseGbc();

        JTextField nameF = field(), specsF = field(), bedsF = field(), icuF = field(), nodeF = field();
        specsF.setToolTipText("e.g. Cardiology, Trauma");
        JLabel errLbl = errLabel();

        int r = 0;
        row(formCard, gc, r++, "Hospital Name", nameF);
        row(formCard, gc, r++, "Specializations (comma-sep)", specsF);
        row(formCard, gc, r++, "Available Beds", bedsF);
        row(formCard, gc, r++, "ICU Units", icuF);
        row(formCard, gc, r++, "Location Node (0-9)", nodeF);
        GridBagConstraints egc = clone(gc, 0, r++); egc.gridwidth = 2;
        formCard.add(errLbl, egc);

        JButton addBtn = accentBtn("+ Add Hospital");
        addBtn.addActionListener(e -> {
            errLbl.setText(" ");
            try {
                String name = nameF.getText().trim();
                if (name.isEmpty()) { errLbl.setText("Name is required."); return; }
                Hospital h = new Hospital();
                h.setName(name);
                for (String s : specsF.getText().split(","))
                    if (!s.trim().isEmpty()) h.addSpecialization(s.trim());
                if (h.getSpecializations().isEmpty()) { errLbl.setText("At least one specialization required."); return; }
                h.setBeds(Integer.parseInt(bedsF.getText().trim()));
                h.setIcu(Integer.parseInt(icuF.getText().trim()));
                int node = Integer.parseInt(nodeF.getText().trim());
                if (node < 0 || node >= sys.getGraph().getNumNodes()) {
                    errLbl.setText("Node must be 0-" + (sys.getGraph().getNumNodes()-1) + "."); return;
                }
                h.setLocNode(node);
                log(sys.addHospital(h));
                clearF(nameF, specsF, bedsF, icuF, nodeF);
                refreshHospitalTable();
                refreshStats();
                status("Hospital added.", GREEN);
            } catch (NumberFormatException ex) { errLbl.setText("Beds, ICU, Node must be whole numbers."); }
        });
        GridBagConstraints bgc = clone(gc, 1, r++); bgc.anchor = GridBagConstraints.WEST;
        formCard.add(addBtn, bgc);

        JPanel rightCard = card("MANAGE");
        rightCard.setLayout(new GridBagLayout());
        GridBagConstraints rc = baseGbc();

        JTextField delIdF = field();
        JLabel delErr = errLabel();
        row(rightCard, rc, 0, "Delete Hospital ID", delIdF);
        GridBagConstraints dec = clone(rc, 0, 1); dec.gridwidth = 2;
        rightCard.add(delErr, dec);

        JButton delBtn = dangerBtn("Delete");
        delBtn.addActionListener(e -> {
            try {
                log(sys.deleteHospital(Integer.parseInt(delIdF.getText().trim())));
                delIdF.setText(""); delErr.setText(" ");
                refreshHospitalTable(); refreshStats();
                status("Hospital deleted.", RED);
            } catch (NumberFormatException ex) { delErr.setText("Enter a valid ID."); }
        });
        GridBagConstraints dbc = clone(rc, 1, 2); dbc.anchor = GridBagConstraints.WEST;
        rightCard.add(delBtn, dbc);

        JButton viewBtn = secBtn("Refresh Table");
        viewBtn.addActionListener(e -> refreshHospitalTable());
        GridBagConstraints vgc = clone(rc, 0, 3); vgc.gridwidth = 2;
        rightCard.add(viewBtn, vgc);

        JPanel right = new JPanel(new BorderLayout(0, 12));
        right.setOpaque(false);
        right.add(rightCard, BorderLayout.NORTH);

        JPanel left = new JPanel(new BorderLayout(0, 12));
        left.setOpaque(false);
        left.add(tsp, BorderLayout.CENTER);
        left.add(formCard, BorderLayout.SOUTH);

        root.add(left, BorderLayout.CENTER);
        root.add(right, BorderLayout.EAST);
        refreshHospitalTable();
        return root;
    }

    private JPanel buildDispatchTab() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBackground(BG);
        root.setBorder(new EmptyBorder(14, 14, 14, 14));

        String[] cols = {"ID", "Name", "Priority", "Specialization", "Node", "Hospital"};
        patientModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table= styledTable(patientModel);
        JScrollPane tsp=styledScroll(table);

        JPanel formCard = card("NEW PATIENT — AUTO DISPATCH");
        formCard.setLayout(new GridBagLayout());
        GridBagConstraints gc = baseGbc();

        JTextField nameF = field();
        JComboBox<String> prioBox = new JComboBox<>(new String[]{
            "1 - Routine", "2 - Low", "3 - Moderate", "4 - Urgent", "5 - Critical"});
        styleCombo(prioBox);
        JTextField specF = field(), nodeF = field();
        JLabel errLbl = errLabel();

        int r = 0;
        row(formCard, gc, r++, "Patient Name", nameF);
        rowC(formCard, gc, r++, "Priority", prioBox);
        row(formCard, gc, r++, "Required Specialization", specF);
        row(formCard, gc, r++, "Location Node (0-9)", nodeF);
        GridBagConstraints egc = clone(gc, 0, r++); egc.gridwidth = 2;
        formCard.add(errLbl, egc);

        JButton dispBtn = accentBtn("Dispatch Ambulance");
        dispBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        dispBtn.addActionListener(e -> {
            errLbl.setText(" ");
            try {
                String name = nameF.getText().trim();
                if (name.isEmpty()) { errLbl.setText("Name is required."); return; }
                String spec = specF.getText().trim();
                if (spec.isEmpty()) { errLbl.setText("Specialization is required."); return; }
                int node = Integer.parseInt(nodeF.getText().trim());
                if (node < 0 || node >= sys.getGraph().getNumNodes()) {
                    errLbl.setText("Node must be 0-" + (sys.getGraph().getNumNodes()-1) + "."); return;
                }
                int prio=prioBox.getSelectedIndex() + 1;
                Patient p = new Patient(0, name, prio, spec, node);
                log(sys.addPatientAndDispatch(p));
                clearF(nameF, specF, nodeF);
                refreshPatientTable();
                refreshStats();
                graphPanel.highlightRoute(sys.getLastRoute(), node);
                status("Ambulance dispatched.", ACCENT);
            } catch (NumberFormatException ex) { errLbl.setText("Node must be a whole number."); }
        });
        GridBagConstraints bgc = clone(gc, 0, r++); bgc.gridwidth = 2;
        formCard.add(dispBtn, bgc);

        JPanel sideCard = card("EXISTING PATIENTS");
        sideCard.setLayout(new GridBagLayout());
        GridBagConstraints sc = baseGbc();

        JTextField pidF = field();
        JLabel serr = errLabel();
        row(sideCard, sc, 0, "Patient ID", pidF);
        GridBagConstraints sec = clone(sc, 0, 1); sec.gridwidth=2;
        sideCard.add(serr, sec);

        JButton reBtn=secBtn("Re-dispatch");
        JButton discBtn=dangerBtn("Discharge");
        reBtn.addActionListener(e -> {
            try {
                log(sys.dispatchForExisting(Integer.parseInt(pidF.getText().trim())));
                graphPanel.highlightRoute(sys.getLastRoute(), sys.getLastDispatchNode());
                refreshPatientTable(); refreshStats(); pidF.setText("");
            } catch (NumberFormatException ex) { serr.setText("Enter a valid ID."); }
        });
        discBtn.addActionListener(e -> {
            try {
                log(sys.dischargePatient(Integer.parseInt(pidF.getText().trim())));
                refreshPatientTable(); refreshStats(); pidF.setText("");
            } catch (NumberFormatException ex) { serr.setText("Enter a valid ID."); }
        });

        JPanel sbtns=new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        sbtns.setOpaque(false);
        sbtns.add(reBtn); sbtns.add(discBtn);
        GridBagConstraints sbc=clone(sc, 0, 2); sbc.gridwidth=2;
        sideCard.add(sbtns, sbc);

        JPanel right=new JPanel(new BorderLayout());
        right.setOpaque(false);
        right.add(sideCard, BorderLayout.NORTH);

        JPanel left = new JPanel(new BorderLayout(0, 12));
        left.setOpaque(false);
        left.add(tsp, BorderLayout.CENTER);
        left.add(formCard, BorderLayout.SOUTH);

        root.add(left, BorderLayout.CENTER);
        root.add(right, BorderLayout.EAST);
        refreshPatientTable();
        return root;
    }

    private JPanel buildTrafficTab() {
        JPanel root = new JPanel(new BorderLayout(12, 0));
        root.setBackground(BG);
        root.setBorder(new EmptyBorder(14, 14, 14, 14));

        JPanel formCard = card("UPDATE TRAFFIC CONDITIONS");
        formCard.setLayout(new GridBagLayout());
        GridBagConstraints gc = baseGbc();

        JTextField edgeF = field();
        JSlider slider = new JSlider(0, 10, 0);
        styleSlider(slider);
        JLabel valLbl = new JLabel("0");
        valLbl.setFont(new Font("SansSerif", Font.BOLD, 15));
        valLbl.setForeground(ACCENT);
        slider.addChangeListener(e -> valLbl.setText(String.valueOf(slider.getValue())));
        JLabel errLbl= errLabel();

        int r = 0;
        row(formCard, gc, r++, "Edge ID", edgeF);
        rowC(formCard, gc, r++, "Traffic Level  (0=clear · 10=blocked)", inlinePanel(slider, valLbl));
        GridBagConstraints egc = clone(gc, 0, r++); egc.gridwidth = 2;
        formCard.add(errLbl, egc);

        JButton updateBtn = accentBtn("Update Edge");
        updateBtn.addActionListener(e -> {
            errLbl.setText(" ");
            try {
                log(sys.updateTraffic(Integer.parseInt(edgeF.getText().trim()), slider.getValue()));
                graphPanel.refresh(); edgeF.setText(""); slider.setValue(0);
                status("Traffic updated.", BLUE);
            } catch (NumberFormatException ex) { errLbl.setText("Enter a valid edge ID."); }
        });
        GridBagConstraints bgc = clone(gc, 1, r++); bgc.anchor=GridBagConstraints.WEST;
        formCard.add(updateBtn, bgc);

        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER_C); sep.setBackground(BORDER_C);
        GridBagConstraints sgc = clone(gc, 0, r++); sgc.gridwidth = 2;
        sgc.fill = GridBagConstraints.HORIZONTAL; sgc.insets = new Insets(12, 0, 12, 0);
        formCard.add(sep, sgc);

        JButton simBtn = secBtn("Simulate Random Traffic Update");
        simBtn.addActionListener(e -> { log(sys.simulateTraffic()); graphPanel.refresh(); status("Simulation complete.", TEXT_SEC); });
        GridBagConstraints smgc = clone(gc, 0, r); smgc.gridwidth = 2;
        formCard.add(simBtn, smgc);

        JPanel legendCard = card("TRAFFIC LEGEND");
        legendCard.setLayout(new BoxLayout(legendCard, BoxLayout.Y_AXIS));
        Object[][] levs = {
            {"0-3", "Clear",    new Color(0x2E, 0xCC, 0x71)},
            {"4-5", "Mild",     new Color(0xA8, 0xD4, 0x20)},
            {"6-7", "Moderate", new Color(0xF0, 0xB8, 0x15)},
            {"8-9", "Severe",   new Color(0xE0, 0x6C, 0x1A)},
            {"10",  "Blocked",  new Color(0xE7, 0x4C, 0x3C)},
        };
        for (Object[] lv : levs) {
            JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3));
            row2.setOpaque(false);
            JPanel swatch = new JPanel() {
                protected void paintComponent(Graphics g) {
                    ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setColor((Color) lv[2]);
                    g.fillRoundRect(0, 3, 34, 9, 6, 6);
                }
            };
            swatch.setPreferredSize(new Dimension(34, 15)); swatch.setOpaque(false);
            JLabel lbl=new JLabel(lv[0] + "  " + lv[1]);
            lbl.setFont(F_SMALL); lbl.setForeground(TEXT_PRI);
            row2.add(swatch); row2.add(lbl);
            legendCard.add(row2);
        }

        JPanel right= new JPanel(new BorderLayout());
        right.setOpaque(false);
        right.add(legendCard, BorderLayout.NORTH);
        root.add(formCard, BorderLayout.CENTER);
        root.add(right, BorderLayout.EAST);
        return root;
    }

    private JPanel buildGraphSetupTab() {
        JPanel root=new JPanel(new BorderLayout(16, 0));
        root.setBackground(BG);
        root.setBorder(new EmptyBorder(14, 14, 14, 14));

        JPanel resetCard=card("DEFINE NEW GRAPH");
        resetCard.setLayout(new GridBagLayout());
        GridBagConstraints gc=baseGbc();

        JTextField nodeCountF=field();
        nodeCountF.setToolTipText("Enter number of nodes (2-50)");
        JLabel resetErr=errLabel();

        row(resetCard, gc, 0, "Number of Nodes (2-" + Graph.MAX_NODES + ")", nodeCountF);
        GridBagConstraints ec=clone(gc, 0, 1); ec.gridwidth=2;
        resetCard.add(resetErr, ec);

        JButton resetBtn=dangerBtn("Reset Graph");
        resetBtn.addActionListener(e -> {
            resetErr.setText(" ");
            try {
                int n=Integer.parseInt(nodeCountF.getText().trim());
                String result=sys.resetGraph(n);
                log(result);
                nodeCountF.setText("");
                graphPanel.refresh();
                refreshStats();
                status("Graph reset.", RED);
            } catch (NumberFormatException ex) { resetErr.setText("Enter a whole number."); }
        });
        GridBagConstraints rb=clone(gc, 1, 2); rb.anchor=GridBagConstraints.WEST;
        resetCard.add(resetBtn, rb);

        JButton demoBtn=secBtn("Load Demo Graph (10 nodes)");
        demoBtn.addActionListener(e -> {
            log(sys.loadDemoGraph());
            graphPanel.refresh();
            refreshStats();
            status("Demo graph loaded.", GREEN);
        });
        GridBagConstraints db=clone(gc, 0, 3); db.gridwidth= 2;
        resetCard.add(demoBtn, db);

        JPanel edgeCard = card("");
        edgeCard.setLayout(new GridBagLayout());
        GridBagConstraints eg= baseGbc();

        JTextField fromF = field(), toF = field(), weightF = field();
        JLabel edgeErr = errLabel();

        int r = 0;
        row(edgeCard, eg, r++, "From Node", fromF);
        row(edgeCard, eg, r++, "To Node", toF);
        row(edgeCard, eg, r++, "Base Weight (seconds)", weightF);
        GridBagConstraints eec = clone(eg, 0, r++); eec.gridwidth = 2;
        edgeCard.add(edgeErr, eec);

        JButton addEdgeBtn = accentBtn("+ Add Edge");
        addEdgeBtn.addActionListener(e -> {
            edgeErr.setText(" ");
            try {
                int from = Integer.parseInt(fromF.getText().trim());
                int to = Integer.parseInt(toF.getText().trim());
                double weight = Double.parseDouble(weightF.getText().trim());
                String result = sys.addGraphEdge(from, to, weight);
                log(result);
                if (result.startsWith("✓")) {
                    clearF(fromF, toF, weightF);
                    graphPanel.refresh();
                    status("Edge added.", GREEN);
                } else {
                    edgeErr.setText(result.replace("✗ ", ""));
                }
            } catch (NumberFormatException ex) { edgeErr.setText("From, To must be integers. Weight must be a number."); }
        });
        GridBagConstraints ab = clone(eg, 1, r++); ab.anchor = GridBagConstraints.WEST;
        edgeCard.add(addEdgeBtn, ab);

        JPanel summaryCard = card("CURRENT GRAPH");
        summaryCard.setLayout(new BorderLayout());
        JTextArea summaryArea = new JTextArea(sys.getRoadGraph());
        summaryArea.setEditable(false);
        summaryArea.setFont(F_MONO);
        summaryArea.setBackground(PANEL);
        summaryArea.setForeground(TEXT_PRI);
        summaryArea.setBorder(new EmptyBorder(4, 4, 4, 4));
        JScrollPane ssp = new JScrollPane(summaryArea);
        ssp.setBorder(null);
        ssp.setPreferredSize(new Dimension(0, 140));
        summaryCard.add(ssp, BorderLayout.CENTER);

        JButton refreshSummary = secBtn("Refresh");
        refreshSummary.addActionListener(e -> summaryArea.setText(sys.getRoadGraph()));
        JPanel sbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        sbar.setOpaque(false);
        sbar.add(refreshSummary);
        summaryCard.add(sbar, BorderLayout.SOUTH);

        JPanel left = new JPanel(new BorderLayout(0, 12));
        left.setOpaque(false);
        left.add(resetCard, BorderLayout.NORTH);
        left.add(summaryCard, BorderLayout.CENTER);
        root.add(left, BorderLayout.CENTER);
        root.add(edgeCard, BorderLayout.EAST);
        return root;
    }

    private JPanel buildGraphViewTab() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);
        root.setBorder(new EmptyBorder(12, 14, 12, 14));

        JScrollPane sp= new JScrollPane(graphPanel);
        sp.setBorder(BorderFactory.createLineBorder(BORDER_C, 1));
        sp.getViewport().setBackground(new Color(0x10, 0x16, 0x20));

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        bar.setBackground(PANEL_LITE);
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_C));

        JButton ref = secBtn("Refresh");
        ref.addActionListener(e -> graphPanel.refresh());
        JButton clear = secBtn("Clear Route Highlight");
        clear.addActionListener(e -> graphPanel.clearHighlight());
        JLabel hint = new JLabel("  Amber = last dispatched route  ·  Blue= ambulance start  ·  Dashed = blocked");
        hint.setFont(F_SMALL); hint.setForeground(TEXT_DIM);

        bar.add(ref); bar.add(clear); bar.add(hint);
        root.add(bar, BorderLayout.NORTH);
        root.add(sp, BorderLayout.CENTER);
        return root;
    }

    private JPanel buildReportTab() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);
        root.setBorder(new EmptyBorder(14, 14, 14, 14));

        JPanel card = card("REPORTS");
        card.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 8));

        JButton g1 = secBtn("Road Graph (base)");
        g1.addActionListener(e -> log(sys.getRoadGraph()));
        JButton g2 = secBtn("Priority Report");
        g2.addActionListener(e -> log(sys.getPriorityReport()));
        JButton g3= dangerBtn("Clear Console");
        g3.addActionListener(e -> console.setText(""));

        card.add(g1); card.add(g2); card.add(g3);
        root.add(card, BorderLayout.NORTH);
        return root;
    }

    private JPanel buildStatusBar() {
        JPanel bar= new JPanel(new BorderLayout());
        bar.setBackground(new Color(0x0F, 0x13, 0x19));
        bar.setBorder(new EmptyBorder(4, 16, 4, 16));
        statusLabel= new JLabel("Ready");
        statusLabel.setFont(F_SMALL);
        statusLabel.setForeground(TEXT_DIM);
        JLabel ver= new JLabel("AmbulanceOS v2.0  ·  BTech CSE AI&DS  ·  Graphic Era University");
        ver.setFont(new Font("SansSerif", Font.PLAIN, 10));
        ver.setForeground(TEXT_DIM);
        bar.add(statusLabel, BorderLayout.WEST);
        bar.add(ver, BorderLayout.EAST);
        return bar;
    }

    private void refreshStats() {
        int beds= sys.getHospitals().stream().mapToInt(Hospital::getBeds).sum();
        int icu= sys.getHospitals().stream().mapToInt(Hospital::getIcu).sum();
        int disp= (int) sys.getPatients().stream().filter(p -> p.getHospitalId() >= 0).count();
        statHospitals.setText(String.valueOf(sys.getHospitals().size()));
        statBeds.setText(String.valueOf(beds));
        statIcu.setText(String.valueOf(icu));
        statPatients.setText(String.valueOf(sys.getPatients().size()));
        statDispatched.setText(String.valueOf(disp));
    }

    private void refreshHospitalTable() {
        hospitalModel.setRowCount(0);
        for (Hospital h : sys.getHospitals())
            hospitalModel.addRow(new Object[]{
                h.getId(), h.getName(), String.join(", ", h.getSpecializations()),
                h.getBeds(), h.getIcu(), h.getLocNode()
            });
    }

    private void refreshPatientTable() {
        patientModel.setRowCount(0);
        for (Patient p : sys.getPatients())
            patientModel.addRow(new Object[]{
                p.getId(), p.getName(), p.priorityLabel(),
                p.getSpecialization(), p.getLocNode(),
                p.getHospitalId() < 0 ? "-" : String.valueOf(p.getHospitalId())
            });
    }

    // card() returns a panel with title label at top and opaque=false inner panel
    private JPanel card(String title) {
        JPanel outer= new JPanel(new BorderLayout()) {
            protected void paintComponent(Graphics g0) {
                Graphics2D g= (Graphics2D) g0;
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(PANEL);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g.setColor(ACCENT);
                g.fillRect(0, 0, 3, getHeight());
            }
        };
        outer.setOpaque(false);
        outer.setBorder(new CompoundBorder(new LineBorder(BORDER_C, 1, true), new EmptyBorder(12, 14, 14, 14)));
        JLabel lbl= new JLabel(title);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 10));
        lbl.setForeground(ACCENT);
        lbl.setBorder(new EmptyBorder(0, 0, 8, 0));
        JPanel inner= new JPanel();
        inner.setOpaque(false);
        outer.add(lbl, BorderLayout.NORTH);
        outer.add(inner, BorderLayout.CENTER);
        return outer;
    }

    // stat card with colored bottom stripe; returns the value label
    private JLabel statCard(JPanel parent, String title, String initVal, Color color) {
        JPanel card= new JPanel(new BorderLayout()) {
            protected void paintComponent(Graphics g0) {
                Graphics2D g= (Graphics2D) g0;
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(PANEL);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g.setColor(color);
                g.fillRect(0, getHeight() - 3, getWidth(), 3);
            }
        };
        card.setOpaque(false);
        card.setBorder(new CompoundBorder(new LineBorder(BORDER_C, 1, true), new EmptyBorder(14, 14, 14, 14)));
        JLabel val= new JLabel(initVal);
        val.setFont(new Font("SansSerif", Font.BOLD, 28));
        val.setForeground(color);
        JLabel lbl= new JLabel(title);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 9));
        lbl.setForeground(TEXT_DIM);
        card.add(val, BorderLayout.CENTER);
        card.add(lbl, BorderLayout.SOUTH);
        parent.add(card);
        return val;
    }
    private JTextField field() {
        JTextField f = new JTextField(18);
        f.setBackground(new Color(0x0E, 0x13, 0x1C));
        f.setForeground(TEXT_PRI);
        f.setCaretColor(ACCENT);
        f.setFont(F_LABEL);
        Border norm = new CompoundBorder(new LineBorder(BORDER_C, 1, true), new EmptyBorder(5, 8, 5, 8));
        Border focus = new CompoundBorder(new LineBorder(ACCENT_DIM, 1, true), new EmptyBorder(5, 8, 5, 8));
        f.setBorder(norm);
        f.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { f.setBorder(focus); }
            public void focusLost(FocusEvent e) { f.setBorder(norm); }
        });
        return f;
    }
    private void styleCombo(JComboBox<?> b) {
        b.setBackground(new Color(0x0E, 0x13, 0x1C));
        b.setForeground(TEXT_PRI);
        b.setFont(F_LABEL);
    }
    private void styleSlider(JSlider s) {
        s.setBackground(PANEL);
        s.setForeground(ACCENT);
        s.setPaintTicks(true); s.setPaintLabels(true);
        s.setMajorTickSpacing(2); s.setMinorTickSpacing(1);
        s.setPreferredSize(new Dimension(210, 45));
        s.setFont(F_SMALL);
    }

    private JLabel errLabel() {
        JLabel l = new JLabel(" ");
        l.setFont(F_ERR);
        l.setForeground(RED);
        return l;
    }

    private JTable styledTable(DefaultTableModel model) {
        JTable t = new JTable(model);
        t.setBackground(PANEL);
        t.setForeground(TEXT_PRI);
        t.setFont(F_LABEL);
        t.setRowHeight(24);
        t.setGridColor(BORDER_C);
        t.setSelectionBackground(PANEL_LITE);
        t.setSelectionForeground(ACCENT);
        t.getTableHeader().setBackground(PANEL_LITE);
        t.getTableHeader().setForeground(TEXT_SEC);
        t.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));
        t.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_C));
        return t;
    }

    private JScrollPane styledScroll(JComponent c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBorder(BorderFactory.createLineBorder(BORDER_C, 1));
        sp.setBackground(PANEL);
        sp.getViewport().setBackground(PANEL);
        return sp;
    }
    private JButton accentBtn(String t) { return btn(t, ACCENT, BG, ACCENT.brighter()); }
    private JButton secBtn(String t) { return btn(t, PANEL_LITE, TEXT_PRI, BORDER_C.brighter()); }
    private JButton dangerBtn(String t) { return btn(t, new Color(0x3A, 0x10, 0x10), new Color(0xFF, 0x80, 0x80), RED); }

    private JButton btn(String text, Color bg, Color fg, Color hover) {
        JButton b = new JButton(text) {
            boolean ov = false;
            {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { ov = true; repaint(); }
                    public void mouseExited(MouseEvent e) { ov = false; repaint(); }
                });
            }
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0;
                g.setColor(ov ? hover : bg);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g.setColor(fg);
                g.setFont(getFont());
                FontMetrics fm = g.getFontMetrics();
                g.drawString(getText(),
                    (getWidth() - fm.stringWidth(getText())) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            }
            protected void paintBorder(Graphics g) {
                ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(ov ? hover.brighter() : BORDER_C);
                ((Graphics2D)g).draw(new RoundRectangle2D.Float(0, 0, getWidth()-1, getHeight()-1, 6, 6));
            }
        };
        b.setFont(F_BOLD);
        b.setForeground(fg);
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(7, 16, 7, 16));
        return b;
    }

    private JLabel pill(String t, Color c) {
        JLabel l = new JLabel(t) {
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0;
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(c.darker().darker());
                g.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                super.paintComponent(g0);
            }
        };
        l.setFont(new Font("SansSerif", Font.BOLD, 10));
        l.setForeground(c);
        l.setBorder(new EmptyBorder(3, 10, 3, 10));
        l.setOpaque(false);
        return l;
    }
    private GridBagConstraints baseGbc() {
        GridBagConstraints g= new GridBagConstraints();
        g.insets = new Insets(4, 6, 4, 6);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;
        return g;
    }
    private GridBagConstraints clone(GridBagConstraints proto, int x, int y) {
        GridBagConstraints g = (GridBagConstraints) proto.clone();
        g.gridx = x; g.gridy = y; g.gridwidth = 1;
        return g;
    }
    private void row(JPanel p, GridBagConstraints gc, int r, String lbl, JTextField f) {
        p.add(lbl(lbl), clone(gc, 0, r));
        p.add(f, clone(gc, 1, r));
    }
    private void rowC(JPanel p, GridBagConstraints gc, int r, String lbl, JComponent c) {
        p.add(lbl(lbl), clone(gc, 0, r));
        p.add(c, clone(gc, 1, r));
    }
    private JLabel lbl(String t) {
        JLabel l = new JLabel(t);
        l.setFont(F_LABEL);
        l.setForeground(TEXT_SEC);
        return l;
    }

    private JPanel inlinePanel(JComponent... cs) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        p.setOpaque(false);
        for (JComponent c : cs) p.add(c);
        return p;
    }
    private void log(String msg) {
        console.append("\n" + msg + "\n");
        console.setCaretPosition(console.getDocument().getLength());
    }
    private void status(String msg, Color c) {
        statusLabel.setText(msg);
        statusLabel.setForeground(c);
    }
    private void clearF(JTextField... fs) { for (JTextField f : fs) f.setText(""); }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainGUI::new);
    }
}


