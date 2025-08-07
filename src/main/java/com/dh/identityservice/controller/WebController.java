package com.dh.identityservice.controller;

import java.time.LocalDate;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.dh.identityservice.dto.request.AuthenticationRequest;
import com.dh.identityservice.dto.request.UserCreationRequest;
import com.dh.identityservice.dto.response.AuthenticationResponse;
import com.dh.identityservice.exception.AppException;
import com.dh.identityservice.service.AuthenticationService;
import com.dh.identityservice.service.UserService;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class WebController {

	@Autowired
	private AuthenticationService authenticationService;

	@Autowired
	private UserService userService;

	@GetMapping("/")
	public String index() {
		return "redirect:/login";
	}

	@GetMapping("/login")
	public String loginPage(Model model, @RequestParam(required = false) String error,
			@RequestParam(required = false) String message) {
		if (error != null) {
			model.addAttribute("error", "Invalid username or password");
		}
		if (message != null) {
			model.addAttribute("message", message);
		}
		return "login";
	}

	@PostMapping("/login")
	public String login(@RequestParam String username, @RequestParam String password, Model model,
			RedirectAttributes redirectAttributes) {
		try {
			AuthenticationRequest request = AuthenticationRequest.builder()
					.username(username)
					.password(password)
					.build();

			AuthenticationResponse response = authenticationService.authenticate(request);
			log.info("User {} logged in successfully", username);

			// In a real application, you would store the token in session or JWT cookie
			// For testing purposes, we'll just redirect to home
			return "redirect:/home";
		} catch (AppException e) {
			log.error("Login failed for user {}: {}", username, e.getMessage());
			redirectAttributes.addAttribute("error", "true");
			return "redirect:/login";
		} catch (Exception e) {
			log.error("Unexpected error during login for user {}: {}", username, e.getMessage());
			redirectAttributes.addAttribute("error", "true");
			return "redirect:/login";
		}
	}

	@GetMapping("/register")
	public String registerPage(Model model, @RequestParam(required = false) String error,
			@RequestParam(required = false) String message) {
		if (error != null) {
			model.addAttribute("error", "Registration failed. Please try again.");
		}
		if (message != null) {
			model.addAttribute("message", message);
		}
		return "register";
	}

	@PostMapping("/register")
	public String register(@RequestParam String username, @RequestParam String password,
			@RequestParam String firstName, @RequestParam String lastName, @RequestParam String dob, Model model,
			RedirectAttributes redirectAttributes) {
		try {
			UserCreationRequest request = UserCreationRequest.builder()
					.username(username)
					.password(password)
					.firstName(firstName)
					.lastName(lastName)
					.dob(LocalDate.parse(dob))
					.build();

			userService.createUser(request);
			log.info("User {} registered successfully", username);

			redirectAttributes.addAttribute("message", "Registration successful! Please login.");
			return "redirect:/login";
		} catch (AppException e) {
			log.error("Registration failed for user {}: {}", username, e.getMessage());
			redirectAttributes.addAttribute("error", "true");
			return "redirect:/register";
		} catch (Exception e) {
			log.error("Unexpected error during registration for user {}: {}", username, e.getMessage());
			redirectAttributes.addAttribute("error", "true");
			return "redirect:/register";
		}
	}

	@GetMapping("/home")
	public String home() {
		return "home";
	}

	@PostMapping("/logout")
	public String logout(RedirectAttributes redirectAttributes) {
		redirectAttributes.addAttribute("message", "You have been logged out successfully.");
		return "redirect:/login";
	}
}
