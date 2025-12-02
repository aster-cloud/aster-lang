package editor.websocket;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import org.jboss.logging.Logger;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.microprofile.config.ConfigProvider;

/**
 * LSP WebSocket 桥接：将浏览器端 WebSocket 消息转发到 Node 版 Aster LSP 服务器（stdio）。
 */
@ServerEndpoint("/ws/lsp")
@ApplicationScoped
public class LSPWebSocketEndpoint {

    private static final Logger LOG = Logger.getLogger(LSPWebSocketEndpoint.class);
    private static final String HEADER_PREFIX = "Content-Length:";

    /** 活跃连接计数器（线程安全） */
    private static final AtomicInteger activeConnections = new AtomicInteger(0);

    private final Map<String, LspSessionBridge> bridges = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    /** 从配置获取最大并发连接数 */
    private static int getMaxConnections() {
        return ConfigProvider.getConfig()
            .getOptionalValue("lsp.max.concurrent.connections", Integer.class)
            .orElse(10);
    }

    /** 从配置获取关闭超时时间（秒） */
    private static int getShutdownTimeoutSeconds() {
        return ConfigProvider.getConfig()
            .getOptionalValue("lsp.shutdown.timeout.seconds", Integer.class)
            .orElse(5);
    }

    /** 获取当前活跃连接数（供监控使用） */
    public static int getActiveConnectionCount() {
        return activeConnections.get();
    }

    /** 应用关闭时清理 ExecutorService */
    @PreDestroy
    void shutdown() {
        LOG.info("正在关闭 LSP WebSocket 端点...");
        // 关闭所有活跃的桥接，并显式关闭 WebSocket 会话
        bridges.forEach((sessionId, bridge) -> {
            try {
                // 先关闭 WebSocket 会话，通知浏览器端
                bridge.closeSession(new CloseReason(
                    CloseReason.CloseCodes.GOING_AWAY,
                    "LSP 服务器正在关闭"
                ));
            } catch (IOException e) {
                LOG.warnv("关闭 WebSocket 会话失败: {0}", sessionId);
            }
            // 然后关闭 LSP 进程
            bridge.shutdown();
        });
        bridges.clear();
        // 重置连接计数器
        activeConnections.set(0);
        // 关闭线程池并等待完成
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                LOG.warn("线程池未能在超时内优雅关闭，已强制终止");
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        LOG.info("LSP WebSocket 端点已关闭");
    }

    @OnOpen
    public void onOpen(Session session) {
        int maxConnections = getMaxConnections();
        int current = activeConnections.incrementAndGet();

        // 检查连接限制
        if (current > maxConnections) {
            activeConnections.decrementAndGet();
            LOG.warnv("LSP 连接被拒绝，当前: {0}, 最大: {1}, sessionId: {2}",
                current - 1, maxConnections, session.getId());
            try {
                session.close(new CloseReason(
                    CloseReason.CloseCodes.TRY_AGAIN_LATER,
                    "LSP 服务器已达连接上限: " + maxConnections
                ));
            } catch (IOException e) {
                LOG.error("关闭超限连接失败", e);
            }
            return;
        }

        try {
            LspSessionBridge bridge = new LspSessionBridge(session, executor, getShutdownTimeoutSeconds());
            bridge.start();
            bridges.put(session.getId(), bridge);
            LOG.infov("LSP WebSocket 已连接: {0}, 活跃连接数: {1}/{2}",
                session.getId(), current, maxConnections);
        } catch (Exception e) {
            activeConnections.decrementAndGet();
            LOG.error("无法启动 LSP 服务器", e);
            closeSession(session, "LSP 初始化失败: " + e.getMessage());
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        LspSessionBridge bridge = bridges.get(session.getId());
        if (bridge != null) {
            bridge.forwardClientMessage(message);
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        LspSessionBridge bridge = bridges.remove(session.getId());
        if (bridge != null) {
            bridge.shutdown();
            int remaining = activeConnections.decrementAndGet();
            LOG.infov("LSP WebSocket 已关闭: {0}, 原因: {1}, 剩余连接数: {2}",
                session.getId(), reason, remaining);
        } else {
            // 连接可能在 onOpen 中被拒绝，此时不需要减少计数
            LOG.infov("LSP WebSocket 关闭（无桥接）: {0}, 原因: {1}", session.getId(), reason);
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        LOG.errorf(throwable, "LSP WebSocket 错误: %s", session != null ? session.getId() : "unknown");
        if (session != null) {
            LspSessionBridge bridge = bridges.remove(session.getId());
            if (bridge != null) {
                bridge.shutdown();
                activeConnections.decrementAndGet();
            }
            closeSession(session, "LSP 通道异常: " + throwable.getMessage());
        }
    }

    private void closeSession(Session session, String message) {
        try {
            session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, message));
        } catch (IOException ioe) {
            LOG.error("关闭 LSP 会话失败", ioe);
        }
    }

    private static Path resolveServerScript() {
        String configured = Optional.ofNullable(System.getProperty("aster.lsp.server.path"))
            .filter(path -> !path.isBlank())
            .orElseGet(() -> Optional.ofNullable(System.getenv("ASTER_LSP_SERVER_PATH")).orElse(""));
        if (!configured.isBlank()) {
            Path target = Paths.get(configured).toAbsolutePath().normalize();
            if (Files.exists(target)) {
                return target;
            }
            throw new IllegalStateException("配置的 LSP 路径不存在: " + target);
        }

        List<Path> defaults = List.of(
            Paths.get("dist/src/lsp/server.js"),
            Paths.get("../dist/src/lsp/server.js"),
            Paths.get("../../dist/src/lsp/server.js")
        );
        for (Path candidate : defaults) {
            Path resolved = candidate.toAbsolutePath().normalize();
            if (Files.exists(resolved)) {
                return resolved;
            }
        }
        throw new IllegalStateException("无法定位 dist/src/lsp/server.js，请先执行 npm run build");
    }

    private static final class LspSessionBridge {
        private static final Logger BRIDGE_LOG = Logger.getLogger(LspSessionBridge.class);
        private static final String LSP_SHUTDOWN_REQUEST = "{\"jsonrpc\":\"2.0\",\"method\":\"shutdown\",\"id\":1}";

        private final Session session;
        private final Process process;
        private final OutputStream serverInput;
        private final InputStream serverOutput;
        private final InputStream serverError;
        private final ExecutorService executor;
        private final AtomicBoolean running = new AtomicBoolean(true);
        private final int shutdownTimeoutSeconds;

        LspSessionBridge(Session session, ExecutorService executor, int shutdownTimeoutSeconds) throws IOException {
            this.session = session;
            this.executor = executor;
            this.shutdownTimeoutSeconds = shutdownTimeoutSeconds;
            Path script = resolveServerScript();
            ProcessBuilder builder = new ProcessBuilder("node", script.toString());
            Path repoRoot = script.toAbsolutePath().normalize();
            for (int i = 0; i < 4 && repoRoot.getParent() != null; i++) {
                repoRoot = repoRoot.getParent();
            }
            builder.directory(repoRoot.toFile());
            builder.redirectErrorStream(false);
            this.process = builder.start();
            this.serverInput = process.getOutputStream();
            this.serverOutput = new BufferedInputStream(process.getInputStream());
            this.serverError = new BufferedInputStream(process.getErrorStream());
        }

        void start() {
            executor.submit(this::pumpStdout);
            executor.submit(this::pumpStderr);
        }

        /** 关闭 WebSocket 会话（供 @PreDestroy 调用） */
        void closeSession(CloseReason reason) throws IOException {
            if (session.isOpen()) {
                session.close(reason);
            }
        }

        void forwardClientMessage(String message) {
            if (!running.get()) {
                return;
            }
            try {
                byte[] payload = message.getBytes(StandardCharsets.UTF_8);
                String header = HEADER_PREFIX + " " + payload.length + "\r\n\r\n";
                synchronized (serverInput) {
                    serverInput.write(header.getBytes(StandardCharsets.UTF_8));
                    serverInput.write(payload);
                    serverInput.flush();
                }
            } catch (IOException e) {
                LOG.error("写入 LSP 进程失败", e);
                shutdown();
            }
        }

        void shutdown() {
            if (!running.compareAndSet(true, false)) {
                return;
            }

            // 优雅关闭：先发送 LSP shutdown 请求
            if (process.isAlive()) {
                try {
                    sendShutdownRequest();
                    BRIDGE_LOG.debug("已发送 LSP shutdown 请求，等待进程退出...");

                    // 等待进程优雅退出
                    boolean exited = process.waitFor(shutdownTimeoutSeconds, TimeUnit.SECONDS);
                    if (exited) {
                        BRIDGE_LOG.debug("LSP 进程已优雅退出");
                    } else {
                        BRIDGE_LOG.warn("LSP 进程未响应 shutdown 请求，强制终止");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    BRIDGE_LOG.warn("等待 LSP 关闭被中断");
                } catch (IOException e) {
                    BRIDGE_LOG.warnf("发送 shutdown 请求失败: %s", e.getMessage());
                }
            }

            // 关闭流
            try {
                serverInput.close();
            } catch (IOException ignored) {
            }
            try {
                serverOutput.close();
            } catch (IOException ignored) {
            }
            try {
                serverError.close();
            } catch (IOException ignored) {
            }

            // 强制终止进程（如果仍在运行）
            if (process.isAlive()) {
                process.destroyForcibly();
                BRIDGE_LOG.debug("LSP 进程已强制终止");
            }
        }

        /**
         * 发送 LSP shutdown 请求（JSON-RPC 2.0 格式）
         */
        private void sendShutdownRequest() throws IOException {
            byte[] payload = LSP_SHUTDOWN_REQUEST.getBytes(StandardCharsets.UTF_8);
            String header = HEADER_PREFIX + " " + payload.length + "\r\n\r\n";
            synchronized (serverInput) {
                serverInput.write(header.getBytes(StandardCharsets.UTF_8));
                serverInput.write(payload);
                serverInput.flush();
            }
        }

        private void pumpStdout() {
            try {
                while (running.get()) {
                    int length = readContentLength(serverOutput);
                    if (length < 0) {
                        break;
                    }
                    if (length == 0) {
                        continue;
                    }
                    byte[] payload = serverOutput.readNBytes(length);
                    if (payload.length < length) {
                        break;
                    }
                    String json = new String(payload, StandardCharsets.UTF_8);
                    session.getAsyncRemote().sendText(json);
                }
            } catch (IOException e) {
                if (running.get()) {
                    LOG.error("读取 LSP 输出失败", e);
                }
            } finally {
                shutdown();
            }
        }

        private void pumpStderr() {
            try {
                byte[] buffer = new byte[1024];
                int read;
                while (running.get() && (read = serverError.read(buffer)) != -1) {
                    String text = new String(buffer, 0, read, StandardCharsets.UTF_8);
                    LOG.warnf("[LSP stderr] %s", text.trim());
                }
            } catch (IOException e) {
                if (running.get()) {
                    LOG.error("读取 LSP 错误输出失败", e);
                }
            }
        }

        private int readContentLength(InputStream in) throws IOException {
            String line;
            int length = -1;
            while ((line = readLine(in)) != null) {
                if (line.isEmpty()) {
                    break;
                }
                String lower = line.toLowerCase();
                if (lower.startsWith(HEADER_PREFIX.toLowerCase())) {
                    length = Integer.parseInt(line.substring(HEADER_PREFIX.length()).trim());
                }
            }
            return length;
        }

        private String readLine(InputStream in) throws IOException {
            StringBuilder builder = new StringBuilder();
            int ch;
            while ((ch = in.read()) != -1) {
                if (ch == '\r') {
                    continue;
                }
                if (ch == '\n') {
                    break;
                }
                builder.append((char) ch);
            }
            if (ch == -1 && builder.length() == 0) {
                return null;
            }
            return builder.toString();
        }
    }
}
