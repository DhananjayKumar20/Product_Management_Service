package com.zestindia.dhananjay.ProductModule.controller;

import com.zestindia.dhananjay.ProductModule.dto.ProductDTO.ItemResponseDTO;
import com.zestindia.dhananjay.ProductModule.dto.ProductDTO.ProductRequestDTO;
import com.zestindia.dhananjay.ProductModule.dto.ProductDTO.ProductResponseDTO;
import com.zestindia.dhananjay.ProductModule.service.Interface.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1")
public class ProductController {

    private final ProductService productService;

//<<-----------------------------------CREATE PRODUCT----------------------------------->>
    @PostMapping("/products")
    public ResponseEntity<ProductResponseDTO> createProduct(
            @RequestBody ProductRequestDTO request){

        ProductResponseDTO create = productService.createProduct(request);
        return ResponseEntity.ok(create);
    }

//<<-----------------------------------UPDATE PRODUCT--------------------------------->>

    @PutMapping("/products/{id}")
    public ResponseEntity<ProductResponseDTO> updateProduct(
            @PathVariable Long id,
            @RequestBody ProductRequestDTO request) {
        ProductResponseDTO updated = productService.updateProduct(id, request);
        return ResponseEntity.ok(updated);
    }

//<<------------------------------------GET ALL PRODUCTS ------------------------------------>>

    @GetMapping("/products")
    public ResponseEntity<Page<ProductResponseDTO>> getAllProducts(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        Page<ProductResponseDTO> products = productService.getAllProducts(page, size);
        return ResponseEntity.ok(products);
    }

//<<-----------------------------------GET PRODUCT BY ID----------------------------------->>

    @GetMapping("/products/{id}")
    public ResponseEntity<ProductResponseDTO> getProductById(@PathVariable Long id) {
        ProductResponseDTO product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }

//<<-----------------------------------DELETE PRODUCT BY ID----------------------------------->>

    @DeleteMapping("/products/{id}")
    public ResponseEntity<String> deleteProduct(@PathVariable Long id) {
        productService.deleteProductById(id);
        return ResponseEntity.ok("Product deleted successfully");
    }

//<<-----------------------------------GET ITEMS OF PRODUCT--------------------------------->>

    @GetMapping("/products/{id}/items")
    public ResponseEntity<ItemResponseDTO> getProductItemById(@PathVariable Long id) {
        ItemResponseDTO item = productService.getProductItemById(id);
        return ResponseEntity.ok(item);
    }
}