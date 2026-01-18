package demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.evaluation.FactCheckingEvaluator;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("e2e")
@SpringBootTestWithTestProfile
class ModelGatewayIntegrationTest {

    @Autowired
    private ModelGateway gateway;

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    private RelevancyEvaluator relevancyEvaluator;
    private FactCheckingEvaluator factCheckingEvaluator;

    @BeforeEach
    public void setup() {
        this.relevancyEvaluator = RelevancyEvaluator.builder().chatClientBuilder(chatClientBuilder).build();
        this.factCheckingEvaluator = FactCheckingEvaluator.builder(chatClientBuilder).build();
    }

    @Test
    void answerThePromptWithTheArtistName() {
        /* Given */
        final String prompt = """
                Who painted the Mona Lisa? \
                Please only answer with the artist's name.""";

        /* When */
        final String assistant = gateway.prompt(prompt);

        /* Then */
        assertThat(assistant)
                .describedAs("""
                        This is a direct question, and the model should respond \
                        with the artist's name. However, depending on the \
                        model's training, it may not follow the instruction \
                        precisely and could return additional information.""")
                .isIn(Set.of("Leonardo",
                        "Leonardo da Vinci.",
                        "Leonardo da Vinci"));
    }

    @Test
    @Disabled("This fails with the llama2 model!")
    void answerRelevantToThePrompt() {
        /* Given */
        final String prompt = "When did humans first land on the Moon?";

        /* When */
        final String assistant = gateway.prompt(prompt);

        /* Then */
        final EvaluationRequest evaluationRequest = new EvaluationRequest(prompt, assistant);
        final EvaluationResponse response = relevancyEvaluator.evaluate(evaluationRequest);
        assertThat(response.isPass())
                .withFailMessage("""
                        ========================================
                        The answer "%s"
                        is not considered relevant to the question
                        "%s".
                        ========================================
                        """, assistant, prompt)
                .isTrue();
    }

    @Test
    @Disabled("This fails with the llama2 model!")
    void answerFactuallyCorrect() {
        /* Given */
        final String prompt = "When did humans first land on the Moon?";

        /* When */
        final String assistant = gateway.prompt(prompt);

        /* Then */
        final EvaluationRequest evaluationRequest = new EvaluationRequest(prompt, assistant);
        final EvaluationResponse response = factCheckingEvaluator.evaluate(evaluationRequest);
        assertThat(response.isPass())
                .withFailMessage("""
                        ========================================
                        The answer "%s"
                        is not considered factually correct
                        "%s".
                        ========================================
                        """, assistant, prompt)
                .isTrue();
    }
}
