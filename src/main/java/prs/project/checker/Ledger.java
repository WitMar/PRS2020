package prs.project.checker;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import prs.project.model.Product;
import prs.project.model.Warehouse;
import prs.project.status.ReplyToAction;
import prs.project.task.Akcja;
import prs.project.task.SterowanieAkcja;
import prs.project.task.WycenaAkcje;
import prs.project.task.WydarzeniaAkcje;
import prs.project.task.ZamowieniaAkcje;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.in;

@Service
@Getter
@Setter
@Slf4j
@NoArgsConstructor
public class Ledger {

    ConcurrentHashMap<Long, ConcurrentLinkedQueue<Akcja>> actions = new ConcurrentHashMap<>();
    ConcurrentHashMap<Long, ConcurrentLinkedQueue<ReplyToAction>> logActions = new ConcurrentHashMap<>();
    ConcurrentHashMap<Long, ConcurrentLinkedQueue<ReplyToAction>> pattern = new ConcurrentHashMap<>();
    ConcurrentHashMap<Long, Warehouse> warehouses = new ConcurrentHashMap<>();
    ConcurrentHashMap<Long, Set<Enum>> types = new ConcurrentHashMap<>();
    ConcurrentHashMap<Long, Long> points = new ConcurrentHashMap<>();
    AtomicLong evaluate = new AtomicLong(0L);

    public void addReply(ReplyToAction odpowiedz) throws InterruptedException {
        if (!logActions.containsKey(odpowiedz.getStudentId())) {

            logActions.put(odpowiedz.getStudentId(), new ConcurrentLinkedQueue<>());

        }

        odpowiedz.setTimestamp(LocalDateTime.now());
        ConcurrentLinkedQueue<ReplyToAction> list = logActions.get(odpowiedz.getStudentId());
        list.add(odpowiedz);

        Thread.sleep(100);

    }

    public void addReplySequencer(ReplyToAction odpowiedz) throws InterruptedException {
        odpowiedz.setTimestamp(LocalDateTime.now());
        if (!pattern.containsKey(odpowiedz.getStudentId())) {

            pattern.put(odpowiedz.getStudentId(), new ConcurrentLinkedQueue<>());


        }

        odpowiedz.setTimestamp(LocalDateTime.now());
        ConcurrentLinkedQueue<ReplyToAction> list = pattern.get(odpowiedz.getStudentId());
        list.add(odpowiedz);

        Thread.sleep(100);

    }

    public void evaluateAll() throws AssertionError {

        evaluate.incrementAndGet();
        log.info("Zakonczono " + evaluate);
        if(evaluate.get() == 40) {
            try {
                Thread.currentThread().sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            evaluate.set(0L);
            logActions.keySet().stream().forEach(indeks -> {
                try {
                    if(!points.containsKey(indeks)) {
                        points.put(indeks, 0L);
                    }
                    long czasStudent = Duration.between(logActions.get(indeks).peek().getTimestamp(), logActions.get(indeks).stream().collect(Collectors.toList()).get(logActions.get(indeks).size() - 1).getTimestamp())
                            .toSeconds();
                    log.warn(indeks + " czas " + czasStudent);
                    long czas = Duration.between(pattern.get(indeks).peek().getTimestamp(), pattern.get(indeks).stream().collect(Collectors.toList()).get(pattern.get(indeks).size() - 1).getTimestamp()).toSeconds();

                    long liczbaAkceptacji = pattern.get(indeks).stream().filter(m -> Boolean.TRUE.equals(m.getZrealizowaneZamowienie())).count();
                    long liczbaNieakceptacji = pattern.get(indeks).stream().filter(m ->
                            m.getTyp().equals(ZamowieniaAkcje.POJEDYNCZE_ZAMOWIENIE)
                                    || m.getTyp().equals(ZamowieniaAkcje.GRUPOWE_ZAMOWIENIE)
                                    || m.getTyp().equals(ZamowieniaAkcje.REZERWACJA)
                                    || m.getTyp().equals(ZamowieniaAkcje.ODBIÓR_REZERWACJI) &&
                                    !m.getZrealizowaneZamowienie()
                    ).count();
                    Optional<ReplyToAction> last = pattern.get(indeks).stream().filter(m -> m.getTyp().equals(SterowanieAkcja.ZAMKNIJ_SKLEP)).findFirst();
                    EnumMap<Product, Long> stanMagazynów = last.get().getStanMagazynów();
                    EnumMap<Product, Long> stanCen = last.get().getGrupaProduktów();

                    List<ReplyToAction> cenyOdpowiedzi = pattern.get(indeks).stream().filter(m -> m.getTyp().equals(WycenaAkcje.PODAJ_CENE))
                            .sorted(Comparator.comparing(ReplyToAction::getId)).collect(Collectors.toList());
                    List<ReplyToAction> inwOdpowiedzi = pattern.get(indeks).stream().filter(m -> m.getTyp().equals(WydarzeniaAkcje.INWENTARYZACJA))
                            .sorted(Comparator.comparing(ReplyToAction::getId)).collect(Collectors.toList());
                    List<ReplyToAction> raportyOdpowiedzi = pattern.get(indeks).stream().filter(m -> m.getTyp().equals(WydarzeniaAkcje.RAPORT_SPRZEDAŻY))
                            .sorted(Comparator.comparing(ReplyToAction::getId)).collect(Collectors.toList());

                    long liczbaAkceptacjiStudent = logActions.get(indeks).stream().filter(m -> Boolean.TRUE.equals(m.getZrealizowaneZamowienie())).count();
                    long liczbaNieakceptacjiStudent = logActions.get(indeks).stream().filter(m ->
                            m.getTyp().equals(ZamowieniaAkcje.POJEDYNCZE_ZAMOWIENIE)
                                    || m.getTyp().equals(ZamowieniaAkcje.GRUPOWE_ZAMOWIENIE)
                                    || m.getTyp().equals(ZamowieniaAkcje.REZERWACJA)
                                    || m.getTyp().equals(ZamowieniaAkcje.ODBIÓR_REZERWACJI) &&
                                    !m.getZrealizowaneZamowienie()
                    ).count();
                    Optional<ReplyToAction> lastStudent = logActions.get(indeks).stream().filter(m -> m.getTyp().equals(SterowanieAkcja.ZAMKNIJ_SKLEP))
                            .findFirst();
                    EnumMap<Product, Long> stanMagazynówStudent = lastStudent.get().getStanMagazynów();
                    EnumMap<Product, Long> stanCenStudent = lastStudent.get().getGrupaProduktów();

                    List<ReplyToAction> cenyOdpowiedziStudent = logActions.get(indeks).stream().filter(m -> m.getTyp().equals(WycenaAkcje.PODAJ_CENE))
                            .sorted(Comparator.comparing(ReplyToAction::getId)).collect(Collectors.toList());
                    List<ReplyToAction> inwOdpowiedziStudent = logActions.get(indeks).stream().filter(m -> m.getTyp().equals(WydarzeniaAkcje.INWENTARYZACJA))
                            .sorted(Comparator.comparing(ReplyToAction::getId)).collect(Collectors.toList());
                    List<ReplyToAction> raportyOdpowiedziStudent = logActions.get(indeks).stream()
                            .filter(m -> m.getTyp().equals(WydarzeniaAkcje.RAPORT_SPRZEDAŻY))
                            .sorted(Comparator.comparing(ReplyToAction::getId)).collect(Collectors.toList());

                    assertThat(liczbaAkceptacjiStudent).as("Twój program zaakceptowal/odrzucil (nie)poprawne zakupy").isEqualTo(liczbaAkceptacji);
                    assertThat(liczbaNieakceptacjiStudent).as("Twój program odrzucil/nie odrzucil niepoprawnych zakupow").isEqualTo(liczbaNieakceptacji);
                    assertThat(stanMagazynówStudent).as("Stan magazynów na koniec sie nie zgadza").containsAllEntriesOf(stanMagazynów);
                    assertThat(stanCenStudent).as("Stan cen na koniec sie nie zgadza").containsAllEntriesOf(stanCen);
                    for (int i = 0; i < cenyOdpowiedziStudent.size(); i++) {
                        assertThat(cenyOdpowiedziStudent.get(i)).as("Różnica w odpowiedzi na ceny").usingRecursiveComparison().ignoringFields("timestamp", "studentId").isEqualTo(cenyOdpowiedzi.get(i));
                    }
                    for (int i = 0; i < inwOdpowiedziStudent.size(); i++) {
                        assertThat(inwOdpowiedziStudent.get(i)).as("Różnica w inwentaryzacji").usingRecursiveComparison().ignoringFields("timestamp", "studentId").isEqualTo(inwOdpowiedzi.get(i));
                    }
                    for (int i = 0; i < raportyOdpowiedziStudent.size(); i++) {
                        assertThat(raportyOdpowiedziStudent.get(i)).as("Różnica w raporcie sprzedaży").usingRecursiveComparison().ignoringFields("timestamp", "studentId").isEqualTo(raportyOdpowiedzi.get(i));
                    }
                    log.warn(indeks + " Poprawnie - 1 pkt CZAS:" + czasStudent);
                    points.put(indeks, points.get(indeks) + 1);
                } catch (AssertionError a) {
                    log.warn(indeks + " Błędnie - 0 pkt ");
                    log.info(a.getMessage());
                }
            });

            log.info(points.entrySet().stream().sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                    .map(p -> "Punkty " + p.getValue() + " " + p.getKey() + "|")
                    .collect(Collectors.joining(" ")));
            clear();
        }

    }
    public void clear() {
        pattern.clear();
        logActions.clear();
        actions.clear();
        warehouses.clear();
        types.clear();
    }
}
