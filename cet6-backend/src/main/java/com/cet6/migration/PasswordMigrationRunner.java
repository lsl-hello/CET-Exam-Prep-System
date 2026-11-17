package com.cet6.migration;

import com.cet6.entity.SysUser;
import com.cet6.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 启动时自动把数据库中的明文密码迁移为 BCrypt 哈希。
 *
 * 幂等：只在 Java 层用 isBcryptHash() 判断。已经哈希的记录跳过，
 * 重启不会重复加密。
 *
 * @author system
 */
@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class PasswordMigrationRunner implements CommandLineRunner {

    private final SysUserService sysUserService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        List<SysUser> allUsers = sysUserService.list();
        List<SysUser> plainUsers = allUsers.stream()
                .filter(u -> !isBcryptHash(u.getPassword()))
                .toList();

        if (plainUsers.isEmpty()) {
            log.info("[PasswordMigration] 所有用户密码均为 BCrypt 哈希，跳过迁移");
            return;
        }

        log.warn("[PasswordMigration] 检测到 {} 条明文密码记录，开始迁移为 BCrypt 哈希", plainUsers.size());

        int migrated = 0;
        for (SysUser user : plainUsers) {
            String raw = user.getPassword();
            if (raw == null || raw.isBlank()) {
                log.warn("[PasswordMigration] 跳过 user id={} ({}): 密码为空", user.getId(), user.getUsername());
                continue;
            }
            String hashed = passwordEncoder.encode(raw);
            user.setPassword(hashed);
            sysUserService.updateById(user);
            migrated++;
            log.info("[PasswordMigration] 已迁移 user id={} ({}) 的密码", user.getId(), user.getUsername());
        }
        log.info("[PasswordMigration] 迁移完成，共处理 {} 条记录", migrated);
    }

    /** 判断是否已是 BCrypt 哈希（$2a$ / $2b$ / $2y$ 开头，长度 60） */
    private boolean isBcryptHash(String s) {
        return s != null
                && s.length() == 60
                && (s.startsWith("$2a$") || s.startsWith("$2b$") || s.startsWith("$2y$"));
    }
}
