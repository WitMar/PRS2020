package prs.project.task;

import java.io.Serializable;
import java.util.EnumMap;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import prs.project.model.Product;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Akcja implements Serializable {

    Long id;
    @JsonSerialize(using = EnumSerializer.class)
    @JsonDeserialize(using = EnumDeserializer.class)
    Enum typ;
    Product product;
    Long liczba;
    EnumMap<Product, Long> grupaProduktów;
    Long cena;




}
