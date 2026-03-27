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

        // --- campos simples ---
        putIfNotEmpty(candidate, "name", clean(row, "Full Name"));
        putIfNotEmpty(candidate, "description", clean(row, "Candidate Description/Summary"));
        putIfNotEmpty(candidate, "creator", clean(row, "Created By"));
        putIfNotEmpty(candidate, "owner", clean(row, "Candidate Owner"));

        putIfNotEmpty(candidate, "job", firstNonEmpty(row, "Current/Last Job Title", "Current Job Title"));
        putIfNotEmpty(candidate, "company", clean(row, "Current Employer"));
        candidate.put("yearofexperience", helper.parseYearsOfExperience(row.get("Experience in Years")));

        putIfNotEmpty(candidate, "birthdate", helper.parseDate(row.get("Date of Birth")));
        putIfNotEmpty(candidate, "address", clean(row, "Street"));

        putIfNotEmpty(candidate, "country", firstNonEmpty(row, "Candidate Country", "Other Country"));
        putIfNotEmpty(candidate, "city", firstNonEmpty(row, "Candidate - City", "City"));

        putIfNotEmpty(candidate, "email", clean(row, "Email"));
        putIfNotEmpty(candidate, "phonenumber", firstNonEmpty(row, "Phone", "Mobile"));
        putIfNotEmpty(candidate, "skype", clean(row, "Skype ID"));

        putIfNotEmpty(candidate, "ccurrency", row.getOrDefault("Currency", "USD"));
        putIfNotEmpty(candidate, "ecurrency", row.getOrDefault("Currency", "USD"));
        putIfNotEmpty(candidate, "worktype", "permanent");

        Integer currentSalary = helper.parseSalary(row.get("Current Salary"));
        if (currentSalary != null) candidate.put("csalary", currentSalary);
        Integer expectedSalary = helper.parseSalary(row.get("Expected Salary"));
        if (expectedSalary != null) candidate.put("esalary", expectedSalary);

        String linkedin = clean(row, "LinkedIn");
        if (!isEmpty(linkedin) && !linkedin.startsWith("http")) linkedin = "https://" + linkedin;
        putIfNotEmpty(candidate, "linkedin", linkedin);

        // --- listas ---
        candidate.put("languages", extractLanguages(row.get("Languages")));
        candidate.put("skills", extractSkills(row));

        candidate.put("experiences", buildExperiences(candidateId, experiencesList, experiencesIIList));
        candidate.put("education", buildEducation(candidateId, educationList));
        candidate.put("note", buildNotes(candidateId, notesList));

        return candidate;
    }

    // =======================
    // Funções auxiliares internas
    // =======================

    private String clean(Map<String, String> row, String key) {
        return helper.cleanValue(row.get(key));
    }

    private String firstNonEmpty(Map<String, String> row, String... keys) {
        for (String key : keys) {
            String val = helper.cleanValue(row.get(key));
            if (!helper.isEmpty(val)) return val;
        }
        return null;
    }

    private boolean isEmpty(String value) {
        return helper.isEmpty(value);
    }

    private List<String> splitByComma(String text) {
        if (helper.isEmpty(text)) return new ArrayList<>();
        return Arrays.stream(text.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private List<String> extractSkills(Map<String, String> row) {
        Set<String> skillsSet = new LinkedHashSet<>();
        if (!isEmpty(row.get("Skill Set"))) skillsSet.addAll(splitByComma(row.get("Skill Set")));
        if (!isEmpty(row.get("Stacks Linkedin"))) skillsSet.addAll(splitByComma(row.get("Stacks Linkedin")));
        return skillsSet.stream().filter(s -> s.length() > 2).limit(20).collect(Collectors.toList());
    }

    private List<String> extractLanguages(String text) {
        if (isEmpty(text)) return new ArrayList<>();
        String[] commonLanguages = {"English", "Portuguese", "Spanish", "French", "Italian", "Dutch",
                "Russian", "Arabic", "Hindi", "German", "Polish", "Swedish", "Danish", "Norwegian",
                "Finnish", "Greek", "Hungarian", "Czech", "Romanian", "Bulgarian", "Serbian", "Croatian",
                "Slovak", "Slovenian", "Lithuanian", "Estonian", "Chinese", "Japanese", "Korean",
                "Bengali", "Vietnamese", "Thai", "Malay", "Indonesian", "Turkish", "Kurdish", "Swahili",
                "Somali", "Ukrainian"};
        List<String> found = new ArrayList<>();
        for (String lang : commonLanguages) if (text.contains(lang)) found.add(lang);
        return found;
    }


    public void putIfNotEmpty(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            if (value instanceof String && !isEmpty((String) value)) map.put(key, value);
            else if (!(value instanceof String)) map.put(key, value);
        }
    }

    // =======================
    // Builders de listas
    // =======================

    private List<Map<String, Object>> buildExperiences(String candidateId,
                                                       List<Map<String, String>> experiencesList,
                                                       List<Map<String, String>> experiencesIIList) {
        List<Map<String, Object>> experiences = new ArrayList<>();
        experiences.addAll(experienceMapper.build(candidateId, experiencesList));
        experiences.addAll(experienceMapper.build(candidateId, experiencesIIList));
        return experiences;
    }

    private List<Map<String, Object>> buildEducation(String candidateId, List<Map<String, String>> educationList) {
        return experienceMapper.buildEducation(candidateId, educationList);
    }

    private List<Map<String, Object>> buildNotes(String candidateId, List<Map<String, String>> notesList) {
        return experienceMapper.buildNotes(candidateId, notesList);
    }

}