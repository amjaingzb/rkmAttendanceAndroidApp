package com.rkm.rkmattendanceapp.util;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;



import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class PdfReceiptGenerator {

    private final byte[] logoBytes;
    private final byte[] signBytes;

    // OpenPDF uses BaseColor instead of java.awt.Color on Android
    private static final BaseColor HEADER_BLUE = new BaseColor(0, 34, 102);
    private static final BaseColor LINK_BLUE = new BaseColor(0, 0, 255);

    public static class GenerationException extends Exception {
        public GenerationException(String message) { super(message); }
        public GenerationException(String message, Throwable cause) { super(message, cause); }
    }

    /**
     * Constructor without images (backward compatibility with existing ViewModel logic)
     */
    public PdfReceiptGenerator() {
        this.logoBytes = null;
        this.signBytes = null;
    }

    /**
     * Recommended Constructor to include branding
     */
    public PdfReceiptGenerator(byte[] logoBytes, byte[] signBytes) {
        this.logoBytes = logoBytes;
        this.signBytes = signBytes;
    }

    public byte[] generatePdfReceipt(
            String receiptNo, String date, String donorName, String address, String mobileNum,
            String emailID, String UIN, String IDType, String amountFigures,
            String amountWords, String purpose, String txnMode, String txnModeDetails
    ) throws GenerationException, IOException {

        // A4 Rotated (Landscape) as per the alternate implementation
        Document document = new Document(PageSize.A4.rotate(), 30, 30, 30, 30);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // --- HEADER SECTION ---
            PdfPTable headerTable = new PdfPTable(new float[]{1, 6});
            headerTable.setWidthPercentage(100);

            // Logo
            PdfPCell logoCell = new PdfPCell();
            if (logoBytes != null) {
                Image logo = Image.getInstance(logoBytes);
                logo.scaleToFit(95, 95);
                logoCell.addElement(logo);
            }
            logoCell.setBorder(Rectangle.NO_BORDER);
            headerTable.addCell(logoCell);

            // Centered Title & Address
            Font titleFont = FontFactory.getFont(FontFactory.TIMES_BOLD, 26, HEADER_BLUE);
            Font subTitleFont = FontFactory.getFont(FontFactory.TIMES_BOLD, 12, HEADER_BLUE);
            Font contactFont = FontFactory.getFont(FontFactory.TIMES_BOLD, 11, LINK_BLUE);

            Paragraph titlePara = new Paragraph();
            titlePara.setAlignment(Element.ALIGN_CENTER);
            titlePara.add(new Chunk("RAMAKRISHNA MATH HALASURU\n", titleFont));
            titlePara.add(new Chunk("(A Branch Centre of Ramakrishna Math, P.O. Belur Math, Dist, Howrah, West Bengal – 711 202)\n", subTitleFont));
            titlePara.add(new Chunk("#113, Swami Vivekananda Road, Halasuru, Bengaluru – 560 008. * Phone: 84317 76931/95354 86502\n", subTitleFont));
            titlePara.add(new Chunk("Email: halasuru@rkmm.org       Website: www.ramakrishnamath.in", contactFont));

            PdfPCell titleCell = new PdfPCell(titlePara);
            titleCell.setBorder(Rectangle.NO_BORDER);
            titleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            headerTable.addCell(titleCell);
            document.add(headerTable);

            document.add(new Paragraph("\n"));

            // --- BODY CONTENT ---
            PdfPTable bodyTable = new PdfPTable(2);
            bodyTable.setWidthPercentage(100);
            bodyTable.setWidths(new float[]{1.5f, 1f});

            Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font normal = FontFactory.getFont(FontFactory.HELVETICA, 11);

            bodyTable.addCell(createLabelValueCell("Receipt No: ", receiptNo, bold, normal));
            bodyTable.addCell(createLabelValueCell("Date: ", date, bold, normal));

            if(address==null)address="";
            PdfPCell donorCell = createLabelValueCell("Received with thanks from: ", donorName+" "+address, bold, normal);
            donorCell.setColspan(2);
            bodyTable.addCell(donorCell);

            bodyTable.addCell(createLabelValueCell("Mobile No: ", mobileNum, bold, normal));
            bodyTable.addCell(createLabelValueCell("Email Id: ", emailID, bold, normal));

            bodyTable.addCell(createLabelValueCell("I D Code: ", IDType, bold, normal));
            bodyTable.addCell(createLabelValueCell("Unique ID Number: ", UIN, bold, normal));

            PdfPCell amountWordsCell = createLabelValueCell("The sum of Rupees: ", amountWords, bold, normal);
            amountWordsCell.setColspan(2);
            bodyTable.addCell(amountWordsCell);

            PdfPCell modeCell = createLabelValueCell("Transaction Mode: ", txnMode + " " + (txnModeDetails != null ? txnModeDetails : ""), bold, normal);
            modeCell.setColspan(2);
            bodyTable.addCell(modeCell);

            PdfPCell purposeCell = createLabelValueCell("Towards: ", purpose, bold, bold);
            purposeCell.setColspan(2);
            bodyTable.addCell(purposeCell);

            document.add(bodyTable);

            // --- BOTTOM SECTION ---
            PdfPTable footerTable = new PdfPTable(3);
            footerTable.setWidthPercentage(100);
            footerTable.setSpacingBefore(40f);
            footerTable.setWidths(new float[]{2f, 1f, 1f});

            // Row 1: Figures and Signature
            PdfPCell amountFigCell = new PdfPCell(new Phrase("Rs. " + amountFigures, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
            amountFigCell.setBorder(Rectangle.NO_BORDER);
            footerTable.addCell(amountFigCell);

            footerTable.addCell(new PdfPCell(new Phrase("")) {{ setBorder(NO_BORDER); }});

            PdfPCell signImgCell = new PdfPCell();
            signImgCell.setBorder(Rectangle.NO_BORDER);
            if (signBytes != null) {
                Image sign = Image.getInstance(signBytes);
                sign.scaleToFit(80, 35);
                sign.setAlignment(Element.ALIGN_CENTER);
                signImgCell.addElement(sign);
            }
            footerTable.addCell(signImgCell);

            // Row 2: Aligned Labels
            footerTable.addCell(createAlignedCell("(Cheque is subject to realisation)", bold, Element.ALIGN_LEFT));
            footerTable.addCell(createAlignedCell("Received By", bold, Element.ALIGN_CENTER));
            footerTable.addCell(createAlignedCell("(Adhyaksha)", bold, Element.ALIGN_CENTER));

            document.add(footerTable);

            document.add(new LineSeparator(1f, 100, BaseColor.BLACK, Element.ALIGN_CENTER, -2));

            // --- DISCLAIMER ---
            Font disclaimerFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Paragraph disclaimer = new Paragraph();
            disclaimer.setSpacingBefore(8f);
            disclaimer.setFont(disclaimerFont);
            disclaimer.add("Donations are exempt from Income Tax Under Section 80G(5)(vi) of the I.T.Act, 1961, vide provisional approval Number AAATR3497GB20214 Dated 28/05/2021, which has been further extended from AY-2022-2023 to AY-2026-2027.\n");
            disclaimer.add("Under Schedule 1, Article 53, Exemptions (b) of Indian Stamp Act, 1899, charitable Institutions are not required to issue any stamped receipt of amount received by them.");
            document.add(disclaimer);

            Paragraph panPara = new Paragraph("Our PAN No. AAATR3497G", bold);
            panPara.setSpacingBefore(5f);
            document.add(panPara);

            document.close();
        } catch (Exception e) {
            throw new GenerationException("Failed to compile PDF receipt", e);
        }

        return out.toByteArray();
    }

    private PdfPCell createLabelValueCell(String label, String value, Font labelFont, Font valueFont) {
        Phrase p = new Phrase();
        p.add(new Chunk(label, labelFont));
        p.add(new Chunk(value != null ? value : "", valueFont));
        PdfPCell cell = new PdfPCell(p);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingBottom(8f);
        return cell;
    }

    private PdfPCell createAlignedCell(String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_BOTTOM);
        return cell;
    }
}