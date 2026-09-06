package com.elotech.taskmanager.auth;

import com.elotech.taskmanager.auth.dto.LoginRequest;
import com.elotech.taskmanager.auth.dto.LoginResponse;
import com.elotech.taskmanager.user.UserRepository;
import com.elotech.taskmanager.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
                .orElseThrow(() -> {
                    log.warn("Falha de autenticacao no login");
                    // mensagem generica: nao revela se o e-mail existe ou se a senha esta errada
                    return new BadCredentialsException("Credenciais invalidas");
                });

        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return new LoginResponse(token, jwtService.getExpiration().toSeconds(),
                user.getId(), user.getName(), user.getEmail());
    }
}
