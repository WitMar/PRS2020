package prs.project.checker;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Getter
@Setter
@Slf4j
@NoArgsConstructor
public class Ledger {

    @Autowired
    ObjectMapper jacksonObjectMapper;

    ConcurrentHashMap<Long, ConcurrentLinkedQueue<Akcja>> actions = new ConcurrentHashMap<>();
    ConcurrentHashMap<Long, ConcurrentLinkedQueue<ReplyToAction>> logActions = new ConcurrentHashMap<>();
    ArrayList<ReplyToAction> pattern = new ArrayList<>();
    ConcurrentHashMap<Long, Warehouse> warehouses = new ConcurrentHashMap<>();
    ConcurrentHashMap<Long, Set<Enum>> types = new ConcurrentHashMap<>();

    public void addReply(ReplyToAction odpowiedz) throws InterruptedException {
        if (!logActions.containsKey(odpowiedz.getStudentId())) {

            logActions.put(odpowiedz.getStudentId(), new ConcurrentLinkedQueue<>());

        }

        odpowiedz.setTimestamp(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC));
        ConcurrentLinkedQueue<ReplyToAction> list = logActions.get(odpowiedz.getStudentId());
        list.add(odpowiedz);

    }

    public void addReplySequencer(ReplyToAction odpowiedz) throws InterruptedException {
        odpowiedz.setTimestamp(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC));
        pattern.add(odpowiedz);
    }

    public void evaluate(long indeks) throws AssertionError {
        long czas = Duration.between(Instant.ofEpochSecond(pattern.get(0).getTimestamp()), Instant.ofEpochSecond(pattern.get(pattern.size() - 1).getTimestamp())).toSeconds();
        long liczbaAkceptacji = pattern.stream().filter(m -> Boolean.TRUE.equals(m.getZrealizowaneZamowienie())).count();
        long liczbaNieakceptacji = pattern.stream().filter(m ->
                m.getTyp().equals(ZamowieniaAkcje.POJEDYNCZE_ZAMOWIENIE)
                        || m.getTyp().equals(ZamowieniaAkcje.GRUPOWE_ZAMOWIENIE)
                        || m.getTyp().equals(ZamowieniaAkcje.REZERWACJA)
                        || m.getTyp().equals(ZamowieniaAkcje.ODBIÓR_REZERWACJI) &&
                        !m.getZrealizowaneZamowienie()
        ).count();
        Optional<ReplyToAction> last = pattern.stream().filter(m -> m.getTyp().equals(SterowanieAkcja.ZAMKNIJ_SKLEP)).findFirst();
        EnumMap<Product, Long> stanMagazynów = last.get().getStanMagazynów();
        EnumMap<Product, Long> stanCen = last.get().getGrupaProduktów();

        List<ReplyToAction> cenyOdpowiedzi = pattern.stream().filter(m -> m.getTyp().equals(WycenaAkcje.PODAJ_CENE))
                .sorted(Comparator.comparing(ReplyToAction::getId)).collect(Collectors.toList());
        List<ReplyToAction> inwOdpowiedzi = pattern.stream().filter(m -> m.getTyp().equals(WydarzeniaAkcje.INWENTARYZACJA))
                .sorted(Comparator.comparing(ReplyToAction::getId)).collect(Collectors.toList());
        List<ReplyToAction> raportyOdpowiedzi = pattern.stream().filter(m -> m.getTyp().equals(WydarzeniaAkcje.RAPORT_SPRZEDAŻY))
                .sorted(Comparator.comparing(ReplyToAction::getId)).collect(Collectors.toList());

        if(logActions.get(indeks).size() > 1) {
            long czasStudent = Duration.between(Instant.ofEpochSecond(logActions.get(indeks).peek()
                            .getTimestamp()), Instant.ofEpochSecond(logActions.get(indeks).stream().collect(Collectors.toList())
                            .get(logActions.get(indeks).size() - 1).getTimestamp()))
                    .toSeconds();
            long liczbaAkceptacjiStudent = logActions.get(indeks).stream().filter(m -> Boolean.TRUE.equals(m.getZrealizowaneZamowienie()))
                    .count();
            long liczbaNieakceptacjiStudent = logActions.get(indeks).stream().filter(m ->
                    m.getTyp()!=null && m.getTyp().equals(ZamowieniaAkcje.POJEDYNCZE_ZAMOWIENIE)
                            || m.getTyp().equals(ZamowieniaAkcje.GRUPOWE_ZAMOWIENIE)
                            || m.getTyp().equals(ZamowieniaAkcje.REZERWACJA)
                            || m.getTyp().equals(ZamowieniaAkcje.ODBIÓR_REZERWACJI) &&
                            !m.getZrealizowaneZamowienie()
            ).count();
            Optional<ReplyToAction> lastStudent = logActions.get(indeks).stream()
                    .filter(m ->  m.getTyp()!=null && m.getTyp().equals(SterowanieAkcja.ZAMKNIJ_SKLEP))
                    .findFirst();
            EnumMap<Product, Long> stanMagazynówStudent = lastStudent.get().getStanMagazynów();
            EnumMap<Product, Long> stanCenStudent = lastStudent.get().getGrupaProduktów();

            List<ReplyToAction> cenyOdpowiedziStudent = logActions.get(indeks).stream()
                    .filter(m -> m.getTyp().equals(WycenaAkcje.PODAJ_CENE))
                    .sorted(Comparator.comparing(ReplyToAction::getId)).collect(Collectors.toList());
            List<ReplyToAction> inwOdpowiedziStudent = logActions.get(indeks).stream()
                    .filter(m -> m.getTyp().equals(WydarzeniaAkcje.INWENTARYZACJA))
                    .sorted(Comparator.comparing(ReplyToAction::getId)).collect(Collectors.toList());
            List<ReplyToAction> raportyOdpowiedziStudent = logActions.get(indeks).stream()
                    .filter(m -> m.getTyp().equals(WydarzeniaAkcje.RAPORT_SPRZEDAŻY))
                    .sorted(Comparator.comparing(ReplyToAction::getId)).collect(Collectors.toList());

            log.warn("Twój program dziala " + czasStudent + " Java działa " + czas + " " + indeks);
            assertThat(liczbaAkceptacjiStudent).as("Twój program blednie przeprocesowal poprawne zakupy" + " " + indeks).isEqualTo(liczbaAkceptacji);
            assertThat(liczbaNieakceptacjiStudent).as("Twój program blednie przeprocesowal niepoprawne zakupy" + " " + indeks)
                    .isEqualTo(liczbaNieakceptacji);
            assertThat(stanMagazynówStudent).as("Stan magazynów na koniec sie nie zgadza" + " " + indeks).containsAllEntriesOf(stanMagazynów);
            assertThat(stanCenStudent).as("Stan cen na koniec sie nie zgadza" + " " + indeks).containsAllEntriesOf(stanCen);
            for (int i = 0; i < cenyOdpowiedziStudent.size(); i++) {
                assertThat(cenyOdpowiedziStudent.get(i)).as("Stan cen na koniec sie nie zgadza" + " " + indeks).usingRecursiveComparison().ignoringFields("timestamp", "studentId")
                        .isEqualTo(cenyOdpowiedzi.get(i));
            }
            for (int i = 0; i < inwOdpowiedziStudent.size(); i++) {
                assertThat(inwOdpowiedziStudent.get(i)).as("Inwentaryzacja sie nie zgadza" + " " + indeks).usingRecursiveComparison().ignoringFields("timestamp", "studentId")
                        .isEqualTo(inwOdpowiedzi.get(i));
            }
            for (int i = 0; i < raportyOdpowiedziStudent.size(); i++) {
                assertThat(raportyOdpowiedziStudent.get(i)).as("Raporty sie nie zgadzaja" + " " + indeks).usingRecursiveComparison().ignoringFields("timestamp", "studentId")
                        .isEqualTo(raportyOdpowiedzi.get(i));
            }
        }
        log.warn("Jeżeli nie ma bledow program zadzialal poprawnie " + indeks);
    }
    public void clear() {
        pattern.clear();
        logActions.clear();
        actions.clear();
        warehouses.clear();
        types.clear();
    }
}
