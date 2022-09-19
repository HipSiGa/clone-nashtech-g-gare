package com.yas.product.service;

import com.yas.product.exception.BadRequestException;
import com.yas.product.exception.NotFoundException;
import com.yas.product.model.Brand;
import com.yas.product.model.Category;
import com.yas.product.model.Product;
import com.yas.product.model.ProductCategory;
import com.yas.product.repository.BrandRepository;
import com.yas.product.repository.CategoryRepository;
import com.yas.product.repository.ProductCategoryRepository;
import com.yas.product.repository.ProductRepository;
import com.yas.product.viewmodel.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ProductServiceTest {

    ProductRepository productRepository;
    MediaService mediaService;
    BrandRepository brandRepository;
    CategoryRepository categoryRepository;
    ProductCategoryRepository productCategoryRepository;
    ProductService productService;

    ProductPostVm productPostVm;
    List<Category> categoryList;
    Category category1;
    Category category2;
    List<Product> products;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        mediaService = mock(MediaService.class);
        brandRepository = mock(BrandRepository.class);
        productRepository = mock(ProductRepository.class);
        categoryRepository = mock(CategoryRepository.class);
        productCategoryRepository = mock(ProductCategoryRepository.class);
        productService = new ProductService(
                productRepository,
                mediaService,
                brandRepository,
                productCategoryRepository,
                categoryRepository);

        productPostVm = new ProductPostVm(
                "name",
                "slug",
                1L,
                List.of(1L, 2L),
                "Short description",
                "description",
                "specification",
                "sku",
                "gtin",
                "meta keyword",
                "meta desciption",
                null
        );

        category1 = new Category(1L, null, null, "null", null, null, null, null, null, null);
        category2 = new Category(2L, null, null, "null", null, null, null, null, null, null);
        categoryList = List.of(category1, category2);
        products = List.of(
                new Product(1L, "product1", null, null, null, null, null, "slug", null, null, 1L, null, null, null),
                new Product(2L, "product2", null, null, null, null, null, "slug", null, null, 2L, null, null, null)
        );
//        Product product = new Product()
    }


    @Test
    void getProducts_ExistProductsInDatabase_Sucsess() {
        //given
        List<ProductListVm> productListVmExpected = List.of(
                new ProductListVm(1L, "product1", "slug"),
                new ProductListVm(2L, "product2", "slug")
        );
        when(productRepository.findAll()).thenReturn(products);

        //when
        List<ProductListVm> productListVmActual = productService.getProducts();

        //then
        assertThat(productListVmActual).hasSameSizeAs(productListVmExpected);
        assertThat(productListVmActual.get(0)).isEqualTo(productListVmExpected.get(0));
        assertThat(productListVmActual.get(1)).isEqualTo(productListVmExpected.get(1));

    }

    @Test
    void createProduct_TheRequestIsValid_Success() {
        //given
        var productCaptor = ArgumentCaptor.forClass(Product.class);
        Brand brand = mock(Brand.class);
        var productCategoryListCaptor = ArgumentCaptor.forClass(List.class);
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        String username = "admin";
        NoFileMediaVm noFileMediaVm = mock(NoFileMediaVm.class);

        when(brandRepository.findById(productPostVm.brandId())).thenReturn(Optional.of(brand));
        when(categoryRepository.findAllById(productPostVm.categoryIds())).thenReturn(categoryList);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.getName()).thenReturn(username);
        when(mediaService.SaveFile(productPostVm.thumbnail(), "", "")).thenReturn(noFileMediaVm);
        Product savedProduct = mock(Product.class);
        when(productRepository.saveAndFlush(productCaptor.capture())).thenReturn(savedProduct);

        //when
        ProductGetDetailVm actualResponse = productService.createProduct(productPostVm);

        //then
        verify(productRepository).saveAndFlush(productCaptor.capture());
        Product productValue = productCaptor.getValue();
        assertThat(productValue.getBrand()).isEqualTo(brand);
        assertThat(productValue.getName()).isEqualTo(productPostVm.name());
        assertThat(productValue.getSlug()).isEqualTo(productPostVm.slug());
        assertThat(productValue.getDescription()).isEqualTo(productPostVm.description());
        assertThat(productValue.getShortDescription()).isEqualTo(productPostVm.shortDescription());
        assertThat(productValue.getSpecification()).isEqualTo(productPostVm.specification());
        assertThat(productValue.getSku()).isEqualTo(productPostVm.sku());
        assertThat(productValue.getGtin()).isEqualTo(productPostVm.gtin());
        assertThat(productValue.getMetaKeyword()).isEqualTo(productPostVm.metaKeyword());
        assertThat(productValue.getMetaDescription()).isEqualTo(productPostVm.metaDescription());
        assertThat(productValue.getCreatedBy()).isEqualTo(username);
        assertThat(productValue.getLastModifiedBy()).isEqualTo(username);
        assertThat(productValue.getThumbnailMediaId()).isEqualTo(noFileMediaVm.id());

        verify(productCategoryRepository).saveAllAndFlush(productCategoryListCaptor.capture());
        List<ProductCategory> productCategoryListValue = productCategoryListCaptor.getValue();
        assertThat(productCategoryListValue).hasSize(2);
        assertThat(productCategoryListValue.get(0).getCategory()).isEqualTo(category1);
        assertThat(productCategoryListValue.get(1).getCategory()).isEqualTo(category2);
        assertThat(productCategoryListValue.get(0).getProduct()).isEqualTo(productValue);
        assertThat(productCategoryListValue.get(1).getProduct()).isEqualTo(productValue);

    }

    @Test
    void createProduct_TheRequestContainsNonExistCategoryIdInCategoryList_ThrowsBadRequestException() {
        //given
        List<Category> categoryList = new ArrayList<>();
        categoryList.add(new Category(1L, null, null, "null", null, null, null, null, null, null));

        List<Long> categoryIds= new ArrayList<>();
        categoryIds.add(1L);
        categoryIds.add(2L);
        categoryIds.add(3L);

        Brand brand = mock(Brand.class);
        ProductPostVm productPostVm = new ProductPostVm(
                "name",
                "slug",
                1L,
                categoryIds,
                "Short description",
                "description",
                "specification",
                "sku",
                "gtin",
                "meta keyword",
                "meta desciption",
                null
        );

        when(brandRepository.findById(productPostVm.brandId())).thenReturn(Optional.of(brand));
        when(categoryRepository.findAllById(productPostVm.categoryIds())).thenReturn(categoryList);
        List<Long> categoryIdsNotFound = productPostVm.categoryIds();

        //when
        BadRequestException exception = assertThrows(BadRequestException.class, () -> productService.createProduct(productPostVm));

        //then
        assertThat(exception.getMessage()).isEqualTo(String.format("Not found categories %s", categoryIdsNotFound));
    }

    @DisplayName("Create product throws Bad Request Exception when brand id is non exist- negative case")
    @Test
    void createProduct_BrandIdIsNonExist_ThrowsBadRequestException() {
        //given
        Brand brand = mock(Brand.class);
        List<Category> emptyCategoryList = Collections.emptyList();

        when(brandRepository.findById(productPostVm.brandId())).thenReturn(Optional.of(brand));
        when(categoryRepository.findAllById(productPostVm.categoryIds())).thenReturn(emptyCategoryList);

        //when
        BadRequestException exception = assertThrows(BadRequestException.class, () -> productService.createProduct(productPostVm));

        //then
        assertThat(exception.getMessage()).isEqualTo(String.format("Not found categories %s", productPostVm.categoryIds()));
    }


    @DisplayName("Create product throws Not Found Exception when brand id is null- negative case")
    @Test
    void createProduct_BrandIdIsNull_ThrowNotFoundException() {
        //given
        when(brandRepository.findById(productPostVm.brandId())).thenReturn(Optional.empty());

        //when
        NotFoundException exception = assertThrows(NotFoundException.class, () -> productService.createProduct(productPostVm));

        //then
        assertThat(exception.getMessage()).isEqualTo(String.format("Brand %s is not found", productPostVm.brandId()));
    }

    @DisplayName("Get product feature success then return list ProductThumbnailVm")
    @Test
    void getFeaturedProducts_WhenEverythingIsOkay_Success() {
        //given
        String url = "sample-url";
        NoFileMediaVm noFileMediaVm = mock(NoFileMediaVm.class);

        when(productRepository.findAll()).thenReturn(products);
        when(mediaService.getMedia(anyLong())).thenReturn(noFileMediaVm);
        when(noFileMediaVm.url()).thenReturn(url);

        //when
        List<ProductThumbnailVm> actualResponse = productService.getFeaturedProducts();

        //then
        assertThat(actualResponse).hasSize(2);
        for (int i = 0; i < actualResponse.size(); i++) {
            Product product = products.get(i);
            assertThat(actualResponse.get(i).id()).isEqualTo(product.getId());
            assertThat(actualResponse.get(i).name()).isEqualTo(product.getName());
            assertThat(actualResponse.get(i).slug()).isEqualTo(product.getSlug());
            assertThat(actualResponse.get(i).thumbnailUrl()).isEqualTo(mediaService.getMedia(product.getThumbnailMediaId()).url());
        }
    }


    @DisplayName("Get products by brand when brand is available with slug then success")
    @Test
    void getProductsByBrand_BrandSlugIsValid_Success() {
        //given
        String brandSlug = "iphone";
        String url = "sample-url";
        Brand existingBrand = mock(Brand.class);
        NoFileMediaVm noFileMediaVm = mock(NoFileMediaVm.class);

        when(brandRepository.findBySlug(brandSlug)).thenReturn(Optional.of(existingBrand));
        when(productRepository.findAllByBrand(existingBrand)).thenReturn(products);
        when(mediaService.getMedia(anyLong())).thenReturn(noFileMediaVm);
        when(noFileMediaVm.url()).thenReturn(url);

        //when
        List<ProductThumbnailVm> actualResponse = productService.getProductsByBrand(brandSlug);

        //then
        assertThat(actualResponse).hasSize(2);
        for (int i = 0; i < actualResponse.size(); i++) {
            Product product = products.get(i);
            assertThat(actualResponse.get(i).id()).isEqualTo(product.getId());
            assertThat(actualResponse.get(i).name()).isEqualTo(product.getName());
            assertThat(actualResponse.get(i).slug()).isEqualTo(product.getSlug());
            assertThat(actualResponse.get(i).thumbnailUrl()).isEqualTo(mediaService.getMedia(product.getThumbnailMediaId()).url());
        }
    }

    @DisplayName("Get products by brand when brand is non exist then throws exception")
    @Test
    void getProductsByBrand_BrandIsNonExist_ThrowsNotFoundException() {
        //given
        String brandSlug = "iphone";
        when(brandRepository.findBySlug(brandSlug)).thenReturn(Optional.empty());

        //when
        NotFoundException exception = assertThrows(NotFoundException.class, () -> productService.getProductsByBrand(brandSlug));

        //then
        assertThat(exception.getMessage()).isEqualTo(String.format("Brand %s is not found", brandSlug));
    }


    @DisplayName("Get products by category when exist with the slug then success")
    @Test
    void getProductsByCategory_CategorySlugIsValid_Success() {
        //given
        String categorySlug = "laptop-macbook";
        String url = "sample-url";
        Category existingCategory = mock(Category.class);
        NoFileMediaVm noFileMediaVm = mock(NoFileMediaVm.class);

        List<ProductCategory> productCategoryList = List.of(
                new ProductCategory(1L, products.get(0), null, 1, true),
                new ProductCategory(2L, products.get(1), null, 2, true)
        );

        when(categoryRepository.findBySlug(categorySlug)).thenReturn(Optional.of(existingCategory));
        when(productCategoryRepository.findAllByCategory(existingCategory)).thenReturn(productCategoryList);
        when(mediaService.getMedia(anyLong())).thenReturn(noFileMediaVm);
        when(noFileMediaVm.url()).thenReturn(url);

        //when
        List<ProductThumbnailVm> actualResponse = productService.getProductsByCategory(categorySlug);

        //then
        assertThat(actualResponse).hasSize(2);
        for (int i = 0; i < actualResponse.size(); i++) {
            Product product = products.get(i);
            assertThat(actualResponse.get(i).id()).isEqualTo(product.getId());
            assertThat(actualResponse.get(i).name()).isEqualTo(product.getName());
            assertThat(actualResponse.get(i).slug()).isEqualTo(product.getSlug());
            assertThat(actualResponse.get(i).thumbnailUrl()).isEqualTo(mediaService.getMedia(product.getThumbnailMediaId()).url());
        }
    }

    @DisplayName("Get products by category when category is non exist then throws exception")
    @Test
    void getProductsByCategory_CategoryIsNonExist_ThrowsNotFoundException() {
        //given
        String categorySlug = "laptop-macbook";
        when(categoryRepository.findBySlug(categorySlug)).thenReturn(Optional.empty());

        //when
        NotFoundException exception = assertThrows(NotFoundException.class, () -> productService.getProductsByCategory(categorySlug));

        //then
        assertThat(exception.getMessage()).isEqualTo(String.format("Category %s is not found", categorySlug));
    }

}