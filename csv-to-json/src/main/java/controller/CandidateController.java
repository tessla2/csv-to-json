package controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import service.CandidateService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

@RestController
public class CandidateController {

    private final CandidateService candidateService;

    public CandidateController(CandidateService candidateService) {
        this.candidateService = candidateService;
    }

    //Json de candidatos
    @GetMapping("/candidates/json")
    public Map<String, Object> getCandidatesJson() {
        Map<String, Object> response = new HashMap<>();
        try {
            // O CandidateService já gera o arquivo no construtor
            File jsonFile = new File("/mnt/user-data/outputs/candidates.json");
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