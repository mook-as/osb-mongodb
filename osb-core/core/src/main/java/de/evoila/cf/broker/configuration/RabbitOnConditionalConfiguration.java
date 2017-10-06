package de.evoila.cf.broker.configuration;

import de.evoila.cf.broker.bean.BackupConfiguration;
import de.evoila.cf.broker.bean.RabbitMQCredentials;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.cloud.config.java.AbstractCloudConfig;

/**
 * Created by reneschollmeyer on 09.08.17.
 */
@Configuration
class RabbitOnConditionalConfiguration{

      @Configuration
      @ConditionalOnBean(RabbitMQCredentials.class)
      static class Default {

            @Autowired
            RabbitMQCredentials conf;
            @Bean
            public CachingConnectionFactory connectionFactory() {
                  CachingConnectionFactory cachingConnectionFactory =
                        new CachingConnectionFactory(conf.getHost(), conf.getPort());
                  cachingConnectionFactory.setUsername(conf.getUsername());
                  cachingConnectionFactory.setPassword(conf.getPassword());
                  cachingConnectionFactory.setVirtualHost(conf.getVhost());
                  return cachingConnectionFactory;
            }

            @Bean
            public AmqpAdmin amqpAdmin() {
                  return new RabbitAdmin(connectionFactory());
            }

            @Bean
            public RabbitTemplate rabbitTemplate() {
                  return new RabbitTemplate(connectionFactory());
            }

      }
      
}
