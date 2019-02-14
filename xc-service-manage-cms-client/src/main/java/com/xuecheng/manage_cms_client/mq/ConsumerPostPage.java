package com.xuecheng.manage_cms_client.mq;

import com.alibaba.fastjson.JSON;
import com.xuecheng.framework.domain.cms.CmsPage;
import com.xuecheng.manage_cms_client.service.PageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ConsumerPostPage {
    @Autowired
    PageService service;

    private static final Logger LOGGER= LoggerFactory.getLogger(ConsumerPostPage.class);

    @RabbitListener(queues ={"${xuecheng.mq.queue}"} )
    public void postPage(String msg){
        //解析消息转成map格式
        Map map = JSON.parseObject(msg, Map.class);
        String pageId = (String) map.get("pageId");
        CmsPage cmsPgae = service.findCmsPgae(pageId);
        if (cmsPgae==null){
            LOGGER.error("receive cms post page,cmsPage is null:{}",msg.toString());
            return ;
        }
        //调用方法讲页面下载到物理路径
        service.savePageToServerPath(pageId);
    }
}
