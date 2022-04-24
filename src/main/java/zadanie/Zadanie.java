package zadanie;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class Zadanie {
    public Items item;

    int sleep;
}
