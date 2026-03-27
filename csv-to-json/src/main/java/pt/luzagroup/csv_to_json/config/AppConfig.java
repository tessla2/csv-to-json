package pt.luzagroup.csv_to_json.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Classe de configuração que mapeia as propriedades do application.yml
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {

    /**
     * Caminho para a pasta com arquivos CSV de entrada
     */
    private String inputPath;

    /**
     * Caminho para a pasta de saída
     */
    private String outputPath;

    /**
     * Nome do arquivo JSON de saída
     */
    private String outputFilename;

    /**
     * Mapa com nomes dos arquivos CSV
     */
    private Map<String, String> csvFiles;

    /**
     * ID inicial para os candidatos
     */
    private Integer startingId = 10001;

    /**
     * Número máximo de skills por candidato
     */
    private Integer maxSkills = 20;

    /**
     * Encoding dos arquivos
     */
    private String fileEncoding = "UTF-8";
}