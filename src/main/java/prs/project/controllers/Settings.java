package prs.project.controllers;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "ustawienia")
@Component
@Getter
@Setter
public class Settings {

    private String wycena;
    private String zamowienia;
    private String zaopatrzenie;
    private String wydarzenia;
    private Long numerIndeksu;
    private Long liczbaZadan;
}
