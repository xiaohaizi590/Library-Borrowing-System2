package net.togogo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 极限压力测试 — 找到系统的瓶颈点
 *
 * 测试场景:
 *   1. 突发流量测试 - 300 线程瞬间并发请求（冲击 Tomcat 线程池上限 200）
 *   2. 数据库连接池耗尽测试 - 500 并发请求不同数据（冲击 HikariCP 上限 10）
 *   3. 乐观锁极限竞争 - 100 人抢同一本书
 *   4. 持续压力测试 - 200 并发不间断请求 30 秒
 *   5. 极端混合负载 - 读写同时冲击
 */
public class ExtremeStressTest {

    private static final String BASE_URL = "http://localhost:8081";
    private static final String JWT_SECRET = "mySecretKeyForJwtTokenMustBeLongEnough123456";
    private static final long JWT_EXPIRE_MS = 86_400_000L;
    private static final int TEST_USER_COUNT = 150;
    private static final String TEST_PASSWORD = "123456";
    private static final int BOOK_STOCK = 200;
    private static final int TEST_BOOK_COUNT = 50;

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final List<String> userTokens = new CopyOnWriteArrayList<>();
    private static final List<String> adminTokens = new CopyOnWriteArrayList<>();
    private static final List<Long> bookIds = new CopyOnWriteArrayList<>();

    // 统计
    private static final AtomicInteger successCount = new AtomicInteger(0);
    private static final AtomicInteger failureCount = new AtomicInteger(0);
    private static final AtomicLong totalLatencyNanos = new AtomicLong(0);
    private static final List<Long> allLatencies = new CopyOnWriteArrayList<>();
    private static long testStartTime;
    private static long testEndTime;

    public static void main(String[] args) throws Exception {
        System.out.println("████████████████████████████████████████████████████");
        System.out.println("██              极限压力测试                      ██");
        System.out.println("██  目标: 找到系统的瓶颈点                        ██");
        System.out.println("██  服务器: " + BASE_URL);
        System.out.println("████████████████████████████████████████████████████");
        System.out.println();

        // 初始化
        printHeader("初始化测试数据");
        initTestData();
        System.out.println("  用户: " + userTokens.size() + " | 图书: " + bookIds.size() + " 本 (库存 " + BOOK_STOCK + ")");
        System.out.println();

        // ==================== 场景 1：突发流量测试 ====================
        printHeader("场景 1: 突发流量 — 300 线程同时启动");
        System.out.println("  目标: 冲击 Tomcat 默认线程池上限 (200)");
        testBurstTraffic();
        System.out.println();

        // ==================== 场景 2：DB 连接池耗尽测试 ====================
        printHeader("场景 2: 数据库连接池耗尽 — 500 并发查不同数据");
        System.out.println("  目标: 冲击 HikariCP 连接池上限 (10)");
        System.out.println("  方式: 500个请求分别查不同图书ID, 触发大量缓存未命中");
        testDbPoolExhaustion();
        System.out.println();

        // ==================== 场景 3：乐观锁极限竞争 ====================
        printHeader("场景 3: 乐观锁极限竞争 — 100 人抢同一本书");
        System.out.println("  目标: 测试 @Version 乐观锁在极限竞争下的表现");
        testOptimisticLockExtreme();
        System.out.println();

        // ==================== 场景 4：持续压力测试 ====================
        printHeader("场景 4: 持续压力 — 200 并发 × 30 秒");
        System.out.println("  目标: 测试系统长时间高负载下的稳定性");
        testSustainedLoad();
        System.out.println();

        // ==================== 场景 5：极端混合负载 ====================
        printHeader("场景 5: 极端混合负载 — 读写同时冲击");
        System.out.println("  目标: 模拟真实业务高峰");
        testExtremeMixed();
        System.out.println();

        printHeader("极限测试完成");
        System.out.println("  系统在以上测试中的表现已记录完毕。");
    }

    // ======================== 突发流量测试 ========================
    private static void testBurstTraffic() throws Exception {
        int concurrency = 300;
        int total = 3000;
        System.out.println("  并发: " + concurrency + " | 总请求: " + total);

        resetStats();
        testStartTime = System.nanoTime();

        // 使用 Semaphore 让所有线程同时开始
        Semaphore gate = new Semaphore(-concurrency + 1);
        CountDownLatch latch = new CountDownLatch(total);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                concurrency, concurrency, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(total));

        for (int i = 0; i < total; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    gate.acquire(); // 等待所有线程就绪
                    String token = userTokens.get(idx % userTokens.size());
                    long start = System.nanoTime();
                    doGet("/api/books/getAll?page=0&size=10", token);
                    long elapsed = System.nanoTime() - start;
                    recordLatency(elapsed);
                } catch (Exception e) {
                    recordFailure();
                } finally {
                    latch.countDown();
                }
            });
        }

        gate.release(concurrency); // 同时释放所有线程
        latch.await(60, TimeUnit.SECONDS);
        testEndTime = System.nanoTime();
        executor.shutdown();

        printStats("突发流量 GET /api/books/getAll");
        resetStats();
    }

    // ======================== DB 连接池耗尽测试 ========================
    private static void testDbPoolExhaustion() throws Exception {
        int concurrency = 500;
        int total = 5000;
        System.out.println("  并发: " + concurrency + " | 请求: " + total + " 次");

        // 先插入更多图书数据确保有足够的不同 ID
        int extraNeeded = Math.max(0, 100 - bookIds.size());
        if (extraNeeded > 0) {
            System.out.println("  补充 " + extraNeeded + " 本图书以获取更多数据...");
            for (int i = 0; i < extraNeeded; i++) {
                String isbn = "EXTREME-" + UUID.randomUUID().toString().substring(0, 8);
                String json = String.format(
                        "{\"title\":\"极端测试书_%s\",\"author\":\"作者\",\"isbn\":\"%s\",\"stock\":50,\"category\":\"极端测试\"}",
                        isbn, isbn);
                try {
                    String resp = doPost("/api/books/create", json, adminTokens.get(0));
                    JsonNode node = objectMapper.readTree(resp);
                    if (node.get("data") != null && node.get("data").get("id") != null) {
                        bookIds.add(node.get("data").get("id").asLong());
                    }
                } catch (Exception ignored) { }
            }
            System.out.println("  现有图书: " + bookIds.size() + " 本");
        }

        resetStats();
        testStartTime = System.nanoTime();

        // 高并发请求不同的图书 ID → 大量缓存未命中 → DB 连接池满载
        runConcurrentTest(concurrency, total, (index) -> {
            // 交替请求不同 ID，大部分是缓存未命中
            long id;
            if (index % 3 == 0) {
                id = 9999999L + index; // 不存在的 ID → 缓存未命中 → DB 查询失败
            } else {
                id = bookIds.get(ThreadLocalRandom.current().nextInt(bookIds.size()));
            }
            String token = userTokens.get(ThreadLocalRandom.current().nextInt(userTokens.size()));
            long start = System.nanoTime();
            doGet("/api/books/getById/" + id, token);
            long elapsed = System.nanoTime() - start;
            recordLatency(elapsed);
        });

        testEndTime = System.nanoTime();
        printStats("DB 连接池冲击 GET /api/books/getById");
        resetStats();
    }

    // ======================== 乐观锁极限竞争 ========================
    private static void testOptimisticLockExtreme() throws Exception {
        // 确保图书库存够
        Long targetBookId = bookIds.get(0);
        int userCount = Math.min(100, userTokens.size());
        System.out.println("  用户数: " + userCount + " | 目标图书ID: " + targetBookId);

        // 先恢复图书库存(直接通过 admin token 调用更新)
        String restoreJson = String.format(
                "{\"title\":\"极端测试借书\",\"author\":\"作者\",\"isbn\":\"RESTORE-%s\",\"stock\":%d}",
                UUID.randomUUID().toString().substring(0, 6), userCount + 50);
        try {
            doPut("/api/books/update/" + targetBookId, restoreJson, adminTokens.get(0));
        } catch (Exception e) {
            System.out.println("  ⚠ 恢复库存失败: " + e.getMessage());
        }

        Thread.sleep(500); // 等待缓存更新

        resetStats();
        testStartTime = System.nanoTime();

        AtomicInteger borrowOk = new AtomicInteger(0);
        AtomicInteger borrowFail = new AtomicInteger(0);

        runConcurrentTest(100, userCount, (index) -> {
            String token = userTokens.get(index % userTokens.size());
            String json = String.format("{\"bookId\":%d,\"borrowDays\":7}", targetBookId);

            long start = System.nanoTime();
            String resp = doPost("/api/books/borrow", json, token);
            long elapsed = System.nanoTime() - start;

            try {
                JsonNode node = objectMapper.readTree(resp);
                if (node.get("code").asInt() == 200) {
                    recordLatency(elapsed);
                    borrowOk.incrementAndGet();
                } else {
                    recordFailure();
                    borrowFail.incrementAndGet();
                }
            } catch (Exception e) {
                recordFailure();
                borrowFail.incrementAndGet();
            }
        });

        testEndTime = System.nanoTime();
        System.out.println("  借书成功: " + borrowOk.get() + " | 失败(乐观锁竞争): " + borrowFail.get());
        printStats("乐观锁竞争 POST /api/books/borrow");
        resetStats();
    }

    // ======================== 持续压力测试 ========================
    private static void testSustainedLoad() throws Exception {
        int concurrency = 200;
        long durationMs = 30_000; // 30秒
        System.out.println("  并发: " + concurrency + " | 持续时间: 30 秒");

        resetStats();
        testStartTime = System.nanoTime();

        AtomicBoolean running = new AtomicBoolean(true);
        CountDownLatch latch = new CountDownLatch(concurrency);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                concurrency, concurrency, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>());

        long deadline = System.currentTimeMillis() + durationMs;
        AtomicInteger reqCount = new AtomicInteger(0);

        for (int t = 0; t < concurrency; t++) {
            executor.submit(() -> {
                try {
                    while (System.currentTimeMillis() < deadline && running.get()) {
                        String token = userTokens.get(ThreadLocalRandom.current().nextInt(userTokens.size()));
                        int choice = ThreadLocalRandom.current().nextInt(5);
                        long start = System.nanoTime();

                        try {
                            if (choice == 0) {
                                // 20% 分页查询
                                doGet("/api/books/getAll?page=0&size=10", token);
                            } else if (choice < 3) {
                                // 40% 按ID查询
                                Long id = bookIds.get(ThreadLocalRandom.current().nextInt(bookIds.size()));
                                doGet("/api/books/getById/" + id, token);
                            } else if (choice < 4) {
                                // 20% 借书
                                Long id = bookIds.get(ThreadLocalRandom.current().nextInt(bookIds.size()));
                                String json = String.format("{\"bookId\":%d,\"borrowDays\":7}", id);
                                doPost("/api/books/borrow", json, token);
                            } else {
                                // 20% 搜索
                                doGet("/api/books/searchByTitle?title=压力测试&page=0&size=10", token);
                            }
                            recordLatency(System.nanoTime() - start);
                            reqCount.incrementAndGet();
                        } catch (Exception e) {
                            recordFailure();
                        }
                    }
                } catch (Exception e) {
                    // 线程中断
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(durationMs + 5000, TimeUnit.MILLISECONDS);
        running.set(false);
        testEndTime = System.nanoTime();
        executor.shutdownNow();

        double elapsedSec = (testEndTime - testStartTime) / 1_000_000_000.0;
        int total = reqCount.get();
        System.out.println("  总请求: " + total + " | 耗时: " + String.format("%.1f", elapsedSec) + "s");
        System.out.println("  平均 QPS: " + String.format("%.0f", total / elapsedSec));
        printStats("持续压力 30s");
        resetStats();
    }

    // ======================== 极端混合负载 ========================
    private static void testExtremeMixed() throws Exception {
        int concurrency = 150;
        int total = 2000;
        System.out.println("  并发: " + concurrency + " | 请求: " + total);

        resetStats();
        testStartTime = System.nanoTime();

        List<Long> tempRecords = new CopyOnWriteArrayList<>();

        runConcurrentTest(concurrency, total, (index) -> {
            String token = userTokens.get(ThreadLocalRandom.current().nextInt(userTokens.size()));
            long start = System.nanoTime();

            try {
                if (index % 5 == 0) {
                    // 20% 分页查询
                    doGet("/api/books/getAll?page=0&size=10", token);
                    recordLatency(System.nanoTime() - start);
                } else if (index % 5 < 3) {
                    // 40% getById (大部分命中缓存)
                    Long id = bookIds.get(ThreadLocalRandom.current().nextInt(bookIds.size()));
                    doGet("/api/books/getById/" + id, token);
                    recordLatency(System.nanoTime() - start);
                } else if (index % 5 < 4) {
                    // 20% 借书
                    Long id = bookIds.get(ThreadLocalRandom.current().nextInt(1, bookIds.size()));
                    String json = String.format("{\"bookId\":%d,\"borrowDays\":7}", id);
                    String resp = doPost("/api/books/borrow", json, token);
                    JsonNode node = objectMapper.readTree(resp);
                    if (node.get("code").asInt() == 200 && node.get("data") != null) {
                        Long recordId = node.get("data").get("id").asLong();
                        tempRecords.add(recordId);
                    }
                    recordLatency(System.nanoTime() - start);
                } else {
                    // 20% 还书
                    if (!tempRecords.isEmpty()) {
                        Long recordId = tempRecords.remove(ThreadLocalRandom.current().nextInt(tempRecords.size()));
                        doPost("/api/books/return/" + recordId, "{}", token);
                        recordLatency(System.nanoTime() - start);
                    } else {
                        // 没记录就查书
                        Long id = bookIds.get(ThreadLocalRandom.current().nextInt(bookIds.size()));
                        doGet("/api/books/getById/" + id, token);
                        recordLatency(System.nanoTime() - start);
                    }
                }
            } catch (Exception e) {
                recordFailure();
            }
        });

        testEndTime = System.nanoTime();
        printStats("极端混合负载");
        resetStats();
    }

    // ======================== 初始化 ========================
    private static void initTestData() throws Exception {
        // 注册用户 & 生成 JWT
        for (int i = 0; i < TEST_USER_COUNT; i++) {
            String username = "extreme_user_" + i;
            String phone = String.format("159%08d", i);
            String json = String.format(
                    "{\"username\":\"%s\",\"password\":\"%s\",\"phone\":\"%s\"}",
                    username, TEST_PASSWORD, phone);
            try { doPost("/api/users/register", json, null); } catch (Exception ignored) { }
            userTokens.add(generateJwtToken(username));
        }
        adminTokens.add(generateJwtToken("admin"));

        // 获取已有图书
        try {
            String resp = doGet("/api/books/getAll?page=0&size=200", adminTokens.get(0));
            JsonNode node = objectMapper.readTree(resp);
            for (JsonNode b : node.get("data").get("content")) {
                bookIds.add(b.get("id").asLong());
            }
        } catch (Exception ignored) { }

        // 创建补充图书
        if (bookIds.size() < TEST_BOOK_COUNT) {
            for (int i = bookIds.size(); i < TEST_BOOK_COUNT; i++) {
                String isbn = "EXTREME-" + UUID.randomUUID().toString().substring(0, 8);
                String json = String.format(
                        "{\"title\":\"极限测试图书_%d\",\"author\":\"极限作者\",\"isbn\":\"%s\",\"stock\":%d,\"category\":\"极限测试\"}",
                        i, isbn, BOOK_STOCK);
                try {
                    String resp = doPost("/api/books/create", json, adminTokens.get(0));
                    JsonNode node = objectMapper.readTree(resp);
                    if (node.get("data") != null && node.get("data").get("id") != null) {
                        bookIds.add(node.get("data").get("id").asLong());
                    }
                } catch (Exception ignored) { }
            }
        }
    }

    // ======================== 工具方法 ========================
    private static String doGet(String path, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15))
                .GET();
        if (token != null) builder.header("Authorization", "Bearer " + token);
        HttpResponse<String> resp = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return resp.body();
    }

    private static String doPost(String path, String json, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(json));
        if (token != null) builder.header("Authorization", "Bearer " + token);
        HttpResponse<String> resp = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return resp.body();
    }

    private static String doPut(String path, String json, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15))
                .PUT(HttpRequest.BodyPublishers.ofString(json));
        if (token != null) builder.header("Authorization", "Bearer " + token);
        HttpResponse<String> resp = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return resp.body();
    }

    private static SecretKey getSigningKey() {
        byte[] keyBytes = JWT_SECRET.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] paddedKey = new byte[32];
            System.arraycopy(keyBytes, 0, paddedKey, 0, keyBytes.length);
            return Keys.hmacShaKeyFor(paddedKey);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private static String generateJwtToken(String username) {
        return Jwts.builder()
                .subject(username)
                .claim("userId", 999L)
                .claim("username", username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + JWT_EXPIRE_MS))
                .signWith(getSigningKey())
                .compact();
    }

    // ======================== 并发测试框架 ========================
    private interface TestTask { void run(int index) throws Exception; }

    private static void runConcurrentTest(int concurrency, int total, TestTask task) throws Exception {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                concurrency, concurrency, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(total));
        CountDownLatch latch = new CountDownLatch(total);

        for (int i = 0; i < total; i++) {
            final int index = i;
            executor.submit(() -> {
                try { task.run(index); }
                catch (Exception e) { recordFailure(); }
                finally { latch.countDown(); }
            });
        }

        latch.await(120, TimeUnit.SECONDS);
        executor.shutdown();
    }

    // ======================== 统计 ========================
    private static synchronized void recordLatency(long nanos) {
        successCount.incrementAndGet();
        totalLatencyNanos.addAndGet(nanos);
        allLatencies.add(nanos);
    }

    private static synchronized void recordFailure() {
        failureCount.incrementAndGet();
    }

    private static void resetStats() {
        successCount.set(0);
        failureCount.set(0);
        totalLatencyNanos.set(0);
        allLatencies.clear();
    }

    private static void printStats(String label) {
        int total = successCount.get() + failureCount.get();
        if (total == 0) { System.out.println("  ⚠ 无有效数据"); return; }

        double elapsedSec = (testEndTime - testStartTime) / 1_000_000_000.0;
        long totalNs = totalLatencyNanos.get();
        List<Long> sorted = allLatencies.stream().sorted().collect(Collectors.toList());

        double avgMs = total == 0 ? 0 : (totalNs / (double) total) / 1_000_000.0;
        long minMs = sorted.isEmpty() ? 0 : sorted.get(0) / 1_000_000;
        long maxMs = sorted.isEmpty() ? 0 : sorted.get(sorted.size() - 1) / 1_000_000;
        double p50 = percentile(sorted, 50) / 1_000_000.0;
        double p90 = percentile(sorted, 90) / 1_000_000.0;
        double p99 = percentile(sorted, 99) / 1_000_000.0;
        double successRate = (successCount.get() * 100.0) / total;
        double qps = elapsedSec > 0 ? total / elapsedSec : 0;

        System.out.println("  ┌──────────────────────────────────────────┐");
        System.out.println("  │ " + padRight(label, 40) + " │");
        System.out.println("  ├──────────────────────────────────────────┤");
        System.out.println("  │ 总请求: " + padRight(total + "", 8) + " 成功: " + padRight(successCount.get() + "", 6) + " 失败: " + padRight(failureCount.get() + "", 6) + " │");
        System.out.println("  │ 成功率: " + padRight(String.format("%.2f%%", successRate), 8) + " QPS: " + padRight(String.format("%.0f", qps), 10) + "       │");
        System.out.println("  │ 耗时: " + padRight(String.format("%.2fs", elapsedSec), 8) + "                    │");
        System.out.println("  │ 延迟 (ms):                                 │");
        System.out.println("  │   平均: " + padRight(String.format("%.2f", avgMs), 8) + " 最小: " + padRight(minMs + "", 6) + " 最大: " + padRight(maxMs + "", 6) + "   │");
        System.out.println("  │   P50: " + padRight(String.format("%.2f", p50), 8) + " P90: " + padRight(String.format("%.2f", p90), 8) + " P99: " + padRight(String.format("%.2f", p99), 8) + "   │");
        System.out.println("  └──────────────────────────────────────────┘");
    }

    private static long percentile(List<Long> sorted, double p) {
        if (sorted.isEmpty()) return 0;
        int index = (int) Math.ceil(p / 100.0 * sorted.size()) - 1;
        index = Math.max(0, Math.min(index, sorted.size() - 1));
        return sorted.get(index);
    }

    private static String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }

    private static void printHeader(String title) {
        System.out.println("═══ " + title + " ═══");
    }
}
