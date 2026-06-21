package com.example.demo.dto;

public record BranchResponse(
        Long id,
        String name,
        String address,
        String phone
) {}
