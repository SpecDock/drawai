package com.drawai.trigger.auth;

import com.drawai.domain.auth.gateway.JwtService;
import com.drawai.trigger.dto.LoginRequest;
import com.drawai.trigger.dto.LoginResponse;
import com.drawai.trigger.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtService jwtService;
    private final String cfgUser;
    private final String cfgPass;
    private final long expirationSeconds;

    public AuthController(JwtService jwtService,
                          @Value("${jwt.user.username}") String cfgUser,
                          @Value("${jwt.user.password}") String cfgPass,
                          @Value("${jwt.expiration-seconds:3600}") long expirationSeconds) {
        this.jwtService = jwtService;
        this.cfgUser = cfgUser;
        this.cfgPass = cfgPass;
        this.expirationSeconds = expirationSeconds;
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest req) {
        if (!cfgUser.equals(req.username()) || !cfgPass.equals(req.password())) {
            log.warn("登录失败");
            return Result.error(401, "bad credentials");
        }
        String token = jwtService.issue(req.username());
        log.info("登录成功");
        return Result.ok(new LoginResponse(token, expirationSeconds));
    }
}
