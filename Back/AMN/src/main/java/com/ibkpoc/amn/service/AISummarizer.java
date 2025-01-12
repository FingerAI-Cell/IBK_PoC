package com.ibkpoc.amn.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
public class AISummarizer {

    @Value("${ai.base-url}") // 환경별 URL 주입
    private String apiUrl;
    private static final Logger logger = LoggerFactory.getLogger(WebService.class);
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public AISummarizer(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl(apiUrl)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public String summarizeData(String rootJson) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("input", Map.of("root", rootJson));
        requestBody.put("config", new HashMap<>());
        requestBody.put("kwargs", new HashMap<>());

        try {
            // ObjectMapper로 JSON 요청 바디 로깅
            ObjectMapper objectMapper = new ObjectMapper();
            String requestBodyJson = objectMapper.writeValueAsString(requestBody);
            logger.info("Request Body: {}", requestBodyJson); // 요청 바디 로그 출력


            return webClient.post()
                    .uri(apiUrl)
                    .contentType(MediaType.APPLICATION_JSON) // Content-Type 명시
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .map(response -> {
                        logger.info("Response Body: {}", response); // 응답 바디 로그 출력
                        return (String) response.get("output");
                    })
                    .block(Duration.ofMinutes(5)); // 타임아웃 설정
        } catch (Exception e) {
            e.printStackTrace();
            return "요약을 생성하지 못했습니다.";
        }
    }
}


//
//@Service
//public class AISummarizer {
//
//    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
//    private static final String API_KEY = "sk-W7P1CoFn9-XYoLz24tl-4KpYWLKZI86vRKDK_XjM8zT3BlbkFJFeX_os_6LZbywZdhpDY4TQp3hvh6BiRrJqSs89H9wA"; // OpenAI API 키
//    private static final int MAX_TOKENS_PER_CHUNK = 1500;
//    private static final int MAX_RESPONSE_TOKENS = 300;
//
//    private final WebClient webClient;
//    private final ObjectMapper objectMapper;
//
//
//    public AISummarizer() {
//        this.webClient = WebClient.builder()
//                .baseUrl(API_URL)
//                .defaultHeader("Authorization", "Bearer " + API_KEY)
//                .build();
//        this.objectMapper = new ObjectMapper();
//    }
//
//    public List<String> summarizeData(List<Map<String, Object>> data) throws Exception {
//        List<List<Map<String, Object>>> chunks = splitIntoDynamicChunks(data, MAX_TOKENS_PER_CHUNK);
//
//        List<String> chunkSummaries = new ArrayList<>();
//
//        for (List<Map<String, Object>> chunk : chunks) {
//            String chunkJson = objectMapper.writeValueAsString(Collections.singletonMap("data", chunk));
//
//            String summary = requestSummary(chunkJson);
//            chunkSummaries.add(summary);
//        }
//
//        String finalInput = String.join("\n\n", chunkSummaries);
//        return Collections.singletonList(requestFinalSummary(finalInput));
//    }
//
//    private List<List<Map<String, Object>>> splitIntoDynamicChunks(List<Map<String, Object>> data, int maxTokens) {
//        List<List<Map<String, Object>>> chunks = new ArrayList<>();
//        List<Map<String, Object>> currentChunk = new ArrayList<>();
//        int currentTokens = 0;
//
//        for (Map<String, Object> entry : data) {
//            String entryJson;
//            try {
//                entryJson = objectMapper.writeValueAsString(entry);
//            } catch (Exception e) {
//                throw new RuntimeException("Failed to serialize entry", e);
//            }
//
//            int entryTokens = estimateTokens(entryJson);
//
//            if (entryTokens > maxTokens) {
//                throw new IllegalArgumentException("Single entry exceeds max token limit.");
//            }
//
//            if (currentTokens + entryTokens > maxTokens) {
//                chunks.add(new ArrayList<>(currentChunk));
//                currentChunk.clear();
//                currentTokens = 0;
//            }
//
//            currentChunk.add(entry);
//            currentTokens += entryTokens;
//        }
//
//        if (!currentChunk.isEmpty()) {
//            chunks.add(currentChunk);
//        }
//
//        return chunks;
//    }
//
//    private int estimateTokens(String text) {
//        // 간단한 토큰 추정: 공백으로 나누어 토큰 길이 계산 (실제 GPT 토큰 계산은 더 복잡함)
//        return text.split("\\s+").length;
//    }
//
//    private String requestSummary(String chunkJson) {
//        Map<String, Object> requestBody = new HashMap<>();
//        requestBody.put("model", "gpt-4");
//        requestBody.put("messages", List.of(
//                Map.of("role", "system", "content", "다음 JSON 데이터를 요약해줘."),
//                Map.of("role", "user", "content", chunkJson)
//        ));
//        requestBody.put("max_tokens", MAX_RESPONSE_TOKENS);
//
//        try{
//
//            return webClient.post()
//                .bodyValue(requestBody)
//                .retrieve()
//                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
//                })
//                .map(response -> {
//                    List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
//
//                    Map<String, Object> firstChoice = choices.get(0);
//                    Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
//                    return (String) message.get("content");
//                })
//                .block();
//        } catch (Exception e) {
//        // 에러 로그 출력 및 기본값 반환
//        e.printStackTrace();
//        return "요약을 생성하지 못했습니다.";
//        }
//    }
//
//    private String requestFinalSummary(String finalInput) {
//        Map<String, Object> requestBody = new HashMap<>();
//        requestBody.put("model", "gpt-4");
//        requestBody.put("messages", List.of(
//                Map.of("role", "system", "content", "다음 내용을 종합적으로 요약해줘."),
//                Map.of("role", "user", "content", finalInput)
//        ));
//        requestBody.put("max_tokens", MAX_RESPONSE_TOKENS);
//        try {
//            return webClient.post()
//                    .bodyValue(requestBody)
//                    .retrieve()
//                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
//                    })
//                    .map(response -> {
//                        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
//
//                        Map<String, Object> firstChoice = choices.get(0);
//                        Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
//                        return (String) message.get("content");
//                    })
//                    .block();
//        } catch (Exception e) {
//            // 에러 로그 출력 및 기본값 반환
//            e.printStackTrace();
//            return "요약을 생성하지 못했습니다.";
//        }
//    }
//}
