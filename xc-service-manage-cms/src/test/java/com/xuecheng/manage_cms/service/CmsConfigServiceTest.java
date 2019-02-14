package com.xuecheng.manage_cms.service;

import org.bson.types.ObjectId;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import static org.junit.Assert.*;
@SpringBootTest
@RunWith(SpringRunner.class)
public class CmsConfigServiceTest {

    @Autowired
    GridFsTemplate gridFsTemplate;


    @Autowired
    PageService service;
    @Autowired
    RestTemplate restTemplate;

    @Test
    public void getConfigById() {

    }

    @Test
    public void testGridFs() throws FileNotFoundException {
        //要存储的文件
        File file = new File("e:/index_banner.ftl");
        //定义输入流
        FileInputStream inputStram = new FileInputStream(file);
        //向GridFS存储文件
        ObjectId objectId = gridFsTemplate.store(inputStram, "轮播图测试文件01", "");
        //得到文件ID
        String fileId = objectId.toString();
        System.out.println(fileId);
    }
    @Test
    public void getHtml() throws FileNotFoundException {
        String html = service.getPageHtml("5c5011ccdf589638d427a4ff");
        System.out.println(html);

    }
}