package com.minimarket.api.controller;

import com.minimarket.api.dto.ApiMessageResponse;
import com.minimarket.api.dto.CompanyDto;
import com.minimarket.api.dto.SaveCompanyDto;
import com.minimarket.api.service.CompanyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/company")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @GetMapping
    public ResponseEntity<CompanyDto> get() {
        var company = companyService.get();
        return company == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(company);
    }

    @PutMapping
    public ResponseEntity<?> update(@RequestBody SaveCompanyDto dto) {
        var result = companyService.update(dto);
        if (!result.success()) {
            return ResponseEntity.badRequest().body(new ApiMessageResponse(result.error()));
        }
        return ResponseEntity.ok(result.data());
    }
}
