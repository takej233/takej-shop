package com.takej.mall.admin.bootstrap;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.takej.mall.admin.entity.SysAdmin;
import com.takej.mall.admin.mapper.SysAdminMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 后台管理员初始化器。
 * 服务启动时按本地配置创建第一个管理员账号。
 */
@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log =
            LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final SysAdminMapper sysAdminMapper;

    /**
     * 初始化密码只会加密后写入数据库，不会输出到日志。
     */
    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    @Value("${takej.admin.bootstrap.enabled:false}")
    private boolean bootstrapEnabled;

    @Value("${takej.admin.bootstrap.username:}")
    private String bootstrapUsername;

    @Value("${takej.admin.bootstrap.password:}")
    private String bootstrapPassword;

    public AdminBootstrapRunner(SysAdminMapper sysAdminMapper) {
        this.sysAdminMapper = sysAdminMapper;
    }

    /**
     * 初始化开关开启时，按用户名检查并创建管理员。
     */
    @Override
    public void run(ApplicationArguments args) {
        // 正式或后续启动时默认跳过，不修改已有管理员数据。
        if (!bootstrapEnabled) {
            return;
        }

        // 开关开启但未填写完整配置时，不创建不完整账号。
        if (!StringUtils.hasText(bootstrapUsername)
                || !StringUtils.hasText(bootstrapPassword)) {
            log.warn("管理员初始化已开启，但用户名或密码未配置");
            return;
        }

        LambdaQueryWrapper<SysAdmin> query =
                new LambdaQueryWrapper<>();

        // 不加 deleted 条件：用户名有唯一索引，避免逻辑删除记录导致重复插入。
        query.eq(SysAdmin::getUsername, bootstrapUsername);

        SysAdmin existing = sysAdminMapper.selectOne(query);

        // 同名管理员已存在时不重复创建，保证多次重启安全。
        if (existing != null) {
            log.info("管理员初始化跳过，用户名已存在：{}",
                    bootstrapUsername);
            return;
        }

        SysAdmin admin = new SysAdmin();
        admin.setUsername(bootstrapUsername);
        admin.setPassword(passwordEncoder.encode(bootstrapPassword));
        admin.setStatus(1);
        admin.setDeleted(0);

        sysAdminMapper.insert(admin);

        // 只记录用户名和 ID，绝不打印密码或密码哈希。
        log.info("管理员初始化成功，adminId={}, username={}",
                admin.getId(),
                admin.getUsername());
    }
}