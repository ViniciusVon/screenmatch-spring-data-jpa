package br.com.alura.screenmatch.service;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;

import io.github.cdimascio.dotenv.Dotenv;

public class ConsultaChatGPT {
    public static String obterTraducao(String texto) {
        Dotenv dotenv = Dotenv.load();
        String chaveApi = dotenv.get("CHAVE_OPENAI");

        if (chaveApi == null || chaveApi.trim().isEmpty()) {
            throw new IllegalArgumentException("A chave da API OpenAI (CHAVE_OPENAI) não foi encontrada no arquivo .env");
        }

        System.setProperty("OPENAI_API_KEY", chaveApi);

        OpenAIClient client = OpenAIOkHttpClient.builder()
                .apiKey(chaveApi)
                .build();

        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model("gpt-4o-mini")
                .addUserMessage("traduza para o português o texto: " + texto)
                .maxCompletionTokens(300)
                .temperature(0.7)
                .build();

        try {
            ChatCompletion resposta = client.chat().completions().create(params);
            if (resposta.choices() != null && !resposta.choices().isEmpty()) {
                return resposta.choices().get(0).message().content().orElse("Não foi possível obter uma tradução.");
            }
            return "Não foi possível obter uma tradução.";

        } catch (Exception e) {
            if (e.getMessage().contains("429")) {
                return "Limite de uso da API atingido. Por favor, verifique sua conta na OpenAI.";
            }
            System.err.println("Erro ao chamar a API da OpenAI: " + e.getMessage());
            return "Erro ao tentar traduzir o texto.";
        }
    }
}
