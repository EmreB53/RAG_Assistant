package org.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class LocalAIChat {
    private static final String OLLAMA_GENERATE_URL = "http://localhost:11434/api/generate";
    private static final String OLLAMA_EMBED_URL = "http://localhost:11434/api/embeddings";
    private static final String CHAT_MODEL = "llama3.2";
    private static final String BACKEND_MODEL = "llama3.2";
    private static final String EMBED_MODEL = "nomic-embed-text";
    private static final String LOG_PATH = "chatlog.json";

    private static final Gson GSON = new Gson();
    private static final Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        List<ChatMessages> messages = new ArrayList<>();
        DatabaseManager db = new DatabaseManager();

        System.out.println("Vector Database connected.");
        System.out.println("Connected to '" + CHAT_MODEL + "'. You may start typing!\n");

        while (true) {
            System.out.print("You :> ");
            String userInput = sc.nextLine();

            if (userInput.equalsIgnoreCase("exit")) break;

            String contextBlock = retrieveContext(userInput, db);
            String finalPrompt = buildPrompt(userInput, contextBlock, messages);

            messages.add(new ChatMessages("User", userInput));

            try {
                String response = askOllama(finalPrompt, CHAT_MODEL);
                System.out.println("\n" + CHAT_MODEL + ": " + response + "\n");
                messages.add(new ChatMessages("AI", response));

                saveChatLog(messages);
                indexResponseInBackground(userInput, response, db);

            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
        sc.close();
    }

    private static String retrieveContext(String userInput, DatabaseManager db) {
        try {
            List<Double> searchVector = getEmbedding(userInput);
            List<String> matchedSummaries = db.getRelevantSummaries(searchVector);

            if (matchedSummaries.isEmpty()) return "";

            StringBuilder context = new StringBuilder();
            for (String summary : matchedSummaries) {
                context.append("- ").append(summary).append("\n");
            }
            return context.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private static void indexResponseInBackground(String userInput, String aiResponse, DatabaseManager db) {
        String transaction = "User: " + userInput + "\nAI: " + aiResponse;
        String prompt = "Write a single, concise 1-sentence summary of the core facts from this interaction. Do not use formatting:\n'" + transaction + "'";

        try {
            String summary = askOllama(prompt, BACKEND_MODEL).trim();
            List<Double> embedding = getEmbedding(summary);

            db.saveMemory(summary, embedding);
            System.out.println("!Vector memory saved to PostgreSQL!\n");
        } catch (Exception e) {
            System.err.println("Failed to index memory: " + e.getMessage() + "\n");
        }
    }

    private static String askOllama(String prompt, String model) throws Exception {
        OReq reqObj = new OReq(model, prompt);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OLLAMA_GENERATE_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(reqObj)))
                .build();

        HttpResponse<String> res = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() == 200) {
            ORep reply = GSON.fromJson(res.body(), ORep.class);
            return reply.response.trim();
        }
        throw new RuntimeException("HTTP Error " + res.statusCode());
    }

    private static List<Double> getEmbedding(String text) throws Exception {
        EmbedReq reqObj = new EmbedReq(EMBED_MODEL, text);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OLLAMA_EMBED_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(reqObj)))
                .build();

        HttpResponse<String> res = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() == 200) {
            EmbedRep reply = GSON.fromJson(res.body(), EmbedRep.class);
            return reply.embedding;
        }
        throw new RuntimeException("HTTP Error " + res.statusCode());
    }

    private static String buildPrompt(String userInput, String contextBlock, List<ChatMessages> messages) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("System: You are a helpful AI assistant. Answer naturally. ");
        promptBuilder.append("If past context is provided use it to inform your answer seamlessly.\n\n");

        if (!contextBlock.isEmpty()) {
            promptBuilder.append("PAST CONTEXT (MEMORIES):\n");
            promptBuilder.append(contextBlock).append("\n");
            promptBuilder.append("------------------------------------------------\n\n");
            System.out.println("Vector memories are being injected into prompt.");
        }

        promptBuilder.append("RECENT CHAT HISTORY:\n");
        int startIndex = Math.max(0, messages.size() - 4);
        for (int i = startIndex; i < messages.size(); i++) {
            ChatMessages m = messages.get(i);
            promptBuilder.append(m.role).append(": ").append(m.content).append("\n");
        }
        promptBuilder.append("-------------------------------------\n\n");
        promptBuilder.append("User: ").append(userInput).append("\nAI: ");
        return promptBuilder.toString();
    }

    private static void saveChatLog(List<ChatMessages> messages) {
        try (FileWriter writer = new FileWriter(LOG_PATH)) {
            PRETTY_GSON.toJson(messages, writer);
        } catch (IOException ignored) {}
    }
}

class OReq {
    String model;
    String prompt;
    boolean stream;

    public OReq(String model, String prompt) {
        this.model = model;
        this.prompt = prompt;
        this.stream = false;
    }
}

class ORep {
    String response;
}

class EmbedReq {
    String model;
    String prompt;

    public EmbedReq(String model, String prompt) {
        this.model = model;
        this.prompt = prompt;
    }
}

class EmbedRep {
    List<Double> embedding;
}

class ChatMessages {
    String role;
    String content;

    public ChatMessages(String role, String content) {
        this.role = role;
        this.content = content;
    }
}