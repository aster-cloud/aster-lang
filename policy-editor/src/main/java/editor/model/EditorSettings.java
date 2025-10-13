package editor.model;

import java.io.Serializable;

/**
 * 前端/代理相关设置
 */
public class EditorSettings implements Serializable {
    private static final long serialVersionUID = 1L;

    // GraphQL 端点（通常指向本服务的 /graphql，或直连后端 GraphQL 完整 URL）
    private String graphqlEndpoint;
    // HTTP 超时（毫秒）
    private int timeoutMillis;
    // 是否启用压缩（Accept-Encoding: gzip）
    private boolean compression;
    // GraphQL 查询缓存 TTL（毫秒），0 表示关闭缓存
    private int cacheTtlMillis = 3000;
    // 远端仓库目录（用于本地文件夹同步）
    private String remoteRepoDir;
    // 当前用户名称（用于审计 actor）
    private String userName = "admin";

    public EditorSettings() {}

    public EditorSettings(String graphqlEndpoint, int timeoutMillis, boolean compression) {
        this.graphqlEndpoint = graphqlEndpoint;
        this.timeoutMillis = timeoutMillis;
        this.compression = compression;
    }

    public String getGraphqlEndpoint() { return graphqlEndpoint; }
    public void setGraphqlEndpoint(String graphqlEndpoint) { this.graphqlEndpoint = graphqlEndpoint; }

    public int getTimeoutMillis() { return timeoutMillis; }
    public void setTimeoutMillis(int timeoutMillis) { this.timeoutMillis = timeoutMillis; }

    public boolean isCompression() { return compression; }
    public void setCompression(boolean compression) { this.compression = compression; }

    public int getCacheTtlMillis() { return cacheTtlMillis; }
    public void setCacheTtlMillis(int cacheTtlMillis) { this.cacheTtlMillis = cacheTtlMillis; }

    public String getRemoteRepoDir() { return remoteRepoDir; }
    public void setRemoteRepoDir(String remoteRepoDir) { this.remoteRepoDir = remoteRepoDir; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
}
