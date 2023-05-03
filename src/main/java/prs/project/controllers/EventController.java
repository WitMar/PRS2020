package prs.project.controllers;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@AllArgsConstructor
@Controller
@Slf4j
@RequestMapping("action")
public class EventController {

    Ledger ledger;
    Settings settings;
    ParallelExecutor parallelExecutor;
    SequenceRunner sequenceRunner;

    @GetMapping(value = "/generate")
    public ResponseEntity<String> generateActions() {
        Generator generator = new Generator(settings.getLiczbaZadan());
        List<Akcja> akcje = generator.generate();

        akcje.forEach(akcja -> {
            parallelExecutor.process(akcja);
        });

        akcje.forEach(akcja -> {
            sequenceRunner.process(akcja);
        });

        return new ResponseEntity<>("Generated", HttpStatus.OK);
    }

    @PostMapping(value = "/log", produces = "application/json")
    public ResponseEntity<ReplyToAction> logAction(@RequestBody @Validated ReplyToAction odpowiedz) throws InterruptedException {
        ledger.addReply(odpowiedz);
        return new ResponseEntity<>(odpowiedz, HttpStatus.OK);
    }

}
