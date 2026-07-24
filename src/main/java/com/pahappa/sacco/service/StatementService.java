package com.pahappa.sacco.service;

import com.pahappa.sacco.dao.AccountDao;
import com.pahappa.sacco.dao.MemberDao;
import com.pahappa.sacco.dao.TransactionDao;
import com.pahappa.sacco.entity.Account;
import com.pahappa.sacco.entity.Member;
import com.pahappa.sacco.entity.Transaction;
import com.pahappa.sacco.exception.BusinessRuleViolationException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

//Generate the account statement PDF deliverable
@ApplicationScoped
public class StatementService {

    private static final float MARGIN = 50;
    private static final float ROW_HEIGHT = 16;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    private final AccountDao accountDao;
    private final MemberDao memberDao;
    private final TransactionDao transactionDao;

    @Inject
    public StatementService(AccountDao accountDao, MemberDao memberDao, TransactionDao transactionDao) {
        this.accountDao = accountDao;
        this.memberDao = memberDao;
        this.transactionDao = transactionDao;
    }

    protected StatementService() {
        this.accountDao = null;
        this.memberDao = null;
        this.transactionDao = null;
    }

    public byte[] generateStatement(Long memberId) {
        Member member = memberDao.findById(memberId)
                .orElseThrow(() -> new BusinessRuleViolationException("Member not found."));
        Account account = accountDao.findByMemberId(memberId)
                .orElseThrow(() -> new BusinessRuleViolationException("No savings account found for this member."));
        List<Transaction> history = transactionDao.findByAccountOrderedDesc(account.getId());

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDPageContentStream content = new PDPageContentStream(document, page);
            float y = PDRectangle.A4.getHeight() - MARGIN;

            y = writeHeader(content, member, account, y);
            y = writeColumnHeaders(content, y);

            for (Transaction tx : history) {
                if (y < MARGIN + ROW_HEIGHT) {
                    content.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    content = new PDPageContentStream(document, page);
                    y = PDRectangle.A4.getHeight() - MARGIN;
                    y = writeColumnHeaders(content, y);
                }
                y = writeRow(content, tx, y);
            }

            content.close();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to generate statement PDF.", e);
        }
    }

    private float writeHeader(PDPageContentStream content, Member member, Account account, float y) throws IOException {
        content.beginText();
        content.setFont(PDType1Font.HELVETICA_BOLD, 16);
        content.newLineAtOffset(MARGIN, y);
        content.showText("Kimwanyi SACCO - Account Statement");
        content.endText();
        y -= 26;

        content.beginText();
        content.setFont(PDType1Font.HELVETICA, 11);
        content.newLineAtOffset(MARGIN, y);
        content.showText("Member: " + member.getFullName() + "   |   Membership No: " + member.getMembershipNumber());
        content.endText();
        y -= 16;

        content.beginText();
        content.setFont(PDType1Font.HELVETICA, 11);
        content.newLineAtOffset(MARGIN, y);
        content.showText("Current Balance: UGX " + account.getBalance());
        content.endText();
        y -= 24;

        return y;
    }

    private float writeColumnHeaders(PDPageContentStream content, float y) throws IOException {
        content.beginText();
        content.setFont(PDType1Font.HELVETICA_BOLD, 10);
        content.newLineAtOffset(MARGIN, y);
        content.showText(padColumns("Date", "Type", "Amount", "Balance After", "Reference"));
        content.endText();
        return y - ROW_HEIGHT;
    }

    private float writeRow(PDPageContentStream content, Transaction tx, float y) throws IOException {
        content.beginText();
        content.setFont(PDType1Font.HELVETICA, 9);
        content.newLineAtOffset(MARGIN, y);
        content.showText(padColumns(
                tx.getCreatedAt().format(DATE_FORMAT),
                tx.getType().toString(),
                tx.getAmount().toString(),
                tx.getBalanceAfter().toString(),
                tx.getReference() == null ? "" : tx.getReference()
        ));
        content.endText();
        return y - ROW_HEIGHT;
    }

    private String padColumns(String... columns) {
        int[] widths = {18, 12, 14, 16, 14};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < columns.length; i++) {
            String col = columns[i] == null ? "" : columns[i];
            sb.append(String.format("%-" + widths[i] + "s", col));
        }
        return sb.toString();
    }
}
