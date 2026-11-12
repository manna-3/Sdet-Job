package com.trcm.qa.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Small CSV reader that understands quoted fields. Used to verify the exported file. */
public final class CsvReader {

    private CsvReader() {
    }

    public static List<String> header(Path file) throws IOException {
        return splitLine(Files.readAllLines(file, StandardCharsets.UTF_8).get(0));
    }

    /** Returns data rows (header excluded) as column-name -> value maps. */
    public static List<Map<String, String>> rows(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        List<String> header = splitLine(lines.get(0));
        List<Map<String, String>> rows = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).isBlank()) {
                continue;
            }
            List<String> cells = splitLine(lines.get(i));
            Map<String, String> row = new LinkedHashMap<>();
            for (int c = 0; c < header.size(); c++) {
                row.put(header.get(c), c < cells.size() ? cells.get(c) : "");
            }
            rows.add(row);
        }
        return rows;
    }

    public static int totalLineCount(Path file) throws IOException {
        return (int) Files.readAllLines(file, StandardCharsets.UTF_8).stream().filter(l -> !l.isBlank()).count();
    }

    static List<String> splitLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (quoted) {
                if (ch == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cell.append('"');
                    i++;
                } else if (ch == '"') {
                    quoted = false;
                } else {
                    cell.append(ch);
                }
            } else if (ch == '"') {
                quoted = true;
            } else if (ch == ',') {
                out.add(cell.toString());
                cell.setLength(0);
            } else {
                cell.append(ch);
            }
        }
        out.add(cell.toString());
        return out;
    }
}
