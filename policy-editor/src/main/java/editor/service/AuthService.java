package editor.service;

import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

/**
 * 获取当前请求身份的用户名。
 *
 * 修复说明：移除了危险的匿名回退逻辑，未认证的请求将显式失败。
 * 这确保了审计日志的准确性和权限控制的有效性。
 */
@RequestScoped
public class AuthService {

    @Inject
    SecurityIdentity identity;

    /**
     * 获取当前已认证用户的用户名。
     *
     * @return 已认证用户的用户名
     * @throws UnauthorizedException 如果用户未认证
     */
    public String currentUser() {
        if (identity == null || identity.isAnonymous() || identity.getPrincipal() == null) {
            throw new UnauthorizedException("Authentication required. Anonymous access is not allowed.");
        }
        return identity.getPrincipal().getName();
    }

    /**
     * 检查当前用户是否已认证。
     *
     * @return true 如果用户已认证，false 否则
     */
    public boolean isAuthenticated() {
        return identity != null && !identity.isAnonymous() && identity.getPrincipal() != null;
    }
}

