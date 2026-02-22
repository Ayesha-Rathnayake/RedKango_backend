//package com.example.backend.dto;
//
//import com.example.backend.validation.StrongPassword;
//import jakarta.validation.constraints.Email;
//import jakarta.validation.constraints.NotBlank;
//import lombok.Data;
//
//@Data
//public class RegisterRequest {
//    @NotBlank private String firstName;
//    @NotBlank private String lastName;
//
//    @Email @NotBlank private String email;
//    @NotBlank private String phone;
//
//    @StrongPassword
//    @NotBlank private String password;
//
//}



package com.example.backend.dto;

import com.example.backend.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be 2-50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be 2-50 characters")
    private String lastName;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(
            regexp = "^[0-9+\\-()\\s]{7,20}$",
            message = "Invalid phone number"
    )
    private String phone;

    @NotBlank(message = "Password is required")
    @StrongPassword // validated by StrongPasswordValidator + policy service
    private String password;
}