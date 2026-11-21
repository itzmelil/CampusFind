package campusfind;

import campusfind.CampusFind.Report;
import campusfind.CampusFind.ReportStore;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CampusFindDesktop extends JPanel {
    private static final Color INK = new Color(36, 10, 16);
    private static final Color MUTED = new Color(117, 103, 106);
    private static final Color RED = new Color(143, 7, 22);
    private static final Color PAPER = new Color(248, 247, 246);
    private static final Color BORDER = new Color(234, 223, 225);
    private static final Color BLUSH = new Color(253, 239, 237);
    private static final Color GREEN = new Color(32, 118, 87);
    private static final String DESKTOP_OWNER = "desktop-demo";
    private static final String LOGO_PATH = "assets/campusfind-logo.png";
    private final ReportStore store;
    private final BufferedImage brandLogo;
    private final Map<String, BufferedImage> images = new HashMap<>();
    private List<Report> saved;
    private boolean student;
    private Page page = Page.HOME;
    private String query = "";
    private String status = "all";
    private String category = "All items";
    private String reportType = "lost";
    private Report draft;
    private Report selected;

    private enum Page { HOME, BOARD, REPORT, PREVIEW, MINE, ALERTS, SAFETY, SIGN_IN, DETAILS }

    CampusFindDesktop(Path directory) throws IOException {
        store = new ReportStore(directory);
        saved = store.all();
        brandLogo = Files.isRegularFile(Path.of(LOGO_PATH)) ? image(LOGO_PATH) : null;
        setLayout(new BorderLayout());
        setBackground(PAPER);
        render();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                JFrame window = new JFrame("CampusFind | University of Mindanao Matina");
                window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                CampusFindDesktop app = new CampusFindDesktop(args.length == 0 ? Path.of("data") : Path.of(args[0]));
                window.setContentPane(app);
                if (app.brandLogo != null) window.setIconImage(app.brandLogo);
                window.setMinimumSize(new Dimension(1100, 680));
                window.setSize(1360, 940);
                window.setLocationRelativeTo(null);
                window.setVisible(true);
            } catch (Exception failure) {
                JOptionPane.showMessageDialog(null, failure.getMessage(), "CampusFind", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void go(Page destination) {
        page = destination;
        render();
    }

    private void render() {
        removeAll();
        add(sidebar(), BorderLayout.WEST);
        add(mainPanel(), BorderLayout.CENTER);
        add(activity(), BorderLayout.EAST);
        revalidate();
        repaint();
    }

    private JPanel sidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(new Color(250, 248, 248));
        sidebar.setPreferredSize(new Dimension(194, 0));
        sidebar.setBorder(new EmptyBorder(22, 12, 16, 12));
        JPanel top = column();
        JPanel brand = row();
        JButton logo = button("", brandLogo == null ? 0 : -1, brandLogo == null, () -> go(Page.HOME));
        if (brandLogo != null) {
            logo.setIcon(new Icon() {
                @Override public int getIconWidth() { return 36; }
                @Override public int getIconHeight() { return 36; }

                @Override public void paintIcon(Component component, Graphics graphics, int x, int y) {
                    Graphics2D g = (Graphics2D) graphics.create();
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    double scale = 36.0 / Math.max(brandLogo.getWidth(), brandLogo.getHeight());
                    int width = (int) Math.round(brandLogo.getWidth() * scale);
                    int height = (int) Math.round(brandLogo.getHeight() * scale);
                    g.drawImage(brandLogo, x + (36 - width) / 2, y + (36 - height) / 2, width, height, null);
                    g.dispose();
                }
            });
            logo.setBorder(new EmptyBorder(2, 2, 2, 2));
        }
        logo.getAccessibleContext().setAccessibleName("CampusFind home");
        brand.add(logo);
        JPanel names = column();
        names.add(label("CampusFind", 17, INK, true));
        names.add(label(student ? "STUDENT ACCESS" : "UM MATINA", 10, MUTED, true));
        brand.add(names);
        top.add(brand);
        top.add(Box.createVerticalStrut(34));
        top.add(nav(student ? "Student home" : "Overview", 1, Page.HOME));
        top.add(nav(student ? "Campus board" : "Browse items", 2, Page.BOARD));
        top.add(nav(student ? "My reports" : "Report item", 3, student ? Page.MINE : Page.REPORT));
        top.add(nav(student ? "Alerts" : "My alerts", 4, Page.ALERTS));
        top.add(nav(student ? "Safety" : "Safety & privacy", 5, Page.SAFETY));
        sidebar.add(top, BorderLayout.NORTH);
        JPanel bottom = column();
        JPanel handoff = card();
        handoff.add(label("Safe handoffs", 13, INK, true));
        handoff.add(copy("Confirm a staffed handoff point before returning an item.", 132, MUTED));
        handoff.add(button("View locations", 7, false, () -> go(Page.SAFETY)));
        bottom.add(handoff);
        bottom.add(Box.createVerticalStrut(8));
        bottom.add(button(student ? "Log out" : "Student sign in", 8, false, () -> {
            if (student) {
                student = false;
                draft = null;
                go(Page.HOME);
            } else go(Page.SIGN_IN);
        }));
        sidebar.add(bottom, BorderLayout.SOUTH);
        return sidebar;
    }

    private JButton nav(String title, int icon, Page destination) {
        boolean active = page == destination || (page == Page.DETAILS && destination == Page.BOARD)
                || (page == Page.REPORT || page == Page.PREVIEW)
                && destination == (student ? Page.MINE : Page.REPORT);
        JButton result = button(title, icon, false, () -> {
            if (destination == Page.REPORT) start("lost");
            else go(destination);
        });
        result.setHorizontalAlignment(SwingConstants.LEFT);
        result.setBackground(active ? Color.WHITE : new Color(250, 248, 248));
        result.setForeground(active ? RED : MUTED);
        result.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        return result;
    }

    private JPanel mainPanel() {
        JPanel frame = new JPanel(new BorderLayout(0, 16));
        frame.setBackground(PAPER);
        frame.setBorder(new EmptyBorder(22, 20, 18, 20));
        JPanel toolbar = new JPanel(new BorderLayout(10, 0));
        toolbar.setOpaque(false);
        String searchHint = "Search item, category or campus location";
        JTextField search = field(query, searchHint);
        search.setToolTipText(searchHint);
        search.getAccessibleContext().setAccessibleName("Search campus reports");
        search.addActionListener(event -> search(search.getText()));
        JPanel searchBox = new JPanel(new BorderLayout(6, 0));
        searchBox.setBackground(Color.WHITE);
        searchBox.add(button("Search", 9, false, () -> search(search.getText())), BorderLayout.WEST);
        searchBox.add(search);
        toolbar.add(searchBox);
        toolbar.add(button("Report item", 10, true, () -> start("lost")), BorderLayout.EAST);
        frame.add(toolbar, BorderLayout.NORTH);
        JPanel content = column();
        switch (page) {
            case HOME -> {
                content.add(hero());
                content.add(Box.createVerticalStrut(18));
                if (student) studentTools(content);
                else board(content, true);
            }
            case BOARD -> board(content, false);
            case REPORT -> report(content);
            case PREVIEW -> {
                intro(content, "REPORT PREVIEW", "Preview " + draft.type() + " report",
                        "Review how the item will appear on the campus board.");
                summary(content, draft, actions(button("Back to report", -1, false, () -> go(Page.REPORT)),
                        button("Submit report", -1, true, this::submit)));
            }
            case DETAILS -> {
                intro(content, "CAMPUS BOARD", "Item details",
                        "Review the report and find a safe handoff location.");
                summary(content, selected, actions(button("View handoff locations", -1, true, () -> go(Page.SAFETY)),
                        button("Back to campus board", -1, false, () -> go(Page.BOARD))));
            }
            case MINE -> {
                intro(content, "STUDENT WORKSPACE", "My reports & claims", "Track your reports and staffed handoffs.");
                if (own().isEmpty()) {
                    message(content, "No reports or claims yet", "Your reports appear here after you submit them.",
                            actions(button("New report", -1, true, () -> start("lost")),
                                    button("Browse campus board", -1, false, () -> go(Page.BOARD))));
                } else {
                    content.add(itemGrid(own()));
                    content.add(actions(button("New report", -1, true, () -> start("lost"))));
                }
            }
            case ALERTS -> {
                intro(content, "STUDENT UPDATES", student ? "Alerts" : "My alerts",
                        "Check updates about reports, claims, and staffed handoffs.");
                message(content, "You’re all caught up", "Updates from campus staff will appear here.",
                        actions(button("View my reports", -1, true, () -> go(student ? Page.MINE : Page.SIGN_IN)),
                                button("Browse items", -1, false, () -> go(Page.BOARD))));
            }
            case SAFETY -> safety(content);
            case SIGN_IN -> {
                intro(content, "UNIVERSITY OF MINDANAO · MATINA", "Student sign in",
                        "Open the Student workspace to manage reports and alerts.");
                message(content, "Student workspace", "Explore CampusFind using the demo student profile.",
                        actions(button("Open Student demo", -1, true, () -> {
                            student = true;
                            go(Page.HOME);
                        }), button("Back to overview", -1, false, () -> go(Page.HOME))));
            }
        }
        JScrollPane body = scroll(content);
        if (page == Page.BOARD || (page == Page.HOME && !student)) {
            body.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        }
        frame.add(body);
        return frame;
    }

    private void search(String value) {
        query = value;
        go(Page.BOARD);
    }

    private JPanel hero() {
        JPanel hero = new Surface(null, 20, true);
        hero.setLayout(new BorderLayout(4, 0));
        hero.setBorder(new EmptyBorder(23, 23, 20, 17));
        JPanel copy = column();
        copy.add(label(student ? "STUDENT WORKSPACE" : "COMMUNITY POWERED", 11, Color.WHITE, true));
        copy.add(Box.createVerticalStrut(11));
        copy.add(label(student ? "Welcome, LYLE" : "Lost something?", 35, Color.WHITE, true));
        copy.add(label(student ? "Find and return items." : "Your campus is looking.", 31, Color.WHITE, true));
        copy.add(Box.createVerticalStrut(10));
        copy.add(copy(student ? "Report, claim, and recover campus items through the Lost & Found Office."
                : "Report and recover lost items at the University of Mindanao, Matina campus in Davao City.",
                315, Color.WHITE));
        copy.add(Box.createVerticalStrut(8));
        copy.add(actions(button("I lost something", -1, false, () -> start("lost")),
                button("I found something", -1, false, () -> start("found"))));
        hero.add(copy, BorderLayout.CENTER);
        JPanel art = new JPanel() {
            @Override protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                drawTiltedPhoto(graphics, "assets/item-1.png", getWidth() - 79, getHeight() - 114, 72, 12);
                drawTiltedPhoto(graphics, "assets/item-2.png", 0, getHeight() - 59, 56, -12);
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(Color.WHITE);
                g.fillOval(getWidth() - 38, getHeight() - 37, 34, 34);
                new UiIcon(11, 16).paintIcon(null, g, getWidth() - 29, getHeight() - 28);
                g.dispose();
            }
        };
        art.setOpaque(false);
        art.setPreferredSize(new Dimension(126, 0));
        hero.add(art, BorderLayout.EAST);
        return hero;
    }

    private void drawTiltedPhoto(Graphics graphics, String file, int x, int y, int size, int angle) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.rotate(Math.toRadians(angle), x + size / 2.0, y + size / 2.0);
        g.setClip(new java.awt.geom.RoundRectangle2D.Double(x, y, size, size, 15, 15));
        g.drawImage(image(file), x, y, size, size, null);
        g.dispose();
    }

    private void studentTools(JPanel content) {
        content.add(label("Student tools", 20, INK, true));
        content.add(Box.createVerticalStrut(8));
        JPanel grid = grid(2);
        grid.add(toolCard("Campus board", "Browse lost and found items on campus.", Page.BOARD));
        grid.add(toolCard("My reports", "Track your submissions and handoffs.", Page.MINE));
        grid.add(toolCard("Alerts", "Check updates about your reports.", Page.ALERTS));
        grid.add(toolCard("Safety", "Find staffed handoff locations and privacy tips.", Page.SAFETY));
        content.add(grid);
    }

    private JPanel toolCard(String title, String description, Page target) {
        JPanel tile = card();
        tile.add(button(title + "  →", -1, false, () -> go(target)));
        tile.add(copy(description, 175, MUTED));
        return tile;
    }

    private void board(JPanel content, boolean overview) {
        if (!overview) intro(content, "CAMPUS BOARD", student ? "Campus board" : "Browse items",
                "Explore lost and found reports by category or status.");
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JPanel titles = column();
        titles.add(label("CAMPUS ACTIVITY", 10, RED, true));
        titles.add(label(overview ? "Recently reported" : "Latest reports", 19, INK, true));
        heading.add(titles);
        JPanel statuses = row();
        for (String filter : List.of("all", "found", "lost")) {
            statuses.add(button(capitalize(filter), -1, status.equals(filter), () -> {
                status = filter;
                render();
            }));
        }
        heading.add(statuses, BorderLayout.EAST);
        content.add(heading);
        JPanel filters = new JPanel(new GridLayout(2, 4, 5, 4));
        filters.setOpaque(false);
        var choices = new ArrayList<>(List.of("All items"));
        choices.addAll(CampusFind.CATEGORIES);
        for (int i = 0; i < choices.size(); i++) {
            String name = choices.get(i);
            filters.add(button(name, 13 + i, category.equals(name), () -> {
                category = name;
                render();
            }));
        }
        content.add(filters);
        if (!query.isBlank()) content.add(actions(label("Results for “" + query + "”", 12, MUTED, false),
                button("Clear search", -1, false, () -> search(""))));
        content.add(Box.createVerticalStrut(10));
        List<Report> visible = CampusFind.filterReports(all(), query, status, category);
        if (visible.isEmpty()) content.add(label("No items match your search.", 14, MUTED, true));
        else content.add(itemGrid(visible));
    }

    private JPanel itemGrid(List<Report> reports) {
        JPanel grid = grid(2);
        for (Report report : reports) grid.add(itemCard(report));
        return grid;
    }

    private JButton itemCard(Report report) {
        JButton card = button("", -1, false, () -> details(report));
        card.setLayout(new BorderLayout(8, 0));
        card.setBorder(new EmptyBorder(8, 8, 8, 8));
        card.getAccessibleContext().setAccessibleName(report.name());
        JLabel photo = new JLabel(report.image().isEmpty() ? "+" : "");
        photo.setIcon(photo(report, 69));
        photo.setPreferredSize(new Dimension(69, 69));
        photo.setHorizontalAlignment(SwingConstants.CENTER);
        photo.setOpaque(true);
        photo.setBackground(BLUSH);
        card.add(photo, BorderLayout.WEST);
        JPanel information = column();
        information.add(label(report.type().toUpperCase() + " · " + report.category(), 10,
                report.type().equals("found") ? GREEN : RED, true));
        information.add(Box.createVerticalStrut(5));
        information.add(copy(report.name(), 190, INK, true));
        information.add(copy(report.location(), 190, MUTED));
        card.add(information);
        Dimension preferred = card.getPreferredSize();
        card.setPreferredSize(new Dimension(preferred.width, Math.max(91, preferred.height)));
        return card;
    }

    private void details(Report report) {
        selected = report;
        go(Page.DETAILS);
    }

    private void start(String type) {
        draft = null;
        reportType = type;
        go(Page.REPORT);
    }

    private void report(JPanel content) {
        boolean found = reportType.equals("found");
        intro(content, "CAMPUS LOST & FOUND", "Report an item",
                "Share enough detail for the Lost & Found Office to identify the item.");
        JPanel form = card();
        form.add(label("What happened?", 15, INK, true));
        JTextField name = field(draft == null ? "" : draft.name());
        JTextField date = field(draft == null ? LocalDate.now().toString() : draft.date());
        JTextField location = field(draft == null ? "" : draft.location());
        JTextArea description = new JTextArea(draft == null ? "" : draft.description(), 3, 18);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        JComboBox<String> categories = new JComboBox<>(CampusFind.CATEGORIES.toArray(String[]::new));
        categories.setSelectedItem(draft == null ? null : draft.category());
        form.add(actions(button("I lost an item", -1, !found, () -> switchType("lost", name, date, location, description, categories)),
                button("I found an item", -1, found, () -> switchType("found", name, date, location, description, categories))));
        fieldRow(form, "Item name", name);
        JPanel pair = grid(2);
        JPanel categoryField = column();
        JPanel dateField = column();
        fieldRow(categoryField, "Category", categories);
        fieldRow(dateField, found ? "Date found (YYYY-MM-DD)" : "Date last seen (YYYY-MM-DD)", date);
        pair.add(categoryField);
        pair.add(dateField);
        form.add(pair);
        fieldRow(form, found ? "Found at" : "Last seen at", location);
        fieldRow(form, "Description", new JScrollPane(description));
        JLabel error = label(" ", 12, RED, true);
        form.add(error);
        form.add(actions(button("Preview report", -1, true, () -> {
            try {
                draft = CampusFind.validateReport(draftFields(name, date, location, description, categories));
                go(Page.PREVIEW);
            } catch (IllegalArgumentException invalid) {
                error.setText(invalid.getMessage());
            }
        }), button("Cancel", -1, false, () -> {
            draft = null;
            go(student ? Page.MINE : Page.HOME);
        })));
        content.add(form);
        content.add(copy("Keep names, student IDs, and other personal information out of your report.", 475, MUTED));
    }

    private void switchType(String type, JTextField name, JTextField date, JTextField location,
                            JTextArea description, JComboBox<String> categories) {
        if (type.equals(reportType)) return;
        var fields = draftFields(name, date, location, description, categories);
        reportType = type;
        draft = new Report("", "", fields.get("name"), fields.get("category"), type,
                fields.get("date"), fields.get("location"), fields.get("description"), "");
        go(Page.REPORT);
    }

    private Map<String, String> draftFields(JTextField name, JTextField date, JTextField location,
                                             JTextArea description, JComboBox<String> categories) {
        Object choice = categories.getSelectedItem();
        return Map.of("type", reportType, "name", name.getText(), "date", date.getText(),
                "location", location.getText(), "description", description.getText(),
                "category", choice == null ? "" : choice.toString());
    }

    private void submit() {
        try {
            var updated = new ArrayList<>(saved);
            updated.addFirst(store.save(draft, DESKTOP_OWNER));
            saved = List.copyOf(updated);
            draft = null;
            query = "";
            status = "all";
            category = "All items";
            go(student ? Page.MINE : Page.BOARD);
        } catch (IOException failure) {
            JOptionPane.showMessageDialog(this, "CampusFind could not save your report. Your draft is still open.",
                    "CampusFind", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void summary(JPanel content, Report report, JPanel buttons) {
        JPanel card = card();
        boolean hasPhoto = !report.image().isEmpty();
        int textWidth = hasPhoto ? 295 : 435;
        JPanel details = column();
        details.add(label(report.type().toUpperCase(java.util.Locale.ROOT) + " · " + report.category(),
                11, report.type().equals("found") ? GREEN : RED, true));
        details.add(Box.createVerticalStrut(5));
        details.add(copy(report.name(), textWidth, INK, font(19, true)));
        details.add(Box.createVerticalStrut(12));
        details.add(label("Campus location", 13, INK, true));
        details.add(copy(report.location(), textWidth, MUTED));
        if (!report.date().isEmpty()) {
            details.add(Box.createVerticalStrut(6));
            details.add(label("Date  " + report.date(), 13, INK, false));
        }
        details.add(Box.createVerticalStrut(8));
        details.add(label("Description", 13, INK, true));
        details.add(copy(report.description(), textWidth, MUTED));

        JPanel overview = new JPanel(new BorderLayout(16, 0));
        overview.setOpaque(false);
        overview.setAlignmentX(LEFT_ALIGNMENT);
        if (hasPhoto) {
            JLabel image = new JLabel(photo(report, 136));
            image.setVerticalAlignment(SwingConstants.TOP);
            image.getAccessibleContext().setAccessibleName("Photo of " + report.name());
            overview.add(image, BorderLayout.WEST);
        }
        overview.add(details, BorderLayout.CENTER);
        card.add(overview);
        card.add(Box.createVerticalStrut(12));
        card.add(buttons);
        content.add(card);
    }

    private void safety(JPanel content) {
        intro(content, "SAFE HANDOFFS", "Safety & privacy",
                "Protect your personal details and arrange returns through a staffed campus location.");
        JPanel guidance = card();
        for (String[] item : new String[][]{
                {"Report privately", "Keep student IDs, addresses, and contact details out of public descriptions."},
                {"Verify ownership", "Confirm identifying details with campus staff."},
                {"Meet at a staffed location", "Confirm pickup or drop-off with campus staff before going."}}) {
            guidance.add(label(item[0], 15, INK, true));
            guidance.add(copy(item[1], 450, MUTED));
            guidance.add(Box.createVerticalStrut(8));
        }
        content.add(guidance);
        content.add(Box.createVerticalStrut(12));
        JPanel locations = card();
        locations.add(label("Staffed handoff locations", 19, INK, true));
        locations.add(copy("Example locations. Ask UM Matina staff to confirm approved points and hours.", 435, MUTED));
        JPanel grid = grid(2);
        for (String location : CampusFind.LOCATIONS) {
            JPanel tile = new Surface(BLUSH, 10, false);
            tile.setBorder(new EmptyBorder(10, 10, 10, 10));
            tile.add(label(location, 13, INK, true));
            grid.add(tile);
        }
        locations.add(grid);
        content.add(locations);
    }

    private JScrollPane activity() {
        JPanel content = column();
        content.setBorder(new EmptyBorder(22, 13, 16, 13));
        if (!student) {
            JButton alerts = button("", 24, false, () -> go(Page.ALERTS));
            alerts.getAccessibleContext().setAccessibleName("Alerts");
            alerts.setToolTipText("Alerts");
            content.add(actions(alerts, button("Student sign in", -1, false, () -> go(Page.SIGN_IN))));
            content.add(Box.createVerticalStrut(18));
        }
        content.add(label(student ? "STUDENT WORKSPACE" : "LIVE OVERVIEW", 10, RED, true));
        content.add(label(student ? "Your activity" : "Campus pulse", 19, INK, true));
        content.add(Box.createVerticalStrut(10));
        JPanel stats = grid(2);
        if (student) {
            stats.add(stat(own().size(), "My reports", BLUSH));
            stats.add(stat(0, "Pending claims", PAPER));
            stats.add(stat(0, "Unread alerts", BLUSH));
            stats.add(stat(0, "Active handoffs", PAPER));
        } else {
            stats.add(stat(all().size(), "Active reports", BLUSH));
            stats.add(stat(1, "Items returned", new Color(238, 245, 242)));
        }
        content.add(stats);
        content.add(Box.createVerticalStrut(16));
        if (!student) {
            content.add(label("Latest reports", 15, INK, true));
            content.add(button("View all", 28, false, () -> go(Page.BOARD)));
            for (Report report : all().stream().limit(3).toList()) {
                JButton latest = button("", -1, false, () -> details(report));
                latest.getAccessibleContext().setAccessibleName(report.name());
                latest.setLayout(new BorderLayout(6, 0));
                if (!report.image().isEmpty()) latest.add(new JLabel(photo(report, 31)), BorderLayout.WEST);
                latest.add(copy(report.name(), 132, RED, font(10, true)), BorderLayout.CENTER);
                latest.setMaximumSize(new Dimension(Integer.MAX_VALUE, latest.getPreferredSize().height));
                content.add(latest);
                content.add(Box.createVerticalStrut(4));
            }
        }
        JPanel safe = new Surface(null, 16, true);
        safe.setLayout(new BoxLayout(safe, BoxLayout.Y_AXIS));
        safe.setBorder(new EmptyBorder(16, 13, 16, 13));
        safe.add(label("Return it safely", 16, Color.WHITE, true));
        safe.add(copy("Arrange pickup or drop-off at a staffed campus location.", 160, Color.WHITE));
        safe.add(button("View locations", -1, false, () -> go(Page.SAFETY)));
        content.add(safe);
        content.setBackground(Color.WHITE);
        content.setOpaque(true);
        JScrollPane scroll = scroll(content);
        scroll.setPreferredSize(new Dimension(234, 0));
        return scroll;
    }

    private JPanel stat(int count, String name, Color fill) {
        JPanel tile = new Surface(fill, 12, false);
        tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
        tile.setBorder(new EmptyBorder(11, 8, 11, 8));
        tile.add(label(Integer.toString(count), 24, INK, true));
        tile.add(label(name, 10, MUTED, false));
        return tile;
    }

    private List<Report> own() {
        return saved.stream().filter(report -> report.owner().equals(DESKTOP_OWNER)).toList();
    }

    private List<Report> all() {
        var all = new ArrayList<>(saved);
        all.addAll(CampusFind.SAMPLES);
        return all;
    }

    private void intro(JPanel content, String tag, String title, String text) {
        content.add(label(tag, 11, RED, true));
        content.add(Box.createVerticalStrut(5));
        content.add(label(title, 30, INK, true));
        content.add(copy(text, 455, MUTED));
        content.add(Box.createVerticalStrut(12));
    }

    private void message(JPanel content, String title, String text, JPanel actions) {
        JPanel card = card();
        card.add(label(title, 20, INK, true));
        card.add(copy(text, 435, MUTED));
        card.add(Box.createVerticalStrut(9));
        card.add(actions);
        content.add(card);
    }

    private static void fieldRow(JPanel panel, String title, JComponent input) {
        panel.add(Box.createVerticalStrut(9));
        JLabel label = label(title, 12, INK, true);
        label.setLabelFor(input instanceof JScrollPane scroll ? scroll.getViewport().getView() : input);
        panel.add(label);
        input.setAlignmentX(LEFT_ALIGNMENT);
        input.setMaximumSize(new Dimension(Integer.MAX_VALUE, input instanceof JScrollPane ? 76 : 34));
        panel.add(input);
    }

    private static JTextField field(String value) {
        return field(value, "");
    }

    private static JTextField field(String value, String hint) {
        JTextField field = new JTextField(value, 14) {
            @Override protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                if (hint.isEmpty() || !getText().isEmpty()) return;
                Graphics g = graphics.create();
                g.setFont(getFont());
                g.setColor(MUTED);
                FontMetrics metrics = g.getFontMetrics();
                g.drawString(hint, getInsets().left, (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
                g.dispose();
            }
        };
        field.setFont(font(13, false));
        field.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(6, 9, 6, 9)));
        return field;
    }

    private JButton button(String text, int icon, boolean primary, Runnable action) {
        JButton button = new JButton() {
            @Override protected void paintComponent(Graphics graphics) {
                if (primary) paintAccent(graphics, getWidth(), getHeight(), 10);
                else {
                    graphics.setColor(getModel().isRollover() ? BLUSH : getBackground());
                    graphics.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                }
                super.paintComponent(graphics);
                if (isFocusOwner()) {
                    Graphics2D outline = (Graphics2D) graphics.create();
                    outline.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    outline.setColor(primary ? Color.WHITE : RED);
                    outline.setStroke(new BasicStroke(1.5f));
                    outline.drawRoundRect(2, 2, getWidth() - 5, getHeight() - 5, 9, 9);
                    outline.dispose();
                }
            }
        };
        button.putClientProperty("html.disable", true);
        button.setText(text);
        if (icon >= 0) button.setIcon(new UiIcon(icon, icon == 0 ? 21 : 16));
        button.setFont(font(12, true));
        button.setBackground(Color.WHITE);
        button.setForeground(primary ? Color.WHITE : RED);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setRequestFocusEnabled(false);
        button.setBorder(new EmptyBorder(8, 9, 8, 9));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setAlignmentX(LEFT_ALIGNMENT);
        button.addActionListener(event -> action.run());
        return button;
    }

    private BufferedImage image(String path) {
        return images.computeIfAbsent(path, name -> {
            try {
                BufferedImage loaded = ImageIO.read(Path.of(name).toFile());
                if (loaded == null) throw new IOException("Unsupported image format: " + name);
                return loaded;
            } catch (IOException error) {
                throw new java.io.UncheckedIOException("Could not load " + name + ". Run CampusFind from its project folder.", error);
            }
        });
    }

    private Icon photo(Report report, int size) {
        if (report.image().isEmpty()) return null;
        BufferedImage source = image(report.image());
        BufferedImage thumbnail = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = thumbnail.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setClip(new java.awt.geom.RoundRectangle2D.Double(0, 0, size, size, 13, 13));
        g.drawImage(source, 0, 0, size, size, null);
        g.dispose();
        return new ImageIcon(thumbnail);
    }

    private static JPanel card() {
        JPanel card = new Surface(Color.WHITE, 15, false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        return card;
    }

    private static JPanel column() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setAlignmentX(LEFT_ALIGNMENT);
        return panel;
    }

    private static JPanel row() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 3));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        return row;
    }

    private static JPanel actions(Component... components) {
        JPanel row = row();
        for (Component component : components) row.add(component);
        return row;
    }

    private static JPanel grid(int columns) {
        JPanel grid = new JPanel(new GridLayout(0, columns, 8, 8));
        grid.setOpaque(false);
        grid.setAlignmentX(LEFT_ALIGNMENT);
        return grid;
    }

    private static JScrollPane scroll(JPanel panel) {
        for (Component child : panel.getComponents()) {
            if (child instanceof JComponent component) component.setAlignmentX(LEFT_ALIGNMENT);
        }
        JPanel wrapper = new ScrollColumn();
        Color background = panel.isOpaque() ? panel.getBackground() : PAPER;
        wrapper.setBackground(background);
        wrapper.add(panel, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(wrapper);
        scroll.getViewport().setBackground(background);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        return scroll;
    }

    private static final class ScrollColumn extends JPanel implements Scrollable {
        ScrollColumn() { super(new BorderLayout()); }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle visible, int axis, int direction) { return 18; }
        @Override public int getScrollableBlockIncrement(Rectangle visible, int axis, int direction) {
            return Math.max(18, visible.height - 18);
        }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    private static JLabel label(String text, int size, Color color, boolean bold) {
        JLabel label = new JLabel();
        label.putClientProperty("html.disable", true);
        label.setText(text);
        label.setFont(font(size, bold));
        label.setForeground(color);
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private static JTextArea copy(String text, int width, Color color) {
        return copy(text, width, color, false);
    }

    private static JTextArea copy(String text, int width, Color color, boolean bold) {
        return copy(text, width, color, font(12, bold));
    }

    private static JTextArea copy(String text, int width, Color color, Font typeface) {
        JTextArea area = new JTextArea(text) {
            @Override public boolean contains(int x, int y) { return false; }
        };
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        area.setFocusable(false);
        area.setOpaque(false);
        area.setBorder(null);
        area.setMargin(new Insets(0, 0, 0, 0));
        area.setFont(typeface);
        area.setForeground(color);
        area.setAlignmentX(LEFT_ALIGNMENT);
        area.setSize(width, Short.MAX_VALUE);
        Dimension size = new Dimension(width, area.getPreferredSize().height);
        area.setPreferredSize(size);
        area.setMinimumSize(size);
        area.setMaximumSize(size);
        return area;
    }

    private static Font font(int size, boolean bold) {
        return new Font(Font.SANS_SERIF, bold ? Font.BOLD : Font.PLAIN, size);
    }

    private static String capitalize(String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static void paintAccent(Graphics graphics, int width, int height, int radius) {
        if (width == 0 || height == 0) return;
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new LinearGradientPaint(0, 0, width, height,
                new float[]{0f, 0.58f, 1f},
                new Color[]{new Color(0x65051D), new Color(0xA90B16), new Color(0xD52B25)}));
        g.fillRoundRect(0, 0, width, height, radius, radius);
        g.dispose();
    }

    private record UiIcon(int glyph, int size) implements Icon {
        @Override public int getIconWidth() { return size; }
        @Override public int getIconHeight() { return size; }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.translate(x, y);
            g.scale(size / 16.0, size / 16.0);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(component == null ? RED : component.getForeground());
            g.setStroke(new BasicStroke(1.35f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            switch (glyph) {
                case 0, 2, 9 -> {
                    g.drawOval(2, 2, 8, 8);
                    path(g, 9, 9, 14, 14);
                }
                case 1, 13 -> {
                    g.drawRoundRect(2, 2, 4, 4, 1, 1);
                    g.drawRoundRect(10, 2, 4, 4, 1, 1);
                    g.drawRoundRect(2, 10, 4, 4, 1, 1);
                    g.drawRoundRect(10, 10, 4, 4, 1, 1);
                }
                case 3, 10 -> {
                    path(g, 3, 8, 13, 8);
                    path(g, 8, 3, 8, 13);
                }
                case 4, 24 -> {
                    g.drawArc(4, 2, 8, 8, 0, 180);
                    path(g, 4, 6, 4, 9, 2, 11, 14, 11, 12, 9, 12, 6);
                    g.drawArc(6, 11, 4, 3, 180, 180);
                }
                case 5 -> {
                    path(g, 8, 2, 13, 4, 13, 9, 11, 12, 8, 14, 5, 12, 3, 9, 3, 4, 8, 2);
                    path(g, 6, 8, 8, 10, 10, 7);
                }
                case 7, 28 -> {
                    path(g, 3, 8, 13, 8);
                    path(g, 9, 4, 13, 8, 9, 12);
                }
                case 8 -> {
                    g.drawOval(5, 2, 6, 6);
                    g.drawArc(3, 9, 10, 8, 0, 180);
                }
                case 11 -> path(g, 3, 8, 6, 11, 13, 4);
                case 14 -> {
                    g.drawRoundRect(4, 1, 8, 14, 2, 2);
                    path(g, 7, 12, 9, 12);
                }
                case 15 -> {
                    g.drawRoundRect(2, 5, 12, 9, 2, 2);
                    g.drawArc(5, 2, 6, 6, 0, 180);
                }
                case 16 -> {
                    g.drawOval(2, 8, 5, 5);
                    path(g, 6, 9, 13, 2, 14, 3);
                    path(g, 10, 5, 12, 7);
                }
                case 17 -> {
                    path(g, 8, 4, 5, 2, 2, 2, 2, 12, 5, 12, 8, 14, 11, 12, 14, 12, 14, 2, 11, 2, 8, 4, 8, 14);
                }
                case 18 -> path(g, 6, 2, 10, 2, 10, 5, 12, 7, 12, 14, 4, 14, 4, 7, 6, 5, 6, 2);
                case 19 -> path(g, 6, 2, 3, 3, 1, 7, 4, 8, 5, 7, 5, 14, 11, 14, 11, 7, 12, 8, 15, 7, 13, 3, 10, 2, 8, 4, 6, 2);
                case 20 -> {
                    for (int dot : new int[]{3, 8, 13}) g.fillOval(dot - 1, 7, 2, 2);
                }
                default -> throw new IllegalArgumentException("Unknown icon: " + glyph);
            }
            g.dispose();
        }

        private static void path(Graphics2D graphics, int... coordinates) {
            var path = new java.awt.geom.Path2D.Float();
            path.moveTo(coordinates[0], coordinates[1]);
            for (int i = 2; i < coordinates.length; i += 2) path.lineTo(coordinates[i], coordinates[i + 1]);
            graphics.draw(path);
        }
    }

    private static final class Surface extends JPanel {
        private final Color fill;
        private final int radius;
        private final boolean shaded;

        Surface(Color fill, int radius, boolean shaded) {
            this.fill = fill;
            this.radius = radius;
            this.shaded = shaded;
            setOpaque(false);
            setAlignmentX(LEFT_ALIGNMENT);
        }

        @Override protected void paintComponent(Graphics graphics) {
            if (shaded) paintAccent(graphics, getWidth(), getHeight(), radius);
            else {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(fill);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
                if (fill.equals(Color.WHITE)) {
                    g.setColor(BORDER);
                    g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
                }
                g.dispose();
            }
            super.paintComponent(graphics);
        }
    }
}
