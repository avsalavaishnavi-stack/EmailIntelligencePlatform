
package com.emailintel.backend.service;

import com.emailintel.backend.dsa.*;
import com.emailintel.backend.model.Email;
import jakarta.annotation.PostConstruct;
import jakarta.mail.*;
import jakarta.mail.internet.MimeMessage;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EmailService {

    private final Map<String, Email> emails = new ConcurrentHashMap<>();

    private final Map<Long, String> contentHashes = new HashMap<>();

    private final Map<String, Trie> categoryTries = new HashMap<>();

    private final Trie searchTrie = new Trie();

    private final Map<String, Integer> spamIndex = new HashMap<>();

    private final LRUCache<String, List<String>> recommendationCache =
            new LRUCache<>(20);

    private final EmailGraph graph = new EmailGraph();

    private final Path inbox = Paths.get("..", "inbox")
            .toAbsolutePath()
            .normalize();

    private final Set<String> spamWords = Set.of(
            "winner",
            "lottery",
            "prize",
            "urgent payment",
            "claim now",
            "free money",
            "casino",
            "viagra",
            "bitcoin profit",
            "click here"
    );

    @PostConstruct
    public void init() {

        // ==============================
        // CATEGORY TRIES
        // ==============================

        categoryTries.put(
                "Academic",
                trie(
                        "exam",
                        "assignment",
                        "college",
                        "faculty",
                        "student"
                )
        );

        categoryTries.put(
                "Work",
                trie(
                        "meeting",
                        "project",
                        "deadline",
                        "office",
                        "work",
                        "client"
                )
        );

        categoryTries.put(
                "Promotions",
                trie(
                        "sale",
                        "offer",
                        "discount",
                        "coupon",
                        "deal"
                )
        );

        // ==============================
        // SPAM INDEX
        // ==============================

        for (String w : spamWords) {
            spamIndex.put(w, 1);
        }

        // ==============================
        // LOAD INBOX
        // ==============================

        try {
            Files.createDirectories(inbox);
            scanInbox();
        } catch (IOException ignored) {
        }
    }

    // =========================================================
    // ADD EMAIL
    // =========================================================

    public synchronized Email addEmail(
            String sender,
            String recipient,
            String subject,
            String body
    ) {

        String id = UUID.randomUUID().toString();

        Email e = new Email(
                id,
                sender,
                recipient,
                subject,
                body,
                LocalDateTime.now()
        );

        analyze(e);

        emails.put(id, e);

        return e;
    }

    // =========================================================
    // UPLOAD EMAIL
    // =========================================================

    public synchronized Email upload(MultipartFile file) throws Exception {

        Path temp = Files.createTempFile("email-", ".eml");

        file.transferTo(temp);

        Email e = parseEml(temp);

        Files.deleteIfExists(temp);

        analyze(e);

        emails.put(e.id, e);

        return e;
    }

    // =========================================================
    // ANALYZE EMAIL
    // =========================================================

    private void analyze(Email e) {

        String text = e.searchableText();

        // =====================================================
        // SPAM DETECTION
        // =====================================================

        int spamScore = 0;

        for (String w : spamIndex.keySet()) {

            if (text.toLowerCase().contains(w.toLowerCase())) {
                spamScore++;
            }
        }

        e.spam =
                spamScore >= 2
                        || (
                        text.toLowerCase().contains("winner")
                                &&
                                text.toLowerCase().contains("claim")
                );

        e.reason = e.spam
                ? "Spam keywords matched: " + spamScore
                : "No strong spam pattern detected";

        // =====================================================
        // CATEGORY
        // =====================================================

        e.category = category(text);

        // =====================================================
        // DUPLICATE DETECTION - RABIN KARP
        // =====================================================

        long h = RabinKarp.hash(normalize(text));

        e.duplicate = contentHashes.containsKey(h);

        contentHashes.putIfAbsent(h, e.id);

        // =====================================================
        // PRIORITY - HEAP LOGIC
        // =====================================================

        e.priority = priority(e, spamScore);

        // =====================================================
        // CONTACT GRAPH
        // =====================================================

        graph.addEdge(
                cleanAddress(e.sender),
                cleanAddress(e.recipient)
        );

        // =====================================================
        // SEARCH INDEX
        // =====================================================

        searchTrie.insert(firstWord(e.subject));

        // =====================================================
        // SIMILARITY - DYNAMIC PROGRAMMING
        // =====================================================

        for (Email old : emails.values()) {

            if (!old.id.equals(e.id)) {

                // Current email content
                String currentContent =
                        (e.subject == null ? "" : e.subject) + " " +
                        (e.body == null ? "" : e.body);

                // Existing email content
                String oldContent =
                        (old.subject == null ? "" : old.subject) + " " +
                        (old.body == null ? "" : old.body);

                // DP similarity score
                double s = Similarity.score(
                        currentContent,
                        oldContent
                );

                // Print score in backend terminal
                System.out.println(
                        "Similarity between \"" +
                                e.subject +
                                "\" and \"" +
                                old.subject +
                                "\" = " +
                                s
                );

                // Similarity threshold
                if (s >= 25) {

                    // Current email -> old email
                    if (!e.similarIds.contains(old.id)) {
                        e.similarIds.add(old.id);
                    }

                    // Old email -> current email
                    // This makes the relationship bidirectional.
                    if (!old.similarIds.contains(e.id)) {
                        old.similarIds.add(e.id);
                    }
                }
            }
        }
    }

    // =========================================================
    // PRIORITY CALCULATION
    // =========================================================

    private int priority(Email e, int spamScore) {

        int p = 50 - spamScore * 15;

        String t = e.searchableText().toLowerCase();

        if (t.contains("urgent")
                || t.contains("deadline")
                || t.contains("exam")) {

            p += 25;
        }

        if (t.contains("meeting")
                || t.contains("assignment")) {

            p += 15;
        }

        if (e.sender != null
                && e.sender.toLowerCase().contains("college")) {

            p += 10;
        }

        return Math.max(
                0,
                Math.min(100, p)
        );
    }

    // =========================================================
    // EMAIL CATEGORIZATION
    // =========================================================

    private String category(String text) {

        String t = text.toLowerCase();

        // -----------------------------------------------------
        // 1. PROMOTIONS
        // -----------------------------------------------------

        String[] promotionWords = {
                "sale",
                "offer",
                "discount",
                "coupon",
                "deal",
                "prize",
                "winner",
                "free money",
                "claim now",
                "click here"
        };

        for (String word : promotionWords) {

            if (t.contains(word)) {
                return "Promotions";
            }
        }

        // -----------------------------------------------------
        // 2. WORK
        // -----------------------------------------------------

        String[] workWords = {
                "meeting",
                "project",
                "deadline",
                "office",
                "work",
                "client"
        };

        for (String word : workWords) {

            if (t.contains(word)) {
                return "Work";
            }
        }

        // -----------------------------------------------------
        // 3. ACADEMIC
        // -----------------------------------------------------

        String[] academicWords = {
                "exam",
                "assignment",
                "college",
                "faculty",
                "student"
        };

        for (String word : academicWords) {

            if (t.contains(word)) {
                return "Academic";
            }
        }

        // -----------------------------------------------------
        // 4. PERSONAL
        // -----------------------------------------------------

        return "Personal";
    }

    // =========================================================
    // CREATE TRIE
    // =========================================================

    private Trie trie(String... words) {

        Trie t = new Trie();

        for (String w : words) {
            t.insert(w);
        }

        return t;
    }

    // =========================================================
    // NORMALIZE TEXT
    // =========================================================

    private String normalize(String s) {

        if (s == null) {
            return "";
        }

        return s
                .replaceAll("\\s+", " ")
                .trim();
    }

    // =========================================================
    // FIRST WORD
    // =========================================================

    private String firstWord(String s) {

        String x = s == null
                ? ""
                : s.toLowerCase()
                .replaceAll("[^a-z0-9 ]", " ")
                .trim();

        return x.isEmpty()
                ? "mail"
                : x.split(" ")[0];
    }

    // =========================================================
    // CLEAN EMAIL ADDRESS
    // =========================================================

    private String cleanAddress(String s) {

        if (s == null) {
            return "unknown";
        }

        int lt = s.indexOf('<');
        int gt = s.indexOf('>');

        return lt >= 0 && gt > lt
                ? s.substring(lt + 1, gt)
                : s.trim();
    }

    // =========================================================
    // GET ALL EMAILS
    // =========================================================

    public List<Email> all() {

        PriorityQueue<Email> heap =
                new PriorityQueue<>(
                        (a, b) ->
                                Integer.compare(
                                        b.priority,
                                        a.priority
                                )
                );

        heap.addAll(emails.values());

        List<Email> out = new ArrayList<>();

        while (!heap.isEmpty()) {
            out.add(heap.poll());
        }

        return out;
    }

    // =========================================================
    // GET ONE EMAIL
    // =========================================================

    public Optional<Email> one(String id) {

        return Optional.ofNullable(
                emails.get(id)
        );
    }

    // =========================================================
    // SEARCH
    // KMP + RABIN-KARP + SUFFIX ARRAY
    // =========================================================

    public List<Email> search(String q) {

        if (q == null || q.isBlank()) {
            return all();
        }

        String p = q.toLowerCase();

        return emails.values()
                .stream()
                .filter(e ->
                        KMP.contains(
                                e.searchableText(),
                                p
                        )
                                ||
                                RabinKarp.contains(
                                        e.searchableText(),
                                        p
                                )
                                ||
                                SuffixArray.contains(
                                        e.searchableText(),
                                        p
                                )
                )
                .sorted(
                        Comparator.comparingInt(
                                (Email e) -> e.priority
                        ).reversed()
                )
                .toList();
    }

    // =========================================================
    // SIMILAR EMAILS
    // LRU CACHE
    // =========================================================

    public List<Email> similar(String id) {

        Email e = emails.get(id);

        if (e == null) {
            return List.of();
        }

        String key = id;

        List<String> cached =
                recommendationCache.get(key);

        if (cached == null) {

            cached = e.similarIds;

            recommendationCache.put(
                    key,
                    cached
            );
        }

        return cached.stream()
                .map(emails::get)
                .filter(Objects::nonNull)
                .toList();
    }

    // =========================================================
    // STATISTICS
    // =========================================================

    public Map<String, Object> stats() {

        long spam =
                emails.values()
                        .stream()
                        .filter(e -> e.spam)
                        .count();

        long dup =
                emails.values()
                        .stream()
                        .filter(e -> e.duplicate)
                        .count();

        return Map.of(
                "total", emails.size(),
                "spam", spam,
                "duplicates", dup,
                "dupes", dup,
                "contacts", graph.vertices(),
                "connections", graph.edges()
        );
    }

    // =========================================================
    // CONTACT NETWORK
    // BFS + DFS
    // =========================================================

    public Map<String, Object> network(String email) {

        return Map.of(
                "bfs", graph.bfs(email),
                "dfs", graph.dfs(email),
                "vertices", graph.vertices(),
                "edges", graph.edges()
        );
    }

    // =========================================================
    // SCAN INBOX
    // =========================================================

    public synchronized void scanInbox() {

        try (
                DirectoryStream<Path> ds =
                        Files.newDirectoryStream(
                                inbox,
                                "*.eml"
                        )
        ) {

            for (Path p : ds) {

                try {

                    boolean alreadyLoaded =
                            emails.values()
                                    .stream()
                                    .anyMatch(
                                            e ->
                                                    e.id.equals(
                                                            p.getFileName()
                                                                    .toString()
                                                    )
                                    );

                    if (!alreadyLoaded) {

                        Email e = parseEml(p);

                        analyze(e);

                        emails.put(
                                e.id,
                                e
                        );
                    }

                } catch (Exception ignored) {
                }
            }

        } catch (IOException ignored) {
        }
    }

    // =========================================================
    // PARSE EML FILE
    // =========================================================

    private Email parseEml(Path p) throws Exception {

        Properties props = new Properties();

        Session session =
                Session.getDefaultInstance(props);

        MimeMessage msg =
                new MimeMessage(
                        session,
                        Files.newInputStream(p)
                );

        String sender =
                msg.getFrom() != null
                        && msg.getFrom().length > 0
                        ? msg.getFrom()[0].toString()
                        : "unknown@example.com";

        String recipient =
                msg.getRecipients(
                                Message.RecipientType.TO
                        ) != null
                        && msg.getRecipients(
                        Message.RecipientType.TO
                ).length > 0
                        ? msg.getRecipients(
                        Message.RecipientType.TO
                )[0].toString()
                        : "me@example.com";

        String subject =
                Optional.ofNullable(
                        msg.getSubject()
                ).orElse("No subject");

        Object content =
                msg.getContent();

        String body =
                content instanceof String
                        ? (String) content
                        : content.toString();

        String id =
                p.getFileName().toString();

        return new Email(
                id,
                sender,
                recipient,
                subject,
                body,
                LocalDateTime.now()
        );
    }
}

