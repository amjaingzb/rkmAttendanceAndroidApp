package com.rkm.rkmattendanceapp.util;
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

// --- Interface and Exception remain as per your documentation ---
class GenerationException extends Exception {
    public GenerationException(String message) { super(message); }
    public GenerationException(String message, Throwable cause) { super(message, cause); }
}

interface ReceiptGenerator {
    byte[] generatePdfReceipt(
            String receiptNo, String date, String donorName, String mobileNum,
            String emailID, String UIN, String IDType, String amountFigures,
            String amountWords, String purpose, String txnMode, String txnModeDetails
    ) throws GenerationException, IOException;
}

public class NativePdfReceiptGenerator implements ReceiptGenerator {

    private final Bitmap logoBitmap;
    private final Bitmap signBitmap;

    public NativePdfReceiptGenerator(Bitmap logo, Bitmap sign) {
        this.logoBitmap = logo;
        this.signBitmap = sign;
    }

    @Override
    public byte[] generatePdfReceipt(
            String receiptNo, String date, String donorName, String mobileNum,
            String emailID, String UIN, String IDType, String amountFigures,
            String amountWords, String purpose, String txnMode, String txnModeDetails
    ) throws GenerationException, IOException {

        PdfDocument document = new PdfDocument();
        // A4 Landscape in points (1/72 inch): 842 x 595
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(842, 595, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        Paint paint = new Paint();
        TextPaint textPaint = new TextPaint();

        int marginLeft = 40;
        int marginRight = 802;
        int center = 421;
        int currentY = 40;

        // --- 1. HEADER (Logo + Centered Text) ---
        if (logoBitmap != null) {
            Bitmap scaledLogo = Bitmap.createScaledBitmap(logoBitmap, 80, 80, true);
            canvas.drawBitmap(scaledLogo, marginLeft, currentY, paint);
        }

        // Title and Address (Centered)
        textPaint.setColor(Color.rgb(0, 34, 102)); // HEADER_BLUE
        textPaint.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));
        textPaint.setTextSize(22);
        textPaint.setTextAlign(Paint.Align.CENTER);

        canvas.drawText("RAMAKRISHNA MATH HALASURU", center + 40, currentY + 25, textPaint);

        textPaint.setTextSize(11);
        canvas.drawText("(A Branch Centre of Ramakrishna Math, P.O. Belur Math, Dist, Howrah, West Bengal – 711 202)", center + 40, currentY + 45, textPaint);
        canvas.drawText("#113, Swami Vivekananda Road, Halasuru, Bengaluru – 560 008. * Phone: 84317 76931/95354 86502", center + 40, currentY + 60, textPaint);

        textPaint.setColor(Color.BLUE);
        canvas.drawText("Email: halasuru@rkmmn.org       Website: www.ramakrishnamath.in", center + 40, currentY + 75, textPaint);

        currentY += 120;

        // --- 2. BODY CONTENT ---
        paint.reset();
        paint.setTextSize(12);
        paint.setColor(Color.BLACK);
        int column2X = 500;

        // Receipt No & Date
        drawLabelValue(canvas, paint, marginLeft, currentY, "Receipt No: ", receiptNo);
        drawLabelValue(canvas, paint, column2X, currentY, "Date: ", date);
        currentY += 25;

        // Received with thanks (Handles wrapping for long addresses)
        currentY = drawWrappedText(canvas, "Received with thanks from: " + donorName, marginLeft, currentY, 750);

        // Mobile & Email
        drawLabelValue(canvas, paint, marginLeft, currentY, "Mobile No: ", mobileNum);
        drawLabelValue(canvas, paint, column2X, currentY, "Email Id: ", emailID);
        currentY += 25;

        // ID Code & UIN
        drawLabelValue(canvas, paint, marginLeft, currentY, "I D Code: ", IDType);
        drawLabelValue(canvas, paint, column2X, currentY, "Unique ID Number: ", UIN);
        currentY += 25;

        // The sum of Rupees (Capitalized T)
        currentY = drawWrappedText(canvas, "The sum of Rupees: " + amountWords, marginLeft, currentY, 750);

        // Transaction Mode
        drawLabelValue(canvas, paint, marginLeft, currentY, "Transaction Mode: ", txnMode + " " + txnModeDetails);
        currentY += 25;

        // Towards (Capitalized T)
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Towards: " + purpose, marginLeft, currentY, paint);
        currentY += 50;

        // --- 3. BOTTOM SECTION (Straight Line) ---
        int footerY = 480;

        // Amount
        paint.setTextSize(18);
        canvas.drawText(amountFigures, marginLeft, footerY - 20, paint);

        // Signature
        if (signBitmap != null) {
            Bitmap scaledSign = Bitmap.createScaledBitmap(signBitmap, 80, 30, true);
            canvas.drawBitmap(scaledSign, 680, footerY - 55, paint);
        }

        // Labels on a straight horizontal line
        paint.setTextSize(11);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("(Cheque is subject to realisation)", marginLeft, footerY, paint);

        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Received By", 421, footerY, paint);
        canvas.drawText("(Adhyaksha)", 720, footerY, paint);

        // Horizontal Line
        paint.setStrokeWidth(1f);
        canvas.drawLine(marginLeft, footerY + 5, marginRight, footerY + 5, paint);

        // --- 4. DISCLAIMER (Increased size 11) ---
        currentY = footerY + 25;
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        paint.setTextSize(11);

        String disc1 = "Donations are exempt from Income Tax Under Section 80G(5)(vi) of the I.T.Act, 1961, wide provisional approval Number AAATR3497GB20214 Dated 28/05/2021...";
        String disc2 = "Under Schedule 1, Article 53, Exemptions (b) of Indian Stamp Act, 1899, charitable Institutions are not required to issue any stamped receipt...";

        currentY = drawWrappedText(canvas, disc1, marginLeft, currentY, 760);
        currentY = drawWrappedText(canvas, disc2, marginLeft, currentY, 760);

        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Our PAN No. AAATR3497G", marginLeft, currentY + 10, paint);

        document.finishPage(page);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            document.writeTo(out);
            document.close();
        } catch (IOException e) {
            throw new GenerationException("I/O Error during PDF write", e);
        }

        return out.toByteArray();
    }

    // Helper to draw Label in Bold and Value in Normal
    private void drawLabelValue(Canvas canvas, Paint paint, int x, int y, String label, String value) {
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText(label, x, y, paint);
        float width = paint.measureText(label);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText(value, x + width, y, paint);
    }

    // Helper to wrap text automatically (Crucial for donor addresses)
    private int drawWrappedText(Canvas canvas, String text, int x, int y, int width) {
        TextPaint tp = new TextPaint();
        tp.setTextSize(12);
        tp.setColor(Color.BLACK);

        StaticLayout sl = StaticLayout.Builder.obtain(text, 0, text.length(), tp, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .build();

        canvas.save();
        canvas.translate(x, y - 10);
        sl.draw(canvas);
        canvas.restore();

        return y + sl.getHeight() + 5;
    }
}

