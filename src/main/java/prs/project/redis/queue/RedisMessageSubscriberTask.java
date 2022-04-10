package prs.project.redis.queue;

import java.io.IOException;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;
import prs.project.ParallelExecutor;
import prs.project.generator.SequenceRunner;
import prs.project.task.Akcja;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;

@Service
@Slf4j
@AllArgsConstructor
public class RedisMessageSubscriberTask implements MessageListener {

    ParallelExecutor parallelExecutor;

    public void onMessage(Message message, byte[] pattern) {

    }
}
