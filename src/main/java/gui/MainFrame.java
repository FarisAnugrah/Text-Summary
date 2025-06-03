package gui;

import summarizer.RuleBasedSummarizer;
import summarizer.Summarizer;
import summarizer.ApiBasedSummarizer;
import summarizer.SummaryType;
import history.SummaryHistoryManager;
import history.PdfExporter;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.awt.*;
import java.io.*;

// import javax.swing.Box;
// import java.awt.Dimension;
// import javax.swing.JButton;
// import javax.swing.JComboBox;
// import javax.swing.JFrame;
// import javax.swing.JPanel;
// import javax.swing.JScrollPane;
// import javax.swing.JTextArea;
// import java.awt.BorderLayout;

public class MainFrame extends JFrame {

    private JTextArea inputArea;
    private JTextArea outputArea;
    private JComboBox<String> methodComboBox;
    private JComboBox<SummaryType> typeComboBox = new JComboBox<>(SummaryType.values());
    private JButton summarizeButton;
    private JButton saveButton;
    private JButton historyButton;
    private final history.SummaryHistoryManager historyManager = new history.SummaryHistoryManager();

    public MainFrame() {
        setTitle("Aplikasi Meringkas Teks");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // panel utama dengan BorderLayout
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        // Area Input (Utara)
        inputArea = new JTextArea(10, 50);
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        panel.add(new JScrollPane(inputArea), BorderLayout.NORTH);

        // Panel Tengah untuk metode dan tombol Ringkas
        JPanel middlePanel = new JPanel();
        methodComboBox = new JComboBox<>(new String[] { "Rule-Based", "AI-Based" });
        summarizeButton = new JButton("Ringkas");

        summarizeButton.addActionListener(_ -> {
    String inputText = inputArea.getText();
    if (inputText.isEmpty()) {
        outputArea.setText("Masukkan teks terlebih dahulu.");
        return;
    }

    summarizeButton.setEnabled(false);
    summarizeButton.setText("Merangkum...");

    new Thread(() -> {
        try {
            Summarizer summarizer;
            String selected = methodComboBox.getSelectedItem().toString();
            if (selected.equals("Rule-Based")) {
                summarizer = new RuleBasedSummarizer();
            } else {
                summarizer = new ApiBasedSummarizer();
            }

            SummaryType selectedType = (SummaryType) typeComboBox.getSelectedItem();
            String summary = summarizer.summarize(inputText, selectedType);

            SwingUtilities.invokeLater(() -> {
                outputArea.setText(summary);
                summarizeButton.setEnabled(true);
                summarizeButton.setText("Ringkas");
                historyManager.saveHistory(summary);
            });
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> {
                outputArea.setText("Terjadi kesalahan saat meringkas: " + e.getMessage());
                summarizeButton.setEnabled(true);
                summarizeButton.setText("Ringkas");
            });
        }
    }).start();
});

        middlePanel.add(methodComboBox);
        middlePanel.add(typeComboBox);
        middlePanel.add(summarizeButton);
        panel.add(middlePanel, BorderLayout.CENTER);

        // Panel Selatan: output + tombol bawah
        JPanel southPanel = new JPanel();
        southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.Y_AXIS));

        // Area Output
        outputArea = new JTextArea(15, 50);
        outputArea.setEditable(false);
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);
        JScrollPane outputScrollPane = new JScrollPane(
                outputArea);
        southPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        southPanel.add(outputScrollPane);

        // Tombol bawah
        JPanel bottomPanel = new JPanel();

        saveButton = new JButton("Simpan");
        saveButton.addActionListener(_ -> {
            String summaryText = outputArea.getText();
            if (summaryText.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Tidak ada ringkasan untuk disimpan.", "Peringatan",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Simpan Ringkasan");
            fileChooser.setAcceptAllFileFilterUsed(false);

            FileNameExtensionFilter pdfFilter = new FileNameExtensionFilter("PDF File (*.pdf)", "pdf");
            FileNameExtensionFilter txtFilter = new FileNameExtensionFilter("Text File (*.txt)", "txt");
            fileChooser.addChoosableFileFilter(pdfFilter);
            fileChooser.addChoosableFileFilter(txtFilter);
            fileChooser.setFileFilter(pdfFilter); // Set default filter to PDF

            int userSelection = fileChooser.showSaveDialog(this);

            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fileChooser.getSelectedFile();
                String path = fileToSave.getAbsolutePath();
                FileNameExtensionFilter selectedFilter = (FileNameExtensionFilter) fileChooser
                        .getFileFilter();

                if (selectedFilter == pdfFilter) {
                    if (!path.toLowerCase().endsWith(".pdf")) {
                        path += ".pdf";
                    }
                    try {
                        PdfExporter.saveTextAsPdf(summaryText, path);
                        JOptionPane.showMessageDialog(this, "Ringkasan disimpan sebagai PDF.", "Sukses",
                                JOptionPane.INFORMATION_MESSAGE);
                    } catch (IOException e) {
                        JOptionPane.showMessageDialog(this, "Gagal menyimpan PDF: " + e.getMessage(), "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } else if (selectedFilter == txtFilter) {
                    if (!path.toLowerCase().endsWith(".txt")) {
                        path += ".txt";
                    }
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
                        writer.write(summaryText);
                        JOptionPane.showMessageDialog(this, "Ringkasan disimpan sebagai teks.", "Sukses",
                                JOptionPane.INFORMATION_MESSAGE);
                    } catch (IOException e) {
                        JOptionPane.showMessageDialog(this, "Gagal menyimpan teks: " + e.getMessage(), "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        historyButton = new JButton("Riwayat");
        historyButton.addActionListener(_ -> {
            String historyText = SummaryHistoryManager.loadHistory();
            JTextArea historyArea = new JTextArea(historyText, 5, 40);
            historyArea.setEditable(false);
            historyArea.setLineWrap(true);
            historyArea.setWrapStyleWord(true);
            JScrollPane scroll = new JScrollPane(historyArea);
            scroll.setPreferredSize(new Dimension(500, 300));
            JOptionPane.showMessageDialog(null, scroll, "Riwayat Ringkasan", JOptionPane.PLAIN_MESSAGE);
        });

        bottomPanel.add(saveButton);
        bottomPanel.add(historyButton);

        southPanel.add(bottomPanel);
        panel.add(southPanel, BorderLayout.SOUTH);

        // Finalize
        add(panel);
        setVisible(true);
    }

}
