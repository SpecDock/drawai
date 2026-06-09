package com.drawai.domain.aiengine.service.process.workshop;

import java.util.function.Consumer;

/**
 * Removes provider reasoning blocks before AI output enters business processing.
 */
public final class ThinkBlockFilter {

    private static final String OPEN_TAG = "<think>";
    private static final String CLOSE_TAG = "</think>";

    private ThinkBlockFilter() {
    }

    public static String strip(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder(text.length());
        int index = 0;
        while (index < text.length()) {
            int openIndex = text.indexOf(OPEN_TAG, index);
            if (openIndex < 0) {
                result.append(text, index, text.length());
                break;
            }

            result.append(text, index, openIndex);
            int closeIndex = text.indexOf(CLOSE_TAG, openIndex + OPEN_TAG.length());
            if (closeIndex < 0) {
                break;
            }
            index = closeIndex + CLOSE_TAG.length();
        }
        return result.toString();
    }

    public static Stream stream() {
        return new Stream(ignored -> {
        });
    }

    public static Stream stream(Consumer<String> thinkingSink) {
        return new Stream(thinkingSink);
    }

    public static final class Stream {

        private final StringBuilder pending = new StringBuilder();
        private final Consumer<String> thinkingSink;
        private boolean inThinkBlock;

        private Stream(Consumer<String> thinkingSink) {
            this.thinkingSink = thinkingSink == null ? ignored -> {
            } : thinkingSink;
        }

        public String append(String chunk) {
            if (chunk != null && !chunk.isEmpty()) {
                pending.append(chunk);
            }
            return drain(false);
        }

        public String finish() {
            return drain(true);
        }

        private String drain(boolean finalChunk) {
            StringBuilder result = new StringBuilder();
            while (!pending.isEmpty()) {
                if (inThinkBlock) {
                    int closeIndex = pending.indexOf(CLOSE_TAG);
                    if (closeIndex >= 0) {
                        emitThinking(pending.substring(0, closeIndex));
                        pending.delete(0, closeIndex + CLOSE_TAG.length());
                        inThinkBlock = false;
                        continue;
                    }
                    if (finalChunk) {
                        emitThinking(pending.toString());
                        pending.setLength(0);
                    } else {
                        int keepLength = possibleTagSuffixLength(CLOSE_TAG);
                        int emitLength = pending.length() - keepLength;
                        if (emitLength > 0) {
                            emitThinking(pending.substring(0, emitLength));
                            pending.delete(0, emitLength);
                        }
                    }
                    break;
                }

                int openIndex = pending.indexOf(OPEN_TAG);
                if (openIndex >= 0) {
                    result.append(pending, 0, openIndex);
                    pending.delete(0, openIndex + OPEN_TAG.length());
                    inThinkBlock = true;
                    continue;
                }

                if (finalChunk) {
                    result.append(pending);
                    pending.setLength(0);
                } else {
                    int keepLength = possibleTagSuffixLength(OPEN_TAG);
                    int emitLength = pending.length() - keepLength;
                    if (emitLength > 0) {
                        result.append(pending, 0, emitLength);
                        pending.delete(0, emitLength);
                    }
                }
                break;
            }
            return result.toString();
        }

        private void emitThinking(String content) {
            if (content != null && !content.isEmpty()) {
                thinkingSink.accept(content);
            }
        }

        private int possibleTagSuffixLength(String tag) {
            int maxLength = Math.min(pending.length(), tag.length() - 1);
            for (int length = maxLength; length > 0; length--) {
                if (endsWithPrefix(tag, length)) {
                    return length;
                }
            }
            return 0;
        }

        private boolean endsWithPrefix(String tag, int prefixLength) {
            int start = pending.length() - prefixLength;
            for (int i = 0; i < prefixLength; i++) {
                if (pending.charAt(start + i) != tag.charAt(i)) {
                    return false;
                }
            }
            return true;
        }
    }
}
