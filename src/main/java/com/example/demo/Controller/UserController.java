package com.example.demo.Controller;

import com.example.demo.Models.User;
import com.example.demo.Repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/users")
public class UserController {
    @Autowired
    private UserRepository userRepository;

    // Show all users (Only for ADMIN and Manager)
    @GetMapping
    public String getUsers(Model model) {
        List<User> users = userRepository.findAll(); 
        model.addAttribute("users", users);          
        return "list-user";                          
    }
}
