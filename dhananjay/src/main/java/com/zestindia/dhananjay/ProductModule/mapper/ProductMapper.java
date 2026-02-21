package com.zestindia.dhananjay.ProductModule.mapper;

import com.zestindia.dhananjay.ProductModule.dto.ProductDTO.ItemResponseDTO;
import com.zestindia.dhananjay.ProductModule.dto.ProductDTO.ProductRequestDTO;
import com.zestindia.dhananjay.ProductModule.dto.ProductDTO.ProductResponseDTO;
import com.zestindia.dhananjay.ProductModule.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductMapper {

    public Product toEntity(ProductRequestDTO dto, User user) {
        Product product = Product.builder()
                .productName(dto.getProductName())
                .createdBy(user.getId())
                .createdOn(LocalDateTime.now())
                .build();

        if (dto.getItems() != null) {
            Item item = Item.builder()
                    .quantity(dto.getItems().getQuantity()) // single quantity
                    .product(product)
                    .build();
            product.setItems(List.of(item)); // single item list
        }

        return product;
    }

    public Product updateEntity(Product product, ProductRequestDTO dto, User user) {

        if (dto.getProductName() != null) {
            product.setProductName(dto.getProductName());
        }

        if (dto.getItems() != null) {
            product.getItems().get(0).setQuantity(dto.getItems().getQuantity());
        }

        product.setModifiedBy(user.getId());
        product.setModifiedOn(LocalDateTime.now());
        return product;
    }
    public ProductResponseDTO toDTO(Product product) {
        return ProductResponseDTO.builder()
                .id(product.getId())
                .productName(product.getProductName())
                .createdBy(product.getCreatedBy())
                .createdOn(product.getCreatedOn())
                .modifiedBy(product.getModifiedBy())
                .modifiedOn(product.getModifiedOn())
                .items(product.getItems() != null && !product.getItems().isEmpty() ?
                        ItemResponseDTO.builder()
                                .id(product.getItems().get(0).getId())
                                .quantity(product.getItems().get(0).getQuantity())
                                .build()
                        : null)
                .build();
    }
}