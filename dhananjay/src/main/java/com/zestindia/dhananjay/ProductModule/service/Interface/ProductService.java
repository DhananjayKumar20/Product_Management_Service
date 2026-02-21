package com.zestindia.dhananjay.ProductModule.service.Interface;

import com.zestindia.dhananjay.ProductModule.dto.ProductDTO.ItemResponseDTO;
import com.zestindia.dhananjay.ProductModule.dto.ProductDTO.ProductRequestDTO;
import com.zestindia.dhananjay.ProductModule.dto.ProductDTO.ProductResponseDTO;
import org.springframework.data.domain.Page;

public interface ProductService {

    ProductResponseDTO createProduct(ProductRequestDTO request);
    ProductResponseDTO updateProduct(Long id, ProductRequestDTO request);
    ProductResponseDTO getProductById(Long id);
    void deleteProductById(Long id);
    ItemResponseDTO getProductItemById(Long id);
    Page<ProductResponseDTO> getAllProducts(int page, int size);
}
