package com.projet.fileanalysis.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO pour un point de mesure de fichier analysé et agrégé.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileAnalysisPointDTO {

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime horodatage;

    private BigDecimal valeur;
}
