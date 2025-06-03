package history;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.util.*;
import java.io.*;

public class PdfExporter {
    public static void saveTextAsPdf(String text, String filePath) throws IOException {
        PDDocument document = new PDDocument();
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        PDPageContentStream contentStream = new PDPageContentStream(document, page);
        contentStream.setFont(PDType1Font.HELVETICA, 12);

        float margin = 50;
        float yStart = page.getMediaBox().getHeight() - margin;
        float width = page.getMediaBox().getWidth() - 2 * margin;
        float leading = 16;

        List<String> lines = wrapText(text, PDType1Font.HELVETICA, 12, width);

        contentStream.beginText();
        contentStream.newLineAtOffset(margin, yStart);

        for (String line : lines) {
            contentStream.showText(line);
            contentStream.newLineAtOffset(0, -leading);
        }

        contentStream.endText();
        contentStream.close();

        document.save(new File(filePath));
        document.close();
    }

    private static List<String> wrapText(String text, PDType1Font font, int fontSize, float maxWidth)
            throws IOException {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();

        for (String word : words) {
            String testLine = line.length() == 0 ? word : line + " " + word;
            float size = font.getStringWidth(testLine) / 1000 * fontSize;
            if (size > maxWidth) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(testLine);
            }
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }
}
