package prs.project.controllers;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import prs.project.checker.Ledger;
import prs.project.generator.Generator;
import prs.project.generator.SequenceRunner;
import prs.project.redis.queue.RedisMessagePublisherTask;
import prs.project.status.ReplyToAction;
import prs.project.task.*;

@AllArgsConstructor
@Controller
@Slf4j
@RequestMapping("action")
public class EventController {

    Ledger ledger;
    RedisMessagePublisherTask redisMessagePublisher;
    Settings settings;

    @GetMapping(value = "/generate")
    public ResponseEntity<String> generateActions() throws URISyntaxException {
        Generator generator = new Generator(settings.getLiczbaZadan());
        List<Akcja> akcje = generator.generate();

        akcje.forEach(akcja -> redisMessagePublisher.publish(akcja));


        String fileName = "students.txt";

        //read file into stream, try-with-resources
        try (Stream<String> stream = Files.lines(Paths.get(ClassLoader.getSystemResource(fileName).toURI()))) {

            stream.filter(line -> line.length() > 2).forEach(line -> {
                List<String> strins = Arrays.stream(line.split(" ")).collect(Collectors.toList());
                Settings settings = new Settings();
                Long indeks = Long.parseLong(strins.get(0));
                settings.setNumerIndeksu(indeks);
                settings.setWycena(strins.get(1));
                settings.setZamowienia(strins.get(2));
                settings.setZaopatrzenie(strins.get(3));
                settings.setWydarzenia(strins.get(4));
                SequenceRunner seq = new SequenceRunner(settings, Collections.EMPTY_LIST, ledger);
                akcje.forEach(akcja -> seq.process(akcja));
            });

        } catch (IOException e) {
            e.printStackTrace();
        }

        return new ResponseEntity<>("Generated", HttpStatus.OK);
    }

    @PostMapping(value = "/log", produces = "application/json")
    public ResponseEntity<ReplyToAction> logAction(@RequestBody ReplyToAction odpowiedz) throws InterruptedException {
        ledger.addReply(odpowiedz);
        return new ResponseEntity<>(odpowiedz, HttpStatus.OK);
    }

}
