package pt.luzagroup.csv_to_json.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pt.luzagroup.csv_to_json.mappers.CandidateMapper;
import pt.luzagroup.csv_to_json.utils.CsvUtils;
import pt.luzagroup.csv_to_json.utils.ParserHelper;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CandidateService {

    private final ParserHelper helper;
    private final CandidateMapper candidateMapper;

    @Value("${app.input-path}")
    private String uploadPath;

    @Value("${app.output-path}")
    private String outputPath;

    @Value("${app.output-filename}")
    private String outputFilename;

    private Map<String, Map<String, String>> candidatesMap;
    private List<Map<String, String>> experiencesList;
    private List<Map<String, String>> experiencesIIList;
    private List<Map<String, String>> educationList;
    private List<Map<String, String>> certificationList;
    private List<Map<String, String>> notesList;

    public void runConversion() throws Exception {
        System.out.println("Carregando arquivos CSV...");

        // --- Leitura CSV ---
        List<Map<String, String>> candidatesList =
                CsvUtils.readCsvWithUniVocity(uploadPath + "Candidates_001.csv");

        candidatesMap = candidatesList.stream()
                .collect(Collectors.toMap(
                        row -> row.get("Candidate Id"),
                        row -> row,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));

        experiencesList = CsvUtils.readCsvWithUniVocity(uploadPath + "Candidates_Experience_Details.csv");
        experiencesIIList = CsvUtils.readCsvWithUniVocity(uploadPath + "Candidates_Experience_Details_II.csv");
        educationList = CsvUtils.readCsvWithUniVocity(uploadPath + "Candidates_Educational_Details.csv");
        certificationList = CsvUtils.readCsvWithUniVocity(uploadPath + "Candidates_Certifications_Details.csv");
        notesList = CsvUtils.readCsvWithUniVocity(uploadPath + "Notes_001.csv");

        // --- Logs de input ---
        System.out.println("Candidatos CSV: " + candidatesMap.size());
        System.out.println("Experiences CSV: " + experiencesList.size());
        System.out.println("Experiences II CSV: " + experiencesIIList.size());
        System.out.println("Education CSV: " + educationList.size());
        System.out.println("Certifications CSV: " + certificationList.size());
        System.out.println("Notes CSV: " + notesList.size());

        // --- Processamento ---
        AtomicInteger counter = new AtomicInteger(10001);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();

        List<Map<String, Object>> result = candidatesMap.entrySet().stream()
                .map(entry -> {
                    try {
                        Map<String, Object> mapped = candidateMapper.mapCandidate(
                                entry.getKey(),
                                entry.getValue(),
                                counter.getAndIncrement(),
                                experiencesList,
                                experiencesIIList,
                                educationList,
                                notesList
                        );

                        success.incrementAndGet();
                        return helper.cleanMap(mapped);

                    } catch (Exception e) {
                        failed.incrementAndGet();
                        System.err.println("❌ Erro no candidateId " + entry.getKey() + ": " + e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 🔥 AQUI está o saveJSON (não mudou de lugar)
        saveJSON(result);

        // --- Resumo final ---
        System.out.println("========== RESUMO ==========");
        System.out.println("Total CSV: " + candidatesMap.size());
        System.out.println("Convertidos com sucesso: " + success.get());
        System.out.println("Falhados: " + failed.get());
        System.out.println("Gerados no JSON: " + result.size());
        System.out.println("============================");

        System.out.println("✅ Conversão concluída!");
    }

    // ----------------------------
    // Salvando JSON
    // ----------------------------
    public void saveJSON(List<Map<String, Object>> data) throws Exception {
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

        Path outputDir = Paths.get(outputPath);
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
            System.out.println("✅ Pasta de saída criada: " + outputDir.toAbsolutePath());
        }

        Path outputFile = outputDir.resolve(outputFilename);

        try (Writer writer = new OutputStreamWriter(
                new FileOutputStream(outputFile.toFile()), StandardCharsets.UTF_8)) {
            gson.toJson(data, writer);
        }

        System.out.println("✅ JSON salvo em: " + outputFile.toAbsolutePath());
    }
}