# ArgoCD 部署指南

## 前置条件：配置仓库访问权限

由于 `aster-cloud/aster-lang` 是私有仓库，需先配置 ArgoCD 访问凭证：

### 方式一：使用 ArgoCD CLI
```bash
argocd repo add https://github.com/aster-cloud/aster-lang.git \
  --username <github-username> \
  --password <github-personal-access-token>
```

### 方式二：使用 Kubernetes Secret
```bash
# 编辑 repo-secret.yaml，填入真实凭证
kubectl apply -f k8s/argocd/repo-secret.yaml
```

### 方式三：使用 GitHub Deploy Key（推荐）
```bash
# 生成 SSH 密钥对
ssh-keygen -t ed25519 -C "argocd-aster-lang" -f ~/.ssh/argocd-aster-lang

# 将公钥添加到 GitHub 仓库的 Deploy Keys（Settings > Deploy Keys）
cat ~/.ssh/argocd-aster-lang.pub

# 使用 ArgoCD CLI 添加仓库
argocd repo add git@github.com:aster-cloud/aster-lang.git \
  --ssh-private-key-path ~/.ssh/argocd-aster-lang
```

## 部署步骤

1. 确认仓库凭证已配置：
```bash
argocd repo list
```

2. 在 `argocd` 命名空间应用 `application.yaml`：
```bash
kubectl apply -f k8s/argocd/application.yaml
```

3. ArgoCD 将追踪 `main` 分支 `k8s/overlays/prod`，自动部署 Policy API / PostgreSQL。

4. 开启 auto-sync + self-heal，可在 ArgoCD UI 中查看健康状态与历史。

5. 如需部署到其它环境，可复制此文件并修改 `path`/`targetRevision`/`destination`。
