package pt.luzagroup.csv_to_json.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.luzagroup.csv_to_json.service.CandidateService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class CandidateController {

    private final CandidateService candidateService;


    //Json de candidatos
    @GetMapping("/candidates/json")
    public Map<String, Object> getCandidatesJson() {
        Map<String, Object> response = new HashMap<>();
        try {
            // O CandidateService já gera o arquivo no construtor
            File jsonFile = new File("/src/resources/outputs/candidates.json");
            if (!jsonFile.exists()) {
                response.put("status", "error");
                response.put("message", "Arquivo JSON não encontrado. Execute o serviço primeiro.");
                return response;
            }

            String jsonContent = new String(Files.readAllBytes(jsonFile.toPath()));
            response.put("status", "success");
            response.put("data", jsonContent);

        } catch (IOException e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }

        return response;
    }
}