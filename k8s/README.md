# K3s 部署指南（Policy API + PostgreSQL）

> 日期：2025-11-12 18:41 NZDT  
> 执行者：Codex

## 目录结构
```
k8s/
├── base/               # 通用资源定义（Namespace、PostgreSQL、Policy API、Ingress、HPA）
├── overlays/
│   ├── dev/            # 开发环境：单副本、低资源、禁用 HPA
│   └── prod/           # 生产环境：多副本、HPA、反亲和
└── README.md           # 本指南
```

## 前置条件
1. 已安装并配置 `kubectl`，`KUBECONFIG` 指向目标 K3s 集群。
2. 已推送最新 `aster/policy-api:latest`（或 GHCR 镜像）至集群可访问的 Registry。
3. StorageClass `default` 可用，用于 PostgreSQL 数据持久化。
4. Traefik Ingress Controller 已随 K3s 安装，可解析 `policy-api.local`。

## 快速部署
```bash
# 语法检查
kubectl apply --dry-run=client -k k8s/base/

# 开发环境部署
kubectl apply -k k8s/overlays/dev/

# 生产环境部署
kubectl apply -k k8s/overlays/prod/
```

## 运行验证
```bash
kubectl get all -n aster-policy
kubectl get pvc -n aster-policy
kubectl get ingress -n aster-policy

# 追踪 Pod 日志
kubectl logs -f deployment/policy-api -n aster-policy

# 本地端口转发
kubectl port-forward -n aster-policy svc/policy-api 8080:8080 &
curl http://localhost:8080/q/health
```

## 监控栈部署
1. **启用 monitoring profile**  
   - 先确保 `k8s/base/` 资源已应用（其中包含 Alertmanager、Postgres/Redis exporter 及对应 ServiceMonitor）。  
   - 运行 `kubectl apply -k k8s/monitoring/` 以部署 Prometheus、Grafana、PrometheusRule 及数据源配置。  
   - 通过 `kubectl get deployments -n aster-policy | grep -E 'prometheus|grafana|alertmanager'` 确认三套组件均为 `Available`。
2. **Grafana 默认凭据与访问方式**  
   - 默认用户名/密码：`admin / admin`（可在 `k8s/monitoring/grafana-deployment.yaml` 中通过 env 覆盖）。  
   - 暴露方式：`kubectl port-forward -n aster-policy svc/grafana 3000:3000`，打开 <http://localhost:3000>，选择 `Prometheus` 数据源即可浏览已随仓库提供的 Dashboard。  
   - 若需对外暴露，可在 `k8s/monitoring` 下添加 Ingress 或通过 LoadBalancer Service 代理。
3. **Prometheus Targets 健康检查**  
   - 端口转发：`kubectl port-forward -n aster-policy svc/prometheus 9090:9090`。  
   - 查看 Targets：`curl -s http://localhost:9090/api/v1/targets | jq '.data.activeTargets[].labels.app'`，确保包含 `policy-api`、`postgres-exporter`、`redis-exporter`。  
   - 若目标处于 `down`，执行 `kubectl describe servicemonitor <name> -n aster-policy` 核对 `endpoints.port` 与 Service 端口是否匹配。

## Secret 配置模板
以下样例全部位于 `aster-policy` namespace，可按需修改后 `kubectl apply -f -` 或 `kubectl create secret generic ...`。

### Alertmanager Slack Webhook
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: alertmanager-slack
  namespace: aster-policy
type: Opaque
stringData:
  SLACK_WEBHOOK_URL: https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXXXXXX
  SLACK_CHANNEL_CRITICAL: '#alerts-critical'
  SLACK_CHANNEL_WARNING: '#alerts-warning'
```
> 将 `k8s/base/monitoring/alertmanager/configmap.yaml` 中的 `PLACEHOLDER` 替换为 `SLACK_WEBHOOK_URL` 的引用或直接内联。

### Alertmanager SMTP 邮件通知
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: alertmanager-smtp
  namespace: aster-policy
type: Opaque
stringData:
  SMTP_SMARTHOST: smtp.gmail.com:587
  SMTP_USERNAME: alerts@example.com
  SMTP_PASSWORD: 'P@ssw0rd!'
  SMTP_FROM: 'Aster Alert <alerts@example.com>'
```
> 对应字段在 `monitoring/alertmanager/config.yml` 的 `email_configs` 段中使用，便于在不同环境注入真实凭据。

### PostgreSQL / Redis 凭据
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: datastore-credentials
  namespace: aster-policy
type: Opaque
stringData:
  POSTGRESQL_USERNAME: postgres
  POSTGRESQL_PASSWORD: change-me
  REDIS_PASSWORD: redis-pass
```
> `postgres-exporter`/Policy API 可引用 `POSTGRESQL_*` 字段；若 Redis 实例启用了密码，可在 `redis-exporter` Deployment 中将 `REDIS_ADDR` 扩展为 `redis://:$(REDIS_PASSWORD)@redis:6379`。

## 故障排查
| 症状 | 诊断步骤 | 解决建议 |
| ---- | -------- | -------- |
| Pod CrashLoopBackOff | `kubectl logs pod/<name>`、`describe pod` | 检查数据库连接、环境变量、镜像标签 |
| PostgreSQL PVC 处于 Pending | `kubectl describe pvc postgres-data-postgres-0` | 确认 StorageClass 与节点磁盘配额 |
| Ingress 404 | `kubectl describe ingress policy-api-ingress` | 确认 Traefik Rule/Host 以及 DNS 解析 |
| HPA 无法扩缩 | `kubectl top pods -n aster-policy` | 验证 Metrics Server 是否就绪，必要时重新部署 metrics-server |

## 升级与回滚
1. **升级镜像**：更新 CI/CD 推送的新标签后执行 `kubectl set image deployment/policy-api policy-api=<IMAGE>`。
2. **扩容/缩容**：调整 `overlays/*/patches/deployment.yaml` 中 `replicas` 或资源限制，重新 `kubectl apply -k`。
3. **回滚**：使用 `kubectl rollout undo deployment/policy-api -n aster-policy`，或重新应用上一版本 kustomization。

## 清理
```bash
kubectl delete -k k8s/overlays/dev/
# 或
kubectl delete -k k8s/overlays/prod/
```

## 注意事项
- Secret 以明文 `stringData` 管理，建议在生产使用外部 Secret 管理（Vault、Sealed Secrets）。
- 若使用 Podman 推送镜像，请在集群节点内配置对应 registry pull secret，并在 Deployment 上引用。
- 如需在 K3s 中启用 HTTPS，可在 `ingress.yaml` 增加 TLS 配置并上传证书。
