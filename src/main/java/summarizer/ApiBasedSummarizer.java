package summarizer;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap; // Import HashMap
import java.util.List;
import java.util.Map;    // Import Map
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

public class ApiBasedSummarizer implements Summarizer {

    private static final String API_URL = "https://api-inference.huggingface.co/models/cahya/t5-base-indonesian-summarization-cased";
    // Sebaiknya gunakan environment variable untuk token seperti diskusi kita sebelumnya
    // Tapi jika Anda memilih hardcode untuk saat ini, pastikan tokennya benar.
    private static final String API_TOKEN_AUTHORIZATION_HEADER = "Bearer"; + System.getenv("HUGGINGFACE_TOKEN"); // Pastikan ini token yang valid

    @Override
    // Ubah signature metode untuk menyertakan SummaryType
    public String summarize(String text, SummaryType type) throws Exception {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        List<String> chunks = splitTextIntoChunks(text, 1000); // Split jika panjang
        StringBuilder fullSummary = new StringBuilder();

        for (String chunk : chunks) {
            // Teruskan 'type' ke summarizeChunk
            String chunkSummary = summarizeChunk(chunk, type);
            fullSummary.append(chunkSummary).append(" ");
        }

        return fullSummary.toString().trim();
    }

    // Ubah signature metode untuk menyertakan SummaryType
    private String summarizeChunk(String textChunk, SummaryType type) throws Exception {
        URL url = new URL(API_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", API_TOKEN_AUTHORIZATION_HEADER);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        // Siapkan parameter berdasarkan SummaryType
        Map<String, Object> parameters = new HashMap<>();
        switch (type) {
            case SHORT:
                parameters.put("min_length", 20);  // perkiraan jumlah token
                parameters.put("max_length", 60); // perkiraan jumlah token
                break;
            case DETAILED:
                parameters.put("min_length", 80);
                parameters.put("max_length", 200); 
                break;
            case DEFAULT:
            default:
                parameters.put("min_length", 40);
                parameters.put("max_length", 120);
                break;
        }
        // Anda bisa menambahkan parameter lain yang didukung model
        // parameters.put("length_penalty", 2.0); 
        // parameters.put("num_beams", 4);

        Map<String, Object> payload = new HashMap<>();
        payload.put("inputs", textChunk);
        payload.put("parameters", parameters);

        ObjectMapper objectMapper = new ObjectMapper();
        String requestBody = objectMapper.writeValueAsString(payload);
        
        // System.out.println("Request Body API: " + requestBody); // Untuk debugging

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = connection.getResponseCode();
        StringBuilder responseContent = new StringBuilder();
        // Baca respons baik untuk kode sukses maupun error stream untuk kode gagal
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(responseCode >= 200 && responseCode < 300 ? 
                                      connection.getInputStream() : connection.getErrorStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                responseContent.append(line.trim());
            }
        }
        
        // System.out.println("Response Code API: " + responseCode); // Untuk debugging
        // System.out.println("Raw API Response: " + responseContent.toString()); // Untuk debugging

        if (responseCode != HttpURLConnection.HTTP_OK) {
            // Coba parse error dari Hugging Face jika ada
            try {
                JsonNode errorNode = objectMapper.readTree(responseContent.toString());
                if (errorNode.has("error")) {
                    throw new RuntimeException("API Error: " + errorNode.get("error").asText() + " (Code: " + responseCode + ")");
                }
            } catch (Exception e) {
                // Jika parsing error gagal, lempar dengan respons mentah
            }
            throw new RuntimeException("Gagal meringkas via API. HTTP Code: " + responseCode + ". Response: " + responseContent.toString());
        }
        
        JsonNode root = objectMapper.readTree(responseContent.toString());

        if (root.isArray() && root.size() > 0) {
            JsonNode summaryNode = root.get(0).get("summary_text");
            if (summaryNode != null) {
                return summaryNode.asText();
            }
        }
        // Jika format tidak sesuai atau ada error lain yang tidak tertangkap di atas
        throw new RuntimeException("Format respons API tidak valid atau 'summary_text' tidak ditemukan. Respons: " + responseContent.toString());
    }

    private List<String> splitTextIntoChunks(String text, int maxChunkCharLength) {
        List<String> chunks = new ArrayList<>();
         if (text == null || text.trim().isEmpty()) {
            return chunks;
        }
        for (int i = 0; i < text.length(); i += maxChunkCharLength) {
            chunks.add(text.substring(i, Math.min(text.length(), i + maxChunkCharLength)));
        }
        return chunks;
    }
}
