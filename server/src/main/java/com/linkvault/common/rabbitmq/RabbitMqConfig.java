package com.linkvault.common.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RabbitMqProperties.class)
@ConditionalOnProperty(prefix = "app.rabbitmq", name = "enabled", havingValue = "true")
public class RabbitMqConfig {

    private static final Logger log = LoggerFactory.getLogger(RabbitMqConfig.class);

    private final RabbitMqProperties properties;

    public RabbitMqConfig(RabbitMqProperties properties) {
        this.properties = properties;
        log.info("Configuring RabbitMQ: exchange={}", properties.getExchange());
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter);
        return rabbitTemplate;
    }

    @Bean
    public TopicExchange eventsExchange() {
        return new TopicExchange(properties.getExchange(), true, false);
    }

    // Queues
    @Bean
    public Queue auditQueue() {
        return QueueBuilder.durable(properties.getAuditQueue())
            .withArgument("x-dead-letter-exchange", "")
            .withArgument("x-dead-letter-routing-key", RabbitMqNames.AUDIT_DLQ)
            .build();
    }

    @Bean
    public Queue previewQueue() {
        return QueueBuilder.durable(properties.getPreviewQueue())
            .withArgument("x-dead-letter-exchange", "")
            .withArgument("x-dead-letter-routing-key", RabbitMqNames.PREVIEW_DLQ)
            .build();
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(properties.getNotificationQueue())
            .withArgument("x-dead-letter-exchange", "")
            .withArgument("x-dead-letter-routing-key", RabbitMqNames.NOTIFICATION_DLQ)
            .build();
    }

    @Bean
    public Queue cleanupQueue() {
        return QueueBuilder.durable(properties.getCleanupQueue())
            .withArgument("x-dead-letter-exchange", "")
            .withArgument("x-dead-letter-routing-key", RabbitMqNames.CLEANUP_DLQ)
            .build();
    }

    // Dead Letter Queues
    @Bean
    public Queue auditDlq() {
        return QueueBuilder.durable(RabbitMqNames.AUDIT_DLQ).build();
    }

    @Bean
    public Queue previewDlq() {
        return QueueBuilder.durable(RabbitMqNames.PREVIEW_DLQ).build();
    }

    @Bean
    public Queue notificationDlq() {
        return QueueBuilder.durable(RabbitMqNames.NOTIFICATION_DLQ).build();
    }

    @Bean
    public Queue cleanupDlq() {
        return QueueBuilder.durable(RabbitMqNames.CLEANUP_DLQ).build();
    }

    // Bindings
    @Bean
    public Binding auditBinding(Queue auditQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(auditQueue).to(eventsExchange).with(properties.getAuditRoutingKey());
    }

    @Bean
    public Binding previewBinding(Queue previewQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(previewQueue).to(eventsExchange).with(properties.getPreviewRoutingKey());
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(notificationQueue).to(eventsExchange).with(properties.getNotificationRoutingKey());
    }

    @Bean
    public Binding cleanupBinding(Queue cleanupQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(cleanupQueue).to(eventsExchange).with(properties.getCleanupRoutingKey());
    }
}
