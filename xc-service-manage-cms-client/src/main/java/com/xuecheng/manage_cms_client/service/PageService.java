package com.xuecheng.manage_cms_client.service;

import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.xuecheng.framework.domain.cms.CmsPage;
import com.xuecheng.framework.domain.cms.CmsSite;
import com.xuecheng.framework.domain.cms.response.CmsCode;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.manage_cms_client.dao.CmsPageRepository;
import com.xuecheng.manage_cms_client.dao.CmsSiteRepository;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.Optional;


@Service
public class PageService {

    private static final Logger LOGGER= LoggerFactory.getLogger(PageService.class);

    @Autowired
    CmsPageRepository cmsPageRepository;

    @Autowired
    GridFSBucket gridFSBucket;

    @Autowired
    GridFsTemplate gridFsTemplate;
    @Autowired
    CmsSiteRepository cmsSiteRepository;


    //接收pageid将其页面静态化输出到本地文件
    public void savePageToServerPath(String pageId){
        //获取cmspage
        CmsPage cmsPgae = this.findCmsPgae(pageId);
        if (cmsPgae==null){
            ExceptionCast.Cast(CmsCode.CMS_CMS_PAGE_NOTEXISTS);
        }
        String htmlFileId = cmsPgae.getHtmlFileId();
        if (StringUtils.isEmpty(htmlFileId)){
            ExceptionCast.Cast(CmsCode.CMS_GENERATEHTML_DATAURLISNULL);
        }
        //获取输入流
        InputStream inputStream = this.getFileById(htmlFileId);
        if (inputStream==null){
            LOGGER.error("getFileById InputStream is null,htmlFileId:{}",htmlFileId);
        }
        //拼接路径 查询站点路径
        String stie = this.findByStie(cmsPgae.getSiteId());
        String path=stie+cmsPgae.getPagePhysicalPath()+cmsPgae.getPageName();
        //输出到本地 文件输入流 物理路径
        FileOutputStream outputStream = null;
        try {
            //输出到本地 文件输入流 物理路径
            outputStream = new FileOutputStream(new File(path));
            IOUtils.copy(inputStream,outputStream);
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //获取Cmspage
    public CmsPage findCmsPgae(String pageId){
        Optional<CmsPage> page = cmsPageRepository.findById(pageId);
        if (page.isPresent()){
            return page.get();
        }
        return null;
    }

    public InputStream getFileById(String filed){
        //获取file文件
        GridFSFile file = gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(filed)));
        //打开下载流
        GridFSDownloadStream downloadStream = gridFSBucket.openDownloadStream(file.getObjectId());
        //创建Resource
        GridFsResource resource = new GridFsResource(file, downloadStream);
        try {
            //返回输入流
            return resource.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    //查询站点路径
    public String findByStie(String stie){
        Optional<CmsSite> byId = cmsSiteRepository.findById(stie);
        if (byId.isPresent()){
            return byId.get().getSitePhysicalPath();
        }
        return null;
    }
}
