package com.projet.reports.pdf;

import com.projet.alerting.model.SeuilAbsolu;
import com.projet.alerting.model.enums.Metrique;
import com.projet.alerting.model.enums.TypeAlerte;
import com.projet.measures.dto.MesureHistoriqueDTO;
import com.projet.kpis.dto.KpiResponseDTO;
import com.projet.measures.model.enums.Granularite;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Builder pour la génération de rapports PDF.
 * Module: reports
 *
 * Palette professionnelle industrielle :
 *   BLEU_SECTION  (#1A3A5C) — titres de section
 *   GRIS_HEADER   (#4A4A4A) — en-tête tableau incidents
 *   ZEBRE_CLAIR   (#F2F6FA) — lignes paires du tableau
 *   SEPARATEUR    (#CCCCCC) — ligne horizontale entre sections
 */
public class RapportPdfBuilder {

    // ── Palette ──────────────────────────────────────────────────────────────
    private static final Color BLEU_SECTION  = new Color(26,  58,  92);   // #1A3A5C
    private static final Color GRIS_HEADER   = new Color(74,  74,  74);   // #4A4A4A
    private static final Color ZEBRE_CLAIR   = new Color(242, 246, 250);  // #F2F6FA
    private static final Color BLANC         = Color.WHITE;
    private static final Color SEPARATEUR    = new Color(204, 204, 204);  // #CCCCCC

    // ── Formatters ────────────────────────────────────────────────────────────
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#0.00");

    /**
     * Construit un rapport PDF complet.
     */
    public byte[] build(RapportContext context) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 50, 50, 60, 50);

        PdfWriter writer = PdfWriter.getInstance(document, outputStream);
        writer.setPageEvent(new FooterPageEvent(context.getNomDemandeur(), context.getDateGeneration()));

        document.open();

        addHeader(document, context);
        addSeparator(document);

        addKpisSection(document, context);
        addSeparator(document);

        addChartsSection(document, context);
        addSeparator(document);

        addIncidentsTable(document, context);
        addSeparator(document);

        addStatisticsSummary(document, context);

        document.close();
        return outputStream.toByteArray();
    }

    // ── Séparateur horizontal ─────────────────────────────────────────────────

    private void addSeparator(Document document) throws DocumentException {
        PdfPTable line = new PdfPTable(1);
        line.setWidthPercentage(100);
        line.setSpacingBefore(6);
        line.setSpacingAfter(6);
        PdfPCell cell = new PdfPCell(new Phrase(" "));
        cell.setBorder(Rectangle.TOP);
        cell.setBorderColorTop(SEPARATEUR);
        cell.setBorderWidthTop(0.5f);
        cell.setPaddingTop(0);
        cell.setPaddingBottom(0);
        line.addCell(cell);
        document.add(line);
    }

    // ── Titre de section ──────────────────────────────────────────────────────

    private Paragraph sectionTitle(String text) {
        Font f = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, BLEU_SECTION);
        Paragraph p = new Paragraph(text, f);
        p.setSpacingBefore(14);
        p.setSpacingAfter(8);
        return p;
    }

    // ── En-tête ───────────────────────────────────────────────────────────────

    private void addHeader(Document document, RapportContext context) throws DocumentException {
        Font titleFont   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BLEU_SECTION);
        Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 11, new Color(80, 80, 80));

        Paragraph title = new Paragraph("Rapport de Supervision", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(4);
        document.add(title);

        Paragraph pointName = new Paragraph(context.getNomPointMesure(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, BLEU_SECTION));
        pointName.setAlignment(Element.ALIGN_CENTER);
        pointName.setSpacingAfter(10);
        document.add(pointName);

        Paragraph periode = new Paragraph(
                "Période : du " + context.getDateDebut().format(DATE_FORMATTER)
                        + " au " + context.getDateFin().format(DATE_FORMATTER),
                subtitleFont);
        periode.setAlignment(Element.ALIGN_CENTER);
        periode.setSpacingAfter(4);
        document.add(periode);

        Paragraph meta = new Paragraph(
                "Généré le " + context.getDateGeneration().format(DATE_FORMATTER)
                        + " par " + context.getNomDemandeur(),
                subtitleFont);
        meta.setAlignment(Element.ALIGN_CENTER);
        meta.setSpacingAfter(10);
        document.add(meta);
    }

    // ── Section KPIs ─────────────────────────────────────────────────────────

    private void addKpisSection(Document document, RapportContext context) throws DocumentException {
        document.add(sectionTitle("Indicateurs de Performance (KPIs)"));

        Font metrFont  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, BLEU_SECTION);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
        Font boldFont   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);

        for (Map.Entry<Metrique, KpiResponseDTO> entry : context.getKpisByMetrique().entrySet()) {
            Metrique metrique = entry.getKey();
            KpiResponseDTO kpi = entry.getValue();

            Paragraph metrTitle = new Paragraph("Métrique : " + metrique.name(), metrFont);
            metrTitle.setSpacingBefore(8);
            metrTitle.setSpacingAfter(4);
            document.add(metrTitle);

            PdfPTable kpiTable = new PdfPTable(2);
            kpiTable.setWidthPercentage(80);
            kpiTable.setHorizontalAlignment(Element.ALIGN_LEFT);
            kpiTable.setSpacingAfter(6);

            addKpiRow(kpiTable, "Taux de conformité", kpi.getTauxConformite(), "%", normalFont, boldFont);
            addKpiRow(kpiTable, "Temps moyen entre incidents", kpi.getTempsMoyenEntreIncidentsHeures(), "h", normalFont, boldFont);
            addKpiRow(kpiTable, "Temps moyen de retour à la normale", kpi.getTempsMoyenRetourNormalHeures(), "h", normalFont, boldFont);

            document.add(kpiTable);
        }
    }

    private void addKpiRow(PdfPTable table, String label, Double value, String unit,
                           Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPaddingBottom(4);
        table.addCell(labelCell);

        String valueStr = value != null ? DECIMAL_FORMAT.format(value) + " " + unit : "—";
        PdfPCell valueCell = new PdfPCell(new Phrase(valueStr, valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setPaddingBottom(4);
        table.addCell(valueCell);
    }

    // ── Section Graphiques ────────────────────────────────────────────────────

    private void addChartsSection(Document document, RapportContext context) throws DocumentException, IOException {
        document.add(sectionTitle("Évolution des Mesures"));

        Font metrFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, BLEU_SECTION);

        for (Map.Entry<Metrique, List<MesureHistoriqueDTO>> entry : context.getHistoriqueByMetrique().entrySet()) {
            Metrique metrique = entry.getKey();
            List<MesureHistoriqueDTO> historique = entry.getValue();
            SeuilAbsolu seuil = context.getSeuilsAbsolu() != null ? context.getSeuilsAbsolu().get(metrique) : null;

            if (historique == null || historique.isEmpty()) continue;

            Paragraph metrTitle = new Paragraph("Métrique : " + metrique.name(), metrFont);
            metrTitle.setSpacingBefore(8);
            metrTitle.setSpacingAfter(4);
            document.add(metrTitle);

            byte[] chartImage = generateChart(metrique, historique, seuil, context.getGranularite());

            if (chartImage != null) {
                Image chart = Image.getInstance(chartImage);
                chart.scaleToFit(490, 280);
                chart.setAlignment(Element.ALIGN_CENTER);
                document.add(chart);
            }

            document.add(Chunk.NEWLINE);
        }
    }

    /**
     * Génère le graphique d'évolution avec DateAxis (axe X lisible).
     *
     * Le format des labels de l'axe X est adapté à la granularité :
     *   TRENTE_MIN ou HORAIRE  → "dd/MM HH:mm"
     *   JOURNALIERE ou MENSUELLE → "dd/MM/yyyy"
     *
     * La granularité transmise est celle déterminée par RapportGenerationService.determinerGranularite(),
     * identique à la règle utilisée sur le dashboard (≤1j→TRENTE_MIN, ≤7j→HORAIRE,
     * ≤31j→JOURNALIERE, sinon MENSUELLE).
     */
    private byte[] generateChart(Metrique metrique, List<MesureHistoriqueDTO> historique,
                                  SeuilAbsolu seuil, Granularite granularite) throws IOException {

        // ── Dataset temporel ──────────────────────────────────────────────────
        TimeSeries series = new TimeSeries(metrique.name());
        for (MesureHistoriqueDTO point : historique) {
            Date date = Date.from(
                    point.getHorodatage().atZone(ZoneId.systemDefault()).toInstant());
            series.addOrUpdate(new Millisecond(date), point.getValeur().doubleValue());
        }

        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(series);

        // Lignes de seuil
        if (seuil != null && historique.size() >= 2) {
            Date firstDate = Date.from(historique.get(0).getHorodatage().atZone(ZoneId.systemDefault()).toInstant());
            Date lastDate  = Date.from(historique.get(historique.size() - 1).getHorodatage().atZone(ZoneId.systemDefault()).toInstant());

            TimeSeries minSeries = new TimeSeries("Seuil Min (" + seuil.getValeurMin() + ")");
            minSeries.add(new Millisecond(firstDate), seuil.getValeurMin().doubleValue());
            minSeries.add(new Millisecond(lastDate),  seuil.getValeurMin().doubleValue());

            TimeSeries maxSeries = new TimeSeries("Seuil Max (" + seuil.getValeurMax() + ")");
            maxSeries.add(new Millisecond(firstDate), seuil.getValeurMax().doubleValue());
            maxSeries.add(new Millisecond(lastDate),  seuil.getValeurMax().doubleValue());

            dataset.addSeries(minSeries);
            dataset.addSeries(maxSeries);
        }

        // ── Création du graphique ──────────────────────────────────────────────
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                "Évolution " + metrique.name(),
                "Date",
                "Valeur",
                dataset,
                true,   // legend
                false,  // tooltips
                false   // urls
        );

        // ── Axe X : DateAxis avec format adapté à la granularité ─────────────
        XYPlot plot = chart.getXYPlot();

        String datePattern = (granularite == Granularite.TRENTE_MIN || granularite == Granularite.HORAIRE)
                ? "dd/MM HH:mm"
                : "dd/MM/yyyy";

        DateAxis dateAxis = new DateAxis("Date");
        dateAxis.setDateFormatOverride(new SimpleDateFormat(datePattern));
        dateAxis.setVerticalTickLabels(true);   // rotation pour éviter le chevauchement
        dateAxis.setTickLabelFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 9));
        plot.setDomainAxis(dateAxis);

        // ── Style du plot ─────────────────────────────────────────────────────
        plot.setBackgroundPaint(new Color(248, 250, 253));
        plot.setDomainGridlinePaint(new Color(210, 215, 220));
        plot.setRangeGridlinePaint(new Color(210, 215, 220));
        chart.setBackgroundPaint(Color.WHITE);

        // ── Style des séries ──────────────────────────────────────────────────
        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, new Color(26, 58, 92));      // bleu section pour la série principale
        renderer.setSeriesStroke(0, new BasicStroke(1.8f));
        renderer.setSeriesShapesVisible(0, false);

        if (seuil != null && dataset.getSeriesCount() > 1) {
            BasicStroke dashedStroke = new BasicStroke(
                    1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10.0f, new float[]{5.0f}, 0.0f);

            renderer.setSeriesPaint(1, new Color(230, 130, 0));   // orange seuil min
            renderer.setSeriesStroke(1, dashedStroke);
            renderer.setSeriesShapesVisible(1, false);

            renderer.setSeriesPaint(2, new Color(180, 30, 30));   // rouge seuil max
            renderer.setSeriesStroke(2, dashedStroke);
            renderer.setSeriesShapesVisible(2, false);
        }

        // ── Export PNG ────────────────────────────────────────────────────────
        BufferedImage bufferedImage = chart.createBufferedImage(800, 380);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(bufferedImage, "png", baos);
        return baos.toByteArray();
    }

    // ── Section Incidents ─────────────────────────────────────────────────────

    private void addIncidentsTable(Document document, RapportContext context) throws DocumentException {
        document.add(sectionTitle("Incidents sur la Période"));

        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
        Font boldFont   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);

        // Résumé
        Paragraph summary = new Paragraph(
                "Total des alertes : " + context.getTotalAlertes()
                        + " (affichage limité aux 50 plus récentes)", normalFont);
        summary.setSpacingAfter(4);
        document.add(summary);

        if (!context.getRepartitionAlertes().isEmpty()) {
            StringBuilder sb = new StringBuilder("Répartition par type : ");
            for (Map.Entry<TypeAlerte, Long> entry : context.getRepartitionAlertes().entrySet()) {
                sb.append(entry.getKey()).append(" : ").append(entry.getValue()).append("  ");
            }
            Paragraph repartitionPara = new Paragraph(sb.toString().trim(), normalFont);
            repartitionPara.setSpacingAfter(8);
            document.add(repartitionPara);
        }

        // Tableau
        if (!context.getAlertes().isEmpty()) {
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2f, 2.2f, 2f, 3f, 2f});
            table.setSpacingBefore(4);

            // En-tête avec fond gris anthracite + texte blanc gras
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BLANC);
            addIncidentHeader(table, "Métrique",     headerFont);
            addIncidentHeader(table, "Type",         headerFont);
            addIncidentHeader(table, "Sévérité",     headerFont);
            addIncidentHeader(table, "Date création",headerFont);
            addIncidentHeader(table, "Statut",       headerFont);

            // Données avec zebra striping
            int i = 0;
            for (AlerteRow alerte : context.getAlertes()) {
                Color rowBg = (i % 2 == 0) ? BLANC : ZEBRE_CLAIR;
                addIncidentCell(table, alerte.getMetrique(),                               normalFont, rowBg);
                addIncidentCell(table, alerte.getTypeAlerte(),                             normalFont, rowBg);
                addIncidentCell(table, alerte.getSeverite(),                               normalFont, rowBg);
                addIncidentCell(table, alerte.getDateCreation().format(DATE_FORMATTER),    normalFont, rowBg);
                addIncidentCell(table, alerte.getStatut(),                                 normalFont, rowBg);
                i++;
            }

            document.add(table);
        } else {
            document.add(new Paragraph("Aucune alerte sur cette période.", normalFont));
        }
    }

    private void addIncidentHeader(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(GRIS_HEADER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPaddingTop(5);
        cell.setPaddingBottom(5);
        table.addCell(cell);
    }

    private void addIncidentCell(PdfPTable table, String text, Font font, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "—", font));
        cell.setBackgroundColor(bg);
        cell.setPaddingTop(4);
        cell.setPaddingBottom(4);
        cell.setPaddingLeft(4);
        table.addCell(cell);
    }

    // ── Section Statistiques ──────────────────────────────────────────────────

    private void addStatisticsSummary(Document document, RapportContext context) throws DocumentException {
        document.add(sectionTitle("Résumé Statistique"));

        Font metrFont  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, BLEU_SECTION);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
        Font boldFont   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);

        for (Map.Entry<Metrique, StatistiquesMetrique> entry : context.getStatistiquesByMetrique().entrySet()) {
            Metrique metrique = entry.getKey();
            StatistiquesMetrique stats = entry.getValue();

            Paragraph metrTitle = new Paragraph("Métrique : " + metrique.name(), metrFont);
            metrTitle.setSpacingBefore(8);
            metrTitle.setSpacingAfter(4);
            document.add(metrTitle);

            PdfPTable statsTable = new PdfPTable(2);
            statsTable.setWidthPercentage(60);
            statsTable.setHorizontalAlignment(Element.ALIGN_LEFT);

            addStatRow(statsTable, "Minimum",  stats.getMin(),     normalFont, boldFont);
            addStatRow(statsTable, "Maximum",  stats.getMax(),     normalFont, boldFont);
            addStatRow(statsTable, "Moyenne",  stats.getMoyenne(), normalFont, boldFont);

            document.add(statsTable);
        }
    }

    private void addStatRow(PdfPTable table, String label, BigDecimal value,
                            Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.BOX);
        labelCell.setBackgroundColor(ZEBRE_CLAIR);
        labelCell.setPaddingBottom(4);
        labelCell.setPaddingLeft(4);
        table.addCell(labelCell);

        String valueStr = value != null ? DECIMAL_FORMAT.format(value) : "—";
        PdfPCell valueCell = new PdfPCell(new Phrase(valueStr, valueFont));
        valueCell.setBorder(Rectangle.BOX);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setPaddingBottom(4);
        valueCell.setPaddingRight(4);
        table.addCell(valueCell);
    }

    // ── Footer ────────────────────────────────────────────────────────────────

    private static class FooterPageEvent extends PdfPageEventHelper {
        private final String nomDemandeur;
        private final LocalDateTime dateGeneration;
        private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        public FooterPageEvent(String nomDemandeur, LocalDateTime dateGeneration) {
            this.nomDemandeur = nomDemandeur;
            this.dateGeneration = dateGeneration;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(120, 120, 120));

            PdfPTable footer = new PdfPTable(2);
            footer.setWidthPercentage(100);
            try { footer.setWidths(new float[]{3, 1}); } catch (DocumentException ignored) {}

            PdfPCell leftCell = new PdfPCell(new Phrase(
                    "Généré par " + nomDemandeur + " — " + dateGeneration.format(formatter),
                    footerFont));
            leftCell.setBorder(Rectangle.TOP);
            leftCell.setBorderColorTop(SEPARATEUR);
            leftCell.setPaddingTop(4);
            footer.addCell(leftCell);

            PdfPCell rightCell = new PdfPCell(new Phrase("Page " + writer.getPageNumber(), footerFont));
            rightCell.setBorder(Rectangle.TOP);
            rightCell.setBorderColorTop(SEPARATEUR);
            rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            rightCell.setPaddingTop(4);
            footer.addCell(rightCell);

            footer.setTotalWidth(document.right() - document.left());
            footer.writeSelectedRows(0, -1, document.left(), document.bottom() - 5,
                    writer.getDirectContent());
        }
    }

    // ── Classes internes ──────────────────────────────────────────────────────

    public static class RapportContext {
        private String nomPointMesure;
        private LocalDateTime dateDebut;
        private LocalDateTime dateFin;
        private LocalDateTime dateGeneration;
        private String nomDemandeur;
        private Granularite granularite;
        private Map<Metrique, KpiResponseDTO> kpisByMetrique;
        private Map<Metrique, List<MesureHistoriqueDTO>> historiqueByMetrique;
        private Map<Metrique, SeuilAbsolu> seuilsAbsolu;
        private List<AlerteRow> alertes;
        private long totalAlertes;
        private Map<TypeAlerte, Long> repartitionAlertes;
        private Map<Metrique, StatistiquesMetrique> statistiquesByMetrique;

        public String getNomPointMesure() { return nomPointMesure; }
        public void setNomPointMesure(String v) { this.nomPointMesure = v; }

        public LocalDateTime getDateDebut() { return dateDebut; }
        public void setDateDebut(LocalDateTime v) { this.dateDebut = v; }

        public LocalDateTime getDateFin() { return dateFin; }
        public void setDateFin(LocalDateTime v) { this.dateFin = v; }

        public LocalDateTime getDateGeneration() { return dateGeneration; }
        public void setDateGeneration(LocalDateTime v) { this.dateGeneration = v; }

        public String getNomDemandeur() { return nomDemandeur; }
        public void setNomDemandeur(String v) { this.nomDemandeur = v; }

        public Granularite getGranularite() { return granularite; }
        public void setGranularite(Granularite v) { this.granularite = v; }

        public Map<Metrique, KpiResponseDTO> getKpisByMetrique() { return kpisByMetrique; }
        public void setKpisByMetrique(Map<Metrique, KpiResponseDTO> v) { this.kpisByMetrique = v; }

        public Map<Metrique, List<MesureHistoriqueDTO>> getHistoriqueByMetrique() { return historiqueByMetrique; }
        public void setHistoriqueByMetrique(Map<Metrique, List<MesureHistoriqueDTO>> v) { this.historiqueByMetrique = v; }

        public Map<Metrique, SeuilAbsolu> getSeuilsAbsolu() { return seuilsAbsolu; }
        public void setSeuilsAbsolu(Map<Metrique, SeuilAbsolu> v) { this.seuilsAbsolu = v; }

        public List<AlerteRow> getAlertes() { return alertes; }
        public void setAlertes(List<AlerteRow> v) { this.alertes = v; }

        public long getTotalAlertes() { return totalAlertes; }
        public void setTotalAlertes(long v) { this.totalAlertes = v; }

        public Map<TypeAlerte, Long> getRepartitionAlertes() { return repartitionAlertes; }
        public void setRepartitionAlertes(Map<TypeAlerte, Long> v) { this.repartitionAlertes = v; }

        public Map<Metrique, StatistiquesMetrique> getStatistiquesByMetrique() { return statistiquesByMetrique; }
        public void setStatistiquesByMetrique(Map<Metrique, StatistiquesMetrique> v) { this.statistiquesByMetrique = v; }
    }

    public static class AlerteRow {
        private String metrique;
        private String typeAlerte;
        private String severite;
        private LocalDateTime dateCreation;
        private String statut;

        public String getMetrique() { return metrique; }
        public void setMetrique(String v) { this.metrique = v; }
        public String getTypeAlerte() { return typeAlerte; }
        public void setTypeAlerte(String v) { this.typeAlerte = v; }
        public String getSeverite() { return severite; }
        public void setSeverite(String v) { this.severite = v; }
        public LocalDateTime getDateCreation() { return dateCreation; }
        public void setDateCreation(LocalDateTime v) { this.dateCreation = v; }
        public String getStatut() { return statut; }
        public void setStatut(String v) { this.statut = v; }
    }

    public static class StatistiquesMetrique {
        private BigDecimal min;
        private BigDecimal max;
        private BigDecimal moyenne;

        public BigDecimal getMin() { return min; }
        public void setMin(BigDecimal v) { this.min = v; }
        public BigDecimal getMax() { return max; }
        public void setMax(BigDecimal v) { this.max = v; }
        public BigDecimal getMoyenne() { return moyenne; }
        public void setMoyenne(BigDecimal v) { this.moyenne = v; }
    }
}
