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

### 方式三：使用 GitHub App（推荐）

GitHub App 提供更细粒度的权限控制、更好的安全性和审计能力。

#### 步骤 1：创建 GitHub App

1. 访问 GitHub Organization Settings > Developer settings > GitHub Apps
2. 点击 **New GitHub App**
3. 填写基本信息：
   - **GitHub App name**: `ArgoCD-AsterCloud`（或其他唯一名称）
   - **Homepage URL**: `https://aster-lang.cloud`
   - **Webhook**: 取消勾选 "Active"（ArgoCD 不需要 webhook）

4. 配置权限（Repository permissions）：
   - **Contents**: `Read-only`（必需，用于读取仓库内容）
   - **Metadata**: `Read-only`（必需，用于访问仓库元数据）

5. 选择安装范围：
   - **Where can this GitHub App be installed?**: `Only on this account`

6. 点击 **Create GitHub App**

#### 步骤 2：生成私钥

1. 在创建的 GitHub App 页面，滚动到 **Private keys** 部分
2. 点击 **Generate a private key**
3. 浏览器会自动下载 `.pem` 文件，保存到安全位置（如 `~/.ssh/argocd-github-app.pem`）

#### 步骤 3：安装 GitHub App 到仓库

1. 在 GitHub App 页面左侧菜单，点击 **Install App**
2. 选择 `aster-cloud` 组织
3. 选择 **Only select repositories**，勾选 `aster-lang`
4. 点击 **Install**
5. 记下 URL 中的 **Installation ID**（例如：`https://github.com/settings/installations/12345678` 中的 `12345678`）

#### 步骤 4：获取 App ID

1. 返回 GitHub App 设置页面
2. 在 **About** 部分找到 **App ID**（例如：`123456`）

#### 步骤 5：配置 ArgoCD

```bash
# 使用 ArgoCD CLI 添加仓库
argocd repo add https://github.com/aster-cloud/aster-lang.git \
  --github-app-id <APP_ID> \
  --github-app-installation-id <INSTALLATION_ID> \
  --github-app-private-key-path ~/.ssh/argocd-github-app.pem
```

或者通过 ArgoCD Web UI：
1. 进入 Settings → Repositories
2. 点击 **Connect Repo using GitHub App**
3. 填写：
   - Repository URL: `https://github.com/aster-cloud/aster-lang.git`
   - GitHub App ID: `<APP_ID>`
   - GitHub App Installation ID: `<INSTALLATION_ID>`
   - GitHub App private key: 粘贴 `.pem` 文件内容
4. 点击 **Connect**

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
