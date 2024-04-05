package com.example.hotelmanagement.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@Data
public class UpdateRoomRequest {
    @NotBlank
    private String roomType;
    @NotBlank
    private BigDecimal roomPrice;
    @NotBlank
    private MultipartFile photo;

}
