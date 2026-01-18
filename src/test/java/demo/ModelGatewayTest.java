package demo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@EnableWireMock(@ConfigureWireMock(baseUrlProperties = "ollama.base.url"))
@SpringBootTestWithTestProfile(properties = "spring.ai.ollama.base-url=${ollama.base.url}")
class ModelGatewayTest {

    @Value("classpath:/fixtures/response.json")
    private Resource responseResource;

    @Autowired
    private ModelGateway gateway;

    @BeforeEach
    public void setup() throws IOException {
        final String cannedResponse = responseResource.getContentAsString(StandardCharsets.UTF_8);
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode responseNode = mapper.readTree(cannedResponse);

        WireMock.reset();
        WireMock.stubFor(WireMock.post("/api/chat")
                .willReturn(ResponseDefinitionBuilder.okForJson(responseNode)));
    }

    @Test
    void returnTheModelsResponse() throws JsonProcessingException {
        /* Given */
        final String prompt = """
                Return only the date when humans first landed on the Moon, \
                formatted strictly as YYYY-MM-DD with no additional text.""";

        /* When */
        final String assistant = gateway.prompt(prompt);

        /* Then */
        assertThat(assistant)
                .isEqualTo("1969-07-20");

        final List<ServeEvent> calls = WireMock.getAllServeEvents();
        assertThat(calls)
                .describedAs("One request was made to the model")
                .hasSize(1);
        final ServeEvent lastRequest = calls.getFirst(); /* The last request */
        final JsonNode messages = new ObjectMapper().readTree(lastRequest.getRequest().getBodyAsString()).get("messages");
        assertThat(messages.isArray())
                .describedAs("Messages are an array of messages/prompts sent to the model")
                .isTrue();
        assertThat(messages.size())
                .describedAs("One message should be expected in this conversation")
                .isEqualTo(1);
        assertMessage(messages.get(0), "user", prompt);
    }

    private static void assertMessage(final JsonNode message, final String role, final String content) {
        assertThat(message.has("role")).isTrue();
        assertThat(message.get("role").isTextual()).isTrue();
        assertThat(message.get("role").asText()).isEqualTo(role);
        assertThat(message.has("content")).isTrue();
        assertThat(message.get("content").isTextual()).isTrue();
        assertThat(message.get("content").asText()).isEqualTo(content);
    }
}
