package history;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.FileReader;

public class SummaryHistoryManager {
    private static final String HISTORY_FILE = "summary_history.txt";

    public static void saveHistory(String summary) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(HISTORY_FILE, true))) {
            writer.write(summary);
            writer.newLine();
            writer.write("-----");
            writer.newLine();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String loadHistory() {
        StringBuilder history = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(HISTORY_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                history.append(line).append("\n");
            }
        } catch (Exception e) {
            return "Belum riwayat tidak ditemukan.";
        }
        return history.toString();
    }
}