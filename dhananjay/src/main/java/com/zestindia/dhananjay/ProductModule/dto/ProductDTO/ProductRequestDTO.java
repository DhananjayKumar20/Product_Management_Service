package com.zestindia.dhananjay.ProductModule.dto.ProductDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequestDTO {
    private String productName;
    //private List<ItemRequestDTO> items;
    private ItemRequestDTO items;
}
