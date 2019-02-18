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
        File file = new File("e:/course.ftl");
        //定义输入流
        FileInputStream inputStram = new FileInputStream(file);
        //向GridFS存储文件
        ObjectId objectId = gridFsTemplate.store(inputStram, "course.ftl", "utf-8");
        //得到文件ID
        String fileId = objectId.toString();
        System.out.println(fileId);

    }
    @Test
    public void getHtml() throws FileNotFoundException {
        String html = service.getPageHtml("5c5011ccdf589638d427a4ff");
        System.out.println(html);

    }

    @Test
    public void sortq(){
        int []a ={3,45,78,64,52,11,64,55,99,11,18};
        int hi=a.length-1;
        int lo=0;
        int key=a[lo];
        while (lo<hi){
            while (a[hi]>=key&&lo<hi){
                hi--;
            }
            a[lo]=a[hi];
            a[hi]=key;
            while (a[lo]<=key&&lo<hi){
                lo++;
            }

        }
        for (int i : a) {
            System.out.println(i);
        }

    }

    @Test
    public void maoao(){
        int []num ={3,45,78,64,52,11,64,55,99,11,18};
        for (int i=0;i<num.length-1;i++) {   //控制多少趟
            for (int j =0;j<num.length-1-i;j++) {  //控制多少次
                if(num[j+1]<num[j]){
                    int temp = num[j];
                    num[j] = num[j+1];
                    num[j+1] = temp;
                }
            }
        }
        for (int i : num) {
            System.out.println(i);
        }

    }

    @Test
    public void charu(){
        int []a ={3,45,78,64,52,11,64,55,99,11,18};
        for(int i=1;i<a.length;i++){
            int temp=a[i];
            int left = i-1;
            while (left>=0&&temp<a[left]){
                a[left+1]=a[left];
                left--;
            }
            a[left+1]=temp;
        }
        for (int i : a) {
            System.out.println(i);
        }
    }


}