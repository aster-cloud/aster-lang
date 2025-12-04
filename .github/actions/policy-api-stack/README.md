# Policy API Stack Action

启动或停止 Policy API 及其依赖服务的复合 Action。

## 前置条件

使用 `action: start` 前，请确保：

1. **GHCR 登录**：如果使用 GHCR 私有镜像，调用者必须先完成登录
   ```yaml
   - uses: docker/login-action@v3
     with:
       registry: ghcr.io
       username: ${{ github.actor }}
       password: ${{ secrets.GITHUB_TOKEN }}
   ```

2. **docker-compose.yml**：确保仓库根目录存在有效的 docker compose 配置文件

## 输入参数

| 参数 | 必需 | 默认值 | 说明 |
|------|------|--------|------|
| `action` | 是 | - | 操作类型：`start` 或 `stop` |
| `image` | 否 | `''` | Policy API 镜像地址（如 `ghcr.io/owner/policy-api:tag`） |
| `timeout` | 否 | `60` | 健康检查超时时间（秒） |
| `port` | 否 | `8081` | Policy API 端口 |

## 使用示例

### 启动服务

```yaml
- name: Login to GHCR
  uses: docker/login-action@v3
  with:
    registry: ghcr.io
    username: ${{ github.actor }}
    password: ${{ secrets.GITHUB_TOKEN }}

- name: Start Policy API Stack
  uses: ./.github/actions/policy-api-stack
  with:
    action: start
    image: ghcr.io/${{ github.repository_owner }}/policy-api:latest
    timeout: '90'
```

### 停止服务

```yaml
- name: Stop Policy API Stack
  if: always()
  uses: ./.github/actions/policy-api-stack
  with:
    action: stop
```

## 错误排查

### 镜像拉取失败

如果看到 `Failed to pull image` 错误：
- 确认已执行 `docker/login-action` 登录 GHCR
- 确认镜像标签存在且可访问
- 检查 `packages: read` 权限是否已授予

### 服务启动失败

如果看到 `Failed to start services` 错误：
- 检查 docker compose 配置文件是否正确
- 确认所需端口未被占用
- 查看 `docker compose logs` 获取详细错误信息

### 健康检查超时

如果服务启动但健康检查超时：
- 增加 `timeout` 参数值
- 检查服务日志确认启动问题
- 确认 `port` 参数与实际配置一致
