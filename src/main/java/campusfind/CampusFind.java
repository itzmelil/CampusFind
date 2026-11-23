package campusfind;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
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

    static final class ReportIndex {
        private final List<Report> reports;
        private final Map<String, Report> byId = new HashMap<>();
        private final Map<String, List<Report>> byCategory = new HashMap<>();

        ReportIndex(List<Report> saved) {
            reports = new ArrayList<>(saved.size() + SAMPLES.size());
            reports.addAll(saved);
            reports.addAll(SAMPLES);
            for (Report report : reports) {
                byId.put(report.id(), report);
                byCategory.computeIfAbsent(report.category(), category -> new ArrayList<>()).add(report);
            }
        }

        List<Report> all() { return List.copyOf(reports); }

        Report get(String id) { return byId.get(id); }

        List<Report> search(String text, String status, String category) {
            String query = normalize(text).strip();
            List<Report> candidates = category.equals("All items") ? reports : inCategory(category);
            var matches = new ArrayList<Report>();
            for (Report report : candidates) {
                if (!status.equals("all") && !report.type().equals(status)) continue;
                String searchable = String.join(" ", report.id(), report.name(), report.category(),
                        report.description(), report.location());
                if (normalize(searchable).contains(query)) matches.add(report);
            }
            return matches;
        }

        List<Report> suggestedMatches(Report target) {
            var ranked = new ArrayList<ScoredReport>();
            Set<String> targetWords = itemWords(target);
            Set<String> targetLocation = words(target.location());
            for (Report candidate : inCategory(target.category())) {
                if (candidate.type().equals(target.type())) continue;
                int itemWords = overlap(targetWords, itemWords(candidate));
                int locationWords = overlap(targetLocation, words(candidate.location()));
                if (itemWords > 0) ranked.add(new ScoredReport(candidate, 3 * itemWords + locationWords));
            }
            mergeSort(ranked, Comparator.comparingInt(ScoredReport::score).reversed());
            return ranked.stream().limit(3).map(ScoredReport::report).toList();
        }

        private List<Report> inCategory(String category) {
            return byCategory.getOrDefault(category, List.of());
        }
    }

    private record ScoredReport(Report report, int score) {}

    private static final Set<String> COMMON_WORDS = Set.of(
            "the", "and", "with", "from", "near", "this", "that", "found", "lost", "item", "items", "campus", "matina");

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    private static Set<String> itemWords(Report report) {
        String description = report.id().startsWith("sample-") ? "" : report.description();
        return words(report.name() + " " + description);
    }

    private static Set<String> words(String text) {
        var result = new HashSet<String>();
        for (String word : normalize(text).split("[^\\p{L}\\p{N}]+")) {
            if (word.length() >= 3 && !COMMON_WORDS.contains(word)) result.add(word);
        }
        return result;
    }

    private static int overlap(Set<String> first, Set<String> second) {
        int count = 0;
        for (String word : first) if (second.contains(word)) count++;
        return count;
    }

    private static <T> void mergeSort(List<T> values, Comparator<? super T> order) {
        mergeSort(values, new ArrayList<>(values), 0, values.size(), order);
    }

    private static <T> void mergeSort(List<T> values, List<T> buffer, int start, int end,
                                      Comparator<? super T> order) {
        if (end - start < 2) return;
        int middle = (start + end) >>> 1;
        mergeSort(values, buffer, start, middle, order);
        mergeSort(values, buffer, middle, end, order);
        for (int i = start; i < end; i++) buffer.set(i, values.get(i));
        int left = start;
        int right = middle;
        for (int i = start; i < end; i++) {
            if (right == end || (left < middle && order.compare(buffer.get(left), buffer.get(right)) <= 0)) {
                values.set(i, buffer.get(left++));
            } else {
                values.set(i, buffer.get(right++));
            }
        }
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
            mergeSort(stored, Comparator.comparingLong((Properties p) -> Long.parseLong(p.getProperty("created", "0")))
                    .reversed().thenComparing(p -> p.getProperty("id", "")));
            return stored.stream().map(p -> new Report(
                    p.getProperty("id"), p.getProperty("owner"), p.getProperty("name"), p.getProperty("category"),
                    p.getProperty("type"), p.getProperty("date"), p.getProperty("location"),
                    p.getProperty("description"), p.getProperty("image"))).toList();
        }
    }
}
