package editor.websocket;

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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * LSP WebSocket 桥接：将浏览器端 WebSocket 消息转发到 Node 版 Aster LSP 服务器（stdio）。
 */
@ServerEndpoint("/ws/lsp")
@ApplicationScoped
public class LSPWebSocketEndpoint {

    private static final Logger LOG = Logger.getLogger(LSPWebSocketEndpoint.class);
    private static final String HEADER_PREFIX = "Content-Length:";

    private final Map<String, LspSessionBridge> bridges = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @OnOpen
    public void onOpen(Session session) {
        try {
            LspSessionBridge bridge = new LspSessionBridge(session, executor);
            bridge.start();
            bridges.put(session.getId(), bridge);
            LOG.infov("LSP WebSocket connected: {0}", session.getId());
        } catch (Exception e) {
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
        }
        LOG.infov("LSP WebSocket closed: {0} reason={1}", session.getId(), reason);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        LOG.errorf(throwable, "LSP WebSocket error for session %s", session != null ? session.getId() : "unknown");
        if (session != null) {
            LspSessionBridge bridge = bridges.remove(session.getId());
            if (bridge != null) {
                bridge.shutdown();
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
        private final Session session;
        private final Process process;
        private final OutputStream serverInput;
        private final InputStream serverOutput;
        private final InputStream serverError;
        private final ExecutorService executor;
        private final AtomicBoolean running = new AtomicBoolean(true);

        LspSessionBridge(Session session, ExecutorService executor) throws IOException {
            this.session = session;
            this.executor = executor;
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
            process.destroy();
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
