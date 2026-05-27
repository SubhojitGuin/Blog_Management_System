package com.project.Blog_Management_System.Service;

import com.project.Blog_Management_System.Dto.CategoryRequestDTO;
import com.project.Blog_Management_System.Dto.CategoryResponseDTO;
import com.project.Blog_Management_System.Dto.PostResponseDTO;
import com.project.Blog_Management_System.Entities.CategoryEntity;
import com.project.Blog_Management_System.Entities.UserEntity;
import com.project.Blog_Management_System.Exceptions.InvalidActionException;
import com.project.Blog_Management_System.Exceptions.ResourceConflictException;
import com.project.Blog_Management_System.Exceptions.ResourceNotFoundException;
import com.project.Blog_Management_System.Repositories.CategoryRepository;
import com.project.Blog_Management_System.Repositories.PostRepository;
import com.project.Blog_Management_System.Utils.MessageService;
import com.project.Blog_Management_System.Utils.TestEntityFactory;
import com.project.Blog_Management_System.Utils.ValidationUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CategoryServiceImplTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private ValidationUtils validationUtils;

    @Mock
    private MessageService messageService;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private CategoryEntity category;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        category = TestEntityFactory.testCategory("main");
        category.setId(UUID.randomUUID());

        user = TestEntityFactory.testUser("main");
        user.setId(UUID.randomUUID());

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("createCategory(CategoryRequestDTO)")
    class CreateCategory {
        @Test
        @DisplayName("returns CategoryResponseDTO when slug is available and category saved")
        void returnsCategoryResponseWhenSlugAvailable() {
            CategoryRequestDTO requestDTO = new CategoryRequestDTO();
            requestDTO.setName(category.getName());
            requestDTO.setDescription(category.getDescription());

            CategoryEntity mappedEntity = new CategoryEntity();
            mappedEntity.setName(category.getName());
            mappedEntity.setDescription(category.getDescription());

            CategoryResponseDTO responseDto = new CategoryResponseDTO();
            responseDto.setId(category.getId());
            responseDto.setName(category.getName());
            responseDto.setSlug(category.getSlug());
            responseDto.setDescription(category.getDescription());

            when(categoryRepository.findBySlug(category.getSlug())).thenReturn(Optional.empty());
            when(modelMapper.map(requestDTO, CategoryEntity.class)).thenReturn(mappedEntity);
            when(categoryRepository.saveAndFlush(mappedEntity)).thenReturn(category);
            when(modelMapper.map(category, CategoryResponseDTO.class)).thenReturn(responseDto);

            CategoryResponseDTO result = categoryService.createCategory(requestDTO);

            assertNotNull(result);
            assertEquals(category.getName(), result.getName());
            assertEquals(category.getSlug(), result.getSlug());

            verify(categoryRepository).findBySlug(category.getSlug());
            verify(modelMapper).map(requestDTO, CategoryEntity.class);
            verify(categoryRepository).saveAndFlush(mappedEntity);
            verify(modelMapper).map(category, CategoryResponseDTO.class);
        }

        @Test
        @DisplayName("throws ResourceConflictException when generated slug already exists")
        void throwsResourceConflictWhenSlugExists() {
            CategoryRequestDTO requestDTO = new CategoryRequestDTO();
            requestDTO.setName(category.getName());
            requestDTO.setDescription(category.getDescription());

            when(categoryRepository.findBySlug(category.getSlug())).thenReturn(Optional.of(new CategoryEntity()));

            ResourceConflictException ex = assertThrows(ResourceConflictException.class, () -> categoryService.createCategory(requestDTO));
            assertNotNull(ex);

            verify(categoryRepository).findBySlug(category.getSlug());
            verifyNoMoreInteractions(categoryRepository);
            verifyNoInteractions(modelMapper);
        }


        @Test
        @DisplayName("invokes mapping and persistence in correct sequence for successful creation")
        void invokesMappingAndPersistenceForSuccessfulCreation() {
            CategoryRequestDTO requestDTO = new CategoryRequestDTO();
            requestDTO.setName(category.getName());
            requestDTO.setDescription(category.getDescription());

            CategoryEntity mappedEntity = new CategoryEntity();
            mappedEntity.setName(category.getName());
            mappedEntity.setDescription(category.getDescription());

            CategoryResponseDTO responseDto = new CategoryResponseDTO();
            responseDto.setId(category.getId());
            responseDto.setName(category.getName());
            responseDto.setSlug(category.getSlug());
            responseDto.setDescription(category.getDescription());

            when(categoryRepository.findBySlug(category.getSlug())).thenReturn(Optional.empty());
            when(modelMapper.map(requestDTO, CategoryEntity.class)).thenReturn(mappedEntity);
            when(categoryRepository.saveAndFlush(mappedEntity)).thenReturn(category);
            when(modelMapper.map(category, CategoryResponseDTO.class)).thenReturn(responseDto);

            CategoryResponseDTO result = categoryService.createCategory(requestDTO);

            assertNotNull(result);
            assertEquals(category.getId(), result.getId());
            assertEquals(category.getName(), result.getName());
            assertEquals(category.getSlug(), result.getSlug());
            assertEquals(category.getDescription(), result.getDescription());

            InOrder inOrder = inOrder(categoryRepository, modelMapper);
            inOrder.verify(modelMapper).map(requestDTO, CategoryEntity.class);
            inOrder.verify(categoryRepository).saveAndFlush(mappedEntity);
            inOrder.verify(modelMapper).map(category, CategoryResponseDTO.class);
        }
    }
    
    @Nested
    @DisplayName("getPostsByCategory(String, UUID, UUID, Integer)")
    class GetPostsByCategory {

        @Test
        @DisplayName("returns a Slice of PostResponseDTO when category exists and repository returns results")
        void returnsSliceWhenCategoryExists() {
            UUID categoryId = category.getId();

            PostResponseDTO dto = new PostResponseDTO();
            dto.setTitle("sample-post");

            Slice<PostResponseDTO> slice =
                    new SliceImpl<>(List.of(dto), PageRequest.of(0, 10), false);

            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
            when(postRepository.findPostsByCategory(eq(categoryId), any(), eq(user.getId()), any())).thenReturn(slice);

            Slice<PostResponseDTO> result = categoryService.getPostsByCategory(category.getSlug(), categoryId, null, 10);

            assertSame(slice, result);
            verify(categoryRepository).findById(categoryId);
            verify(postRepository).findPostsByCategory(eq(categoryId), any(), eq(user.getId()), any());
            verify(validationUtils).isInvalidCategory(category, category.getSlug());
        }

        @Test
        @DisplayName("propagates exception when category is invalid according to ValidationUtils")
        void throwsWhenCategoryIsInvalid() {
            UUID missingId = UUID.randomUUID();

            when(categoryRepository.findById(missingId)).thenReturn(Optional.empty());
            doThrow(new IllegalArgumentException("invalid category")).when(validationUtils).isInvalidCategory(any(), eq("missing-slug"));

            assertThrows(IllegalArgumentException.class, () -> categoryService.getPostsByCategory("missing-slug", missingId, null, 5));

            verify(categoryRepository).findById(missingId);
            verify(validationUtils).isInvalidCategory(null, "missing-slug");
            verifyNoInteractions(postRepository);
        }
    }

    @Nested
    @DisplayName("getAllCategories()")
    class GetAllCategories {

        @Test
        @DisplayName("returns mapped list of CategoryResponseDTO for all categories")
        void returnsMappedDtoListForAllCategories() {
            CategoryEntity c1 = TestEntityFactory.testCategory("a");
            c1.setId(UUID.randomUUID());
            CategoryEntity c2 = TestEntityFactory.testCategory("b");
            c2.setId(UUID.randomUUID());

            when(categoryRepository.findAll()).thenReturn(List.of(c1, c2));
            when(modelMapper.map(any(CategoryEntity.class), eq(CategoryResponseDTO.class)))
                    .thenAnswer(invocation -> {
                        CategoryEntity src = invocation.getArgument(0);
                        CategoryResponseDTO dto = new CategoryResponseDTO();
                        dto.setName(src.getName());
                        dto.setSlug(src.getSlug());
                        return dto;
                    });

            List<CategoryResponseDTO> result = categoryService.getAllCategories();

            assertEquals(2, result.size());
            assertTrue(result.stream().anyMatch(d -> d.getName().equals(c1.getName())));
            assertTrue(result.stream().anyMatch(d -> d.getName().equals(c2.getName())));

            verify(categoryRepository).findAll();
            verify(modelMapper, times(2)).map(any(CategoryEntity.class), eq(CategoryResponseDTO.class));
        }
    }

    @Nested
    @DisplayName("getCategoryDetails(String, UUID)")
    class GetCategoryDetails {

        @Test
        @DisplayName("returns mapped CategoryResponseDTO when category exists")
        void returnsMappedDtoWhenCategoryExists() {
            UUID id = category.getId();

            CategoryResponseDTO responseDto = new CategoryResponseDTO();
            responseDto.setId(id);
            responseDto.setName(category.getName());
            responseDto.setSlug(category.getSlug());
            responseDto.setDescription(category.getDescription());

            when(categoryRepository.findById(id)).thenReturn(Optional.of(category));
            when(modelMapper.map(category, CategoryResponseDTO.class)).thenReturn(responseDto);

            CategoryResponseDTO result = categoryService.getCategoryDetails(category.getSlug(), id);

            assertNotNull(result);
            assertEquals(category.getName(), result.getName());
            assertEquals(category.getSlug(), result.getSlug());

            verify(categoryRepository).findById(id);
            verify(validationUtils).isInvalidCategory(category, category.getSlug());
            verify(modelMapper).map(category, CategoryResponseDTO.class);
        }

        @Test
        @DisplayName("propagates exception when requested category is invalid according to ValidationUtils")
        void throwsWhenCategoryMissingOrInvalid() {
            UUID missingId = UUID.randomUUID();

            when(categoryRepository.findById(missingId)).thenReturn(Optional.empty());
            doThrow(new ResourceNotFoundException("not found")).when(validationUtils).isInvalidCategory(any(), eq("no-slug"));

            assertThrows(ResourceNotFoundException.class, () -> categoryService.getCategoryDetails("no-slug", missingId));

            verify(categoryRepository).findById(missingId);
            verify(validationUtils).isInvalidCategory(null, "no-slug");
            verify(modelMapper, never()).map(any(CategoryEntity.class), eq(CategoryResponseDTO.class));
        }
    }

    @Nested
    @DisplayName("updateCategory(String, UUID, CategoryRequestDTO)")
    class UpdateCategory {

        @Test
        @DisplayName("returns updated CategoryResponseDTO when category exists and slug is available")
        void returnsUpdatedCategoryResponseWhenSlugAvailable() {
            CategoryRequestDTO requestDTO = new CategoryRequestDTO();
            requestDTO.setName(category.getName() + " updated");
            requestDTO.setDescription(category.getDescription() + " updated");

            CategoryEntity categoryToUpdate = category;

            CategoryResponseDTO responseDto = new CategoryResponseDTO();
            responseDto.setId(categoryToUpdate.getId());
            responseDto.setName(requestDTO.getName());
            responseDto.setDescription(requestDTO.getDescription());
            responseDto.setSlug("test-category-main-updated");

            when(categoryRepository.findById(categoryToUpdate.getId())).thenReturn(Optional.of(categoryToUpdate));
            when(categoryRepository.findBySlug("test-category-main-updated")).thenReturn(Optional.empty());
            doAnswer(invocation -> {
                CategoryRequestDTO source = invocation.getArgument(0);
                CategoryEntity target = invocation.getArgument(1);
                target.setName(source.getName());
                target.setDescription(source.getDescription());
                return null;
            }).when(modelMapper).map(requestDTO, categoryToUpdate);
            when(modelMapper.map(categoryToUpdate, CategoryResponseDTO.class)).thenReturn(responseDto);
            when(categoryRepository.saveAndFlush(categoryToUpdate)).thenReturn(categoryToUpdate);

            CategoryResponseDTO result = categoryService.updateCategory(categoryToUpdate.getSlug(), categoryToUpdate.getId(), requestDTO);

            assertNotNull(result);
            assertEquals(requestDTO.getName(), result.getName());
            assertEquals(requestDTO.getDescription(), result.getDescription());

            verify(categoryRepository).findById(categoryToUpdate.getId());
            verify(categoryRepository).findBySlug("test-category-main-updated");
            verify(categoryRepository).saveAndFlush(categoryToUpdate);
            verify(modelMapper).map(requestDTO, categoryToUpdate);
            verify(modelMapper).map(categoryToUpdate, CategoryResponseDTO.class);
        }

        @Test
        @DisplayName("throws ResourceConflictException when new category slug already exists")
        void throwsResourceConflictWhenNewSlugExists() {
            CategoryRequestDTO requestDTO = new CategoryRequestDTO();
            requestDTO.setName(category.getName() + " updated");
            requestDTO.setDescription(category.getDescription() + " updated");

            CategoryEntity categoryToUpdate = category;

            when(categoryRepository.findById(categoryToUpdate.getId())).thenReturn(Optional.of(categoryToUpdate));
            when(categoryRepository.findBySlug("test-category-main-updated")).thenReturn(Optional.of(TestEntityFactory.testCategory("existing")));

            ResourceConflictException ex = assertThrows(ResourceConflictException.class,
                    () -> categoryService.updateCategory(categoryToUpdate.getSlug(), categoryToUpdate.getId(), requestDTO));

            assertNotNull(ex);
            verify(categoryRepository).findById(categoryToUpdate.getId());
            verify(categoryRepository).findBySlug("test-category-main-updated");
            verify(categoryRepository, never()).saveAndFlush(any(CategoryEntity.class));
            verifyNoInteractions(modelMapper);
        }

        @Test
        @DisplayName("propagates validation failure when category does not exist")
        void throwsWhenCategoryDoesNotExist() {
            UUID missingId = UUID.randomUUID();

            when(categoryRepository.findById(missingId)).thenReturn(Optional.empty());
            doThrow(new ResourceNotFoundException("invalid category")).when(validationUtils).isInvalidCategory(null, "missing-slug");

            assertThrows(ResourceNotFoundException.class,
                    () -> categoryService.updateCategory("missing-slug", missingId, new CategoryRequestDTO()));

            verify(categoryRepository).findById(missingId);
            verify(validationUtils).isInvalidCategory(null, "missing-slug");
            verifyNoMoreInteractions(categoryRepository);
            verifyNoInteractions(modelMapper);
        }
    }

    @Nested
    @DisplayName("deleteCategory(String, UUID, String)")
    class DeleteCategory {

        @Test
        @DisplayName("reassigns posts and deletes the old category when both categories exist")
        void reassignsPostsAndDeletesOldCategory() {
            CategoryEntity oldCategory = TestEntityFactory.testCategory("old");
            oldCategory.setId(category.getId());
            CategoryEntity newCategory = TestEntityFactory.testCategory("new");
            newCategory.setId(UUID.randomUUID());

            when(categoryRepository.findById(oldCategory.getId())).thenReturn(Optional.of(oldCategory));
            when(categoryRepository.findBySlug(newCategory.getSlug())).thenReturn(Optional.of(newCategory));
            doNothing().when(postRepository).updatePostsCategory(oldCategory, newCategory);

            categoryService.deleteCategory(oldCategory.getSlug(), oldCategory.getId(), newCategory.getSlug());

            verify(categoryRepository).findById(oldCategory.getId());
            verify(categoryRepository).findBySlug(newCategory.getSlug());
            verify(postRepository).updatePostsCategory(oldCategory, newCategory);
            verify(categoryRepository).deleteById(oldCategory.getId());
        }

        @Test
        @DisplayName("throws InvalidActionException when trying to delete the uncategorised category")
        void throwsInvalidActionWhenDeletingUncategorisedCategory() {
            when(messageService.get("exception.invalid.action.uncategorised_category_deletion"))
                    .thenReturn("cannot delete uncategorised category");

            InvalidActionException ex = assertThrows(InvalidActionException.class,
                    () -> categoryService.deleteCategory("uncategorised", UUID.randomUUID(), "new-category"));

            assertNotNull(ex);
            verify(messageService).get("exception.invalid.action.uncategorised_category_deletion");
            verifyNoInteractions(categoryRepository, postRepository, modelMapper, validationUtils);
        }

        @Test
        @DisplayName("propagates validation failure when replacement category does not exist")
        void throwsWhenReplacementCategoryDoesNotExist() {
            CategoryEntity oldCategory = TestEntityFactory.testCategory("old-missing-new");
            oldCategory.setId(category.getId());

            when(categoryRepository.findById(oldCategory.getId())).thenReturn(Optional.of(oldCategory));
            when(categoryRepository.findBySlug("missing-new-category")).thenReturn(Optional.empty());
            doNothing().when(validationUtils).isInvalidCategory(oldCategory, oldCategory.getSlug());
            doThrow(new ResourceNotFoundException("invalid replacement category"))
                    .when(validationUtils).isInvalidCategory(null, "missing-new-category");

            assertThrows(ResourceNotFoundException.class,
                    () -> categoryService.deleteCategory(oldCategory.getSlug(), oldCategory.getId(), "missing-new-category"));

            verify(categoryRepository).findById(oldCategory.getId());
            verify(categoryRepository).findBySlug("missing-new-category");
            verify(validationUtils).isInvalidCategory(oldCategory, oldCategory.getSlug());
            verify(validationUtils).isInvalidCategory(null, "missing-new-category");
            verifyNoInteractions(postRepository);
        }
    }
}
