package pt.luzagroup.csv_to_json.mappers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pt.luzagroup.csv_to_json.utils.ParserHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ExperienceMapper {

    private final ParserHelper helper;

    public Map<String, Object> mapExperience(Map<String, String> exp, boolean isSecondList) {
        Map<String, Object> e = new LinkedHashMap<>();

        String position = firstNonEmpty(
                isSecondList ? exp.get("Other Occupation") : exp.get("Occupation / Title"),
                exp.get("Occupation")
        );

        putIfNotEmpty(e, "position_name", position);
        putIfNotEmpty(e, "employer", clean(exp.get("Company")));
        putIfNotEmpty(e, "started_at", helper.parseDate(exp.get("Work Duration_From")));

        String endDate = helper.parseDate(exp.get("Work Duration_To"));
        if (!isEmpty(endDate)) e.put("ended_at", endDate);

        putIfNotEmpty(e, "description", clean(exp.get("Summary")));

        return cleanMap(e);
    }


    public List<Map<String, Object>> build(String candidateId, List<Map<String, String>> experiencesList) {
        List<Map<String, Object>> experiences = new ArrayList<>();
        for (Map<String, String> exp : experiencesList) {
            if (candidateId.equals(exp.get("Candidate Id"))) {
                Map<String, Object> e = new LinkedHashMap<>();
                String position = helper.cleanValue(exp.get("Occupation / Title"));
                if (helper.isEmpty(position)) position = helper.cleanValue(exp.get("Occupation"));

                putIfNotEmpty(e, "position_name", position);
                putIfNotEmpty(e, "employer", helper.cleanValue(exp.get("Company")));
                putIfNotEmpty(e, "started_at", helper.parseDate(exp.get("Work Duration_From")));
                String endDate = helper.parseDate(exp.get("Work Duration_To"));
                if (!helper.isEmpty(endDate)) e.put("ended_at", endDate);
                putIfNotEmpty(e, "description", helper.cleanValue(exp.get("Summary")));

                experiences.add(helper.cleanMap(e));
            }
        }
        return experiences;
    }

    public List<Map<String, Object>> buildNotes(String candidateId, List<Map<String, String>> notesList) {
        List<Map<String, Object>> notes = new ArrayList<>();
        for (Map<String, String> note : notesList) {
            if (candidateId.equals(note.get("Candidate Id"))) {
                Map<String, Object> n = new LinkedHashMap<>();
                putIfNotEmpty(n, "content", helper.cleanValue(note.get("Note Content")));
                putIfNotEmpty(n, "creator", helper.cleanValue(note.get("Created By")));
                String createdTime = helper.cleanValue(note.get("Created Time"));
                if (!helper.isEmpty(createdTime)) {
                    String[] parts = createdTime.split(" ");
                    String createdDate = helper.parseDate(parts[0]);
                    if (!helper.isEmpty(createdDate)) n.put("created_at", createdDate);
                }
                if (!n.isEmpty()) notes.add(helper.cleanMap(n));
            }
        }
        return notes;
    }

    public List<Map<String, Object>> buildEducation(String candidateId, List<Map<String, String>> educationList) {
        List<Map<String, Object>> education = new ArrayList<>();

        for (Map<String, String> edu : educationList) {
            if (candidateId.equals(edu.get("Candidate Id"))) {
                Map<String, Object> e = new LinkedHashMap<>();
                putIfNotEmpty(e, "school", helper.cleanValue(edu.get("Institute / School")));
                String degree = helper.cleanValue(edu.get("Degree"));
                if (helper.isEmpty(degree)) degree = helper.cleanValue(edu.get("Academic Degree"));
                putIfNotEmpty(e, "degree", degree);
                putIfNotEmpty(e, "started_at", helper.parseDate(edu.get("Duration_From")));
                String endDate = helper.parseDate(edu.get("Duration_To"));
                if (!helper.isEmpty(endDate)) e.put("ended_at", endDate);
                putIfNotEmpty(e, "specialization", helper.cleanValue(edu.get("Major / Department")));
                putIfNotEmpty(e, "description", helper.cleanValue(edu.get("Details")));
                if (!e.isEmpty()) education.add(helper.cleanMap(e));
            }
        }

        return education;
    }

    // =======================
    // Funções auxiliares internas
    // =======================

    private String clean(String value) {
        return helper.cleanValue(value);
    }

    private boolean isEmpty(String value) {
        return helper.isEmpty(value);
    }

    private String firstNonEmpty(String... values) {
        for (String val : values) {
            val = clean(val);
            if (!isEmpty(val)) return val;
        }
        return null;
    }

    private void putIfNotEmpty(Map<String, Object> map, String key, Object value) {
        if (value instanceof String && !isEmpty((String) value)) map.put(key, value);
        else if (value != null) map.put(key, value);
    }

    private Map<String, Object> cleanMap(Map<String, Object> map) {
        return helper.cleanMap(map);
    }


}