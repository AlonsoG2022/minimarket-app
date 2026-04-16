package com.minimarket.api.controller;

import com.minimarket.api.dto.ApiMessageResponse;
import com.minimarket.api.dto.ProductDto;
import com.minimarket.api.dto.SaveProductDto;
import com.minimarket.api.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductsController {

    private final ProductService productService;

    public ProductsController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<ProductDto> getAll() {
        return productService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getById(@PathVariable Integer id) {
        var product = productService.getById(id);
        return product == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(product);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody SaveProductDto dto) {
        var result = productService.create(dto);
        if (!result.success()) {
            return ResponseEntity.badRequest().body(new ApiMessageResponse(result.error()));
        }

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(result.data().id())
            .toUri();

        return ResponseEntity.created(location).body(result.data());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody SaveProductDto dto) {
        var result = productService.update(id, dto);
        if (!result.success()) {
            if ("Producto no encontrado.".equals(result.error())) {
                return ResponseEntity.status(404).body(new ApiMessageResponse(result.error()));
            }

            return ResponseEntity.badRequest().body(new ApiMessageResponse(result.error()));
        }

        return ResponseEntity.ok(result.data());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        var result = productService.delete(id);
        if (!result.success()) {
            return ResponseEntity.status(404).body(new ApiMessageResponse(result.error()));
        }

        return ResponseEntity.noContent().build();
    }
}
