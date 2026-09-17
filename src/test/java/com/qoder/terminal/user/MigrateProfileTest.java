package com.qoder.terminal.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * `make db-migrate` / AutoWonder QA 数据库步骤使用 migrate profile：
 * 只需要数据库连接即可完成迁移，不得依赖 JWT 私钥、邀请码等业务配置。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("migrate")
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = {"qoder.jwt.private-key=", "qoder.jwt.allow-ephemeral-key=false"})
class MigrateProfileTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void migratesWithoutJwtKey() {
        Integer applied = jdbc.queryForObject(
                "select count(*) from qoder_user.flyway_schema_history where success and version = '1'", Integer.class);
        assertThat(applied).isEqualTo(1);
    }
}
