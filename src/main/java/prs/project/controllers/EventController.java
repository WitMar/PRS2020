package prs.project.controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import prs.project.ParallelExecutor;
import prs.project.checker.Ledger;
import prs.project.generator.Generator;
import prs.project.generator.SequenceRunner;
import prs.project.status.ReplyToAction;
import prs.project.task.Akcja;

import com.fasterxml.jackson.databind.ObjectMapper;

@AllArgsConstructor
@Controller
@Slf4j
@RequestMapping("action")
public class EventController {

    Ledger ledger;
    Settings settings;
    ParallelExecutor parallelExecutor;
    SequenceRunner sequenceRunner;
    ObjectMapper jacksonObjectMapper;
    List<Handshake> clients = new ArrayList();

    @GetMapping(value = "/generate")
    public ResponseEntity<String> generateActions() {
        Generator generator = new Generator(settings.getLiczbaZadan());
        List<Akcja> akcje = generator.generate();

        akcje.forEach(akcja -> {
            sequenceRunner.process(akcja);
        });

        clients.stream().forEach(client -> {
            String ip = client.getIp_addr();
            Integer port = client.getPort();
            HttpClient httpClient = HttpClientBuilder.create().build();
            try {
                StringEntity entity = new StringEntity(jacksonObjectMapper.writeValueAsString(akcje),
                        "application/json",
                        "UTF-8");
                HttpPost request = new HttpPost("http://"+ip+":"+port+"/push-data");
                request.setEntity(entity);
                request.setHeader("Accept", "application/json");
                request.setHeader("Content-type", "application/json");
                HttpResponse response = httpClient.execute(request);
            } catch (Exception ex) {
                log.error(ex.toString());
            }
        });

        return new ResponseEntity<>("Generated", HttpStatus.OK);
    }

    @PostMapping(value = "/log", produces = "application/json")
    public ResponseEntity<ReplyToAction> logAction(@RequestBody @Validated ReplyToAction odpowiedz) throws InterruptedException {
        ledger.addReply(odpowiedz);
        return new ResponseEntity<>(odpowiedz, HttpStatus.OK);
    }

    @PostMapping(value = "/replies")
    public String getReplies(@RequestBody @Validated List<ReplyToAction> odpowiedzi) throws InterruptedException {
        Long indekss = null;
        for (ReplyToAction odp : odpowiedzi) {
            try {
                indekss = odp.getStudentId();
                ledger.addReply(odp);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if(Objects.nonNull(indekss)) {
            ledger.evaluate(indekss);
        } else {
            ledger.evaluate(settings.getNumerIndeksu());
        }
        sequenceRunner.clear();

        return "OK";
    }

    @PostMapping(value = "/handshake")
    public ResponseEntity<String> getHandshake(@RequestBody @Validated  Handshake hello) {
        clients.add(hello);
        return new ResponseEntity<>("OK", HttpStatus.OK);
    }
}
