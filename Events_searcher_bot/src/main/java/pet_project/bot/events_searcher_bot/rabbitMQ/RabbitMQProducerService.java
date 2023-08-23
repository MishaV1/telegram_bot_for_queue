package pet_project.bot.events_searcher_bot.rabbitMQ;

public interface RabbitMQProducerService {
    void sendMessage(String message, String routingKey);
}
