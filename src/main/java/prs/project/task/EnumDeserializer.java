package prs.project.task;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
public class EnumDeserializer extends JsonDeserializer<Enum> {

    /**
     *Gender code when parsing JSON(Integer value)To a Human object.
     */
    @Override
    public Enum deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        Map<String, Enum> mapStringToEnum = new HashMap<>();
        Arrays.stream(WycenaAkcje.values()).forEach(val -> mapStringToEnum.put(val.toString(), WycenaAkcje.valueOf(val.toString())));
        Arrays.stream(WydarzeniaAkcje.values()).forEach(val -> mapStringToEnum.put(val.toString(), WydarzeniaAkcje.valueOf(val.toString())));
        Arrays.stream(ZaopatrzenieAkcje.values()).forEach(val -> mapStringToEnum.put(val.toString(), ZaopatrzenieAkcje.valueOf(val.toString())));
        Arrays.stream(ZamowieniaAkcje.values()).forEach(val -> mapStringToEnum.put(val.toString(), ZamowieniaAkcje.valueOf(val.toString())));
        Arrays.stream(SterowanieAkcja.values()).forEach(val -> mapStringToEnum.put(val.toString(), SterowanieAkcja.valueOf(val.toString())));

        return mapStringToEnum.get(parser.getValueAsString());
    }
}
