package com.xuecheng.filesystem.service;

import com.alibaba.fastjson.JSON;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.PutObjectResult;
import com.xuecheng.filesystem.dao.FileSystemRepository;
import com.xuecheng.framework.domain.filesystem.FileSystem;
import com.xuecheng.framework.domain.filesystem.response.UploadFileResult;
import com.xuecheng.framework.model.response.CommonCode;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

@Service
public class FileSystemService {
    @Value("${xuecheng.AccessKeyID}")
    String accessKeyId;
    @Value("${xuecheng.AccessKeySecret}")
    String accessKeySecret;
    @Autowired
    FileSystemRepository fileSystemRepository;

    String endpoint = "http://oss-cn-beijing.aliyuncs.com";
    String path ="https://hnnw.oss-cn-beijing.aliyuncs.com/image/";

    public UploadFileResult upload(MultipartFile file, String filetag, String businesskey, String metadata) {

        String fileId = this.upload(file);
        //创建文件信息对象
        FileSystem fileSystem = new FileSystem();
        //文件id
        fileSystem.setFileId(fileId);
        //文件在文件系统中的路径
        fileSystem.setFilePath(path+fileId);
        //业务标识
        fileSystem.setBusinesskey(businesskey);
        //标签
        fileSystem.setFiletag(filetag);
        //元数据
        if(StringUtils.isNotEmpty(metadata)){
            try {
                Map map = JSON.parseObject(metadata, Map.class);
                fileSystem.setMetadata(map);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //名称
        fileSystem.setFileName(file.getOriginalFilename());
        //大小
        fileSystem.setFileSize(file.getSize());
        //文件类型
        fileSystem.setFileType(file.getContentType());
        fileSystemRepository.save(fileSystem);
        return new UploadFileResult(CommonCode.SUCCESS,fileSystem);}


    public String upload(MultipartFile multipartFile) {
        String originalFilename = multipartFile.getOriginalFilename();

        String s = StringUtils.substringAfter(originalFilename, ".");

        String yyyymmdd = new SimpleDateFormat("YYYYMMdd").format(new Date());

        String s1 = RandomStringUtils.randomAlphabetic(5);

        String a = yyyymmdd + s1 + "." + s;


        // Endpoint以杭州为例，其它Region请按实际情况填写。
        endpoint = "http://oss-cn-beijing.aliyuncs.com";
        // 创建OSSClient实例。
        OSSClient ossClient = new OSSClient(endpoint, accessKeyId, accessKeySecret);
        try {
            PutObjectResult hnnw = ossClient.putObject("hnnw", "image/"+a, multipartFile.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // 关闭OSSClient。
            ossClient.shutdown();
        }
        return a;

    }
}
