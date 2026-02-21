package com.zestindia.dhananjay.ProductModule.dto.ProductDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponseDTO {
    private Long id;
    private String productName;
    private Long createdBy;
    private LocalDateTime createdOn;
    private Long modifiedBy;
    private LocalDateTime modifiedOn;
    //private List<ItemResponseDTO> items;
    private ItemResponseDTO items;
}
