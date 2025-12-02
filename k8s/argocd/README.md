# ArgoCD 部署指南

1. 在 `argocd` 命名空间应用 `application.yaml`：
```bash
kubectl apply -f k8s/argocd/application.yaml
```
2. ArgoCD 将追踪 `main` 分支 `k8s/overlays/prod`，自动部署 Policy API / PostgreSQL。
3. 开启 auto-sync + self-heal，可在 ArgoCD UI 中查看健康状态与历史。
4. 如需部署到其它环境，可复制此文件并修改 `path`/`targetRevision`/`destination`。
