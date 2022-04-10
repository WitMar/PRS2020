package prs.project.generator;

import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import prs.project.checker.Ledger;
import prs.project.controllers.Settings;
import prs.project.model.Product;
import prs.project.model.Warehouse;
import prs.project.status.ReplyToAction;
import prs.project.task.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Stream;

@Slf4j
public class SequenceRunner {

    Settings settings;
    List<Akcja> akcje = new ArrayList<>();
    boolean active = true;
    Set<Enum> mojeTypy = new HashSet<>();
    ConcurrentLinkedDeque<Akcja> kolejka = new ConcurrentLinkedDeque();
    Warehouse magazyn = new Warehouse();
    EnumMap<Product, Long> sprzedaz = new EnumMap(Product.class);
    EnumMap<Product, Long> rezerwacje = new EnumMap(Product.class);
    Long promoLicznik = 0L;
    Ledger ledger;
    Thread thread;

    public SequenceRunner(Settings settings, List<Akcja> akcje, Ledger ledger) {
        this.ledger = ledger;
        this.settings = settings;
        this.akcje = akcje;
        Arrays.stream(Product.values()).forEach(p -> sprzedaz.put(p, 0L));
        Arrays.stream(Product.values()).forEach(p -> rezerwacje.put(p, 0L));

        mojeTypy.addAll(Wycena.valueOf(settings.getWycena()).getAkceptowane());
        mojeTypy.addAll(Zamowienia.valueOf(settings.getZamowienia()).getAkceptowane());
        mojeTypy.addAll(Zaopatrzenie.valueOf(settings.getZaopatrzenie()).getAkceptowane());
        mojeTypy.addAll(Wydarzenia.valueOf(settings.getWydarzenia()).getAkceptowane());
        mojeTypy.addAll(Arrays.asList(SterowanieAkcja.values()));
        thread = new Thread(() ->
        {
            while (active) {
                threadProcess();
            }
        });
        thread.start();
    }

    public void process(Akcja jednaAkcja) {
        Stream.of(jednaAkcja)
                .filter(akcja -> mojeTypy.contains(akcja.getTyp()))
                .forEach(akcja -> {
                    kolejka.add(akcja);
                });
    }

    public void threadProcess() {
        Akcja akcja = kolejka.pollFirst();
        if (akcja != null) {
            ReplyToAction odpowiedz = procesujAkcje(akcja);
            try {
                wyslijOdpowiedzLokalnie(odpowiedz);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private ReplyToAction procesujAkcje(Akcja akcja) {
        log.info("Procesuje " + akcja.getTyp());
        ReplyToAction odpowiedz = ReplyToAction.builder()
                .typ(akcja.getTyp())
                .id(akcja.getId())
                .build();

        if (WycenaAkcje.PODAJ_CENE.equals(akcja.getTyp())) {
            odpowiedz.setProdukt(akcja.getProduct());
            odpowiedz.setCena(magazyn.getCeny().get(akcja.getProduct()));
            if (mojeTypy.contains(Wycena.PROMO_CO_10_WYCEN)) {
                promoLicznik++;
                if (promoLicznik == 10)
                    odpowiedz.setCena(0L);
            }
        }
        if (WycenaAkcje.ZMIEN_CENE.equals(akcja.getTyp())) {
            magazyn.getCeny().put(akcja.getProduct(), akcja.getCena());
            odpowiedz.setProdukt(akcja.getProduct());
            odpowiedz.setCenaZmieniona(true);
            odpowiedz.setCena(akcja.getCena());
        }

        if (WydarzeniaAkcje.RAPORT_SPRZEDAŻY.equals(akcja.getTyp())) {
            odpowiedz.setRaportSprzedaży(sprzedaz.clone());
        }
        if (WydarzeniaAkcje.INWENTARYZACJA.equals(akcja.getTyp())) {
            odpowiedz.setStanMagazynów(magazyn.getStanMagazynowy().clone());
        }
        if (WydarzeniaAkcje.WYCOFANIE.equals(akcja.getTyp())) {
            magazyn.getStanMagazynowy().put(akcja.getProduct(), -9999999L);
            odpowiedz.setProdukt(akcja.getProduct());
            odpowiedz.setZrealizowaneWycofanie(true);
        }
        if (WydarzeniaAkcje.PRZYWROCENIE.equals(akcja.getTyp())) {
            magazyn.getStanMagazynowy().put(akcja.getProduct(), 0L);
            odpowiedz.setProdukt(akcja.getProduct());
            odpowiedz.setZrealizowanePrzywrócenie(true);
        }

        if (ZamowieniaAkcje.POJEDYNCZE_ZAMOWIENIE.equals(akcja.getTyp())) {
            odpowiedz.setProdukt(akcja.getProduct());
            odpowiedz.setLiczba(akcja.getLiczba());
            Long naMagazynie = magazyn.getStanMagazynowy().get(akcja.getProduct());
            if (naMagazynie >= akcja.getLiczba()) {
                odpowiedz.setZrealizowaneZamowienie(true);
                magazyn.getStanMagazynowy().put(akcja.getProduct(), naMagazynie - akcja.getLiczba());
                sprzedaz.put(akcja.getProduct(), sprzedaz.get(akcja.getProduct()) + akcja.getLiczba());
            } else {
                odpowiedz.setZrealizowaneZamowienie(false);
            }
        }
        if (ZamowieniaAkcje.GRUPOWE_ZAMOWIENIE.equals(akcja.getTyp())) {
            odpowiedz.setGrupaProduktów(akcja.getGrupaProduktów());
            odpowiedz.setZrealizowaneZamowienie(true);
            akcja.getGrupaProduktów().entrySet().stream()
                    .forEach(produkt -> {
                        Long naMagazynie = magazyn.getStanMagazynowy().get(produkt.getKey());
                        if (naMagazynie < produkt.getValue()) {
                            odpowiedz.setZrealizowaneZamowienie(false);
                        }
                    });
            if (odpowiedz.getZrealizowaneZamowienie()) {
                akcja.getGrupaProduktów().entrySet().stream()
                        .forEach(produkt -> {
                            Long naMagazynie = magazyn.getStanMagazynowy().get(produkt.getKey());
                            magazyn.getStanMagazynowy().put(produkt.getKey(), naMagazynie - produkt.getValue());
                            sprzedaz.put(produkt.getKey(), sprzedaz.get(produkt.getKey()) + produkt.getValue());
                        });
            }
        }
        if (ZamowieniaAkcje.REZERWACJA.equals(akcja.getTyp())) {
            odpowiedz.setProdukt(akcja.getProduct());
            odpowiedz.setLiczba(akcja.getLiczba());
            Long naMagazynie = magazyn.getStanMagazynowy().get(akcja.getProduct());
            if (naMagazynie >= akcja.getLiczba()) {
                odpowiedz.setZrealizowaneZamowienie(true);
                magazyn.getStanMagazynowy().put(akcja.getProduct(), naMagazynie - akcja.getLiczba());
                rezerwacje.put(akcja.getProduct(), rezerwacje.get(akcja.getProduct()) + akcja.getLiczba());
            } else {
                odpowiedz.setZrealizowaneZamowienie(false);
            }
        }
        if (ZamowieniaAkcje.ODBIÓR_REZERWACJI.equals(akcja.getTyp())) {
            odpowiedz.setProdukt(akcja.getProduct());
            odpowiedz.setLiczba(akcja.getLiczba());
            Long naMagazynie = rezerwacje.get(akcja.getProduct());
            if (naMagazynie >= akcja.getLiczba()) {
                odpowiedz.setZrealizowaneZamowienie(true);
                rezerwacje.put(akcja.getProduct(), rezerwacje.get(akcja.getProduct()) - akcja.getLiczba());
            } else {
                odpowiedz.setZrealizowaneZamowienie(false);
            }
        }

        if (ZaopatrzenieAkcje.POJEDYNCZE_ZAOPATRZENIE.equals(akcja.getTyp())) {
            odpowiedz.setProdukt(akcja.getProduct());
            odpowiedz.setLiczba(akcja.getLiczba());
            Long naMagazynie = magazyn.getStanMagazynowy().get(akcja.getProduct());
            odpowiedz.setZebraneZaopatrzenie(true);
            if (magazyn.getStanMagazynowy().get(akcja.getProduct()) >= 0) {
                magazyn.getStanMagazynowy().put(akcja.getProduct(), naMagazynie + akcja.getLiczba());
            }
        }
        if (ZaopatrzenieAkcje.GRUPOWE_ZAOPATRZENIE.equals(akcja.getTyp())) {
            odpowiedz.setGrupaProduktów(akcja.getGrupaProduktów());
            odpowiedz.setZebraneZaopatrzenie(true);
            akcja.getGrupaProduktów().entrySet().stream()
                    .forEach(produkt -> {
                        Long naMagazynie = magazyn.getStanMagazynowy().get(produkt.getKey());
                        if (magazyn.getStanMagazynowy().get(produkt.getKey()) >= 0) {
                            magazyn.getStanMagazynowy().put(produkt.getKey(), naMagazynie + produkt.getValue());
                        }
                    });
        }

        if (SterowanieAkcja.ZAMKNIJ_SKLEP.equals(akcja.getTyp())) {
            odpowiedz.setStanMagazynów(magazyn.getStanMagazynowy());
            odpowiedz.setGrupaProduktów(magazyn.getCeny());
            magazyn = new Warehouse();
            Arrays.stream(Product.values()).forEach(p -> rezerwacje.put(p, 0L));
            Arrays.stream(Product.values()).forEach(p -> sprzedaz.put(p, 0L));
            Long promoLicznik = 0L;
            ledger.evaluateAll();
        }
        return odpowiedz;
    }

    public void wyslijOdpowiedz(ReplyToAction odpowiedz) throws IOException {
        odpowiedz.setStudentId(settings.getNumerIndeksu());
        HttpPost post = new HttpPost("http://localhost:8080/action/log");

        JsonMapper mapper = new JsonMapper();
        String json = mapper.writeValueAsString(odpowiedz);
        StringEntity entity = new StringEntity(json);
        log.info(json);
        post.setEntity(entity);
        post.setHeader("Accept", "application/json");
        post.setHeader("Content-type", "application/json");

        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse response = httpClient.execute(post)) {

            HttpEntity rEntity = response.getEntity();
            if (rEntity != null) {
                // return it as a String
                String result = EntityUtils.toString(rEntity);
                log.info(result);
            }
        }
    }

    public void wyslijOdpowiedzLokalnie(ReplyToAction odpowiedz) throws IOException {
        odpowiedz.setStudentId(settings.getNumerIndeksu());
        try {
            ledger.addReplySequencer(odpowiedz);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (SterowanieAkcja.ZAMKNIJ_SKLEP.equals(odpowiedz.getTyp())) {

            magazyn = new Warehouse();
            Arrays.stream(Product.values()).forEach(p -> rezerwacje.put(p, 0L));
            Arrays.stream(Product.values()).forEach(p -> sprzedaz.put(p, 0L));

            Long promoLicznik = 0L;
            stop();
        }
    }

    private void stop() {

        log.info("Zamykam " + Thread.currentThread().getThreadGroup().getParent().activeCount());

        active = false;
    }
}

