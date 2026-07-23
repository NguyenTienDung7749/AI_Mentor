package com.example.aimentor.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AiResponseParserTest {

    @Test
    public void parse_structuredJson_mapsAllSections() {
        String json = "{"
                + "\"subject\":\"Science\","
                + "\"difficulty\":\"Intermediate\","
                + "\"directAnswer\":\"Gravity attracts masses.\","
                + "\"simplified\":\"Objects pull on each other.\","
                + "\"steps\":[\"Identify the masses\",\"Apply the law\"],"
                + "\"keyConcepts\":[\"Gravity\"],"
                + "\"commonMistakes\":[\"Ignoring distance\"],"
                + "\"followUps\":[\"What happens when distance doubles?\"]"
                + "}";

        AiAnswer answer = AiResponseParser.parse(json, "General", "Beginner");

        assertEquals("Science", answer.getSubject());
        assertEquals("Intermediate", answer.getDifficulty());
        assertEquals("Gravity attracts masses.", answer.getDirectAnswer());
        assertEquals(2, answer.getSteps().size());
        assertEquals(1, answer.getFollowUps().size());
    }

    @Test
    public void parse_markdownFencedJson_isAccepted() {
        String fenced = "```json\n"
                + "{\"direct_answer\":\"4\",\"steps\":[\"Add 2 and 2\"]}"
                + "\n```";

        AiAnswer answer = AiResponseParser.parse(
                fenced, SubjectClassifier.MATH, "Beginner");

        assertEquals("4", answer.getDirectAnswer());
        assertEquals(SubjectClassifier.MATH, answer.getSubject());
        assertEquals("Beginner", answer.getDifficulty());
    }

    @Test
    public void parse_plainText_keepsUsefulProviderOutput() {
        AiAnswer answer = AiResponseParser.parse(
                "Photosynthesis converts light energy into chemical energy.",
                SubjectClassifier.SCIENCE, "Intermediate");

        assertTrue(answer.getDirectAnswer().contains("Photosynthesis"));
        assertEquals(SubjectClassifier.SCIENCE, answer.getSubject());
    }

    @Test(expected = IllegalArgumentException.class)
    public void parse_emptyText_isRejected() {
        AiResponseParser.parse(" ", "General", "Intermediate");
    }

    @Test(expected = IllegalArgumentException.class)
    public void parse_truncatedJson_isRejectedInsteadOfStoredAsPlainText() {
        AiResponseParser.parse(
                "{\"directAnswer\":\"This object was cut off",
                "General", "Intermediate");
    }
}
