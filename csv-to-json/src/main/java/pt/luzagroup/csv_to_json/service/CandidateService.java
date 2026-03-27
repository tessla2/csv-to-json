package pt.luzagroup.csv_to_json.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import com.opencsv.exceptions.CsvMalformedLineException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class CandidateService {

    @Value("${app.input-path}")
    private String uploadPath;

    @Value("${app.output-path}")
    private String outputPath;

    @Value("${app.output-filename}")
    private String outputFilename;

    private static final int STARTING_ID = 10001;

    private Map<String, Map<String, String>> candidatesMap;
    private List<Map<String, String>> experiencesList;
    private List<Map<String, String>> experiencesIIList;
    private List<Map<String, String>> educationList;
    private List<Map<String, String>> certificationList;
    private List<Map<String, String>> notesList;

    public void runConversion() throws Exception {
        System.out.println("Carregando arquivos CSV...");

            candidatesMap = loadCSVAsMap(uploadPath + "Candidates_001.csv", "Candidate Id");
            experiencesList = loadCSVAsList(uploadPath + "Candidates_Experience_Details.csv");
            experiencesIIList = loadCSVAsList(uploadPath + "Candidates_Experience_Details_II.csv");
            educationList = loadCSVAsList(uploadPath + "Candidates_Educational_Details.csv");
            certificationList = loadCSVAsList(uploadPath + "Candidates_Certifications_Details.csv");
            notesList = loadCSVAsList(uploadPath + "Notes_001.csv");

            System.out.println("Candidatos: " + candidatesMap.size());
            System.out.println("Experiências: " + experiencesList.size());
            System.out.println("Experiências II: " + experiencesIIList.size());
            System.out.println("Educação: " + educationList.size());
            System.out.println("Certificações: " + certificationList.size());
            System.out.println("Notas: " + notesList.size());

        List<Map<String, Object>> result = processAllCandidates();

        saveJSON(result);

        System.out.println("✅ Conversão concluída!");
    }

    // ----------------------------
    // CSV Loaders
    // ----------------------------
    private Map<String, Map<String, String>> loadCSVAsMap(String filePath, String keyColumn)
            throws IOException, CsvException {
        Map<String, Map<String, String>> result = new LinkedHashMap<>();

        CSVParser parser = new CSVParserBuilder()
                .withSeparator(',')
                .withQuoteChar('"')
                .withStrictQuotes(false)
                .withIgnoreQuotations(false)
                .build();

        try (CSVReader reader = new CSVReaderBuilder(
                new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))
                .withCSVParser(parser)
                .build()) {

            List<String[]> rows = reader.readAll();
            if (rows.isEmpty()) return result;

            String[] headers = rows.get(0);
            int keyIndex = Arrays.asList(headers).indexOf(keyColumn);

            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                Map<String, String> rowMap = new HashMap<>();
                for (int j = 0; j < headers.length && j < row.length; j++) {
                    rowMap.put(headers[j], row[j]);
                }
                if (keyIndex >= 0 && keyIndex < row.length) {
                    result.put(row[keyIndex], rowMap);
                }
            }
        }

        return result;
    }

    private List<Map<String, String>> loadCSVAsList(String filePath) throws IOException, CsvException {
        List<Map<String, String>> result = new ArrayList<>();

        CSVParser parser = new CSVParserBuilder()
                .withSeparator(',')
                .withQuoteChar('"')
                .withStrictQuotes(false)
                .withIgnoreQuotations(false)
                .build();

        try (CSVReader reader = new CSVReaderBuilder(
                new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))
                .withCSVParser(parser)
                .build()) {

            String[] headers = reader.readNext();
            if (headers == null) return result;

            String[] row;
            int lineNum = 1; // linha do CSV (contando header)
            while ((row = reader.readNext()) != null) {
                lineNum++;
                try {
                    Map<String, String> rowMap = new HashMap<>();
                    for (int j = 0; j < headers.length && j < row.length; j++) {
                        rowMap.put(headers[j], row[j]);
                    }
                    result.add(rowMap);
                } catch (Exception e) {
                    System.err.println("❌ Erro na linha " + lineNum + ": " + e.getMessage());
                    // ignora a linha e continua
                }
            }
        } catch (CsvMalformedLineException e) {
            System.err.println("❌ CSV mal formatado: " + e.getMessage());
        }

        return result;
    }

    // ----------------------------
    // Processamento de candidatos
    // ----------------------------
    private List<Map<String, Object>> processAllCandidates() {
        List<Map<String, Object>> result = new ArrayList<>();
        int counter = STARTING_ID;

        for (Map.Entry<String, Map<String, String>> entry : candidatesMap.entrySet()) {
            String candidateId = entry.getKey();
            Map<String, String> row = entry.getValue();
            Map<String, Object> candidate = processCandidate(candidateId, row, counter);
            result.add(cleanMap(candidate));
            counter++;
        }
        return result;
    }

    private Map<String, Object> processCandidate(String candidateId, Map<String, String> row, int id) {
        Map<String, Object> candidate = new LinkedHashMap<>();

        candidate.put("id", id);
        putIfNotEmpty(candidate, "name", cleanValue(row.get("Full Name")));
        putIfNotEmpty(candidate, "description", cleanValue(row.get("Candidate Description/Summary")));
        putIfNotEmpty(candidate, "creator", cleanValue(row.get("Created By")));
        putIfNotEmpty(candidate, "owner", cleanValue(row.get("Candidate Owner")));

        String job = cleanValue(row.get("Current/Last Job Title"));
        if (isEmpty(job)) job = cleanValue(row.get("Current Job Title"));
        putIfNotEmpty(candidate, "job", job);

        putIfNotEmpty(candidate, "company", cleanValue(row.get("Current Employer")));

        candidate.put("yearofexperience", parseYearsOfExperience(row.get("Experience in Years")));

        putIfNotEmpty(candidate, "birthdate", parseDate(row.get("Date of Birth")));
        putIfNotEmpty(candidate, "address", cleanValue(row.get("Street")));

        String country = cleanValue(row.get("Candidate Country"));
        if (isEmpty(country)) country = cleanValue(row.get("Other Country"));
        putIfNotEmpty(candidate, "country", country);

        String city = cleanValue(row.get("Candidate - City"));
        if (isEmpty(city)) city = cleanValue(row.get("City"));
        putIfNotEmpty(candidate, "city", city);

        putIfNotEmpty(candidate, "email", cleanValue(row.get("Email")));
        String phone = cleanValue(row.get("Phone"));
        if (isEmpty(phone)) phone = cleanValue(row.get("Mobile"));
        putIfNotEmpty(candidate, "phonenumber", phone);

        putIfNotEmpty(candidate, "skype", cleanValue(row.get("Skype ID")));

        Integer currentSalary = parseSalary(row.get("Current Salary"));
        if (currentSalary != null) candidate.put("csalary", currentSalary);
        putIfNotEmpty(candidate, "ccurrency", cleanValue(row.getOrDefault("Currency", "USD")));

        Integer expectedSalary = parseSalary(row.get("Expected Salary"));
        if (expectedSalary != null) candidate.put("esalary", expectedSalary);
        putIfNotEmpty(candidate, "ecurrency", cleanValue(row.getOrDefault("Currency", "USD")));
        putIfNotEmpty(candidate, "worktype", "permanent");

        String linkedin = cleanValue(row.get("LinkedIn"));
        if (!isEmpty(linkedin) && !linkedin.startsWith("http")) linkedin = "https://" + linkedin;
        putIfNotEmpty(candidate, "linkedin", linkedin);


        List<String> languages = extractLanguages(row.get("Languages"));
        if (!languages.isEmpty()) candidate.put("languages", languages);

        List<String> skills = extractSkills(row);
        if (!skills.isEmpty()) candidate.put("skills", skills);

        List<Map<String, Object>> experiences = buildExperiences(candidateId);
        if (!experiences.isEmpty()) candidate.put("experiences", experiences);

        List<Map<String, Object>> education = buildEducation(candidateId);
        if (!education.isEmpty()) candidate.put("education", education);

        List<Map<String, Object>> notes = buildNotes(candidateId);
        if (!notes.isEmpty()) candidate.put("note", notes);

        return candidate;
    }

    private List<Map<String, Object>> buildExperiences(String candidateId) {
        List<Map<String, Object>> experiences = new ArrayList<>();

        for (Map<String, String> exp : experiencesList) {
            if (candidateId.equals(exp.get("Candidate Id"))) {
                Map<String, Object> e = new LinkedHashMap<>();
                String position = cleanValue(exp.get("Occupation / Title"));
                if (isEmpty(position)) position = cleanValue(exp.get("Occupation"));
                putIfNotEmpty(e, "position_name", position);
                putIfNotEmpty(e, "employer", cleanValue(exp.get("Company")));
                putIfNotEmpty(e, "started_at", parseDate(exp.get("Work Duration_From")));
                String endDate = parseDate(exp.get("Work Duration_To"));
                if (!isEmpty(endDate)) e.put("ended_at", endDate);
                putIfNotEmpty(e, "description", cleanValue(exp.get("Summary")));
                if (!e.isEmpty()) experiences.add(cleanMap(e));
            }
        }

        for (Map<String, String> exp : experiencesIIList) {
            if (candidateId.equals(exp.get("Candidate Id"))) {
                Map<String, Object> e = new LinkedHashMap<>();
                String position = cleanValue(exp.get("Other Occupation"));
                if (isEmpty(position)) position = cleanValue(exp.get("Occupation"));
                putIfNotEmpty(e, "position_name", position);
                putIfNotEmpty(e, "employer", cleanValue(exp.get("Company")));
                putIfNotEmpty(e, "started_at", parseDate(exp.get("Work Duration_From")));
                String endDate = parseDate(exp.get("Work Duration_To"));
                if (!isEmpty(endDate)) e.put("ended_at", endDate);
                putIfNotEmpty(e, "description", cleanValue(exp.get("Summary")));
                if (!e.isEmpty()) experiences.add(cleanMap(e));
            }
        }

        return experiences;
    }

    private List<Map<String, Object>> buildEducation(String candidateId) {
        List<Map<String, Object>> education = new ArrayList<>();

        for (Map<String, String> edu : educationList) {
            if (candidateId.equals(edu.get("Candidate Id"))) {
                Map<String, Object> e = new LinkedHashMap<>();
                putIfNotEmpty(e, "school", cleanValue(edu.get("Institute / School")));
                String degree = cleanValue(edu.get("Degree"));
                if (isEmpty(degree)) degree = cleanValue(edu.get("Academic Degree"));
                putIfNotEmpty(e, "degree", degree);
                putIfNotEmpty(e, "started_at", parseDate(edu.get("Duration_From")));
                String endDate = parseDate(edu.get("Duration_To"));
                if (!isEmpty(endDate)) e.put("ended_at", endDate);
                putIfNotEmpty(e, "specialization", cleanValue(edu.get("Major / Department")));
                putIfNotEmpty(e, "description", cleanValue(edu.get("Details")));
                if (!e.isEmpty()) education.add(cleanMap(e));
            }
        }

        return education;
    }

    private List<Map<String, Object>> buildNotes(String candidateId) {
        List<Map<String, Object>> notes = new ArrayList<>();

        for (Map<String, String> note : notesList) {
            if (candidateId.equals(note.get("Candidate Id"))) {
                Map<String, Object> n = new LinkedHashMap<>();
                putIfNotEmpty(n, "content", cleanValue(note.get("Note Content")));
                putIfNotEmpty(n, "creator", cleanValue(note.get("Created By")));
                String createdTime = cleanValue(note.get("Created Time"));
                if (!isEmpty(createdTime)) {
                    String[] parts = createdTime.split(" ");
                    String createdDate = parseDate(parts[0]);
                    if (!isEmpty(createdDate)) n.put("created_at", createdDate);
                }
                if (!n.isEmpty()) notes.add(cleanMap(n));
            }
        }

        return notes;
    }

    private List<String> extractSkills(Map<String, String> row) {
        Set<String> skillsSet = new LinkedHashSet<>();
        String skillSet = row.get("Skill Set");
        if (!isEmpty(skillSet)) skillsSet.addAll(splitByComma(skillSet));
        String stacks = row.get("Stacks Linkedin");
        if (!isEmpty(stacks)) skillsSet.addAll(splitByComma(stacks));
        return skillsSet.stream().filter(s -> s.length() > 2).limit(20).collect(Collectors.toList());
    }

    private List<String> extractLanguages(String text) {
        if (isEmpty(text)) return new ArrayList<>();
        String[] commonLanguages = {"English",
                "Portuguese",
                "Spanish",
                "French",
                "Italian",
                "Dutch",
                "Russian",
                "Arabic",
                "Hindi",
                "German",
                "Polish",
                "Swedish",
                "Danish",
                "Norwegian",
                "Finnish",
                "Greek",
                "Hungarian",
                "Czech",
                "Romanian",
                "Bulgarian",
                "Serbian",
                "Croatian",
                "Slovak",
                "Slovenian",
                "Lithuanian",
                "Estonian",
                "Chinese",
                "Japanese",
                "Korean",
                "Bengali",
                "Vietnamese",
                "Thai",
                "Malay",
                "Indonesian",
                "Turkish",
                "Kurdish",
                "Swahili",
                "Somali",
                "Ukrainian"
        };
        List<String> found = new ArrayList<>();
        for (String lang : commonLanguages) if (text.contains(lang)) found.add(lang);
        return found;
    }


    private List<String> splitByComma(String text) {
        if (isEmpty(text)) return new ArrayList<>();
        return Arrays.stream(text.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
    }

    private String parseDate(String dateStr) {
        if (isEmpty(dateStr)) return null;
        dateStr = dateStr.trim();

        Pattern monthYearPattern = Pattern.compile("^([A-Za-z]{3})-(\\d{4})$");
        Matcher matcher = monthYearPattern.matcher(dateStr);
        if (matcher.matches()) {
            Map<String, String> monthMap = Map.ofEntries(
                    Map.entry("Jan", "01"), Map.entry("Feb", "02"), Map.entry("Mar", "03"),
                    Map.entry("Apr", "04"), Map.entry("May", "05"), Map.entry("Jun", "06"),
                    Map.entry("Jul", "07"), Map.entry("Aug", "08"), Map.entry("Sep", "09"),
                    Map.entry("Oct", "10"), Map.entry("Nov", "11"), Map.entry("Dec", "12")
            );
            return matcher.group(2) + "-" + monthMap.getOrDefault(matcher.group(1), "01") + "-01";
        }

        String[] formats = {"dd-MM-yyyy", "yyyy-MM-dd", "MM/dd/yyyy", "dd/MM/yyyy"};
        for (String fmt : formats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(fmt);
                sdf.setLenient(false);
                Date date = sdf.parse(dateStr);
                return new SimpleDateFormat("yyyy-MM-dd").format(date);
            } catch (ParseException ignored) {}
        }

        return null;
    }

    private Integer parseSalary(String salaryStr) {
        if (isEmpty(salaryStr)) return null;
        try {
            String cleaned = salaryStr.replaceAll("[^\\d.]", "").replace(",", "");
            if (cleaned.isEmpty()) return null;
            return (int) Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int parseYearsOfExperience(String yearsStr) {
        if (isEmpty(yearsStr)) return 0;
        try { return (int) Double.parseDouble(yearsStr); } catch (NumberFormatException e) { return 0; }
    }

    private String cleanValue(String value) {
        if (value == null || value.trim().isEmpty() || value.equalsIgnoreCase("nan") || value.equalsIgnoreCase("null"))
            return null;
        return value.trim();
    }

    private boolean isEmpty(String str) { return str == null || str.trim().isEmpty(); }

    private void putIfNotEmpty(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            if (value instanceof String && !isEmpty((String) value)) map.put(key, value);
            else if (!(value instanceof String)) map.put(key, value);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T cleanMap(T obj) {
        if (obj instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) obj;
            Map<String, Object> cleaned = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                Object value = cleanMap(entry.getValue());
                if (value != null) {
                    if (value instanceof String && !isEmpty((String) value)) cleaned.put(entry.getKey(), value);
                    else if (value instanceof List && !((List<?>) value).isEmpty()) cleaned.put(entry.getKey(), value);
                    else if (value instanceof Map && !((Map<?, ?>) value).isEmpty()) cleaned.put(entry.getKey(), value);
                    else if (!(value instanceof String) && !(value instanceof List) && !(value instanceof Map))
                        cleaned.put(entry.getKey(), value);
                }
            }
            return (T) cleaned;
        } else if (obj instanceof List) {
            List<Object> list = (List<Object>) obj;
            List<Object> cleaned = new ArrayList<>();
            for (Object item : list) {
                Object cleanedItem = cleanMap(item);
                if (cleanedItem != null) cleaned.add(cleanedItem);
            }
            return (T) cleaned;
        }
        return obj;
    }

    public void saveJSON(List<Map<String, Object>> data) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

        Path outputDir = Paths.get(outputPath); // outputPath vem do AppConfig
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir); // cria a pasta se não existir
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