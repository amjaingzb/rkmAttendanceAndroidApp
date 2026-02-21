package com.rkm.rkmattendanceapp.util;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import kotlinx.coroutines.channels.ActorKt;

public class PdfReceiptGenerator {

    public static class GenerationException extends Exception {
        public GenerationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    Bitmap logo , sign ;

    public PdfReceiptGenerator(Bitmap logo, Bitmap sign) {
        this.logo=logo;
        this.sign=sign;
    }

    public byte[] generatePdfReceipt(
            String receiptNo,
            String date,
            String donorName,
            String mobileNum,
            String emailID,
            String UIN,
            String IDType,
            String amountFigures,
            String amountWords,
            String purpose,
            String txnMode,
            String txnModeDetails
    ) throws GenerationException, IOException {
        try {



            // Initialize the generator
            ReceiptGenerator generator = new NativePdfReceiptGenerator(logo, sign);

            // Generate the PDF data
            byte[] pdfData = generator.generatePdfReceipt(
                    "RDB1003", "30-Sep-25", "RAHUL SHARMA, FLAT 402, INDIRANAGAR, BENGALURU",
                    "9876543210", "rahul@example.com", "AFBPJ7070B", "PAN",
                    "1,00,000.00", "Indian Rupees One Lakh Only",
                    "FUND LAND AND BUILDING", "Electronic Transfer", "TXN123456"
            );

            // Write bytes to file
            try (FileOutputStream fos = new FileOutputStream("Final_Receipt.pdf")) {
                fos.write(pdfData);
            }

            System.out.println("Interface implementation successful!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public byte[] generatePdfReceipt_Amit(
            String receiptNo,
            String date,
            String donorName,
            String mobileNum,
            String emailID,
            String UIN,
            String IDType,
            String amountFigures,
            String amountWords,
            String purpose,
            String txnMode,
            String txnModeDetails
    ) throws GenerationException, IOException {

        PdfDocument document = new PdfDocument();
        int pageHeight = 842; 
        int pageWidth = 595;
        
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        try {
            drawReceiptContent(canvas, pageWidth, receiptNo, date, donorName, mobileNum, emailID, UIN, IDType, amountFigures, amountWords, purpose, txnMode, txnModeDetails);
            
            document.finishPage(page);

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            document.writeTo(stream);
            return stream.toByteArray();

        } catch (Exception e) {
            throw new GenerationException("Failed to generate PDF", e);
        } finally {
            document.close();
        }
    }

    private void drawReceiptContent(Canvas canvas, int width, String receiptNo, String date, String donorName, 
                                    String mobile, String email, String uin, String idType, String amount, 
                                    String words, String purpose, String mode, String details) {
        
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(12);
        
        TextPaint textPaint = new TextPaint();
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(12);
        textPaint.setColor(Color.BLACK);

        int startX = 40;
        int startY = 50;
        int lineSpacing = 25;
        int currentY = startY;

        // --- Header ---
        Paint titlePaint = new Paint();
        titlePaint.setColor(Color.BLACK);
        titlePaint.setTextSize(20);
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        titlePaint.setTextAlign(Paint.Align.CENTER);
        
        canvas.drawText("Ramakrishna Math", width / 2f, currentY, titlePaint);
        currentY += 30;
        
        titlePaint.setTextSize(14);
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("Ulsoor, Bangalore - 560008", width / 2f, currentY, titlePaint);
        currentY += 20;
        
        canvas.drawLine(startX, currentY, width - startX, currentY, paint);
        currentY += 30;

        titlePaint.setTextSize(18);
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("DONATION RECEIPT", width / 2f, currentY, titlePaint);
        currentY += 40;

        // --- Meta Data ---
        paint.setTextSize(12);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Receipt No: " + receiptNo, startX, currentY, paint);
        
        String dateStr = "Date: " + date;
        float dateWidth = paint.measureText(dateStr);
        canvas.drawText(dateStr, width - startX - dateWidth, currentY, paint);
        currentY += lineSpacing * 1.5;

        // --- Donor Details ---
        drawField(canvas, textPaint, "Received with thanks from:", donorName, startX, width - startX, currentY);
        currentY += lineSpacing;
        
        if (mobile != null && !mobile.isEmpty()) {
            canvas.drawText("Mobile: " + mobile, startX, currentY, paint);
            currentY += lineSpacing;
        }
        
        if (uin != null && !uin.isEmpty()) {
            canvas.drawText(idType + ": " + uin, startX, currentY, paint);
            currentY += lineSpacing;
        }

        currentY += 10;
        canvas.drawLine(startX, currentY, width - startX, currentY, paint);
        currentY += 25;

        // --- Amount & Purpose ---
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Amount:", startX, currentY, paint);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("Rs. " + amount, startX + 100, currentY, paint);
        currentY += lineSpacing;

        currentY = drawWrappedText(canvas, textPaint, "In Words: " + words, startX, currentY, width - 2 * startX);
        currentY += 10; 

        currentY = drawWrappedText(canvas, textPaint, "Towards: " + purpose, startX, currentY, width - 2 * startX);
        currentY += 10;

        // --- Payment Details ---
        canvas.drawText("Mode: " + mode, startX, currentY, paint);
        currentY += lineSpacing;
        if (details != null && !details.isEmpty()) {
            currentY = drawWrappedText(canvas, textPaint, "Details: " + details, startX, currentY, width - 2 * startX);
        }

        // --- Footer ---
        currentY += 60;
        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Authorized Signatory", width - startX, currentY, paint);
        
        currentY += 40;
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(10);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));
        canvas.drawText("Generated via SevaConnect", width / 2f, currentY, paint);
    }

    private void drawField(Canvas canvas, TextPaint paint, String label, String value, int x, int width, int y) {
        canvas.drawText(label + " " + value, x, y, paint);
    }

    private int drawWrappedText(Canvas canvas, TextPaint paint, String text, int x, int y, int width) {
        StaticLayout layout = StaticLayout.Builder.obtain(text, 0, text.length(), paint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0, 1.0f)
                .setIncludePad(false)
                .build();
        
        canvas.save();
        canvas.translate(x, y);
        layout.draw(canvas);
        canvas.restore();
        return y + layout.getHeight();
    }
}