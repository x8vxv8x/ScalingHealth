package com.smd.scalinghealth.config;

import com.smd.scalinghealth.ScalingHealth;

public final class ConfigLineParser {
    private ConfigLineParser() {}

    public static String[] split(String optionName, String line, int expectedParts, String delimiterRegex) {
        String trimmed = line == null ? "" : line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return null;
        }

        String[] parts = trimmed.split(delimiterRegex);
        if (parts.length < expectedParts) {
            ScalingHealth.LOGGER.warn("配置项 \"{}\" 中存在格式错误的行，需要 {} 个字段: {}", optionName, expectedParts, line);
            return null;
        }

        if (parts.length > expectedParts) {
            ScalingHealth.LOGGER.warn("配置项 \"{}\" 中存在多余字段，仅使用前 {} 个字段: {}", optionName, expectedParts, line);
        }

        return parts;
    }

    public static Float parseFloat(String optionName, String value, String line) {
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException ex) {
            ScalingHealth.LOGGER.warn("配置项 \"{}\" 中无法解析浮点数 \"{}\": {}", optionName, value, line);
            return null;
        }
    }

    public static Integer parseInt(String optionName, String value, String line) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            ScalingHealth.LOGGER.warn("配置项 \"{}\" 中无法解析整数 \"{}\": {}", optionName, value, line);
            return null;
        }
    }
}
