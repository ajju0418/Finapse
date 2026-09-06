package com.finapse.service;

import com.finapse.dto.StatementParseResult;
import com.finapse.exception.EncryptedPdfException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.*;

class EncryptedPdfStatementParserTest {

    private GenericPdfStatementParser genericParser;
    private HdfcPdfStatementParser hdfcParser;
    private HdfcCreditCardPdfStatementParser hdfcCcParser;
    private PdfStatementParser pdfStatementParser;

    private static final String CORRECT_PASSWORD = "testPassword123";
    private byte[] encryptedPdfBytes;

    @BeforeEach
    void setUp() throws Exception {
        genericParser = new GenericPdfStatementParser();
        hdfcParser = new HdfcPdfStatementParser();
        hdfcCcParser = new HdfcCreditCardPdfStatementParser();
        pdfStatementParser = new PdfStatementParser(hdfcParser, hdfcCcParser, genericParser);

        // Create an encrypted PDF in memory
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);

            try (PDPageContentStream stream = new PDPageContentStream(doc, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(100, 700);
                stream.showText("01/01/2026 Monthly Subscription 499.00 (Dr)");
                stream.endText();
            }

            AccessPermission ap = new AccessPermission();
            StandardProtectionPolicy spp = new StandardProtectionPolicy("ownerSecretAdmin", CORRECT_PASSWORD, ap);
            spp.setEncryptionKeyLength(128);
            doc.protect(spp);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            encryptedPdfBytes = baos.toByteArray();
        }
    }

    @Test
    void parse_withCorrectPassword_unlocksAndParsesTransactions() {
        StatementParseResult result = pdfStatementParser.parse(
                new ByteArrayInputStream(encryptedPdfBytes),
                "statement.pdf",
                null,
                CORRECT_PASSWORD
        );

        assertThat(result.records()).isNotEmpty();
        assertThat(result.records().getFirst().description()).contains("Monthly Subscription");
    }

    @Test
    void parse_withNullPassword_throwsEncryptedPdfException() {
        assertThatThrownBy(() -> pdfStatementParser.parse(
                new ByteArrayInputStream(encryptedPdfBytes),
                "statement.pdf",
                null,
                null
        )).isInstanceOf(EncryptedPdfException.class)
          .hasMessageContaining("password-protected");
    }

    @Test
    void parse_withWrongPassword_throwsEncryptedPdfException() {
        assertThatThrownBy(() -> pdfStatementParser.parse(
                new ByteArrayInputStream(encryptedPdfBytes),
                "statement.pdf",
                null,
                "wrongPassword"
        )).isInstanceOf(EncryptedPdfException.class)
          .hasMessageContaining("password-protected");
    }

    @Test
    void parse_unencryptedPdf_parsesWithoutPassword() throws Exception {
        byte[] unencryptedBytes;
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);

            try (PDPageContentStream stream = new PDPageContentStream(doc, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(100, 700);
                stream.showText("01/01/2026 Regular Expense 150.00 (Dr)");
                stream.endText();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            unencryptedBytes = baos.toByteArray();
        }

        StatementParseResult result = pdfStatementParser.parse(
                new ByteArrayInputStream(unencryptedBytes),
                "statement.pdf"
        );

        assertThat(result.records()).isNotEmpty();
    }
}
