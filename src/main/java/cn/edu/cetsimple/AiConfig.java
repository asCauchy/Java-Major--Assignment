package cn.edu.cetsimple;

import java.util.prefs.Preferences;

public record AiConfig(
        Provider provider,
        Protocol protocol,
        String endpoint,
        String model,
        String apiKey,
        String authHeader,
        String authPrefix
) {
    private static final Preferences PREFS = Preferences.userNodeForPackage(AiConfig.class);
    private static volatile String sessionApiKey = "";

    public enum Provider {
        DEEPSEEK(
                "DeepSeek（深度求索）",
                Protocol.CHAT_COMPLETIONS,
                "https://api.deepseek.com/chat/completions",
                "deepseek-flash",
                "DEEPSEEK_API_KEY"
        ),
        KIMI(
                "Kimi（月之暗面）",
                Protocol.CHAT_COMPLETIONS,
                "https://api.moonshot.cn/v1/chat/completions",
                "kimi-k3",
                "MOONSHOT_API_KEY"
        ),
        QWEN(
                "通义千问（阿里云百炼）",
                Protocol.CHAT_COMPLETIONS,
                "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions",
                "qwen-plus",
                "DASHSCOPE_API_KEY"
        ),
        ZHIPU(
                "智谱 GLM",
                Protocol.CHAT_COMPLETIONS,
                "https://open.bigmodel.cn/api/paas/v4/chat/completions",
                "glm-5.3",
                "ZHIPUAI_API_KEY"
        ),
        OPENAI(
                "OpenAI",
                Protocol.RESPONSES,
                "https://api.openai.com/v1/responses",
                "",
                "OPENAI_API_KEY"
        ),
        CUSTOM(
                "自定义兼容接口",
                Protocol.CHAT_COMPLETIONS,
                "",
                "",
                "AI_API_KEY"
        );

        private final String displayName;
        private final Protocol protocol;
        private final String endpoint;
        private final String model;
        private final String environmentKey;

        Provider(String displayName, Protocol protocol, String endpoint, String model, String environmentKey) {
            this.displayName = displayName;
            this.protocol = protocol;
            this.endpoint = endpoint;
            this.model = model;
            this.environmentKey = environmentKey;
        }

        public Protocol protocol() { return protocol; }
        public String endpoint() { return endpoint; }
        public String model() { return model; }
        public String environmentKey() { return environmentKey; }

        public String toString() {
            return displayName;
        }
    }

    public enum Protocol {
        CHAT_COMPLETIONS("OpenAI 兼容 Chat Completions"),
        RESPONSES("OpenAI Responses API");

        private final String displayName;

        Protocol(String displayName) {
            this.displayName = displayName;
        }

        public String toString() {
            return displayName;
        }
    }

    public static AiConfig load() {
        String savedEndpoint = PREFS.get("endpoint", "https://api.openai.com/v1/chat/completions");
        Provider provider;
        try {
            provider = Provider.valueOf(PREFS.get("provider", inferProvider(savedEndpoint).name()));
        } catch (IllegalArgumentException exception) {
            provider = inferProvider(savedEndpoint);
        }
        Protocol protocol;
        try {
            protocol = Protocol.valueOf(PREFS.get("protocol", Protocol.CHAT_COMPLETIONS.name()));
        } catch (IllegalArgumentException exception) {
            protocol = Protocol.CHAT_COMPLETIONS;
        }
        String key = sessionApiKey;
        if (key.isBlank()) key = System.getenv().getOrDefault(provider.environmentKey(), "");
        if (key.isBlank()) key = System.getenv().getOrDefault("AI_API_KEY", "");
        if (key.isBlank()) key = System.getenv().getOrDefault("OPENAI_API_KEY", "");
        return new AiConfig(
                provider,
                protocol,
                savedEndpoint,
                PREFS.get("model", ""),
                key,
                PREFS.get("authHeader", "Authorization"),
                PREFS.get("authPrefix", "Bearer")
        );
    }

    public static void save(AiConfig config) {
        PREFS.put("provider", config.provider().name());
        PREFS.put("protocol", config.protocol().name());
        PREFS.put("endpoint", config.endpoint().trim());
        PREFS.put("model", config.model().trim());
        PREFS.put("authHeader", config.authHeader().trim());
        PREFS.put("authPrefix", config.authPrefix().trim());
        sessionApiKey = config.apiKey() == null ? "" : config.apiKey().trim();
    }

    public boolean isReady() {
        return endpoint != null && !endpoint.isBlank() && model != null && !model.isBlank();
    }

    public String providerSummary() {
        if (!isReady()) return "尚未配置模型接口";
        return provider + " · " + model;
    }

    private static Provider inferProvider(String endpoint) {
        String value = endpoint == null ? "" : endpoint.toLowerCase();
        if (value.contains("deepseek.com")) return Provider.DEEPSEEK;
        if (value.contains("moonshot.cn") || value.contains("moonshot.ai") || value.contains("kimi.com")) {
            return Provider.KIMI;
        }
        if (value.contains("dashscope") || value.contains("maas.aliyuncs.com")) return Provider.QWEN;
        if (value.contains("bigmodel.cn")) return Provider.ZHIPU;
        if (value.contains("api.openai.com")) return Provider.OPENAI;
        return Provider.CUSTOM;
    }
}
