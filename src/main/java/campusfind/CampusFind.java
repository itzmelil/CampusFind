package campusfind;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

public final class CampusFind {
    static final List<String> CATEGORIES =
            List.of("Electronics", "Bags", "Keys", "Books", "Bottles", "Clothing", "Other");
    static final List<String> LOCATIONS = List.of(
            "Library", "Main entrance", "Student services area", "Campus security desk");
    static final List<Report> SAMPLES = List.of(
            sample(1, "Blue game controller", "Other", "found", "Student commons"),
            sample(2, "Black leather shoulder bag", "Bags", "lost", "Campus canteen"),
            sample(3, "Over-ear wired headphones", "Electronics", "found", "Covered walkway"),
            sample(4, "Phone with clear case", "Electronics", "lost", "Classroom corridor"),
            sample(5, "Black canvas backpack", "Bags", "found", "Student services area"),
            sample(6, "Midnight blue headphones", "Electronics", "found", "Library"));

    private CampusFind() {}

    static List<Report> filterReports(List<Report> reports, String search, String status, String category) {
        String query = search.strip().toLowerCase(Locale.ROOT);
        return reports.stream().filter(report ->
                (status.equals("all") || report.type.equals(status))
                && (category.equals("All items") || report.category.equals(category))
                && String.join(" ", report.name, report.category, report.location)
                         .toLowerCase(Locale.ROOT).contains(query)).toList();
    }

    static Report validateReport(Map<String, String> fields) {
        String type = required(fields, "type", "Report type", 5);
        String category = required(fields, "category", "Category", 20);
        String date = required(fields, "date", "Date", 10);
        if (!type.equals("lost") && !type.equals("found")) {
            throw new IllegalArgumentException("Choose a lost or found report.");
        }
        if (!CATEGORIES.contains(category)) {
            throw new IllegalArgumentException("Choose a valid category.");
        }
        try {
            LocalDate.parse(date);
        } catch (java.time.DateTimeException invalid) {
            throw new IllegalArgumentException("Enter a valid date.");
        }
        return new Report("", "", required(fields, "name", "Item name", 100),
                category, type, date, required(fields, "location", "Campus location", 120),
                required(fields, "description", "Description", 500), "");
    }

    private static String required(Map<String, String> fields, String key, String label, int limit) {
        String value = fields.getOrDefault(key, "").strip();
        if (value.isEmpty() || value.length() > limit) {
            throw new IllegalArgumentException(label + " must be between 1 and " + limit + " characters.");
        }
        return value;
    }

    private static Report sample(int number, String name, String category, String type, String location) {
        return new Report("sample-" + number, "", name, category, type, "", "Matina Campus · " + location,
                "Contact campus staff to confirm identifying details and arrange a staffed handoff.",
                "./assets/item-" + number + ".png");
    }

    record Report(String id, String owner, String name, String category, String type,
                          String date, String location, String description, String image) {
        private Map<String, String> fields() {
            return Map.of("id", id, "name", name, "category", category, "type", type, "date", date,
                    "location", location, "description", description, "image", image);
        }
    }

    static final class ReportStore {
        private final Path directory;

        ReportStore(Path directory) throws IOException {
            this.directory = Files.createDirectories(directory);
        }

        Report save(Report draft, String owner) throws IOException {
            String id = UUID.randomUUID().toString();
            Report report = new Report(id, owner, draft.name, draft.category, draft.type, draft.date,
                    draft.location, draft.description, "");
            var fields = new Properties();
            fields.putAll(report.fields());
            fields.setProperty("owner", owner);
            fields.setProperty("created", Long.toString(System.currentTimeMillis()));
            Path temp = Files.createTempFile(directory, "report-", ".tmp");
            try {
                try (var output = Files.newOutputStream(temp)) {
                    fields.store(output, null);
                }
                Files.move(temp, directory.resolve(id + ".properties"), java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } finally {
                Files.deleteIfExists(temp);
            }
            return report;
        }

        List<Report> all() throws IOException {
            var stored = new ArrayList<Properties>();
            try (var files = Files.newDirectoryStream(directory, "*.properties")) {
                for (Path file : files) {
                    var fields = new Properties();
                    try (var input = Files.newInputStream(file)) {
                        fields.load(input);
                    }
                    stored.add(fields);
                }
            }
            stored.sort(Comparator.comparing((Properties p) -> p.getProperty("created", "")).reversed());
            return stored.stream().map(p -> new Report(
                    p.getProperty("id"), p.getProperty("owner"), p.getProperty("name"), p.getProperty("category"),
                    p.getProperty("type"), p.getProperty("date"), p.getProperty("location"),
                    p.getProperty("description"), p.getProperty("image"))).toList();
        }
    }
}
