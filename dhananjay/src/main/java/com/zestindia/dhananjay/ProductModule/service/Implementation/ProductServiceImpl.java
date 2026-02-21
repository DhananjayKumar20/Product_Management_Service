package com.zestindia.dhananjay.ProductModule.service.Implementation;

import com.zestindia.dhananjay.ProductModule.config.JwtUtils;
import com.zestindia.dhananjay.ProductModule.dto.ProductDTO.ItemResponseDTO;
import com.zestindia.dhananjay.ProductModule.dto.ProductDTO.ProductRequestDTO;
import com.zestindia.dhananjay.ProductModule.dto.ProductDTO.ProductResponseDTO;
import com.zestindia.dhananjay.ProductModule.entity.Product;
import com.zestindia.dhananjay.ProductModule.entity.User;
import com.zestindia.dhananjay.ProductModule.entity.UserType;
import com.zestindia.dhananjay.ProductModule.exception.ProductException;
import com.zestindia.dhananjay.ProductModule.exception.UserNotFoundException;
import com.zestindia.dhananjay.ProductModule.mapper.ProductMapper;
import com.zestindia.dhananjay.ProductModule.repository.ProductRepository;
import com.zestindia.dhananjay.ProductModule.repository.UserRepository;
import com.zestindia.dhananjay.ProductModule.service.Interface.ProductService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ProductServiceImpl implements ProductService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final JwtUtils jwtUtils;

//<<-----------------------------------CREATE PRODUCT--------------------------------->>

    @Override
    @Transactional
    public ProductResponseDTO createProduct(ProductRequestDTO request){

        // Extract userId from JWT
        Long userId = jwtUtils.extractUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if(user.getUserType() != UserType.ADMIN) {
            throw new ProductException("Only ADMIN users can create products");
        }

        if(request.getProductName()==null || request.getProductName().isBlank())
            throw new ProductException("Product name is required");

        Product product = productMapper.toEntity(request, user);
        Product saved = productRepository.save(product);
        return productMapper.toDTO(saved);
    }

//<<-----------------------------------UPDATE PRODUCT--------------------------------->>

    @Override
    @Transactional
    public ProductResponseDTO updateProduct(Long productId, ProductRequestDTO request){

        // Extract userId from JWT
        Long userId = jwtUtils.extractUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if(user.getUserType() != UserType.ADMIN) {
            throw new ProductException("Only ADMIN users can update products");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductException("Product not found with id: " + productId));

        if (!product.getCreatedBy().equals(user.getId())) {
            throw new ProductException("You are not allowed to update this product");
        }

        Product updated = productMapper.updateEntity(product, request, user);
        Product saved = productRepository.save(updated);
        return productMapper.toDTO(saved);
    }

//<<-----------------------------------GET PRODUCT BY ID--------------------------------->>

    @Override
    @Transactional
    public ProductResponseDTO getProductById(Long productId) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductException("Product not found with id: " + productId));

        return productMapper.toDTO(product);
    }

//<<-----------------------------------DELETE PRODUCT BY ID--------------------------------->>

    @Override
    @Transactional
    public void deleteProductById(Long productId) {
        // Extract userId from JWT
        Long userId = jwtUtils.extractUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if(user.getUserType() != UserType.ADMIN) {
            throw new ProductException("Only ADMIN users can delete products");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductException("Product not found with id: " + productId));

        if (!product.getCreatedBy().equals(user.getId())) {
            throw new RuntimeException("You are not allowed to delete this product");
        }
        productRepository.delete(product);
    }

//<<-----------------------------------GET ITEMS OF PRODUCT BY ID--------------------------------->>

    @Override
    @Transactional
    public ItemResponseDTO getProductItemById(Long productId) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductException("Product not found with id: " + productId));

        if (product.getItems() == null || product.getItems().isEmpty()) {
            return null;
        }
        return ItemResponseDTO.builder()
                .id(product.getItems().get(0).getId())
                .quantity(product.getItems().get(0).getQuantity())
                .build();
    }

//<<-----------------------------------GET ALL PRODUCT--------------------------------->>

    @Override
    public Page<ProductResponseDTO> getAllProducts(int page, int size) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "id")
        );
        Page<Product> productPage = productRepository.findAll(pageable);
        return productPage.map(productMapper::toDTO);
    }
}
