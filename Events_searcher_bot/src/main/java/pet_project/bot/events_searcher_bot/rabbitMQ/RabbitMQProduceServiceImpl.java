package pet_project.bot.events_searcher_bot.rabbitMQ;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RabbitMQProduceServiceImpl implements RabbitMQProducerService{

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public void sendMessage(String message, String routingKey) {
        rabbitTemplate.convertAndSend("testExchange", routingKey, message);
    }
}
