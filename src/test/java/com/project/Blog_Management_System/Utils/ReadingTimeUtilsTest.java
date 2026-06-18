package com.project.Blog_Management_System.Utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReadingTimeUtilsTest {

    @Nested
    @DisplayName("estimate()")
    class Estimate {

        @Test
        @DisplayName("returns 1 minute when content is null")
        void returnsOneForNullContent() {
            int result = ReadingTimeUtils.estimate(null);
            assertEquals(1, result);
        }

        @Test
        @DisplayName("returns 1 minute when content is empty")
        void returnsOneForEmptyContent() {
            int result = ReadingTimeUtils.estimate("");
            assertEquals(1, result);
        }

        @Test
        @DisplayName("returns 1 minute when content contains only blank spaces")
        void returnsOneForBlankContent() {
            int result = ReadingTimeUtils.estimate("     \n \t   ");
            assertEquals(1, result);
        }

        @ParameterizedTest
        @CsvSource({
                "1, 1",       // 1 word -> 1 min (minimum boundary)
                "50, 1",      // 50 words -> 1 min
                "199, 1",     // 199 words -> 1 min
                "200, 1",     // 200 words -> 1 min (exact threshold)
                "201, 2",     // 201 words -> 2 min (rounds up ceiling)
                "350, 2",     // 350 words -> 2 min
                "400, 2",     // 400 words -> 2 min (exact threshold)
                "401, 3",     // 401 words -> 3 min (rounds up ceiling)
                "1000, 5"     // 1000 words -> 5 min (exact threshold)
        })
        @DisplayName("calculates exact reading time based on word limits (200 WPM)")
        void calculatesCorrectReadingTime(int wordCount, int expectedMinutes) {
            // Generate a string with a precise number of space-separated words
            String content = String.join(" ", java.util.Collections.nCopies(wordCount, "word"));

            int result = ReadingTimeUtils.estimate(content);

            assertEquals(expectedMinutes, result);
        }

        @Test
        @DisplayName("ignores duplicate consecutive whitespaces, tabs, and newlines when counting words")
        void ignoresExtraWhitespacesAndNewlines() {
            // 3 words surrounded by multi-space delimiters, tabs, and newlines
            String messyContent = "   Word1 \n\n\t  Word2    \r\n Word3   ";

            int result = ReadingTimeUtils.estimate(messyContent);

            assertEquals(1, result);
        }
    }
}
