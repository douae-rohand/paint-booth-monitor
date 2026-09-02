package com.projet.measures.service;

import com.projet.audit.annotation.Audite;
import com.projet.audit.model.enums.ActionAudit;
import com.projet.measures.dto.MesureCabineDTO;
import com.projet.measures.dto.MesureEtuveDTO;
import com.projet.measures.model.PointMesure;
import com.projet.measures.repository.MesureRepository;
import com.projet.measures.repository.PointMesureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Service pour l'export des mesures historiques en CSV et PDF.
 * Module: measures
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MesureExportService {

    private final MesureRepository mesureRepository;
    private final PointMesureRepository pointMesureRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    /**
     * Exporte l'historique des mesures de la cabine en CSV.
     *
     * @param dateDebut Date de début de la période (optionnel)
     * @param dateFin Date de fin de la période (optionnel)
     * @param seulementDepassements Si true, ne retourne que les lignes avec au moins un dépassement
     * @return ByteArray contenant le fichier CSV
     */
    @Audite(ActionAudit.EXPORT_MESURES)
    public byte[] exportCabineCSV(LocalDateTime dateDebut, LocalDateTime dateFin, boolean seulementDepassements) {
        List<MesureCabineDTO> mesures = getAllHistoriqueCabine(dateDebut, dateFin, seulementDepassements);
        
        if (mesures.isEmpty()) {
            log.info("Aucune mesure cabine à exporter");
            return new byte[0];
        }

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                     .setDelimiter(';')
                     .setHeader("Date", "Heure", "Température", "Humidité")
                     .build())) {

            for (MesureCabineDTO mesure : mesures) {
                printer.printRecord(
                        mesure.timestampCycle().format(DATE_FORMATTER),
                        mesure.timestampCycle().format(TIME_FORMATTER),
                        formatDecimal(mesure.temperature()),
                        formatDecimal(mesure.humidite())
                );
            }

            printer.flush();
            log.info("Export CSV cabine terminé: {} enregistrements", mesures.size());
            return outputStream.toByteArray();

        } catch (IOException e) {
            log.error("Erreur lors de l'export CSV cabine", e);
            throw new RuntimeException("Erreur lors de l'export CSV", e);
        }
    }

    /**
     * Exporte l'historique des mesures de l'étuve en CSV.
     *
     * @param zone Nom de la zone (optionnel)
     * @param dateDebut Date de début de la période (optionnel)
     * @param dateFin Date de fin de la période (optionnel)
     * @param seulementDepassements Si true, ne retourne que les lignes avec dépassement
     * @return ByteArray contenant le fichier CSV
     */
    @Audite(ActionAudit.EXPORT_MESURES)
    public byte[] exportEtuveCSV(String zone, LocalDateTime dateDebut, LocalDateTime dateFin, boolean seulementDepassements) {
        List<MesureEtuveDTO> mesures = getAllHistoriqueEtuve(zone, dateDebut, dateFin, seulementDepassements);
        
        if (mesures.isEmpty()) {
            log.info("Aucune mesure étuve à exporter");
            return new byte[0];
        }

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                     .setDelimiter(';')
                     .setHeader("Date", "Heure", "Zone", "Température")
                     .build())) {

            for (MesureEtuveDTO mesure : mesures) {
                printer.printRecord(
                        mesure.dateMesure().format(DATE_FORMATTER),
                        mesure.dateMesure().format(TIME_FORMATTER),
                        mesure.zone(),
                        formatDecimal(mesure.temperature())
                );
            }

            printer.flush();
            log.info("Export CSV étuve terminé: {} enregistrements", mesures.size());
            return outputStream.toByteArray();

        } catch (IOException e) {
            log.error("Erreur lors de l'export CSV étuve", e);
            throw new RuntimeException("Erreur lors de l'export CSV", e);
        }
    }

    /**
     * Exporte l'historique des mesures de la cabine en PDF.
     *
     * @param dateDebut Date de début de la période (optionnel)
     * @param dateFin Date de fin de la période (optionnel)
     * @param seulementDepassements Si true, ne retourne que les lignes avec au moins un dépassement
     * @return ByteArray contenant le fichier PDF
     */
    @Audite(ActionAudit.EXPORT_MESURES)
    public byte[] exportCabinePDF(LocalDateTime dateDebut, LocalDateTime dateFin, boolean seulementDepassements) {
        List<MesureCabineDTO> mesures = getAllHistoriqueCabine(dateDebut, dateFin, seulementDepassements);
        
        if (mesures.isEmpty()) {
            log.info("Aucune mesure cabine à exporter");
            return new byte[0];
        }

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            com.lowagie.text.Document document = new com.lowagie.text.Document(com.lowagie.text.PageSize.A4, 50, 50, 50, 50);
            com.lowagie.text.pdf.PdfWriter writer = com.lowagie.text.pdf.PdfWriter.getInstance(document, outputStream);
            document.open();

            // Couleurs modernes
            Color primaryColor = new Color(253, 186, 140); // Orange pastel
            Color headerBgColor = new Color(243, 244, 246); // Light gray
            Color rowEvenColor = new Color(255, 255, 255); // White
            Color rowOddColor = new Color(249, 250, 251); // Light gray
            Color textColor = new Color(30, 41, 59); // Dark slate

            // Titre avec couleur
            com.lowagie.text.Font titleFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 22, com.lowagie.text.Font.BOLD, primaryColor);
            com.lowagie.text.Paragraph title = new com.lowagie.text.Paragraph("Historique des mesures", titleFont);
            title.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            title.setSpacingAfter(10);
            document.add(title);

            // Sous-titre
            com.lowagie.text.Font subtitleFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 16, com.lowagie.text.Font.BOLD, textColor);
            com.lowagie.text.Paragraph subtitle = new com.lowagie.text.Paragraph("Cabine", subtitleFont);
            subtitle.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(15);
            document.add(subtitle);

            // Période avec style moderne
            com.lowagie.text.Font labelFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.BOLD, textColor);
            com.lowagie.text.Font valueFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.NORMAL, textColor);
            
            String periodeText = "Période: ";
            if (dateDebut != null && dateFin != null) {
                periodeText += dateDebut.format(DATE_FORMATTER) + " au " + dateFin.format(DATE_FORMATTER);
            } else if (dateDebut != null) {
                periodeText += "à partir du " + dateDebut.format(DATE_FORMATTER);
            } else if (dateFin != null) {
                periodeText += "jusqu'au " + dateFin.format(DATE_FORMATTER);
            } else {
                periodeText += "Toutes les mesures";
            }
            com.lowagie.text.Paragraph periode = new com.lowagie.text.Paragraph(periodeText, valueFont);
            periode.setSpacingBefore(15);
            document.add(periode);

            // Date de génération et nombre total
            String metaText = "Généré le: " + LocalDateTime.now().format(DATE_FORMATTER) + " à " + 
                    LocalDateTime.now().format(TIME_FORMATTER) + " | Total: " + mesures.size() + " mesures";
            com.lowagie.text.Paragraph meta = new com.lowagie.text.Paragraph(metaText, valueFont);
            meta.setSpacingAfter(20);
            document.add(meta);

            // Tableau avec design moderne
            com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(4);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10);
            table.setSpacingAfter(10);
            float[] columnWidths = {2f, 2f, 2f, 2f};
            table.setWidths(columnWidths);

            // En-têtes avec fond coloré
            com.lowagie.text.Font headerFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 11, com.lowagie.text.Font.BOLD, textColor);
            table.addCell(createCellWithBg("Date", headerFont, headerBgColor));
            table.addCell(createCellWithBg("Heure", headerFont, headerBgColor));
            table.addCell(createCellWithBg("Température", headerFont, headerBgColor));
            table.addCell(createCellWithBg("Humidité", headerFont, headerBgColor));

            // Données avec alternance de couleurs
            com.lowagie.text.Font dataFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.NORMAL, textColor);
            for (int i = 0; i < mesures.size(); i++) {
                MesureCabineDTO mesure = mesures.get(i);
                Color rowColor = (i % 2 == 0) ? rowEvenColor : rowOddColor;
                table.addCell(createCellWithBg(mesure.timestampCycle().format(DATE_FORMATTER), dataFont, rowColor));
                table.addCell(createCellWithBg(mesure.timestampCycle().format(TIME_FORMATTER), dataFont, rowColor));
                table.addCell(createCellWithBg(formatDecimal(mesure.temperature()), dataFont, rowColor));
                table.addCell(createCellWithBg(formatDecimal(mesure.humidite()), dataFont, rowColor));
            }

            document.add(table);
            document.close();
            writer.close();

            log.info("Export PDF cabine terminé: {} enregistrements", mesures.size());
            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("Erreur lors de l'export PDF cabine", e);
            throw new RuntimeException("Erreur lors de l'export PDF", e);
        }
    }

    /**
     * Exporte l'historique des mesures de l'étuve en PDF.
     *
     * @param zone Nom de la zone (optionnel)
     * @param dateDebut Date de début de la période (optionnel)
     * @param dateFin Date de fin de la période (optionnel)
     * @param seulementDepassements Si true, ne retourne que les lignes avec dépassement
     * @return ByteArray contenant le fichier PDF
     */
    @Audite(ActionAudit.EXPORT_MESURES)
    public byte[] exportEtuvePDF(String zone, LocalDateTime dateDebut, LocalDateTime dateFin, boolean seulementDepassements) {
        List<MesureEtuveDTO> mesures = getAllHistoriqueEtuve(zone, dateDebut, dateFin, seulementDepassements);
        
        if (mesures.isEmpty()) {
            log.info("Aucune mesure étuve à exporter");
            return new byte[0];
        }

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            com.lowagie.text.Document document = new com.lowagie.text.Document(com.lowagie.text.PageSize.A4, 50, 50, 50, 50);
            com.lowagie.text.pdf.PdfWriter writer = com.lowagie.text.pdf.PdfWriter.getInstance(document, outputStream);
            document.open();

            // Couleurs modernes
            Color primaryColor = new Color(253, 186, 140); // Orange pastel
            Color headerBgColor = new Color(243, 244, 246); // Light gray
            Color rowEvenColor = new Color(255, 255, 255); // White
            Color rowOddColor = new Color(249, 250, 251); // Light gray
            Color textColor = new Color(30, 41, 59); // Dark slate

            // Titre avec couleur
            com.lowagie.text.Font titleFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 22, com.lowagie.text.Font.BOLD, primaryColor);
            com.lowagie.text.Paragraph title = new com.lowagie.text.Paragraph("Historique des mesures", titleFont);
            title.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            title.setSpacingAfter(10);
            document.add(title);

            // Sous-titre
            com.lowagie.text.Font subtitleFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 16, com.lowagie.text.Font.BOLD, textColor);
            com.lowagie.text.Paragraph subtitle = new com.lowagie.text.Paragraph("Étuve", subtitleFont);
            subtitle.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(15);
            document.add(subtitle);

            // Période avec style moderne
            com.lowagie.text.Font valueFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.NORMAL, textColor);
            
            String periodeText = "Période: ";
            if (dateDebut != null && dateFin != null) {
                periodeText += dateDebut.format(DATE_FORMATTER) + " au " + dateFin.format(DATE_FORMATTER);
            } else if (dateDebut != null) {
                periodeText += "à partir du " + dateDebut.format(DATE_FORMATTER);
            } else if (dateFin != null) {
                periodeText += "jusqu'au " + dateFin.format(DATE_FORMATTER);
            } else {
                periodeText += "Toutes les mesures";
            }
            com.lowagie.text.Paragraph periode = new com.lowagie.text.Paragraph(periodeText, valueFont);
            periode.setSpacingBefore(15);
            document.add(periode);

            // Date de génération et nombre total
            String metaText = "Généré le: " + LocalDateTime.now().format(DATE_FORMATTER) + " à " + 
                    LocalDateTime.now().format(TIME_FORMATTER) + " | Total: " + mesures.size() + " mesures";
            com.lowagie.text.Paragraph meta = new com.lowagie.text.Paragraph(metaText, valueFont);
            meta.setSpacingAfter(20);
            document.add(meta);

            // Tableau avec design moderne
            com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(4);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10);
            table.setSpacingAfter(10);
            float[] columnWidths = {2f, 2f, 2f, 2f};
            table.setWidths(columnWidths);

            // En-têtes avec fond coloré
            com.lowagie.text.Font headerFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 11, com.lowagie.text.Font.BOLD, textColor);
            table.addCell(createCellWithBg("Date", headerFont, headerBgColor));
            table.addCell(createCellWithBg("Heure", headerFont, headerBgColor));
            table.addCell(createCellWithBg("Zone", headerFont, headerBgColor));
            table.addCell(createCellWithBg("Température", headerFont, headerBgColor));

            // Données avec alternance de couleurs
            com.lowagie.text.Font dataFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.NORMAL, textColor);
            for (int i = 0; i < mesures.size(); i++) {
                MesureEtuveDTO mesure = mesures.get(i);
                Color rowColor = (i % 2 == 0) ? rowEvenColor : rowOddColor;
                table.addCell(createCellWithBg(mesure.dateMesure().format(DATE_FORMATTER), dataFont, rowColor));
                table.addCell(createCellWithBg(mesure.dateMesure().format(TIME_FORMATTER), dataFont, rowColor));
                table.addCell(createCellWithBg(mesure.zone(), dataFont, rowColor));
                table.addCell(createCellWithBg(formatDecimal(mesure.temperature()), dataFont, rowColor));
            }

            document.add(table);
            document.close();
            writer.close();

            log.info("Export PDF étuve terminé: {} enregistrements", mesures.size());
            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("Erreur lors de l'export PDF étuve", e);
            throw new RuntimeException("Erreur lors de l'export PDF", e);
        }
    }

    /**
     * Exporte l'historique des mesures de la cabine en Excel (.xlsx).
     *
     * @param dateDebut Date de début de la période (optionnel)
     * @param dateFin Date de fin de la période (optionnel)
     * @param seulementDepassements Si true, ne retourne que les lignes avec au moins un dépassement
     * @return ByteArray contenant le fichier Excel
     */
    @Audite(ActionAudit.EXPORT_MESURES)
    public byte[] exportCabineExcel(LocalDateTime dateDebut, LocalDateTime dateFin, boolean seulementDepassements) {
        List<MesureCabineDTO> mesures = getAllHistoriqueCabine(dateDebut, dateFin, seulementDepassements);
        
        if (mesures.isEmpty()) {
            log.info("Aucune mesure cabine à exporter");
            return new byte[0];
        }

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             XSSFWorkbook workbook = new XSSFWorkbook()) {
            
            Sheet sheet = workbook.createSheet("Mesures Cabine");
            
            // Style pour l'en-tête
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 11);
            
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.ORANGE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            
            // Style pour les données
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            
            // Créer l'en-tête
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Date", "Heure", "Température", "Humidité"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // Remplir les données
            for (int i = 0; i < mesures.size(); i++) {
                MesureCabineDTO mesure = mesures.get(i);
                Row row = sheet.createRow(i + 1);
                
                Cell dateCell = row.createCell(0);
                dateCell.setCellValue(mesure.timestampCycle().format(DATE_FORMATTER));
                dateCell.setCellStyle(dataStyle);
                
                Cell heureCell = row.createCell(1);
                heureCell.setCellValue(mesure.timestampCycle().format(TIME_FORMATTER));
                heureCell.setCellStyle(dataStyle);
                
                Cell tempCell = row.createCell(2);
                tempCell.setCellValue(formatDecimal(mesure.temperature()));
                tempCell.setCellStyle(dataStyle);
                
                Cell humidCell = row.createCell(3);
                humidCell.setCellValue(formatDecimal(mesure.humidite()));
                humidCell.setCellStyle(dataStyle);
            }
            
            // Ajuster automatiquement les colonnes
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // Activer les filtres
            sheet.setAutoFilter(new CellRangeAddress(0, mesures.size(), 0, headers.length - 1));
            
            // Gel de la première ligne
            sheet.createFreezePane(0, 1);
            
            workbook.write(outputStream);
            log.info("Export Excel cabine terminé: {} enregistrements", mesures.size());
            return outputStream.toByteArray();
            
        } catch (IOException e) {
            log.error("Erreur lors de l'export Excel cabine", e);
            throw new RuntimeException("Erreur lors de l'export Excel", e);
        }
    }

    /**
     * Exporte l'historique des mesures de l'étuve en Excel (.xlsx).
     *
     * @param zone Nom de la zone (optionnel)
     * @param dateDebut Date de début de la période (optionnel)
     * @param dateFin Date de fin de la période (optionnel)
     * @param seulementDepassements Si true, ne retourne que les lignes avec dépassement
     * @return ByteArray contenant le fichier Excel
     */
    @Audite(ActionAudit.EXPORT_MESURES)
    public byte[] exportEtuveExcel(String zone, LocalDateTime dateDebut, LocalDateTime dateFin, boolean seulementDepassements) {
        List<MesureEtuveDTO> mesures = getAllHistoriqueEtuve(zone, dateDebut, dateFin, seulementDepassements);
        
        if (mesures.isEmpty()) {
            log.info("Aucune mesure étuve à exporter");
            return new byte[0];
        }

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             XSSFWorkbook workbook = new XSSFWorkbook()) {
            
            Sheet sheet = workbook.createSheet("Mesures Étuve");
            
            // Style pour l'en-tête
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 11);
            
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.ORANGE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            
            // Style pour les données
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            
            // Créer l'en-tête
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Date", "Heure", "Zone", "Température"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // Remplir les données
            for (int i = 0; i < mesures.size(); i++) {
                MesureEtuveDTO mesure = mesures.get(i);
                Row row = sheet.createRow(i + 1);
                
                Cell dateCell = row.createCell(0);
                dateCell.setCellValue(mesure.dateMesure().format(DATE_FORMATTER));
                dateCell.setCellStyle(dataStyle);
                
                Cell heureCell = row.createCell(1);
                heureCell.setCellValue(mesure.dateMesure().format(TIME_FORMATTER));
                heureCell.setCellStyle(dataStyle);
                
                Cell zoneCell = row.createCell(2);
                zoneCell.setCellValue(mesure.zone());
                zoneCell.setCellStyle(dataStyle);
                
                Cell tempCell = row.createCell(3);
                tempCell.setCellValue(formatDecimal(mesure.temperature()));
                tempCell.setCellStyle(dataStyle);
            }
            
            // Ajuster automatiquement les colonnes
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // Activer les filtres
            sheet.setAutoFilter(new CellRangeAddress(0, mesures.size(), 0, headers.length - 1));
            
            // Gel de la première ligne
            sheet.createFreezePane(0, 1);
            
            workbook.write(outputStream);
            log.info("Export Excel étuve terminé: {} enregistrements", mesures.size());
            return outputStream.toByteArray();
            
        } catch (IOException e) {
            log.error("Erreur lors de l'export Excel étuve", e);
            throw new RuntimeException("Erreur lors de l'export Excel", e);
        }
    }

    /**
     * Récupère toutes les mesures de la cabine sans pagination.
     */
    private List<MesureCabineDTO> getAllHistoriqueCabine(LocalDateTime dateDebut, LocalDateTime dateFin, boolean seulementDepassements) {
        // Résoudre l'ID du PointMesure cabine
        List<PointMesure> cabines = pointMesureRepository.findByTypeEmplacement("CABINE");
        if (cabines.isEmpty()) {
            log.warn("Aucun point de mesure de type CABINE trouvé");
            return List.of();
        }

        PointMesure cabine = cabines.stream()
                .filter(PointMesure::isActif)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Aucun point de mesure cabine actif trouvé"));

        // Récupérer toutes les données sans pagination (limit = Integer.MAX_VALUE)
        List<Object[]> results = mesureRepository.findHistoriqueCabine(
                cabine.getId(), dateDebut, dateFin, seulementDepassements, Integer.MAX_VALUE, 0);

        List<MesureCabineDTO> dtos = new ArrayList<>();
        for (Object[] row : results) {
            LocalDateTime timestampCycle = (LocalDateTime) row[0];
            String caisseId = (String) row[1];
            BigDecimal temperature = row[2] != null ? (BigDecimal) row[2] : null;
            BigDecimal humidite = row[3] != null ? (BigDecimal) row[3] : null;
            boolean depassementTemperature = row[4] != null && ((Boolean) row[4]);
            boolean depassementHumidite = row[5] != null && ((Boolean) row[5]);

            dtos.add(new MesureCabineDTO(
                    timestampCycle,
                    caisseId,
                    temperature,
                    humidite,
                    depassementTemperature,
                    depassementHumidite
            ));
        }

        return dtos;
    }

    /**
     * Récupère toutes les mesures de l'étuve sans pagination.
     */
    private List<MesureEtuveDTO> getAllHistoriqueEtuve(String zone, LocalDateTime dateDebut, LocalDateTime dateFin, boolean seulementDepassements) {
        // Résoudre les IDs des points de mesure étuve
        List<Long> idsZones = null;
        if (zone != null && !zone.trim().isEmpty()) {
            PointMesure pointMesure = pointMesureRepository.findByNom(zone)
                    .orElseThrow(() -> new IllegalArgumentException("Point de mesure non trouvé pour la zone: " + zone));

            if (!"ETUVE".equalsIgnoreCase(pointMesure.getTypeEmplacement())) {
                throw new IllegalArgumentException("Le point de mesure " + zone + " n'est pas de type ETUVE");
            }

            idsZones = List.of(pointMesure.getId());
        } else {
            List<PointMesure> etuves = pointMesureRepository.findByTypeEmplacement("ETUVE");
            idsZones = etuves.stream()
                    .filter(PointMesure::isActif)
                    .map(PointMesure::getId)
                    .toList();

            if (idsZones.isEmpty()) {
                log.warn("Aucun point de mesure de type ETUVE actif trouvé");
                return List.of();
            }
        }

        // Récupérer toutes les données sans pagination (limit = Integer.MAX_VALUE)
        List<Object[]> results = mesureRepository.findHistoriqueEtuve(
                idsZones != null ? idsZones.toArray(new Long[0]) : null, 
                dateDebut, dateFin, seulementDepassements, Integer.MAX_VALUE, 0);

        List<MesureEtuveDTO> dtos = new ArrayList<>();
        for (Object[] row : results) {
            java.util.UUID idMesure = (java.util.UUID) row[0];
            LocalDateTime dateMesure = (LocalDateTime) row[1];
            String zoneNom = (String) row[2];
            BigDecimal temperature = row[3] != null ? (BigDecimal) row[3] : null;
            boolean depassement = row[4] != null && ((Boolean) row[4]);

            dtos.add(new MesureEtuveDTO(
                    idMesure,
                    dateMesure,
                    zoneNom,
                    temperature,
                    depassement
            ));
        }

        return dtos;
    }

    /**
     * Formate un BigDecimal pour l'affichage.
     */
    private String formatDecimal(BigDecimal value) {
        return value != null ? value.toString() : "—";
    }

    /**
     * Crée une cellule de tableau avec fond coloré.
     */
    private com.lowagie.text.pdf.PdfPCell createCellWithBg(String text, com.lowagie.text.Font font, Color bgColor) {
        com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(text, font));
        cell.setBackgroundColor(bgColor);
        cell.setPadding(8);
        cell.setBorderColor(new Color(229, 231, 235));
        cell.setBorderWidth(0.5f);
        return cell;
    }

    /**
     * Crée une cellule de tableau simple.
     */
    private com.lowagie.text.pdf.PdfPCell createCell(String text, com.lowagie.text.Font font) {
        com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(text, font));
        cell.setPadding(8);
        return cell;
    }
}
