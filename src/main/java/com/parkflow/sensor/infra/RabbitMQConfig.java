package com.parkflow.sensor.infra;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;

/**
 * Configuration for RabbitMQ topology in Milestone M3.
 * Defines the main sensor exchange, Dead Letter Exchange (DLX), and queues.
 * Also configures the listener container to use Java 21 Virtual Threads, 
 * since Spring Boot 3 enables virtual threads for Tomcat but not automatically for RabbitMQ listeners.
 * Reference: parkflow_final_plan.md §9
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_SENSOR = "parkflow.sensor";
    public static final String EXCHANGE_DLX = "parkflow.dlx";
    
    public static final String QUEUE_SENSOR_EVENTS = "q.sensor.events";
    public static final String QUEUE_SENSOR_EVENTS_DLQ = "q.sensor.events.dlq";

    /**
     * Main exchange for sensor events.
     * Chosen as TopicExchange to allow routing keys like `sensor.{lotId}`.
     */
    @Bean
    public TopicExchange sensorExchange() {
        return new TopicExchange(EXCHANGE_SENSOR);
    }

    /**
     * Dead Letter Exchange for all unprocessable messages.
     * DirectExchange is sufficient as we just route exactly to DLQs.
     */
    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(EXCHANGE_DLX);
    }

    /**
     * Queue for processing sensor events.
     * Configured with x-dead-letter-exchange to ensure failed messages (e.g. exhausted retries)
     * are not lost but routed to DLX.
     */
    @Bean
    public Queue sensorEventsQueue() {
        return QueueBuilder.durable(QUEUE_SENSOR_EVENTS)
                .withArgument("x-dead-letter-exchange", EXCHANGE_DLX)
                .withArgument("x-dead-letter-routing-key", QUEUE_SENSOR_EVENTS_DLQ)
                .build();
    }

    /**
     * Dead Letter Queue for sensor events.
     */
    @Bean
    public Queue sensorEventsDlq() {
        return QueueBuilder.durable(QUEUE_SENSOR_EVENTS_DLQ).build();
    }

    /**
     * Bind the sensor events queue to the topic exchange.
     * Routing key `sensor.#` means it receives all messages starting with `sensor.`.
     */
    @Bean
    public Binding bindingSensorEvents(Queue sensorEventsQueue, TopicExchange sensorExchange) {
        return BindingBuilder.bind(sensorEventsQueue).to(sensorExchange).with("sensor.#");
    }

    /**
     * Bind the DLQ to the DLX.
     */
    @Bean
    public Binding bindingSensorEventsDlq(Queue sensorEventsDlq, DirectExchange dlxExchange) {
        return BindingBuilder.bind(sensorEventsDlq).to(dlxExchange).with(QUEUE_SENSOR_EVENTS_DLQ);
    }

    /**
     * Custom RabbitListenerContainerFactory to explicitly use Virtual Threads.
     * This is required because `@RabbitListener` methods otherwise run on a standard thread pool,
     * which doesn't scale as well for high-throughput I/O bound tasks like DB writes.
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            SimpleRabbitListenerContainerFactoryConfigurer configurer) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setTaskExecutor(Executors.newVirtualThreadPerTaskExecutor());
        return factory;
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
