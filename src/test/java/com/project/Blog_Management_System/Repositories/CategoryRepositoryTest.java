package com.project.Blog_Management_System.Repositories;

import com.project.Blog_Management_System.Entities.CategoryEntity;
import com.project.Blog_Management_System.Utils.TestEntityFactory;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Feature("Repository / Category Repository Tests")
public class CategoryRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    private CategoryEntity category;

    @BeforeEach
    public void setUp() {
        category = categoryRepository.saveAndFlush(TestEntityFactory.testCategory("1"));
    }

    @Nested
    @DisplayName("findBySlug(String)")
    @Story("Find Category by slug")
    @Severity(SeverityLevel.CRITICAL)
    class FindBySlug {

        @Test
        @DisplayName("returns the category when a category with the given slug exists")
        public void returnsCategoryWhenSlugExists() {
            Optional<CategoryEntity> found = categoryRepository.findBySlug(category.getSlug());

            assertTrue(found.isPresent());
            assertEquals(category.getId(), found.get().getId());
            assertEquals(category.getName(), found.get().getName());
            assertEquals(category.getSlug(), found.get().getSlug());
            assertEquals(category.getDescription(), found.get().getDescription());
        }

        @Test
        @DisplayName("returns empty when no category exists with the given slug")
        public void returnsEmptyWhenSlugDoesNotExist() {
            Optional<CategoryEntity> found = categoryRepository.findBySlug("non-existent-slug");
            assertFalse(found.isPresent());
        }

        @Test
        @DisplayName("returns only the category matching the exact slug")
        public void returnsOnlyMatchingCategory() {
            CategoryEntity anotherCategory = saveCategory("another-category");
            Optional<CategoryEntity> found = categoryRepository.findBySlug(anotherCategory.getSlug());

            assertTrue(found.isPresent());
            assertEquals(anotherCategory.getId(), found.get().getId());
            assertNotEquals(category.getId(), found.get().getId());
        }

        @Test
        @DisplayName("is case-sensitive when matching slug")
        public void isCaseSensitiveWhenMatching() {
            String originalSlug = category.getSlug();
            String uppercaseSlug = originalSlug.toUpperCase();

            Optional<CategoryEntity> found = categoryRepository.findBySlug(uppercaseSlug);

            assertFalse(found.isPresent());
        }
    }

    @Nested
    @DisplayName("saveAndFlush()")
    @Story("Save Category")
    @Severity(SeverityLevel.CRITICAL)
    class SaveCategory {

        @Test
        @DisplayName("saves a category successfully with generated id")
        public void savesCategorySuccessfully() {
            CategoryEntity newCategory = TestEntityFactory.testCategory("new-category");
            CategoryEntity saved = categoryRepository.saveAndFlush(newCategory);

            assertNotNull(saved.getId());
            assertEquals(newCategory.getName(), saved.getName());
            assertEquals(newCategory.getDescription(), saved.getDescription());
        }

        @Test
        @DisplayName("does not allow duplicate category names")
        public void preventsNameDuplication() {
            CategoryEntity duplicate = TestEntityFactory.testCategory("duplicate-category");
            duplicate.setName(category.getName());

            assertThrows(DataIntegrityViolationException.class, () -> categoryRepository.saveAndFlush(duplicate));
        }

        @Test
        @DisplayName("does not allow duplicate slugs")
        public void preventsSlugDuplication() {
            CategoryEntity duplicate1 = TestEntityFactory.testCategory("duplicate-category");
            categoryRepository.saveAndFlush(duplicate1);
            CategoryEntity duplicate2 = TestEntityFactory.testCategory("duplicate-category");
            assertThrows(DataIntegrityViolationException.class, () -> categoryRepository.saveAndFlush(duplicate2));
        }
    }

    @Nested
    @DisplayName("findById(UUID)")
    @Story("Find Category by ID")
    @Severity(SeverityLevel.CRITICAL)
    class FindById {

        @Test
        @DisplayName("returns the category when the id exists")
        public void returnsCategoryWhenIdExists() {
            Optional<CategoryEntity> found = categoryRepository.findById(category.getId());

            assertTrue(found.isPresent());
            assertEquals(category.getId(), found.get().getId());
            assertEquals(category.getName(), found.get().getName());
        }

        @Test
        @DisplayName("returns empty when no category exists with the given id")
        public void returnsEmptyWhenIdDoesNotExist() {
            Optional<CategoryEntity> found = categoryRepository.findById(UUID.randomUUID());
            assertFalse(found.isPresent());
        }
    }

    @Nested
    @DisplayName("deleteById(UUID)")
    @Story("Delete Category by ID")
    @Severity(SeverityLevel.CRITICAL)
    class DeleteById {

        @Test
        @DisplayName("removes the category so it can no longer be found")
        public void removesCategory() {
            categoryRepository.deleteById(category.getId());
            Optional<CategoryEntity> found = categoryRepository.findById(category.getId());

            assertFalse(found.isPresent());
        }

        @Test
        @DisplayName("does not throw when deleting a non-existent category")
        public void doesNotThrowWhenDeletingNonExistent() {
            assertDoesNotThrow(() -> categoryRepository.deleteById(UUID.randomUUID()));
        }

        @Test
        @DisplayName("deletes only the specified category and leaves others intact")
        public void deletesOnlySpecifiedCategory() {
            CategoryEntity anotherCategory = saveCategory("another-category");

            categoryRepository.deleteById(category.getId());

            Optional<CategoryEntity> deleted = categoryRepository.findById(category.getId());
            Optional<CategoryEntity> retained = categoryRepository.findById(anotherCategory.getId());

            assertFalse(deleted.isPresent());
            assertTrue(retained.isPresent());
        }
    }

    @Nested
    @DisplayName("findAll()")
    @Story("Find All Categories")
    @Severity(SeverityLevel.CRITICAL)
    class FindAll {

        @Test
        @DisplayName("returns all categories when multiple exist")
        public void returnsAllCategoriesWhenMultipleExist() {
            CategoryEntity category2 = saveCategory("second");
            CategoryEntity category3 = saveCategory("third");

            List<CategoryEntity> allCategories = categoryRepository.findAll();

            assertEquals(3, allCategories.size());
            assertTrue(allCategories.stream().anyMatch(c -> c.getId().equals(category.getId())));
            assertTrue(allCategories.stream().anyMatch(c -> c.getId().equals(category2.getId())));
            assertTrue(allCategories.stream().anyMatch(c -> c.getId().equals(category3.getId())));
        }

        @Test
        @DisplayName("returns an empty list when no categories exist")
        public void returnsEmptyListWhenNoCategoriesExist() {
            categoryRepository.deleteAll();

            List<CategoryEntity> allCategories = categoryRepository.findAll();

            assertTrue(allCategories.isEmpty());
        }
    }

    private @NonNull CategoryEntity saveCategory(String suffix) {
        return categoryRepository.saveAndFlush(TestEntityFactory.testCategory(suffix));
    }
}


