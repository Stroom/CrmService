package com.example.config;

import com.example.config.QueueChangeProperties.QueueProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class RabbitMQConfig {

    @Value("${crm.update.exchange.name}")
    private String crmUpdateExchangeName;

    private final QueueChangeProperties queueChangeProperties;

    @Bean
    public Declarables customerChangeQueue() {
        return createDeclarables(queueChangeProperties.getCustomer());
    }

    @Bean
    public Declarables customerRelationChangeQueue() {
        return createDeclarables(queueChangeProperties.getCustomerRelation());
    }

    @Bean
    public Declarables idDocumentChangeQueue() {
        return createDeclarables(queueChangeProperties.getIdDocument());
    }

    @Bean
    public Declarables idNumberChangeQueue() {
        return createDeclarables(queueChangeProperties.getIdNumber());
    }

    private Declarables createDeclarables(QueueProperties queueProperties) {
        final String queueName = queueProperties.getQueueName();
        final String exchangeName = queueProperties.getExchangeName();
        final String routingKey = queueProperties.getRoutingKey();

        final Queue queue = QueueBuilder.durable(queueName).build();
        final DirectExchange exchange = new DirectExchange(exchangeName);
        final Binding binding = BindingBuilder.bind(queue).to(exchange).with(routingKey);

        return new Declarables(
            queue,
            exchange,
            binding
        );
    }

    @Bean
    public TopicExchange crmUpdateExchange() {
        return new TopicExchange(crmUpdateExchangeName);
    }

    @Bean
    public Jackson2JsonMessageConverter converter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(converter());
        return rabbitTemplate;
    }
} 