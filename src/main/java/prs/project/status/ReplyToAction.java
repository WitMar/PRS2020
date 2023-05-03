package prs.project.status;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.EnumMap;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import prs.project.model.Product;
import prs.project.task.EnumDeserializer;
import prs.project.task.EnumSerializer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class ReplyToAction implements Serializable {

    @JsonSerialize(using = EnumSerializer.class)
    @JsonDeserialize(using = EnumDeserializer.class)
    Enum typ;

    Long id;
    Product product;
    Long liczba;
    Long cena;
    EnumMap<Product, Long> grupaProduktów;
    EnumMap<Product, Long> stanMagazynów;
    EnumMap<Product, Long> raportSprzedaży;
    Boolean cenaZmieniona;
    Boolean zrealizowanePrzywrócenie;
    Boolean zrealizowaneZamowienie;
    Boolean zrealizowaneWycofanie;
    Boolean zebraneZaopatrzenie;
    Long studentId;
    Long timestamp;
}
