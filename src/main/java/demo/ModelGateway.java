package demo;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import static java.util.Objects.requireNonNull;

@Service
class ModelGateway {

    private final ChatClient client;

    ModelGateway(final ChatClient.Builder builder) {
        this.client = builder.build();
    }

    String prompt(final String prompt) {
        requireNonNull(prompt);

        return client.prompt(prompt)
                .call()
                .content();
    }
}
