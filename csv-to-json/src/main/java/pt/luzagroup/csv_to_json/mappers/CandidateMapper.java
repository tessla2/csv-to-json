package pt.luzagroup.csv_to_json.mappers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pt.luzagroup.csv_to_json.utils.ParserHelper;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CandidateMapper {

    private final ParserHelper helper;
    private final ExperienceMapper experienceMapper;

    public Map<String, Object> mapCandidate(String candidateId,
                                            Map<String, String> row,
                                            int id,
                                            List<Map<String, String>> experiencesList,
                                            List<Map<String, String>> experiencesIIList,
                                            List<Map<String, String>> educationList,
                                            List<Map<String, String>> notesList) {

        Map<String, Object> candidate = new LinkedHashMap<>();
        candidate.put("id", id);

        Map<String, String> normalized = normalizeRow(row);


        String candidateIdUnique = normalized.get("candidate_id"); // "Candidate ID" vira "candidate_id" após normalização
        if (!isEmpty(candidateIdUnique)) {
            candidate.put("id", candidateIdUnique);          // id principal do candidato
            candidate.put("candidate_ID", candidateIdUnique); // mantém campo separado se precisar
        } else {
            throw new IllegalArgumentException("Candidate ID não pode ser vazio!");
        }

        // --- Campos principais ---
        put(candidate, "name", normalized.get("full_name"));
        put(candidate, "description", firstNonEmpty(normalized,
                "candidate_description_summary", "description"));

        put(candidate, "creator", normalized.get("created_by"));
        put(candidate, "owner", normalized.get("candidate_owner"));

        put(candidate, "company", normalized.get("current_employer"));
        put(candidate, "yearofexperience",
                helper.parseYearsOfExperience(normalized.get("experience_in_years")));

        put(candidate, "country", firstNonEmpty(normalized,
                "candidate_country", "other_country"));

        put(candidate, "city", firstNonEmpty(normalized,
                "candidate_city", "city"));

        // 🔥 Campos adicionais
        put(candidate, "availability", normalized.get("availability"));
        put(candidate, "nationalities", normalized.get("citizenship"));
        put(candidate, "family_situation", normalized.get("family_situation"));

        // --- Boolean fields corrigidos (true / false / null) ---
        candidate.put("workvisaeucitizenship",
                helper.parseBooleanField(firstNonEmpty(normalized,
                        "workvisaeucitizenship",
                        "work_visa_eu_citizenship")));

        candidate.put("canrelocate",
                helper.parseBooleanField(firstNonEmpty(normalized,
                        "canrelocate",
                        "can_relocate")));

        candidate.put("gdpragreement",
                helper.parseBooleanField(firstNonEmpty(normalized,
                        "gdpragreement",
                        "gdpr_agreement")));

        // --- Contato ---
        put(candidate, "email", normalized.get("email"));
        put(candidate, "phonenumber", firstNonEmpty(normalized,
                "phone", "mobile"));

        // --- Salário ---
        Integer currentSalary = helper.parseSalary(normalized.get("current_salary"));
        if (currentSalary != null) candidate.put("csalary", currentSalary);

        Integer expectedSalary = helper.parseSalary(normalized.get("expected_salary"));
        if (expectedSalary != null) candidate.put("esalary", expectedSalary);

        put(candidate, "ccurrency", normalized.getOrDefault("currency", "EUR"));
        put(candidate, "ecurrency", normalized.getOrDefault("currency", "EUR"));
        put(candidate, "worktype", "permanent");

        // --- LinkedIn ---
        String linkedin = normalized.get("linkedin");
        if (!isEmpty(linkedin) && !linkedin.startsWith("http")) {
            linkedin = "https://" + linkedin;
        }
        put(candidate, "linkedin", linkedin);

        // --- Listas ---
        candidate.put("languages", extractLanguages(normalized.get("languages")));
        candidate.put("skills", extractSkills(normalized));

        candidate.put("experiences", buildExperiences(candidateId, experiencesList, experiencesIIList));
        candidate.put("education", buildEducation(candidateId, educationList));
        candidate.put("note", buildNotes(candidateId, notesList));

        // 🔥 Fallback seguro: adiciona campos não tratados
        normalized.forEach((key, value) -> {
            if (!candidate.containsKey(key)
                    && !"stacks_linkedin".equals(key)
                    && !"workvisaeucitizenship".equals(key)
                    && !"work_visa_eu_citizenship".equals(key)
                    && !"canrelocate".equals(key)
                    && !"can_relocate".equals(key)
                    && !"gdpragreement".equals(key)
                    && !"gdpr_agreement".equals(key)) {
                put(candidate, key, value);
            }
        });

        return candidate;
    }

    // =======================
    // Normalização
    // =======================
    private Map<String, String> normalizeRow(Map<String, String> row) {
        Map<String, String> normalized = new HashMap<>();
        row.forEach((key, value) -> {
            if (key == null) return;

            String normalizedKey = key
                    .toLowerCase()
                    .trim()
                    .replaceAll("[^a-z0-9]+", "_");

            normalized.put(normalizedKey, helper.cleanValue(value));
        });
        return normalized;
    }

    // =======================
    // Helpers
    // =======================
    private void put(Map<String, Object> map, String key, Object value) {
        if (value == null) return;
        if (value instanceof String && isEmpty((String) value)) return;
        map.put(key, value);
    }

    private String firstNonEmpty(Map<String, String> map, String... keys) {
        for (String key : keys) {
            String val = map.get(key);
            if (!isEmpty(val)) return val;
        }
        return null;
    }

    private boolean isEmpty(String value) {
        return helper.isEmpty(value);
    }

    private List<String> splitByComma(String text) {
        if (isEmpty(text)) return new ArrayList<>();
        return Arrays.stream(text.split(","))
                .map(String::trim)
                .map(s -> s.replaceAll("\n", ""))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private List<String> extractSkills(Map<String, String> row) {
        if (isEmpty(row.get("stacks_linkedin"))) return new ArrayList<>();

        Set<String> skills = new LinkedHashSet<>(splitByComma(row.get("stacks_linkedin")));
        return skills.stream().limit(20).collect(Collectors.toList());
    }

    private List<String> extractLanguages(String text) {
        if (isEmpty(text)) return new ArrayList<>();

        String[] langs = {"English", "Portuguese", "Spanish", "French", "German"};
        List<String> found = new ArrayList<>();

        for (String l : langs) {
            if (text.contains(l)) found.add(l);
        }

        return found;
    }

    // =======================
    // Builders
    // =======================
    private List<Map<String, Object>> buildExperiences(String candidateId,
                                                       List<Map<String, String>> exp1,
                                                       List<Map<String, String>> exp2) {
        List<Map<String, Object>> list = new ArrayList<>();
        list.addAll(experienceMapper.build(candidateId, exp1));
        list.addAll(experienceMapper.build(candidateId, exp2));
        return list;
    }

    private List<Map<String, Object>> buildEducation(String candidateId,
                                                     List<Map<String, String>> educationList) {
        return experienceMapper.buildEducation(candidateId, educationList);
    }

    private List<Map<String, Object>> buildNotes(String candidateId,
                                                 List<Map<String, String>> notesList) {
        return experienceMapper.buildNotes(candidateId, notesList);
    }
}