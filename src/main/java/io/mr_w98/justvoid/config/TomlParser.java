package io.mr_w98.justvoid.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class TomlParser {

    private final String text;
    private int pos;
    private int line = 1;

    TomlParser(String text) {
        this.text = text.startsWith("﻿") ? text.substring(1) : text;
    }

    Map<String, Object> parseDocument() throws ConfigException {
        Map<String, Object> values = new LinkedHashMap<>();
        while (true) {
            skipBlankAndComments();
            if (atEnd()) return values;

            parseKeyValue(values);
            skipSpaces();
            skipComment();
            if (!atEnd() && !consumeNewline()) throw error("Expected end of line");
        }
    }

    private void parseKeyValue(Map<String, Object> values) throws ConfigException {
        String key = parseKey();
        skipSpaces();
        expect('=');
        skipSpaces();
        Object value = parseValue();
        if (values.put(key, value) != null) throw error("Duplicate setting '" + key + "'");
    }

    private String parseKey() throws ConfigException {
        if (atEnd()) throw error("Expected a setting name");
        if (peek() == '"' || peek() == '\'') return parseString();

        int start = pos;
        while (!atEnd() && isBareKeyChar(peek())) {
            pos++;
        }
        if (start == pos) throw error("Expected a setting name");
        return text.substring(start, pos);
    }

    private Object parseValue() throws ConfigException {
        if (atEnd()) throw error("Expected a value");

        char c = peek();
        if (c == '"' || c == '\'') return parseString();
        if (c == '[') return parseArray();
        if (c == '{') return parseInlineTable();
        if (c == '-' || c == '+' || (c >= '0' && c <= '9')) return parseInteger();
        if (text.startsWith("true", pos)) {
            pos += 4;
            return Boolean.TRUE;
        }
        if (text.startsWith("false", pos)) {
            pos += 5;
            return Boolean.FALSE;
        }
        throw error("Unsupported value");
    }

    private Long parseInteger() throws ConfigException {
        int start = pos;
        if (peek() == '-' || peek() == '+') {
            pos++;
        }
        while (!atEnd() && ((peek() >= '0' && peek() <= '9') || peek() == '_')) {
            pos++;
        }

        String digits = text.substring(start, pos).replace("_", "");
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException e) {
            throw error("Invalid number");
        }
    }

    private List<Object> parseArray() throws ConfigException {
        expect('[');
        List<Object> items = new ArrayList<>();
        while (true) {
            skipBlankAndComments();
            if (atEnd()) throw error("Unclosed list");
            if (peek() == ']') {
                pos++;
                return items;
            }

            items.add(parseValue());
            skipBlankAndComments();
            if (!atEnd() && peek() == ',') {
                pos++;
            } else if (atEnd() || peek() != ']') {
                throw error("Expected ',' or ']'");
            }
        }
    }

    private Map<String, Object> parseInlineTable() throws ConfigException {
        expect('{');
        Map<String, Object> values = new LinkedHashMap<>();
        skipSpaces();
        if (!atEnd() && peek() == '}') {
            pos++;
            return values;
        }

        while (true) {
            skipSpaces();
            parseKeyValue(values);
            skipSpaces();
            if (!atEnd() && peek() == ',') {
                pos++;
            } else {
                expect('}');
                return values;
            }
        }
    }

    private String parseString() throws ConfigException {
        char quote = text.charAt(pos++);
        StringBuilder out = new StringBuilder();
        while (true) {
            if (atEnd() || peek() == '\n' || peek() == '\r') throw error("Unclosed string");

            char c = text.charAt(pos++);
            if (c == quote) return out.toString();
            if (c == '\\' && quote == '"') {
                out.append(parseEscape());
            } else {
                out.append(c);
            }
        }
    }

    private String parseEscape() throws ConfigException {
        if (atEnd()) throw error("Unclosed string");

        char c = text.charAt(pos++);
        switch (c) {
            case '"':
                return "\"";
            case '\\':
                return "\\";
            case 'b':
                return "\b";
            case 't':
                return "\t";
            case 'n':
                return "\n";
            case 'f':
                return "\f";
            case 'r':
                return "\r";
            case 'u':
                return parseUnicode(4);
            case 'U':
                return parseUnicode(8);
            default:
                throw error("Invalid escape '\\" + c + "' (use '...' strings for patterns with backslashes)");
        }
    }

    private String parseUnicode(int digits) throws ConfigException {
        if (pos + digits > text.length()) throw error("Invalid unicode escape");

        String hex = text.substring(pos, pos + digits);
        pos += digits;
        try {
            return new String(Character.toChars(Integer.parseInt(hex, 16)));
        } catch (IllegalArgumentException e) {
            throw error("Invalid unicode escape");
        }
    }

    private void skipBlankAndComments() {
        while (!atEnd()) {
            char c = peek();
            if (c == ' ' || c == '\t') {
                pos++;
            } else if (c == '#') {
                skipComment();
            } else if (!consumeNewline()) {
                return;
            }
        }
    }

    private void skipSpaces() {
        while (!atEnd() && (peek() == ' ' || peek() == '\t')) {
            pos++;
        }
    }

    private void skipComment() {
        if (atEnd() || peek() != '#') return;

        while (!atEnd() && peek() != '\n' && peek() != '\r') {
            pos++;
        }
    }

    private boolean consumeNewline() {
        if (text.startsWith("\r\n", pos)) {
            pos += 2;
        } else if (peek() == '\n' || peek() == '\r') {
            pos++;
        } else {
            return false;
        }
        line++;
        return true;
    }

    private void expect(char expected) throws ConfigException {
        if (atEnd() || peek() != expected) throw error("Expected '" + expected + "'");
        pos++;
    }

    private boolean isBareKeyChar(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '-' || c == '_';
    }

    private boolean atEnd() {
        return pos >= text.length();
    }

    private char peek() {
        return text.charAt(pos);
    }

    private ConfigException error(String message) {
        return new ConfigException("config.toml line " + line + ": " + message + ".");
    }
}
