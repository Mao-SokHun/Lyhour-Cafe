package com.example.demo.Service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import com.example.demo.Models.Order;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class PdfExportService {

    private final ReportExportService reportExportService;

    public PdfExportService(ReportExportService reportExportService) {
        this.reportExportService = reportExportService;
    }

    public byte[] exportSalesPdf() throws IOException {
        return buildPdf("Lyhour Coffee — Sales Report", linesFromCsv(reportExportService.exportSalesCsv()));
    }

    public byte[] exportSummaryPdf() throws IOException {
        return buildPdf("Lyhour Coffee — Summary Report", linesFromCsv(reportExportService.exportSummaryCsv()));
    }

    public byte[] exportReceiptPdf(Order order) throws IOException {
        List<String> lines = List.of(
                "Lyhour Coffee",
                "Order #" + order.getId(),
                "Customer: " + order.getUsername(),
                "Date: " + order.getOrderDate(),
                "Subtotal: $" + order.getSubtotal(),
                "Tax: $" + order.getTaxAmount(),
                "Discount: $" + order.getDiscountAmount(),
                "Total: $" + order.getTotalPrice(),
                "Payment: " + order.getPaymentMethod() + " (" + order.getPaymentStatus() + ")",
                "Thank you!"
        );
        return buildPdf("Receipt #" + order.getId(), lines);
    }

    private List<String> linesFromCsv(byte[] csv) {
        return new String(csv).lines().limit(40).toList();
    }

    private byte[] buildPdf(String title, List<String> lines) throws IOException {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(fontBold, 14);
                cs.newLineAtOffset(50, 750);
                cs.showText(title);
                cs.setFont(font, 10);
                for (String line : lines) {
                    cs.newLineAtOffset(0, -16);
                    String text = line.length() > 95 ? line.substring(0, 95) : line;
                    cs.showText(text.replace("\t", " "));
                }
                cs.endText();
            }
            doc.save(out);
            return out.toByteArray();
        }
    }
}
