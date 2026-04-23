package com.project.Blog_Management_System.Utils;

public class ReadingTimeUtils {

    private static final int WORDS_PER_MINUTE = 200;

    /**
     * Estimates the reading time in minutes for a given content string.
     * The estimation is based on an average reading speed of 200 words per minute.
     *
     * @param content The content to estimate reading time for.
     * @return Estimated reading time in minutes, with a minimum of 1 minute.
     */
    public static int estimate(String content) {
        if (content == null || content.isBlank()) {
            return 1;
        }
        long wordCount = java.util.Arrays.stream(content.trim().split("\\s+"))
                .filter(w -> !w.isBlank())
                .count();

        return (int) Math.max(1, Math.ceil((double) wordCount / WORDS_PER_MINUTE));
    }
}

