package com.finapse.controller;

import com.finapse.dto.CategoryResponse;
import com.finapse.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAll() {
        List<CategoryResponse> categories = categoryService.getAll().stream()
                .map(CategoryResponse::from)
                .sorted(Comparator.comparing(CategoryResponse::displayName))
                .toList();
        return ResponseEntity.ok(categories);
    }
}
