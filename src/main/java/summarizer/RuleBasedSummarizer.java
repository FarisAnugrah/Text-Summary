package summarizer;

import java.util.*;
import java.util.stream.Collectors;

public class RuleBasedSummarizer implements Summarizer {

    @Override
    public String summarize(String text, SummaryType type) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }

        String[] sentences = text.split("(?<=[.!?])\\s*");
        int totalSentences = sentences.length;

        if (totalSentences == 0) {
            return "";
        }

        Map<String, Integer> wordFreq = new HashMap<>();
        for (String sentence : sentences) {
            String[] words = sentence.toLowerCase().replaceAll("[^a-z ]", "").split("\\s+");
            for (String word : words) {
                if (word.isEmpty()) continue;
                wordFreq.put(word, wordFreq.getOrDefault(word, 0) + 1);
            }
        }

        Map<String, Integer> sentenceScore = new HashMap<>();
        for (String sentence : sentences) {
            int score = 0;
            for (String word : sentence.toLowerCase().replaceAll("[^a-z ]", "").split("\\s+")) {
                score += wordFreq.getOrDefault(word, 0);
            }
            sentenceScore.put(sentence, score);
        }

        int targetJumlahKalimat;

        switch (type) {
            case SHORT:
                if (totalSentences <= 2) {
                    targetJumlahKalimat = 1; // Jika teks sangat pendek, ambil 1 kalimat.
                } else {
                    targetJumlahKalimat = 2; // Untuk teks lebih panjang, maksimal 2 kalimat untuk "pendek".
                                             // Anda bisa juga menggunakan 1 jika ingin sangat singkat:
                                             // targetJumlahKalimat = 1;
                }
                break;
            case DETAILED:
                if (totalSentences <= 3) {
                    targetJumlahKalimat = totalSentences; // Ambil semua jika teksnya 3 kalimat atau kurang.
                } else if (totalSentences <= 8) {
                    targetJumlahKalimat = Math.min(totalSentences, 4); // Ambil hingga 4 kalimat.
                } else {
                    // Untuk teks lebih panjang, ambil sekitar 50% atau maksimal 5-7 kalimat.
                    targetJumlahKalimat = Math.max(4, (int) Math.round(totalSentences * 0.50));
                    targetJumlahKalimat = Math.min(targetJumlahKalimat, 7); // Batas atas absolut misal 7
                }
                break;
            case DEFAULT:
            default:
                if (totalSentences <= 2) {
                    targetJumlahKalimat = 1;
                } else if (totalSentences <= 6) {
                    targetJumlahKalimat = Math.min(totalSentences, 3); // Ambil hingga 3 kalimat.
                } else {
                    // Untuk teks lebih panjang, ambil sekitar 30-40% atau maksimal 3-4 kalimat.
                    targetJumlahKalimat = Math.max(3, (int) Math.round(totalSentences * 0.35));
                    targetJumlahKalimat = Math.min(targetJumlahKalimat, 4); // Batas atas absolut misal 4
                }
                break;
        }

        // Pastikan targetJumlahKalimat valid (minimal 1 jika ada kalimat, dan tidak melebihi total kalimat)
        if (totalSentences > 0) {
            targetJumlahKalimat = Math.max(1, targetJumlahKalimat);
            targetJumlahKalimat = Math.min(targetJumlahKalimat, totalSentences);
        } else {
            targetJumlahKalimat = 0; // Seharusnya tidak terjadi karena sudah dicek di awal
        }
        
        // Jika targetJumlahKalimat jadi 0 padahal ada kalimat, setidaknya ambil 1.
        if (targetJumlahKalimat == 0 && totalSentences > 0) {
            targetJumlahKalimat = 1;
        }


        // Urutkan kalimat berdasarkan skornya (dari tertinggi ke rendah)
        List<Map.Entry<String, Integer>> sortedByScoreEntries = sentenceScore.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toList());

        // Pilih N kalimat teratas berdasarkan skor
        Set<String> topScoringSentences = new HashSet<>();
        for (int i = 0; i < Math.min(targetJumlahKalimat, sortedByScoreEntries.size()); i++) {
            topScoringSentences.add(sortedByScoreEntries.get(i).getKey());
        }

        // Bangun ringkasan dengan mengambil kalimat yang terpilih sesuai urutan aslinya
        StringBuilder summaryBuilder = new StringBuilder();
        for (String originalSentence : sentences) {
            if (topScoringSentences.contains(originalSentence)) {
                summaryBuilder.append(originalSentence.trim()).append(" ");
                // Hapus dari set agar jika ada duplikat kalimat (jarang terjadi) tidak menambahkannya lagi
                // dan membantu jika kita ingin memastikan jumlah kalimat yang tepat.
                // Namun, untuk ringkasan berbasis seleksi, ini mungkin tidak perlu.
                // topScoringSentences.remove(originalSentence); // Opsional
            }
        }

        return summaryBuilder.toString().trim();
    }
}
