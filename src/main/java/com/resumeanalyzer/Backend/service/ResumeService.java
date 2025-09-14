package com.resumeanalyzer.Backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;

@Service
public class ResumeService {

    private final OkHttpClient client = new OkHttpClient();

    public Map<String, Object> analyzeResume(MultipartFile file, String apiKey) throws Exception {
        String resumeText = extractText(file);

        // ✅ Force AI to return only JSON
       String prompt = """
    You are a resume analyzer. Analyze the resume text below.

    IMPORTANT: Return ONLY valid JSON (no explanations, no text before or after).  
    JSON format must be exactly:  
    {
      "score": number between 0-100,
      "top3Improvements": ["exactly 3 clear, actionable, high-impact changes"],
      "suggestionsToStandOut": ["concise tips on formatting, phrasing, or keywords"],
      "quote": "a short motivational quote"
    }

    Analyze the following resume and provide a sleek, professional, and to-the-point evaluation. 
    Keep the tone professional, direct, and free of unnecessary detail.

    Resume Text:
    """ + resumeText;


        // ✅ Build request
        JSONObject requestBody = new JSONObject()
                .put("contents", new JSONArray()
                        .put(new JSONObject()
                                .put("parts", new JSONArray()
                                        .put(new JSONObject().put("text", prompt))
                                )
                        )
                );

        RequestBody body = RequestBody.create(
                requestBody.toString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();

        Response response = client.newCall(request).execute();
        String responseBody = response.body().string();

        // ✅ Debug: print full Gemini response
        System.out.println("🔍 Full AI Response: " + responseBody);

        Map<String, Object> result = new HashMap<>();
       try {
    // ✅ Extract AI text
    JSONObject json = new JSONObject(responseBody);
    JSONArray candidates = json.optJSONArray("candidates");

    if (candidates != null && candidates.length() > 0) {
        String aiOutput = candidates.getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text");

        System.out.println("🔍 Extracted AI Text: " + aiOutput);

        // ✅ Clean up: extract JSON part using regex
        String cleanedJson = aiOutput.replaceAll("(?s).*?(\\{.*\\}).*", "$1");

        System.out.println("🔍 Cleaned JSON: " + cleanedJson);

        // ✅ Parse cleaned JSON
        result = new ObjectMapper().readValue(
                cleanedJson,
                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}
        );
    } else {
        throw new RuntimeException("No AI response found");
    }

} catch (Exception e) {
    System.err.println("⚠️ JSON Parsing Failed: " + e.getMessage());

    result.put("score", 60);
    result.put("suggestions", List.of(
            "AI output parsing failed",
            "Add measurable achievements",
            "Tailor resume to specific job roles"
    ));
    result.put("quote", "Keep improving, your best is yet to come!");
}

        return result;
    }

    // ✅ Extract text from PDF/DOCX
    private String extractText(MultipartFile file) throws Exception {
        String name = file.getOriginalFilename().toLowerCase();
        InputStream input = file.getInputStream();

        if (name.endsWith(".pdf")) {
            try (PDDocument document = PDDocument.load(input)) {
                return new PDFTextStripper().getText(document);
            }
        } else if (name.endsWith(".docx")) {
            try (XWPFDocument document = new XWPFDocument(input)) {
                StringBuilder sb = new StringBuilder();
                document.getParagraphs().forEach(p -> sb.append(p.getText()).append("\n"));
                return sb.toString();
            }
        } else {
            throw new IllegalArgumentException("Unsupported file format. Please upload PDF or DOCX.");
        }
    }
}
