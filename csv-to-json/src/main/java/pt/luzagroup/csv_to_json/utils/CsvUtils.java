package pt.luzagroup.csv_to_json.utils;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class CsvUtils {

    public static List<Map<String, String>> readCsvWithUniVocity(String path) throws IOException {
        CsvParserSettings settings = new CsvParserSettings();

        // Aumenta limite de caracteres por linha (default 4096)
        settings.getFormat().setLineSeparator("\n");
        settings.setMaxCharsPerColumn(100_000); // ou 1_000_000 se precisar muito
        settings.setHeaderExtractionEnabled(true);

        CsvParser parser = new CsvParser(settings);
        List<Map<String, String>> rows = new ArrayList<>();

        try (Reader reader = new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8)) {
            parser.beginParsing(reader);
            String[] row;
            while ((row = parser.parseNext()) != null) {
                Map<String, String> rowMap = new LinkedHashMap<>();
                for (int i = 0; i < row.length; i++) {
                    rowMap.put(parser.getContext().headers()[i], row[i]);
                }
                rows.add(rowMap);
            }
        }

        return rows;
    }
}