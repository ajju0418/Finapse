package com.finapse.service;

import com.finapse.entity.Category;
import com.finapse.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<Category> getAll() {
        return categoryRepository.findAll();
    }

    public Optional<Category> findByName(String name) {
        return categoryRepository.findByName(name);
    }

    public Category findOrThrow(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new com.finapse.exception.ResourceNotFoundException(
                        "Category not found: " + id));
    }
}
