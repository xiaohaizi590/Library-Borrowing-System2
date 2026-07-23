package net.togogo;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 高并发压力测试
 * <p>
 * 本测试绕过验证码流程，直接生成 JWT Token 进行测试。
 * 测试前请确保:
 * 1. 服务器已启动 (localhost:8081)
 * 2. MySQL 和 Redis 已启动
 * 3. Nacos 已启动 (如使用)
 * <p>
 * 测试场景:
 * - 并发读取测试 (GET /api/books/getAll, GET /api/books/getById)
 * - 并发借书测试 (POST /api/books/borrow) — 测试 @Version 乐观锁
 * - 并发还书测试 (POST /api/books/return/{id})
 * - 混合负载测试 (读写混合)
 * <p>
 * 输出指标: QPS, 平均/最小/最大延迟, P50/P90/P99 延迟, 成功率
 */
public class StressTest {

    // ======================== 配置参数 ========================
    private static final String BASE_URL = "http://localhost:8081";
    private static final String JWT_SECRET = "mySecretKeyForJwtTokenMustBeLongEnough123456";
    private static final long JWT_EXPIRE_MS = 86_400_000L; // 24h

    // 并发测试参数
    private static final int READ_CONCURRENCY = 50;         // 读取并发数
    private static final int READ_TOTAL = 2000;              // 读取总请求数
    private static final int WRITE_CONCURRENCY = 30;         // 写入并发数
    private static final int WARMUP_COUNT = 100;             // 预热请求数

    // 测试数据
    private static final int TEST_USER_COUNT = 30;           // 测试用户数
    private static final String TEST_PASSWORD = "123456";
    private static final int BOOK_STOCK = 50;                // 测试图书库存
    private static final int TEST_BOOK_COUNT = 10;           // 测试图书数量

    // ======================== 运行时状态 ========================
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 用户 token 池
    private static final List<String> userTokens = new CopyOnWriteArrayList<>();
    private static final List<String> adminTokens = new CopyOnWriteArrayList<>();

    // 测试图书 ID
    private static final List<Long> bookIds = new CopyOnWriteArrayList<>();
    // 成功创建的借阅记录 ID
    private static final List<Long> borrowRecordIds = new CopyOnWriteArrayList<>();

    // 统计
    private static final AtomicLong totalLatencyNanos = new AtomicLong(0);
    private static final AtomicLong totalLatencyNanosWarmup = new AtomicLong(0);
    private static final AtomicInteger successCount = new AtomicInteger(0);
    private static final AtomicInteger failureCount = new AtomicInteger(0);
    private static final ThreadLocal<List<Long>> latencyList = ThreadLocal.withInitial(ArrayList::new);
    private static final List<Long> allLatenciesWarmup = new CopyOnWriteArrayList<>();
    private static final List<Long> allLatencies = new CopyOnWriteArrayList<>();

    // ======================== 主流程 ========================
    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║       图书借阅系统 - 高并发压力测试           ║");
        System.out.println("╠══════════════════════════════════════════════╣");
        System.out.println("║ 目标服务器: " + BASE_URL);
        System.out.println("║ 读取并发: " + READ_CONCURRENCY + " | 写入并发: " + WRITE_CONCURRENCY);
        System.out.println("╚══════════════════════════════════════════════╝");
        System.out.println();

        // 1. 初始化测试数据
        printHeader("初始化测试数据");
        initTestData();
        System.out.println("  ✓ 测试用户: " + TEST_USER_COUNT + " 个");
        System.out.println("  ✓ 测试图书: " + bookIds.size() + " 本 (每本库存 " + BOOK_STOCK + ")");
        System.out.println("  ✓ JWT Token: " + userTokens.size() + " 个可用");
        System.out.println();

        // 2. 预热
        printHeader("❄ 系统预热 (" + WARMUP_COUNT + " 次请求)");
        warmup();
        System.out.println("  ✓ 预热完成");
        System.out.println();

        // 3. 并发读取测试
        printHeader("测试 1: 并发读取测试");
        testConcurrentRead();
        System.out.println();

        // 4. 并发借书测试
        printHeader("测试 2: 并发借书测试 (乐观锁竞争)");
        testConcurrentBorrow();
        System.out.println();

        // 5. 并发还书测试
        printHeader("测试 3: 并发还书测试");
        testConcurrentReturn();
        System.out.println();

        // 6. 混合负载测试
        printHeader("测试 4: 混合负载测试 (读写混合)");
        testMixedWorkload();
        System.out.println();

        // 7. 最终报告
        printHeader("最终测试报告");
        System.out.println("  所有测试完成！");
    }

    // ======================== 初始化 ========================
    private static void initTestData() throws Exception {
        // 注册测试用户并生成 JWT Token
        for (int i = 1; i <= TEST_USER_COUNT; i++) {
            String username = "stress_user_" + i;
            String phone = String.format("139%08d", i);
            String json = String.format(
                    "{\"username\":\"%s\",\"password\":\"%s\",\"phone\":\"%s\"}",
                    username, TEST_PASSWORD, phone);

            String resp = doPost("/api/users/register", json, null);
            // 忽略重复注册的错误

            String token = generateJwtToken(username);
            userTokens.add(token);
        }

        // 使用已存在的 admin 账号（由 DataInitializer 创建）
        adminTokens.add(generateJwtToken("admin"));

        // 尝试获取已有测试图书，没有再创建
        try {
            String resp = doGet("/api/books/getAll?page=0&size=" + TEST_BOOK_COUNT, adminTokens.get(0));
            JsonNode node = objectMapper.readTree(resp);
            JsonNode content = node.get("data").get("content");
            if (content != null && content.size() > 0) {
                for (JsonNode book : content) {
                    bookIds.add(book.get("id").asLong());
                }
                System.out.println("  (使用已有图书 " + bookIds.size() + " 本)");
            }
        } catch (Exception ignored) { }

        // 如果已有图书不够，创建缺失的
        if (bookIds.size() < TEST_BOOK_COUNT) {
            for (int i = bookIds.size() + 1; i <= TEST_BOOK_COUNT; i++) {
                String isbn = "STRESS-TEST-" + String.format("%04d", i) + "-" + System.currentTimeMillis();
                String json = String.format(
                        "{\"title\":\"压力测试图书_%d\",\"author\":\"测试作者\",\"isbn\":\"%s\",\"stock\":%d,\"category\":\"测试\"}",
                        i, isbn, BOOK_STOCK);
                try {
                    String resp = doPost("/api/books/create", json, adminTokens.get(0));
                    JsonNode node = objectMapper.readTree(resp);
                    if (node.get("data") != null && node.get("data").get("id") != null) {
                        Long bookId = node.get("data").get("id").asLong();
                        bookIds.add(bookId);
                    }
                } catch (Exception e) {
                    System.out.println("  ⚠ 创建图书 " + i + " 失败: " + e.getMessage());
                }
            }
        }
    }

    // ======================== 预热 ========================
    private static void warmup() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(READ_CONCURRENCY);
        CountDownLatch latch = new CountDownLatch(WARMUP_COUNT);
        AtomicInteger done = new AtomicInteger(0);

        for (int i = 0; i < WARMUP_COUNT; i++) {
            executor.submit(() -> {
                try {
                    int idx = done.getAndIncrement() % bookIds.size();
                    Long bookId = bookIds.isEmpty() ? 1L : bookIds.get(Math.max(0, idx));
                    String token = userTokens.get(ThreadLocalRandom.current().nextInt(userTokens.size()));

                    long start = System.nanoTime();
                    doGet("/api/books/getById/" + bookId, token);
                    long elapsed = System.nanoTime() - start;

                    totalLatencyNanosWarmup.addAndGet(elapsed);
                    allLatenciesWarmup.add(elapsed);
                } catch (Exception e) {
                    // 预热允许失败
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        long avgNs = allLatenciesWarmup.isEmpty() ? 0 : totalLatencyNanosWarmup.get() / allLatenciesWarmup.size();
        System.out.println("  ✓ 预热完成, 平均延迟: " + (avgNs / 1_000_000) + " ms");
    }

    // ======================== 并发读取测试 ========================
    private static void testConcurrentRead() throws Exception {
        System.out.println("  [GET /api/books/getAll] 并发 " + READ_CONCURRENCY + ", 请求 " + READ_TOTAL + " 次");
        runConcurrentTest(READ_CONCURRENCY, READ_TOTAL, (index) -> {
            String token = userTokens.get(ThreadLocalRandom.current().nextInt(userTokens.size()));
            long start = System.nanoTime();
            doGet("/api/books/getAll?page=0&size=10", token);
            recordLatency(System.nanoTime() - start);
        });
        printStats("GET /api/books/getAll");

        System.out.println();

        // getById 测试
        System.out.println("  [GET /api/books/getById] 并发 " + READ_CONCURRENCY + ", 请求 " + READ_TOTAL + " 次");
        resetStats();
        runConcurrentTest(READ_CONCURRENCY, READ_TOTAL, (index) -> {
            Long bookId = bookIds.get(index % bookIds.size());
            String token = userTokens.get(ThreadLocalRandom.current().nextInt(userTokens.size()));
            long start = System.nanoTime();
            doGet("/api/books/getById/" + bookId, token);
            recordLatency(System.nanoTime() - start);
        });
        printStats("GET /api/books/getById");
        resetStats();
    }

    // ======================== 并发借书测试 ========================
    private static void testConcurrentBorrow() throws Exception {
        System.out.println("  [POST /api/books/borrow] 并发 " + WRITE_CONCURRENCY + " 个用户同时借阅同一本书");
        System.out.println("  目标: 测试 @Version 乐观锁在高并发下的表现");

        Long targetBookId = bookIds.get(0);
        // 使用 TEST_USER_COUNT 个用户并发借同一本书
        int borrowCount = Math.min(TEST_USER_COUNT, BOOK_STOCK);

        runConcurrentTest(WRITE_CONCURRENCY, borrowCount, (index) -> {
            String token = userTokens.get(index % userTokens.size());
            String json = String.format("{\"bookId\":%d,\"borrowDays\":7}", targetBookId);

            long start = System.nanoTime();
            String resp = doPost("/api/books/borrow", json, token);
            long elapsed = System.nanoTime() - start;

            try {
                JsonNode node = objectMapper.readTree(resp);
                int code = node.get("code").asInt();
                if (code == 200) {
                    recordLatency(elapsed);
                    Long recordId = node.get("data").get("id").asLong();
                    borrowRecordIds.add(recordId);
                } else {
                    recordFailure();
                }
            } catch (Exception e) {
                recordFailure();
            }
        });

        System.out.println("  ✓ 借书成功: " + borrowRecordIds.size() + " / " + borrowCount);
        printStats("POST /api/books/borrow");
        resetStats();
    }

    // ======================== 并发还书测试 ========================
    private static void testConcurrentReturn() throws Exception {
        System.out.println("  [POST /api/books/return/{id}] 并发 " + WRITE_CONCURRENCY + " 个用户同时还书");
        System.out.println("  使用 " + borrowRecordIds.size() + " 条借阅记录进行还书测试");

        int returnTotal = borrowRecordIds.size();
        if (returnTotal == 0) {
            System.out.println("  ⚠ 没有借阅记录可归还，跳过还书测试");
            return;
        }

        runConcurrentTest(WRITE_CONCURRENCY, returnTotal, (index) -> {
            Long recordId = borrowRecordIds.get(index);
            String token = userTokens.get(index % userTokens.size());

            long start = System.nanoTime();
            doPost("/api/books/return/" + recordId, "{}", token);
            recordLatency(System.nanoTime() - start);
        });

        printStats("POST /api/books/return/{id}");
        resetStats();
    }

    // ======================== 混合负载测试 ========================
    private static void testMixedWorkload() throws Exception {
        int totalRequests = 500;
        System.out.println("  混合负载: 40%读取 + 30%借书 + 30%还书, 共 " + totalRequests + " 次请求");
        System.out.println("  并发: " + WRITE_CONCURRENCY);

        // 创建更多借阅记录供混合测试使用
        List<Long> mixedRecordIds = new CopyOnWriteArrayList<>(borrowRecordIds);
        AtomicInteger readCount = new AtomicInteger(0);
        AtomicInteger borrowCount = new AtomicInteger(0);
        AtomicInteger returnCount = new AtomicInteger(0);

        runConcurrentTest(WRITE_CONCURRENCY, totalRequests, (index) -> {
            long start = System.nanoTime();
            String token = userTokens.get(ThreadLocalRandom.current().nextInt(userTokens.size()));
            int choice = ThreadLocalRandom.current().nextInt(10);

            try {
                if (choice < 4) {
                    // 40% 读取
                    Long bookId = bookIds.get(ThreadLocalRandom.current().nextInt(bookIds.size()));
                    doGet("/api/books/getById/" + bookId, token);
                    readCount.incrementAndGet();
                } else if (choice < 7) {
                    // 30% 借书
                    Long bookId = bookIds.get(ThreadLocalRandom.current().nextInt(1, bookIds.size()));
                    String json = String.format("{\"bookId\":%d,\"borrowDays\":7}", bookId);
                    String resp = doPost("/api/books/borrow", json, token);
                    JsonNode node = objectMapper.readTree(resp);
                    if (node.get("code").asInt() == 200 && node.get("data") != null) {
                        Long recordId = node.get("data").get("id").asLong();
                        mixedRecordIds.add(recordId);
                    }
                    borrowCount.incrementAndGet();
                } else {
                    // 30% 还书
                    if (!mixedRecordIds.isEmpty()) {
                        Long recordId = mixedRecordIds.remove(ThreadLocalRandom.current().nextInt(mixedRecordIds.size()));
                        doPost("/api/books/return/" + recordId, "{}", token);
                        returnCount.incrementAndGet();
                    } else {
                        // 没有记录则改为读取
                        Long bookId = bookIds.get(ThreadLocalRandom.current().nextInt(bookIds.size()));
                        doGet("/api/books/getById/" + bookId, token);
                        readCount.incrementAndGet();
                    }
                }
                recordLatency(System.nanoTime() - start);
            } catch (Exception e) {
                recordFailure();
            }
        });

        System.out.println("  ✓ 读取: " + readCount.get() + " | 借书: " + borrowCount.get() + " | 还书: " + returnCount.get());
        printStats("Mixed Workload");
        resetStats();
    }

    // ======================== 并发测试框架 ========================
    private interface TestTask {
        void run(int index) throws Exception;
    }

    private static void runConcurrentTest(int concurrency, int totalRequests, TestTask task) throws Exception {
        successCount.set(0);
        failureCount.set(0);
        totalLatencyNanos.set(0);
        allLatencies.clear();

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                concurrency, concurrency, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(totalRequests));
        CountDownLatch latch = new CountDownLatch(totalRequests);

        for (int i = 0; i < totalRequests; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    task.run(index);
                    if (latencyList.get().isEmpty()) {
                        // 如果task里没调recordLatency，说明它自己处理了统计
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(60, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // 收集线程本地的延迟数据
        // (already recorded in allLatencies via recordLatency)
    }

    // ======================== HTTP 请求封装 ========================
    private static String doGet(String path, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .GET();

        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }

        HttpResponse<String> resp = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) {
            throw new RuntimeException("HTTP " + resp.statusCode() + ": " + resp.body());
        }
        return resp.body();
    }

    private static String doPost(String path, String json, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(json));

        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }

        HttpResponse<String> resp = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 500) {
            throw new RuntimeException("HTTP " + resp.statusCode() + ": " + resp.body());
        }
        return resp.body();
    }

    // ======================== JWT Token 生成 ========================
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
        Date now = new Date();
        Date expiration = new Date(now.getTime() + JWT_EXPIRE_MS);
        return Jwts.builder()
                .subject(username)
                .claim("userId", 999L)
                .claim("username", username)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey())
                .compact();
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
        if (total == 0) {
            System.out.println("  ⚠ 无有效数据");
            return;
        }

        long totalNs = totalLatencyNanos.get();
        List<Long> sorted = allLatencies.stream().sorted().collect(Collectors.toList());

        double avgMs = total == 0 ? 0 : (totalNs / (double) total) / 1_000_000.0;
        long minMs = sorted.isEmpty() ? 0 : sorted.get(0) / 1_000_000;
        long maxMs = sorted.isEmpty() ? 0 : sorted.get(sorted.size() - 1) / 1_000_000;
        double p50 = percentile(sorted, 50) / 1_000_000.0;
        double p90 = percentile(sorted, 90) / 1_000_000.0;
        double p99 = percentile(sorted, 99) / 1_000_000.0;

        // QPS
        // 估计总耗时: 用最大延迟作为保守估计 (实际应用中应该用实际经过时间)
        double qps = 0;
        if (maxMs > 0) {
            double timeSeconds = (maxMs / 1000.0);
            qps = total / Math.max(timeSeconds, 0.001);
        }

        double successRate = (successCount.get() * 100.0) / total;

        System.out.println("  ┌─────── 统计: " + label + " ───────┐");
        System.out.println("  │ 总请求: " + total + " | 成功: " + successCount.get() + " | 失败: " + failureCount.get());
        System.out.println("  │ 成功率: " + String.format("%.2f", successRate) + "%");
        System.out.println("  │ QPS: " + String.format("%.0f", qps) + " req/s");
        System.out.println("  │ 延迟 (ms):");
        System.out.println("  │   平均: " + String.format("%.2f", avgMs));
        System.out.println("  │   最小: " + minMs + " | 最大: " + maxMs);
        System.out.println("  │   P50: " + String.format("%.2f", p50));
        System.out.println("  │   P90: " + String.format("%.2f", p90));
        System.out.println("  │   P99: " + String.format("%.2f", p99));
        System.out.println("  └──────────────────────────────────┘");
    }

    private static long percentile(List<Long> sorted, double percentile) {
        if (sorted.isEmpty()) return 0;
        int index = (int) Math.ceil(percentile / 100.0 * sorted.size()) - 1;
        index = Math.max(0, Math.min(index, sorted.size() - 1));
        return sorted.get(index);
    }

    private static void printHeader(String title) {
        int width = 56;
        int padding = (width - title.length() - 2) / 2;
        String leftPad = "═".repeat(Math.max(0, padding));
        String rightPad = "═".repeat(Math.max(0, width - padding - title.length() - 2));
        System.out.println("  " + leftPad + " " + title + " " + rightPad);
    }
}
