package pt.luzagroup.csv_to_json.runner;


import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import pt.luzagroup.csv_to_json.service.CandidateService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Runner que executa a conversão automaticamente ao iniciar a aplicação
 */
@Slf4j
@Component

public class CsvConverterRunner implements CommandLineRunner {


    private final CandidateService service;
    private final pt.luzagroup.csv_to_json.config.AppConfig config;

    public CsvConverterRunner(CandidateService service, pt.luzagroup.csv_to_json.config.AppConfig config) {
        this.service = service;
        this.config = config;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("=================================================");
        log.info("Iniciando conversão de CSV para JSON");
        log.info("=================================================");

        // Validar que a pasta de entrada existe
        Path inputPath = Paths.get(config.getInputPath());
        if (!Files.exists(inputPath)) {
            log.error("❌ Pasta de entrada não encontrada: {}", inputPath);
            log.error("Por favor, verifique o caminho no application.yml");
            return;
        }

        // Criar pasta de saída se não existir
        Path outputPath = Paths.get(config.getOutputPath());
        if (!Files.exists(outputPath)) {
            Files.createDirectories(outputPath);
            log.info("✅ Pasta de saída criada: {}", outputPath);
        }

        try {
            service.runConversion();

            log.info("✅ Conversão concluída com sucesso!");

        } catch (Exception e) {
            log.error("❌ Erro durante a conversão: {}", e.getMessage(), e);
            throw e;
        }
    }
}