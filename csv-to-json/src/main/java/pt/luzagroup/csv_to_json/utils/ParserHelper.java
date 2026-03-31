package pt.luzagroup.csv_to_json.utils;

import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



@Component
public class ParserHelper {

    public String parseDate(String dateStr) {
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

    public Integer parseSalary(String salaryStr) {
        if (isEmpty(salaryStr)) return null;
        try {
            String cleaned = salaryStr.replaceAll("[^\\d.]", "").replace(",", "");
            if (cleaned.isEmpty()) return null;
            return (int) Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public int parseYearsOfExperience(String yearsStr) {
        if (isEmpty(yearsStr)) return 0;
        try { return (int) Double.parseDouble(yearsStr); } catch (NumberFormatException e) { return 0; }
    }
    public Boolean parseBooleanField(String value) {
        if (value == null) return null;

        value = value.trim().toLowerCase();

        if (value.isEmpty()) return null;

        // 🔥 limpeza básica
        value = value.replaceAll("[^a-z0-9]", "");

        // positivos
        if (value.matches("^(yes|y|true|1|available)$")) return true;

        // negativos
        if (value.matches("^(no|n|false|0|notavailable)$")) return false;

        // aqui pode remover completamente o tratamento "yesandno"
        // if (value.contains("yes") && value.contains("no")) return null;

        if (value.contains("yes") || value.contains("true")) return true;
        if (value.contains("no") || value.contains("false")) return false;

        return null;
    }



    public String cleanValue(String value) {
        if (value == null || value.trim().isEmpty() || value.equalsIgnoreCase("nan") || value.equalsIgnoreCase("null"))
            return null;
        return value.trim();
    }

    public boolean isEmpty(String str) { return str == null || str.trim().isEmpty(); }


    @SuppressWarnings("unchecked")
    public <T> T cleanMap(T obj) {
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
}
