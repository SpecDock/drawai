package com.drawai.app.test;

import com.drawai.domain.aiengine.service.agent.StreamingChatModelContext;
import com.drawai.domain.aiengine.types.RoleNameTypes;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author specdock
 * @Date 2026/6/4
 * @Time 20:22
 */

@Slf4j
@SpringBootTest(properties = {
        "jwt.secret=test-jwt-secret-key-must-be-at-least-32-bytes",
        "jwt.user.username=test-user",
        "jwt.user.password=test-password"
})
public class ConfigTest {

    @Resource
    StreamingChatModelContext streamingChatModelContext;

    @Test
    public void testConfig(){
        log.info("============ChatModelContext: {}", streamingChatModelContext.getModel(RoleNameTypes.FIXER.value()).toString());
    }
}
