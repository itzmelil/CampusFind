package campusfind;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public final class CampusFind {
    private static final List<String> CATEGORIES =
            List.of("Electronics", "Bags", "Keys", "Books", "Bottles", "Clothing", "Other");
    private static final List<String> LOCATIONS = List.of(
            "Library", "Main entrance", "Student services area", "Campus security desk");
    private static final List<Report> SAMPLES = List.of(
            sample(1, "Blue game controller", "Other", "found", "Student commons"),
            sample(2, "Black leather shoulder bag", "Bags", "lost", "Campus canteen"),
            sample(3, "Over-ear wired headphones", "Electronics", "found", "Covered walkway"),
            sample(4, "Phone with clear case", "Electronics", "lost", "Classroom corridor"),
            sample(5, "Black canvas backpack", "Bags", "found", "Student services area"),
            sample(6, "Midnight blue headphones", "Electronics", "found", "Library"));

    private final Path root = Path.of("").toAbsolutePath();
    private final ReportStore reports;

    private CampusFind(Path data) throws IOException {
        reports = new ReportStore(data);
    }

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8000;
        Path data = args.length > 1 ? Path.of(args[1]) : Path.of("data");
        CampusFind app = new CampusFind(data);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/", app::handle);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();
        System.out.println("CampusFind is running at http://127.0.0.1:" + server.getAddress().getPort());
    }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            try {
                String path = exchange.getRequestURI().getPath();
                String method = exchange.getRequestMethod();
                if (path.equals("/api/board") && method.equals("GET")) {
                    board(exchange);
                } else if ((path.equals("/api/preview") || path.equals("/api/reports")) && method.equals("POST")) {
                    Report draft = readDraft(exchange);
                    if (path.equals("/api/reports")) {
                        draft = reports.save(draft, browserId(exchange));
                    }
                    json(exchange, 200, draft.json());
                } else if (path.startsWith("/api/")) {
                    json(exchange, 404, object(Map.of("error", "This endpoint is unavailable.")));
                } else if (method.equals("GET")) {
                    servePage(exchange, path);
                } else {
                    send(exchange, 405, "text/plain; charset=utf-8", "Method not allowed.".getBytes(StandardCharsets.UTF_8));
                }
            } catch (IllegalArgumentException invalid) {
                json(exchange, 400, object(Map.of("error", invalid.getMessage())));
            } catch (IOException failure) {
                System.err.println(failure.getMessage());
                if (exchange.getResponseCode() == -1) {
                    json(exchange, 500, object(Map.of("error", "CampusFind could not complete this request.")));
                }
            }
        }
    }

    private void board(HttpExchange exchange) throws IOException {
        var filters = decode(exchange.getRequestURI().getRawQuery());
        var saved = reports.all();
        String owner = browserId(exchange);
        var all = new ArrayList<>(saved);
        all.addAll(SAMPLES);
        String query = filters.getOrDefault("query", "").strip().toLowerCase(Locale.ROOT);
        String status = filters.getOrDefault("status", "all");
        String category = filters.getOrDefault("category", "All items");
        List<Report> visible = all.stream().filter(report ->
                (status.equals("all") || report.type.equals(status))
                && (category.equals("All items") || report.category.equals(category))
                && String.join(" ", report.name, report.category, report.location)
                         .toLowerCase(Locale.ROOT).contains(query)).toList();
        List<Report> own = saved.stream().filter(report -> report.owner.equals(owner)).toList();
        json(exchange, 200, "{\"reports\":" + reportList(all) + ",\"visible\":" + reportList(visible)
                + ",\"own\":" + reportList(own) + ",\"categories\":" + stringList(CATEGORIES)
                + ",\"locations\":" + stringList(LOCATIONS) + "}");
    }

    private void servePage(HttpExchange exchange, String path) throws IOException {
        String type = switch (path) {
            case "/", "/index.html" -> "text/html; charset=utf-8";
            case "/src/app.js" -> "text/javascript; charset=utf-8";
            case "/src/styles.css" -> "text/css; charset=utf-8";
            case "/assets/manrope.woff2" -> "font/woff2";
            default -> path.matches("/assets/(item|icon)-[0-9]+\\.png") ? "image/png" : null;
        };
        Path file = root.resolve(path.equals("/") ? "index.html" : path.substring(1));
        if (type == null || !Files.isRegularFile(file)) {
            send(exchange, 404, "text/plain; charset=utf-8", "Page not found.".getBytes(StandardCharsets.UTF_8));
            return;
        }
        send(exchange, 200, type, Files.readAllBytes(file));
    }

    private static Report readDraft(HttpExchange exchange) throws IOException {
        byte[] body = exchange.getRequestBody().readNBytes(8193);
        if (body.length > 8192) {
            throw new IllegalArgumentException("The report is too long.");
        }
        var fields = decode(new String(body, StandardCharsets.UTF_8));
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

    private static Map<String, String> decode(String input) {
        var values = new LinkedHashMap<String, String>();
        if (input == null || input.isEmpty()) return values;
        for (String field : input.split("&")) {
            String[] pair = field.split("=", 2);
            if (pair.length == 2) {
                values.put(URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
            }
        }
        return values;
    }

    private static String browserId(HttpExchange exchange) {
        String cookie = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookie != null) {
            for (String value : cookie.split(";")) {
                String trimmed = value.trim();
                if (trimmed.matches("campusfind=[a-f0-9-]{36}")) {
                    return trimmed.substring("campusfind=".length());
                }
            }
        }
        String id = UUID.randomUUID().toString();
        exchange.getResponseHeaders().add("Set-Cookie",
                "campusfind=" + id + "; Path=/api; HttpOnly; SameSite=Strict; Max-Age=31536000");
        return id;
    }

    private static Report sample(int number, String name, String category, String type, String location) {
        return new Report("sample-" + number, "", name, category, type, "", "Matina Campus · " + location,
                "Contact campus staff to confirm identifying details and arrange a staffed handoff.",
                "./assets/item-" + number + ".png");
    }

    private static String reportList(List<Report> reports) {
        return reports.stream().map(Report::json).collect(Collectors.joining(",", "[", "]"));
    }

    private static String stringList(List<String> values) {
        return values.stream().map(CampusFind::quote).collect(Collectors.joining(",", "[", "]"));
    }

    private static String object(Map<String, String> fields) {
        return fields.entrySet().stream().map(entry -> quote(entry.getKey()) + ":" + quote(entry.getValue()))
                .collect(Collectors.joining(",", "{", "}"));
    }

    private static String quote(String value) {
        var quoted = new StringBuilder("\"");
        for (char character : value.toCharArray()) {
            switch (character) {
                case '"' -> quoted.append("\\\"");
                case '\\' -> quoted.append("\\\\");
                default -> {
                    if (character < 32) quoted.append(String.format("\\u%04x", (int) character));
                    else quoted.append(character);
                }
            }
        }
        return quoted.append('"').toString();
    }

    private static void json(HttpExchange exchange, int status, String body) throws IOException {
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        send(exchange, status, "application/json; charset=utf-8", body.getBytes(StandardCharsets.UTF_8));
    }

    private static void send(HttpExchange exchange, int status, String type, byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", type);
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
    }

    private record Report(String id, String owner, String name, String category, String type,
                          String date, String location, String description, String image) {
        private Map<String, String> fields() {
            return Map.of("id", id, "name", name, "category", category, "type", type, "date", date,
                    "location", location, "description", description, "image", image);
        }

        private String json() {
            return object(fields());
        }
    }

    private static final class ReportStore {
        private final Path directory;

        private ReportStore(Path directory) throws IOException {
            this.directory = Files.createDirectories(directory);
        }

        private Report save(Report draft, String owner) throws IOException {
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

        private List<Report> all() throws IOException {
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
