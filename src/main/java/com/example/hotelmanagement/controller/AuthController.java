package com.example.hotelmanagement.controller;

import com.example.hotelmanagement.exception.UserAlreadyExistsException;
import com.example.hotelmanagement.model.User;
import com.example.hotelmanagement.request.LoginRequest;
import com.example.hotelmanagement.response.JwtResponse;
import com.example.hotelmanagement.security.jwt.JwtUtils;
import com.example.hotelmanagement.security.user.HotelUserDetails;
import com.example.hotelmanagement.service.IUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final IUserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    @PostMapping("/register-user")
    public ResponseEntity<?> registerUser(@RequestBody User user){
        try{
            userService.registerUser(user);
            return ResponseEntity.ok("Registration successful!");
        }catch (UserAlreadyExistsException e){
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest request){
        //xac thuc
        Authentication authentication=
                authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(),request.getPassword()));

        //cho phép Spring Security biết được người dùng hiện tại đã xác thực là ai và có quyền truy cập gì trong suốt quá trình xử lý yêu cầu
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt= jwtUtils.generateJwtTokenForUser(authentication);
        HotelUserDetails userDetails= (HotelUserDetails) authentication.getPrincipal();
        List<String> roles=userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).toList();
        return ResponseEntity.ok(new JwtResponse(
                userDetails.getId(),
                userDetails.getEmail(),
                jwt,
                roles
        ));
    }
}
