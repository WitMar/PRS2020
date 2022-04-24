package zadanie;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.assertj.core.api.Assertions.assertThat;

public class ShopZadanie {

    ConcurrentHashMap<Items, Integer> warehouse = new ConcurrentHashMap();
    AtomicLong sold = new AtomicLong(0L);
    BlockingQueue<Zadanie> kolejka;

    public ShopZadanie(BlockingQueue<Zadanie> kolejka) {
        this.kolejka = kolejka;
        warehouse.put(Items.SZKLANKA, 2);
    }

    public void wykonaj(Zadanie zadanie) {
        if(warehouse.containsKey(zadanie.item) &&
                warehouse.get(zadanie.item) > 0) {
            try {
                Thread.sleep(zadanie.sleep);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            sold.incrementAndGet();
        }
    }

    public void uruchom() {
        Lock zamek = new ReentrantLock();

        // Wszystkie wątki działają w taki sam sposób
        Thread thread1 = new Thread(() -> {
            zamek.lock();
            while(kolejka.size() > 0) {
              Zadanie zadanie = kolejka.poll();
              zamek.unlock();
              wykonaj(zadanie);
              zamek.lock();
            }
            zamek.unlock();
        });

        Thread thread2 = new Thread(() -> {
            zamek.lock();
            while(kolejka.size() > 0) {
                Zadanie zadanie = kolejka.poll();
                zamek.unlock();
                wykonaj(zadanie);
                zamek.lock();
            }
            zamek.unlock();
        });

        Thread thread3 = new Thread(() -> {
            zamek.lock();
            while(kolejka.size() > 0) {
                Zadanie zadanie = kolejka.poll();
                zamek.unlock();
                wykonaj(zadanie);
                zamek.lock();
            }
            zamek.unlock();
        });


        thread1.start();
        thread2.start();
        thread3.start();

        try {
            thread1.join();
            thread2.join();
            thread3.join();
            // Sprawdzamy czy nie sprzedano więcej niż 2 sztuk
            assertThat(sold.get()).isLessThanOrEqualTo(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
