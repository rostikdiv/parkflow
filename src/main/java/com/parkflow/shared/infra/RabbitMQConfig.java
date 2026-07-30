package com.parkflow.shared.infra;

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
 * Configuration for RabbitMQ topology.
 * Defines exchanges, DLX, and queues for Sensor, Payment, and Notification modules.
 * Also configures the listener container to use Java 21 Virtual Threads.
 * Reference: parkflow_final_plan.md §9
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_SENSOR = "parkflow.sensor";
    public static final String EXCHANGE_PAYMENT = "parkflow.payment";
    public static final String EXCHANGE_NOTIFICATION = "parkflow.notification";
    public static final String EXCHANGE_DLX = "parkflow.dlx";
    
    public static final String QUEUE_SENSOR_EVENTS = "q.sensor.events";
    public static final String QUEUE_SENSOR_EVENTS_DLQ = "q.sensor.events.dlq";

    public static final String QUEUE_PAYMENT_COMMANDS = "q.payment.commands";
    public static final String QUEUE_PAYMENT_COMMANDS_DLQ = "q.payment.commands.dlq";

    public static final String QUEUE_PAYMENT_RESULTS = "q.payment.results";
    public static final String QUEUE_PAYMENT_RESULTS_DLQ = "q.payment.results.dlq";

    public static final String QUEUE_NOTIFICATION_COMMANDS = "q.notification.commands";
    public static final String QUEUE_NOTIFICATION_COMMANDS_DLQ = "q.notification.commands.dlq";

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

    // --- Payment Topology ---
    @Bean
    public DirectExchange paymentExchange() {
        return new DirectExchange(EXCHANGE_PAYMENT);
    }

    @Bean
    public Queue paymentCommandsQueue() {
        return QueueBuilder.durable(QUEUE_PAYMENT_COMMANDS)
                .withArgument("x-dead-letter-exchange", EXCHANGE_DLX)
                .withArgument("x-dead-letter-routing-key", QUEUE_PAYMENT_COMMANDS_DLQ)
                .build();
    }

    @Bean
    public Queue paymentCommandsDlq() {
        return QueueBuilder.durable(QUEUE_PAYMENT_COMMANDS_DLQ).build();
    }

    @Bean
    public Binding bindingPaymentCommands(Queue paymentCommandsQueue, DirectExchange paymentExchange) {
        return BindingBuilder.bind(paymentCommandsQueue).to(paymentExchange).with("payment.command");
    }

    @Bean
    public Binding bindingPaymentCommandsDlq(Queue paymentCommandsDlq, DirectExchange dlxExchange) {
        return BindingBuilder.bind(paymentCommandsDlq).to(dlxExchange).with(QUEUE_PAYMENT_COMMANDS_DLQ);
    }

    @Bean
    public Queue paymentResultsQueue() {
        return QueueBuilder.durable(QUEUE_PAYMENT_RESULTS)
                .withArgument("x-dead-letter-exchange", EXCHANGE_DLX)
                .withArgument("x-dead-letter-routing-key", QUEUE_PAYMENT_RESULTS_DLQ)
                .build();
    }

    @Bean
    public Queue paymentResultsDlq() {
        return QueueBuilder.durable(QUEUE_PAYMENT_RESULTS_DLQ).build();
    }

    @Bean
    public Binding bindingPaymentResults(Queue paymentResultsQueue, DirectExchange paymentExchange) {
        return BindingBuilder.bind(paymentResultsQueue).to(paymentExchange).with("payment.result");
    }

    @Bean
    public Binding bindingPaymentResultsDlq(Queue paymentResultsDlq, DirectExchange dlxExchange) {
        return BindingBuilder.bind(paymentResultsDlq).to(dlxExchange).with(QUEUE_PAYMENT_RESULTS_DLQ);
    }

    // --- Notification Topology ---
    @Bean
    public DirectExchange notificationExchange() {
        return new DirectExchange(EXCHANGE_NOTIFICATION);
    }

    @Bean
    public Queue notificationCommandsQueue() {
        return QueueBuilder.durable(QUEUE_NOTIFICATION_COMMANDS)
                .withArgument("x-dead-letter-exchange", EXCHANGE_DLX)
                .withArgument("x-dead-letter-routing-key", QUEUE_NOTIFICATION_COMMANDS_DLQ)
                .build();
    }

    @Bean
    public Queue notificationCommandsDlq() {
        return QueueBuilder.durable(QUEUE_NOTIFICATION_COMMANDS_DLQ).build();
    }

    @Bean
    public Binding bindingNotificationCommands(Queue notificationCommandsQueue, DirectExchange notificationExchange) {
        return BindingBuilder.bind(notificationCommandsQueue).to(notificationExchange).with("notify.email");
    }

    @Bean
    public Binding bindingNotificationCommandsDlq(Queue notificationCommandsDlq, DirectExchange dlxExchange) {
        return BindingBuilder.bind(notificationCommandsDlq).to(dlxExchange).with(QUEUE_NOTIFICATION_COMMANDS_DLQ);
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
