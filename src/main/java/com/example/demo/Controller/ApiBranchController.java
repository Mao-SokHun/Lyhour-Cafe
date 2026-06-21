package com.example.demo.Controller;

import com.example.demo.Repositories.BranchRepository;
import com.example.demo.dto.BranchResponse;
import com.example.demo.dto.OrderMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/branches")
public class ApiBranchController {

    private final BranchRepository branchRepository;

    public ApiBranchController(BranchRepository branchRepository) {
        this.branchRepository = branchRepository;
    }

    @GetMapping
    public List<BranchResponse> list() {
        return branchRepository.findByActiveTrue().stream()
                .map(OrderMapper::toBranchResponse)
                .toList();
    }
}
