package com.postread.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.postread.repositories.UserRepository;
import com.postread.security.User;

import java.util.Optional;

@Controller
@RequestMapping("/auth")
public class SecurityController {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private AuthenticationManager authenticationManager;
    private JWTCore jwtCore;

    @Autowired
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Autowired
    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Autowired
    public void setAuthenticationManager(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Autowired
    public void setJwtCore(JWTCore jwtCore) {
        this.jwtCore = jwtCore;
    }

    @PostMapping("/login")
    public ModelAndView signin(@RequestParam("username") String username,
                               @RequestParam("password") String password,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        Authentication authentication;
        ModelAndView modelAndView = new ModelAndView();

        try {
            // Валидация входных данных
            if (username == null || username.trim().isEmpty() ||
                    password == null || password.trim().isEmpty()) {
                modelAndView.setViewName("login");
                modelAndView.addObject("error", "Имя пользователя и пароль обязательны");
                return modelAndView;
            }

            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username.trim(), password)
            );
        } catch (BadCredentialsException e) {
            modelAndView.setViewName("login");
            modelAndView.addObject("error", "Неверное имя пользователя или пароль");
            return modelAndView;
        } catch (Exception e) {
            modelAndView.setViewName("login");
            modelAndView.addObject("error", "Ошибка аутентификации: " + e.getMessage());
            return modelAndView;
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Генерируем JWT токен
        String jwt = jwtCore.generateToken(authentication);

        // Сохраняем в сессии
        session.setAttribute("username", username);
        session.setAttribute("token", jwt);

        // Получаем ID пользователя
        Optional<User> userOpt = userRepository.findByName(username);
        if (userOpt.isPresent()) {
            session.setAttribute("userId", userOpt.get().getId());
        }

        modelAndView.setViewName("redirect:/articles");
        redirectAttributes.addFlashAttribute("success", "Добро пожаловать, " + username + "!");
        return modelAndView;
    }

    @GetMapping("/login")
    public ModelAndView showSigninPage(@RequestParam(value = "error", required = false) String error,
                                       @RequestParam(value = "logout", required = false) String logout) {
        ModelAndView modelAndView = new ModelAndView("login");

        if (error != null) {
            modelAndView.addObject("error", "Неверное имя пользователя или пароль");
        }
        if (logout != null) {
            modelAndView.addObject("message", "Вы успешно вышли из системы");
        }

        modelAndView.addObject("pageTitle", "Вход в систему");
        return modelAndView;
    }

    @GetMapping("/signup")
    public ModelAndView showSignupPage() {
        ModelAndView modelAndView = new ModelAndView("signup");
        modelAndView.addObject("signupRequest", new SignupRequest());
        modelAndView.addObject("pageTitle", "Регистрация");
        return modelAndView;
    }

    @PostMapping("/signup")
    public ModelAndView signup(@ModelAttribute @Valid SignupRequest signupRequest,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes) {
        ModelAndView modelAndView = new ModelAndView();

        // Валидация
        if (signupRequest.getUsername() == null || signupRequest.getUsername().trim().isEmpty()) {
            bindingResult.rejectValue("username", "error.username", "Имя пользователя обязательно");
        } else if (signupRequest.getUsername().length() < 3) {
            bindingResult.rejectValue("username", "error.username", "Имя пользователя должно содержать минимум 3 символа");
        }

        if (signupRequest.getEmail() == null || signupRequest.getEmail().trim().isEmpty()) {
            bindingResult.rejectValue("email", "error.email", "Email обязателен");
        } else if (!isValidEmail(signupRequest.getEmail())) {
            bindingResult.rejectValue("email", "error.email", "Некорректный формат email");
        }

        if (signupRequest.getPassword() == null || signupRequest.getPassword().isEmpty()) {
            bindingResult.rejectValue("password", "error.password", "Пароль обязателен");
        } else if (signupRequest.getPassword().length() < 6) {
            bindingResult.rejectValue("password", "error.password", "Пароль должен содержать минимум 6 символов");
        }

        if (bindingResult.hasErrors()) {
            modelAndView.setViewName("signup");
            modelAndView.addObject("signupRequest", signupRequest);
            return modelAndView;
        }

        String username = signupRequest.getUsername().trim();
        String email = signupRequest.getEmail().trim().toLowerCase();

        // Проверка существующего пользователя
        if (userRepository.existsUserByName(username)) {
            modelAndView.setViewName("signup");
            modelAndView.addObject("error", "Пользователь с таким именем уже существует");
            modelAndView.addObject("signupRequest", signupRequest);
            return modelAndView;
        }

        if (userRepository.existsUserByEmail(email)) {
            modelAndView.setViewName("signup");
            modelAndView.addObject("error", "Пользователь с таким email уже существует");
            modelAndView.addObject("signupRequest", signupRequest);
            return modelAndView;
        }

        // Создание пользователя
        User user = new User();
        user.setName(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(signupRequest.getPassword()));

        userRepository.save(user);

        redirectAttributes.addFlashAttribute("success", "Регистрация успешна! Теперь вы можете войти в систему.");
        modelAndView.setViewName("redirect:/auth/login");
        return modelAndView;
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            new SecurityContextLogoutHandler().logout(request, response, authentication);
        }
        return "redirect:/auth/login?logout";
    }

    // Вспомогательный метод для валидации email
    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }
}