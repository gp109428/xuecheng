package com.xuecheng;

import org.junit.Test;
import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;

public class test {

    @Test
    public void test() throws IOException {
        //文件
        File mp4 = new File("E://video/lucene.mp4");
        //目录
        //File path = new File("E://video/test//");
        String s = "E://video/test/";
        //分块大小计算分块数量
        long chunkSize = 1024*1024*1;
        long l = (long) Math.ceil(mp4.length() * 1.0 / chunkSize);
        //读文件,写文件
        byte[] b =new byte[1024];
        RandomAccessFile accessFile=new RandomAccessFile(mp4,"r");
        //创建分块文件
        for (int i=0;i<l;i++){
            File file = new File(s+i);
            boolean newFile = file.createNewFile();
            if (newFile){
                RandomAccessFile raf_write = new RandomAccessFile(file, "rw");
                //int len =-1;
                while (accessFile.read(b)!=-1){
                    raf_write.write(b,0,b.length);
                    if (file.length()>=chunkSize){
                        break;
                    }
                }
                raf_write.close();
            }
        }
        //关流
        accessFile.close();
    }

    @Test
    public void test1() throws IOException {
        /*
        文件合并流程：
        1、找到要合并的文件并按文件合并的先后进行排序。
        2、创建合并文件
        3、依次从合并的文件中读取数据向合并文件写入数
         */
        File newFile = new File("E://video/1.mp4");
        newFile.createNewFile();
        RandomAccessFile accessFile_write = new RandomAccessFile(newFile, "rw");
        accessFile_write.seek(0);
        File file = new File("E://video/test//");
        File[] files = file.listFiles();
        // 转成集合，便于排序
        List<File> fileList = new ArrayList<File>(Arrays.asList(files));
        Collections.sort(fileList, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                if (Integer.parseInt(o1.getName())>Integer.parseInt(o2.getName())){
                    return 1;
                }
                return -1;
            }
        });
        //缓冲区
        byte[] b = new byte[1024];
        //合并文件
        for (File file1 : fileList) {
            RandomAccessFile r = new RandomAccessFile(file1, "rw");
            while (r.read(b)!=-1) {
                accessFile_write.write(b, 0, b.length);
            }
            r.close();
        }
        accessFile_write.close();
    }
}
