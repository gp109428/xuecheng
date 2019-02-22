package com.xuecheng.manage_media.service;

import com.alibaba.fastjson.JSON;
import com.xuecheng.framework.domain.media.MediaFile;
import com.xuecheng.framework.domain.media.response.CheckChunkResult;
import com.xuecheng.framework.domain.media.response.MediaCode;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.manage_media.config.RabbitMQConfig;
import com.xuecheng.manage_media.dao.MediaFileRepository;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;

@Service
public class MediaUploadService {
    @Autowired
    MediaFileRepository mediaFileRepository;

    //上传文件根目录
    @Value("${xc-service-manage-media.upload-location}")
    String uploadPath;

    @Value("${xc-service-manage-media.mq.routingkey-media-video}")
    String routingkey;
    @Autowired
    RabbitTemplate rabbitTemplate;


    //文件注册
    public ResponseResult register(String fileMd5, String fileName, Long fileSize, String mimetype, String fileExt) {

        //获取文件路径
        String filePath = this.getFilePath(fileMd5, fileExt);
        File file = new File(filePath);

        //2、查询数据库文件是否存在
        Optional<MediaFile> optional = mediaFileRepository.findById(fileMd5);
        if (optional.isPresent()&&file.exists()){
            ExceptionCast.Cast(MediaCode.UPLOAD_FILE_REGISTER_EXIST); //如果都存在就抛出异常返回
        }
        boolean fileFold = createFileFold(fileMd5);
        if(!fileFold){
            //上传文件目录创建失败
            ExceptionCast.Cast(MediaCode.UPLOAD_FILE_REGISTER_CREATEFOLDER_FAIL);
        }
        return new ResponseResult(CommonCode.SUCCESS);

    }

    //得到文件所在目录
    private String getFileFolderPath(String fileMd5){
        String fileFolderPath = uploadPath+ fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/" ;
        return fileFolderPath;
    }


    //根据文件md5创建目录
    private boolean createFileFold(String fileMd5) {
        String fileFolderPath = this.getFileFolderPath(fileMd5);
        File file = new File(fileFolderPath);
        if (!file.exists()){
            boolean mkdirs = file.mkdirs();
            return mkdirs;
        }
        return true;
    }
    //获取文件路径
    /**
     * 根据文件md5得到文件路径
     * 规则：
     * 一级目录：md5的第一个字符
     * 二级目录：md5的第二个字符
     * 三级目录：md5
     * 文件名：md5+文件扩展名
     * @param fileMd5 文件md5值
     * @param fileExt 文件扩展名
     * @return 文件路径
     */
    private String getFilePath(String fileMd5,String fileExt){
        return uploadPath+fileMd5.substring(0,1)+"/"+fileMd5.substring(1,2)+"/"+fileMd5+"/"+fileMd5+"."+fileExt;
    }


    //检查分块
    public CheckChunkResult checkchunk(String fileMd5, Integer chunk, Integer chunkSize) {
        //获取分块路径
        String chunkFileFolderPath = this.getChunkFileFolderPath(fileMd5);
        //拼接分块文件
        File file = new File(chunkFileFolderPath+chunk);
        if (file.exists()){
            return new CheckChunkResult(MediaCode.CHUNK_FILE_EXIST_CHECK,true);
        }
        return new CheckChunkResult(MediaCode.CHUNK_FILE_EXIST_CHECK,false);
    }

    //获取分块路径
    private String getChunkFileFolderPath(String fileMd5) {
        return this.getFileFolderPath(fileMd5)+"chunks/";   //文件目录下的chunks就是分块目录
    }

    public ResponseResult uploadchunk(MultipartFile file, String fileMd5, Integer chunk) {
        if(file == null){
            ExceptionCast.Cast(MediaCode.UPLOAD_FILE_REGISTER_ISNULL);
        }
        //创建块文件目录
        boolean fileFold = createChunkFileFolder(fileMd5);
        //块文件
        File chunkfile = new File(getChunkFileFolderPath(fileMd5) + chunk);

        FileOutputStream fileOutputStream = null;
        InputStream inputStream=null;
        try {
             inputStream = file.getInputStream();
            fileOutputStream = new FileOutputStream(chunkfile);
            IOUtils.copy(inputStream, fileOutputStream);
        } catch (IOException e) {
            e.printStackTrace();
            ExceptionCast.Cast(MediaCode.CHUNK_FILE_UPLOAD_FAIL);
        }finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return new ResponseResult(CommonCode.SUCCESS);
    }

    //创建块文件目录
    private boolean createChunkFileFolder(String fileMd5) {
        //创建上传文件目录
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
        File chunkFileFolder = new File(chunkFileFolderPath);
        if (!chunkFileFolder.exists()) {
            //创建文件夹
            boolean mkdirs = chunkFileFolder.mkdirs();
            return mkdirs;
        }
        return true;
    }

    //分块合成
    public ResponseResult mergechunks(String fileMd5, String fileName, Long fileSize, String mimetype, String fileExt) {
        //获取块文件的路径
        String chunkfileFolderPath = getChunkFileFolderPath(fileMd5);
        File chunkfileFolder = new File(chunkfileFolderPath);
        if(!chunkfileFolder.exists()){
            chunkfileFolder.mkdirs();
        }
        //合并文件路径
        File mergeFile = new File(getFilePath(fileMd5,fileExt));
        //创建合并文件
        //合并文件存在先删除再创建
        if(mergeFile.exists()){
            mergeFile.delete();
        }
        boolean newFile = false;
        try {
            newFile = mergeFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(!newFile){
            ExceptionCast.Cast(MediaCode.MERGE_FILE_CHECKFAIL);
        }
        //获取块文件，此列表是已经排好序的列表
        List<File> chunkFiles = getChunkFiles(chunkfileFolder);
        //合并文件
        mergeFile = mergeFile(mergeFile, chunkFiles);
        if(mergeFile == null){
            ExceptionCast.Cast(MediaCode.MERGE_FILE_FAIL);
        }
        //校验文件
        boolean checkResult = this.checkFileMd5(mergeFile, fileMd5);
        if(!checkResult){
            ExceptionCast.Cast(MediaCode.MERGE_FILE_CHECKFAIL);
        }
        //将文件信息保存到数据库
        MediaFile mediaFile = new MediaFile();
        mediaFile.setFileId(fileMd5);
        mediaFile.setFileName(fileMd5+"."+fileExt);
        mediaFile.setFileOriginalName(fileName);
        //文件路径保存相对路径
        mediaFile.setFilePath(getFileFolderPath(fileMd5));
        mediaFile.setFileSize(fileSize);
        mediaFile.setUploadTime(new Date());
        mediaFile.setMimeType(mimetype);
        mediaFile.setFileType(fileExt);
        //状态为上传成功
        mediaFile.setFileStatus("301002");
        MediaFile save = mediaFileRepository.save(mediaFile);
        sendProcessVideoMsg(save.getFileId());
        return new ResponseResult(CommonCode.SUCCESS);




    }

    //像MQ发送视频消息
    public ResponseResult sendProcessVideoMsg(String mediaId){
        Optional<MediaFile> byId = mediaFileRepository.findById(mediaId);
        if (!byId.isPresent()){
            ExceptionCast.Cast(MediaCode.UPLOAD_FILE_REGISTER_ISNULL);
        }
        HashMap<String, String> map = new HashMap<>();
        map.put("mediaId",mediaId);
        String string = JSON.toJSONString(map);
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.EX_MEDIA_PROCESSTASK,routingkey,string);
        } catch (AmqpException e) {
            e.printStackTrace();
            return new ResponseResult(CommonCode.FAIL);
        }
        return new ResponseResult(CommonCode.SUCCESS);
    }

    //获得文件相对路径
    private String getFileFolderRelativePath(String fileMd5, String fileExt) {
        return fileMd5.substring(0,1)+"/"+fileMd5.substring(1,2)+"/"+fileMd5+"/"+fileMd5+"."+fileExt;
    }

    //校验文件的md5值
    private boolean checkFileMd5(File mergeFile, String fileMd5) {
        if (mergeFile==null|| StringUtils.isEmpty(fileMd5)){
            return false;
        }
        FileInputStream fileInputStream=null;
        try {
             fileInputStream = new FileInputStream(mergeFile);
            String md5Hex = DigestUtils.md5Hex(fileInputStream);
            fileInputStream.close();
            return md5Hex.equalsIgnoreCase(fileMd5)?true:false;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    //合并文件
    private File mergeFile(File mergeFile, List<File> chunkFiles) {
        try {
            RandomAccessFile writ_file = new RandomAccessFile(mergeFile, "rw");
            writ_file.seek(0);
            byte[] b=new byte[1024];
            for (File file : chunkFiles) {
                RandomAccessFile accessFile = new RandomAccessFile(file, "r");
                int len=-1;
                while ((len = accessFile.read(b))!=-1){
                    writ_file.write(b,0,len);
                }
                accessFile.close();
            }
            writ_file.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mergeFile;
    }

    private List<File> getChunkFiles(File chunkfileFolder) {
        File[] files = chunkfileFolder.listFiles();
        ArrayList<File> files1 = new ArrayList<>(Arrays.asList(files));
        Collections.sort(files1, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                if(Integer.parseInt(o1.getName())>Integer.parseInt(o2.getName())){
                    return 1;
                }
                return -1;
            }
        });
        return files1;
    }
}
