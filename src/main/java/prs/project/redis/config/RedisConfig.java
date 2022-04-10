package prs.project.redis.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import prs.project.ParallelExecutor;
import prs.project.controllers.Settings;
import prs.project.generator.SequenceRunner;
import prs.project.redis.queue.RedisMessagePublisherTask;
import prs.project.redis.queue.RedisMessageSubscriberTask;
import prs.project.task.Akcja;

import java.util.concurrent.ConcurrentHashMap;

@Configuration
@ComponentScan("prs.project")
@PropertySource("classpath:application.yml")
public class RedisConfig {

    @Autowired
    ParallelExecutor parallelExecutor;

    @Autowired
    Settings settings;

    @Bean
    ConcurrentHashMap<Long, Long> state() {
        return new ConcurrentHashMap<Long, Long>();
    }

    @Bean
    JedisConnectionFactory jedisConnectionFactory() {
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration("localhost", 8989);
        return new JedisConnectionFactory(redisStandaloneConfiguration);
    }

    @Bean
    public RedisTemplate<Long, Akcja> redisTemplate() {
        final RedisTemplate<Long, Akcja> template = new RedisTemplate<Long, Akcja>();
        template.setConnectionFactory(jedisConnectionFactory());
        template.setValueSerializer(new GenericToStringSerializer<Akcja>(Akcja.class));
        return template;
    }

    @Bean(name = "listenerTask")
    MessageListenerAdapter messageListenerTask() {
        return new MessageListenerAdapter(new RedisMessageSubscriberTask(parallelExecutor));
    }

    @Bean(name = "containerTask")
    RedisMessageListenerContainer redisContainerTask() {
        final RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(jedisConnectionFactory());
        container.addMessageListener(messageListenerTask(), topicTask());
        return container;
    }

    @Bean(name = "publisherTask")
    RedisMessagePublisherTask redisPublisherTask() {
        return new RedisMessagePublisherTask(redisTemplate(), topicTask());
    }

    @Bean(name = "topicTask")
    ChannelTopic topicTask() {
        return new ChannelTopic("pubsub:task");
    }


}
