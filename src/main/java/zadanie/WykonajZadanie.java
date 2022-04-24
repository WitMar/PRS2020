package zadanie;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.IntStream;

public class WykonajZadanie {

    public static void main(String[] args) {
        BlockingQueue<Zadanie> kolejka = new LinkedBlockingQueue<>();

        IntStream.rangeClosed(1,3).forEach(it -> {
            kolejka.add(Zadanie.builder()
                    .item(Items.SZKLANKA)
                    .sleep(1000)
                    .build());
        });

        ShopZadanie zadanko = new ShopZadanie(kolejka);
        zadanko.uruchom();
    }
}
