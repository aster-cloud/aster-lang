# aster-lang

## Unreleased

### 🚨 Breaking Changes

- **Capability enforcement now enabled by default**: The `ASTER_CAP_EFFECTS_ENFORCE` environment variable now defaults to enabled. Set `ASTER_CAP_EFFECTS_ENFORCE=0` to explicitly disable. This ensures production security by default. (#阶段1.2)

### ✨ New Features

- **Structured logging system**: Added JSON-formatted logging with `LOG_LEVEL` environment variable support, performance metrics tracking, and component-level logging. (#阶段1.4)
- **Error ID system**: Introduced centralized error codes (E1xxx-E9xxx) for better error tracking and diagnostics. (#快速胜利项)
- **Health check script**: Added `scripts/health-check.ts` to validate critical environment variables before deployment. (#快速胜利项)

### 🔒 Security

- **Dependency security scanning**: Integrated `audit-ci` into CI pipeline to detect vulnerabilities (moderate level and above). (#阶段1.1)
- **Dependabot configuration**: Automated weekly dependency updates for npm and GitHub Actions. (#快速胜利项)

### 🐛 Bug Fixes

- **Type system**: Fixed TypeVar comparison logic in `tEquals` to check name equality instead of unconditionally returning true. Added negative test case `bad_generic_return_type.cnl`. (#阶段1.3)
- **Type inference**: Upgraded type mismatch warnings to errors in `unifyTypes` function to prevent type safety issues at runtime. (#阶段1.3)

### 📚 Documentation

- **Operations documentation**: Added comprehensive deployment, configuration, rollback, and troubleshooting guides in `docs/operations/`. (#阶段1.5)

### ⚙️ Infrastructure

- **CI timeout protection**: Added 30-minute timeout wrapper for CI scripts using `timeout-cli`. (#快速胜利项)

### 🔧 Internal Improvements

- **Logger optimization**: Simplified metadata spreading and extracted `parseLogLevel` function for better code clarity.

### ⚠️ Known Issues

- **Development dependency vulnerabilities**: Three moderate-level vulnerabilities exist in the vitepress documentation build chain (esbuild ≤0.24.2). These affect only `devDependencies` and do not impact production runtime or CI/CD pipelines. Risk assessment: Production 0/10, Development 3/10. Decision: Accept risk and monitor for upstream fixes via Dependabot. See `.claude/operations-log.md` for detailed analysis. (#阶段1巩固)

## 0.2.0

### Minor Changes

- ee13e5e: Initial release: CNL → AST → Core IR pipeline, golden tests, property/fuzz tests, benchmarks, structured diagnostics, LSP foundation, CI.
