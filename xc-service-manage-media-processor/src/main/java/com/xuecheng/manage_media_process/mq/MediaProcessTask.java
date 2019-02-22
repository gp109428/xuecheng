package com.xuecheng.manage_media_process.mq;

import com.alibaba.fastjson.JSON;
import com.xuecheng.framework.domain.media.MediaFile;
import com.xuecheng.framework.domain.media.MediaFileProcess_m3u8;
import com.xuecheng.framework.utils.HlsVideoUtil;
import com.xuecheng.framework.utils.Mp4VideoUtil;
import com.xuecheng.manage_media_process.dao.MediaFileRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class MediaProcessTask {
    //ffmpeg绝对路径
    @Value("${xc-service-manage-media.ffmpeg-path}")
    String ffmpeg_path;
    //上传文件根目录
    @Value("${xc-service-manage-media.video-location}")
    String serverPath;
    @Autowired
    MediaFileRepository mediaFileRepository;

    //得到视频ID转码
    @RabbitListener(queues = "${xc-service-manage-media.mq.queue-media-video-processor}",containerFactory = "customContainerFactory")
    public void receiveMediaProcessTask(String msg) throws IOException {
        Map map = JSON.parseObject(msg, Map.class);
        //媒资文件id
        String mediaId = (String) map.get("mediaId");
        Optional<MediaFile> byId = mediaFileRepository.findById(mediaId);
        if (!byId.isPresent()){
            return;
        }
        MediaFile mediaFile = byId.get();
        String fileType = mediaFile.getFileType();
        //判断文件类型,只处理avi格式文件
        if (StringUtils.isEmpty(fileType)||!"avi".equalsIgnoreCase(fileType)){
            mediaFile.setProcessStatus("303004");//处理状态为无需处理
            mediaFileRepository.save(mediaFile);
            return;
        }else{
            mediaFile.setProcessStatus("303001");//处理状态为未处理
            mediaFileRepository.save(mediaFile);
        }
        //生成mp4
        String video_path =mediaFile.getFilePath()+mediaFile.getFileName();
        String mp4_name = mediaFile.getFileId()+".mp4";
        String mp4folder_path = mediaFile.getFilePath();
        Mp4VideoUtil videoUtil = new Mp4VideoUtil(ffmpeg_path,video_path,mp4_name,mp4folder_path);
        String result = videoUtil.generateMp4();
        if(result == null || !result.equals("success")){
            //操作失败写入处理日志
            mediaFile.setProcessStatus("303003");//处理状态为处理失败
            MediaFileProcess_m3u8 mediaFileProcess_m3u8 = new MediaFileProcess_m3u8();
            mediaFileProcess_m3u8.setErrormsg(result);
            mediaFile.setMediaFileProcess_m3u8(mediaFileProcess_m3u8);
            mediaFileRepository.save(mediaFile);
            return ;
        }
        video_path = mediaFile.getFilePath()+mp4_name;//此地址为mp4的地址
        String m3u8_name = mediaFile.getFileId()+".m3u8";
        String m3u8folder_path = mediaFile.getFilePath()+"hls/";
        HlsVideoUtil hlsVideoUtil = new HlsVideoUtil(ffmpeg_path,video_path,m3u8_name,m3u8folder_path);
        result = hlsVideoUtil.generateM3u8();
        if(result == null || !result.equals("success")){
            //操作失败写入处理日志
            mediaFile.setProcessStatus("303003");//处理状态为处理失败
            MediaFileProcess_m3u8 mediaFileProcess_m3u8 = new MediaFileProcess_m3u8();
            mediaFileProcess_m3u8.setErrormsg(result);
            mediaFile.setMediaFileProcess_m3u8(mediaFileProcess_m3u8);
            mediaFileRepository.save(mediaFile);
            return ;
        }
        //获取m3u8列表
        List<String> ts_list = hlsVideoUtil.get_ts_list();
        //更新处理状态为成功
        mediaFile.setProcessStatus("303002");//处理状态为处理成功
        MediaFileProcess_m3u8 mediaFileProcess_m3u8 = new MediaFileProcess_m3u8();
        mediaFileProcess_m3u8.setTslist(ts_list);
        mediaFile.setMediaFileProcess_m3u8(mediaFileProcess_m3u8);
        //m3u8文件url
        mediaFile.setFileUrl(mediaFile.getFilePath()+"hls/"+m3u8_name);
        mediaFileRepository.save(mediaFile);


    }

}
