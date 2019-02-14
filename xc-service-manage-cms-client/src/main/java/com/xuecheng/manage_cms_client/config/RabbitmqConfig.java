package com.xuecheng.manage_cms_client.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitmqConfig {

    //定义队列bean名字
    public static final String QUEUE_CMS_POSTPAGE = "queue_cms_postpage";

    //定义交换机名字
    public static final String EX_ROUTING_CMS_POSTPAGE="ex_routing_cms_postpage";
    //队列的名称
    @Value("${xuecheng.mq.queue}")
    public  String queue_cms_postpage_name;
    //routingKey 即站点Id
    @Value("${xuecheng.mq.routingKey}")
    public  String routingKey;

    //声明交换机
    @Bean(EX_ROUTING_CMS_POSTPAGE)
    public Exchange EXCHANGE_TOPICS_INFORM(){
        return ExchangeBuilder.directExchange(EX_ROUTING_CMS_POSTPAGE).durable(true).build();
    }

    //声明队列
    @Bean(QUEUE_CMS_POSTPAGE)
    public Queue QUEUE_CMS_POSTPAGE(){
        Queue queue = new Queue(queue_cms_postpage_name);
        return queue;
    }
    //队列绑定交换机
//    public Binding BINDING_QUEUE_INFORM_SMS(@Qualifier(QUEUE_CMS_POSTPAGE) Queue queue,@Qualifier(EX_ROUTING_CMS_POSTPAGE) Exchange exchange){
//        return BindingBuilder.bind(queue).to(exchange).with(routingKey).noargs();
//    }
    @Bean
    public Binding BINDING_QUEUE_INFORM_SMS(@Qualifier(QUEUE_CMS_POSTPAGE) Queue queue, @Qualifier(EX_ROUTING_CMS_POSTPAGE) Exchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(routingKey).noargs();
    }
}
