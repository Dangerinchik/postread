package com.postread.data;

import lombok.Getter;

@Getter
public enum ReactionType {
    LIKE(1, "👍", "Нравится", "positive"),
    DISLIKE(2, "👎", "Не нравится", "negative"),
    LOVE(3, "❤️", "Люблю", "positive"),
    LAUGH(4, "😂", "Смешно", "positive"),
    SAD(5, "😢", "Грустно", "negative"),
    ANGRY(6, "😠", "Злюсь", "negative"),
    SURPRISE(7, "😮", "Удивительно", "neutral"),
    FIRE(8, "🔥", "Огонь!", "positive"),
    THINKING(9, "🤔", "Заставляет задуматься", "neutral"),
    CLAP(10, "👏", "Аплодирую", "positive"),
    CONFUSED(11, "😕", "Не понял", "negative"),
    CELEBRATE(12, "🎉", "Праздную", "positive");

    private final int code;
    private final String emoji;
    private final String description;
    private final String category;

    ReactionType(int code, String emoji, String description, String category) {
        this.code = code;
        this.emoji = emoji;
        this.description = description;
        this.category = category;
    }

    public static ReactionType fromCode(int code) {
        for (ReactionType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown reaction code: " + code);
    }

    public static ReactionType fromEmoji(String emoji) {
        for (ReactionType type : values()) {
            if (type.emoji.equals(emoji)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown reaction emoji: " + emoji);
    }
}