package com.xuecheng.manage_cms.service;

import com.alibaba.fastjson.JSON;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.xuecheng.framework.domain.cms.CmsPage;
import com.xuecheng.framework.domain.cms.CmsSite;
import com.xuecheng.framework.domain.cms.CmsTemplate;
import com.xuecheng.framework.domain.cms.request.QueryPageRequest;
import com.xuecheng.framework.domain.cms.response.CmsCode;
import com.xuecheng.framework.domain.cms.response.CmsPageResult;
import com.xuecheng.framework.domain.cms.response.CmsPostPageResult;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.QueryResult;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.manage_cms.config.RabbitmqConfig;
import com.xuecheng.manage_cms.controller.CmsSiteRepository;
import com.xuecheng.manage_cms.dao.CmsPageRepository;
import com.xuecheng.manage_cms.dao.CmsTemplateRepository;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class PageService {
    @Autowired
    private CmsPageRepository cmsPageRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private CmsTemplateRepository cmsTemplateRepository;

    @Autowired
    private GridFsTemplate gridFsTemplate;

    @Autowired
    private  GridFSBucket gridFSBucket;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private CmsSiteRepository cmsSiteRepository;
    /**
     * 页面列表分页查询
     * @param page 当前页码
     * @param size 页面显示个数
     * @param queryPageRequest 查询条件
     * @return 页面列表
     */
    public QueryResponseResult findList(int page,int size,QueryPageRequest queryPageRequest){
        //防止空指针,事先判定
        if (queryPageRequest==null){
             queryPageRequest = new QueryPageRequest();
        }
        PageRequest pageRequest = PageRequest.of(page - 1, size); //分页
        //Page<CmsPage> list = cmsPageRepository.findAll(pageRequest);//查询全部
        CmsPage cmsPage = new CmsPage();
        //站点ID
        if(StringUtils.isNotEmpty(queryPageRequest.getSiteId())){
            cmsPage.setSiteId(queryPageRequest.getSiteId());
        }
        //页面别名
        if(StringUtils.isNotEmpty(queryPageRequest.getPageAliase())){
            cmsPage.setPageAliase(queryPageRequest.getPageAliase());
        }
        //模板ID
        if (StringUtils.isEmpty(queryPageRequest.getTemplateId())){
            cmsPage.setTemplateId(queryPageRequest.getTemplateId());
        }
        //条件匹配器  设置别名为模糊查询
        ExampleMatcher matcher =ExampleMatcher.matching()
                .withMatcher("pageAliase",ExampleMatcher.GenericPropertyMatchers.contains());
        Example<CmsPage> example = Example.of(cmsPage,matcher);
        Page<CmsPage> list = cmsPageRepository.findAll(example, pageRequest);
        QueryResult result = new QueryResult();
        result.setList(list.getContent());//设置内容
        result.setTotal(list.getTotalElements());//设置总个数
        return new QueryResponseResult(CommonCode.SUCCESS,result);
    }
    public CmsPage findById(String id){
        Optional<CmsPage> one = cmsPageRepository.findById(id);
        if (one.isPresent()){
            return one.get();
        }
        return null;
    }

    //增加页面
    public CmsPageResult add(CmsPage cmsPage){
        CmsPage one = cmsPageRepository.findByPageNameAndAndPageWebPathAndSiteId(cmsPage.getPageName(), cmsPage.getPageWebPath(), cmsPage.getSiteId());
        if(one!=null){  //检验是否有添加
            ExceptionCast.Cast(CmsCode.CMS_ADDPAGE_EXISTSNAME);
        }
        cmsPage.setPageId(null);//初始化ID
        cmsPageRepository.save(cmsPage);
        return new CmsPageResult(CommonCode.SUCCESS,cmsPage);
    }

    //修改页面
    public CmsPageResult edit(String id, CmsPage cmsPage) {
        CmsPage one = findById(id);
        if (one ==null){
            ExceptionCast.Cast(CmsCode.CMS_GENERATEHTML_DATAURLISNULL);
        }
        //更新模板id
        one.setTemplateId(cmsPage.getTemplateId());
        //更新所属站点
        one.setSiteId(cmsPage.getSiteId());
        //更新页面别名
        one.setPageAliase(cmsPage.getPageAliase());
        //更新页面名称
        one.setPageName(cmsPage.getPageName());
        //更新访问路径
        one.setPageWebPath(cmsPage.getPageWebPath());
        //更新物理路径
        one.setPagePhysicalPath(cmsPage.getPagePhysicalPath());
        //更新dataUrl
        one.setDataUrl(cmsPage.getDataUrl());

        CmsPage save = cmsPageRepository.save(one);
        if (save ==null){
            ExceptionCast.Cast(CmsCode.CMS_GENERATEHTML_DATAURLISNULL);
        }
        return new CmsPageResult(CommonCode.SUCCESS, save);
    }

    public ResponseResult dele(String id) {
        CmsPage one = this.findById(id);
        if(one!=null){
            //删除页面
            cmsPageRepository.deleteById(id);
            return new ResponseResult(CommonCode.SUCCESS);
        }
        return new ResponseResult(CommonCode.FAIL);
    }

    /**
     *
     * 2,获取模型数据
     * 3.获取模板
     * 执行静态化
     * @param pageId
     * @return
     */
    public String getPageHtml(String pageId){

        //获取页面模型数据
        Map model=getDataUrl(pageId);
        if(model==null){
            ExceptionCast.Cast(CmsCode.CMS_GENERATEHTML_DATAISNULL);
        }
        //获取模板
        String templateContent = getTemplateByPageId(pageId);
        if (StringUtils.isEmpty(templateContent)){
            ExceptionCast.Cast(CmsCode.CMS_GENERATEHTML_TEMPLATEISNULL);
        }
        //执行静态化
        String html = generateHtml(templateContent, model);
        if (StringUtils.isEmpty(html)){
            ExceptionCast.Cast(CmsCode.CMS_COURSE_PERVIEWISNULL);
        }
        return html;

    }

    //执行静态化
    private String generateHtml(String templateContent, Map model) {

        try {
            //生成配置类
            Configuration configuration = new Configuration(Configuration.getVersion());
            //模板加载器
            StringTemplateLoader stringTemplateLoader = new StringTemplateLoader();
            stringTemplateLoader.putTemplate("template",templateContent);
            //配置模板加载器
            configuration.setTemplateLoader(stringTemplateLoader);
            //获取模板
            Template template1 = configuration.getTemplate("template");
            String html = FreeMarkerTemplateUtils.processTemplateIntoString(template1, model);
            return html;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //获取模板
    private String getTemplateByPageId(String pageId) {
        CmsPage page = this.findById(pageId);
        if(page == null){
            //页面不存在
            ExceptionCast.Cast(CmsCode.CMS_CMS_PAGE_NOTEXISTS);
        }
        String id = page.getTemplateId();//获取模板id
        Optional<CmsTemplate> id1 = cmsTemplateRepository.findById(id);
        if (!id1.isPresent()){
            ExceptionCast.Cast(CmsCode.CMS_GENERATEHTML_TEMPLATEISNULL);
        }
        CmsTemplate cmsTemplate = id1.get();//获取模板实体类
        String templateFileId = cmsTemplate.getTemplateFileId();
        //取出模板文件内容
        GridFSFile one = gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(templateFileId)));
        //打开下载流
        GridFSDownloadStream stream = gridFSBucket.openDownloadStream(one.getObjectId());
        //创建GridFsResource
        GridFsResource fsResource = new GridFsResource(one,stream);
        try {
            //转换成String格式返回
            String content = IOUtils.toString(fsResource.getInputStream(), "utf-8");
            return content;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    //获取模型数据
    private Map getDataUrl(String pageId) {
        CmsPage page = this.findById(pageId);
        if(page == null){
            //页面不存在
            ExceptionCast.Cast(CmsCode.CMS_CMS_PAGE_NOTEXISTS);
        }
        String url = page.getDataUrl();
        if(StringUtils.isEmpty(url)){
            ExceptionCast.Cast(CmsCode.CMS_GENERATEHTML_DATAURLISNULL);
        }
        ResponseEntity<Map> forEntity = restTemplate.getForEntity(url, Map.class);
        Map body = forEntity.getBody();

        return body;
    }

    //页面发布
    public ResponseResult postPage(String pageId){
        //执行静态化
        String html = this.getPageHtml(pageId);
        if (StringUtils.isEmpty(html)){
            ExceptionCast.Cast(CmsCode.CMS_GENERATEHTML_SAVEHTMLERROR);
        }
        //存入grid,更新cmsfeilid
        CmsPage cmsPage= saveHtml(pageId,html);
        //发送消息
        sendPostPage(cmsPage);
        return new ResponseResult(CommonCode.SUCCESS);
    }

    //发送消息mq
    private void sendPostPage(CmsPage cmsPage) {
        Map<String,String> msgMap = new HashMap<>();
        msgMap.put("pageId",cmsPage.getPageId());
        //消息内容
        String msg = JSON.toJSONString(msgMap);
        rabbitTemplate.convertAndSend(RabbitmqConfig.EX_ROUTING_CMS_POSTPAGE,cmsPage.getSiteId(),msg);
    }

    //存入grid,更新cmsfeilid
    private CmsPage saveHtml(String pageId, String html) {
        CmsPage byId = this.findById(pageId);
        if (byId==null){
            ExceptionCast.Cast(CommonCode.INVALID_PARAM);
        }
        //存储之前先删除
        String htmlFileId = byId.getHtmlFileId();
        if(StringUtils.isNotEmpty(htmlFileId)){
            gridFsTemplate.delete(Query.query(Criteria.where("_id").is(htmlFileId)));
        }
        //储存
        ObjectId store =null;
        try {
            InputStream inputStream = IOUtils.toInputStream(html, "utf-8");
            store = gridFsTemplate.store(inputStream, byId.getPageName());
        } catch (IOException e) {
            e.printStackTrace();
        }
        byId.setHtmlFileId(store.toHexString());
        CmsPage save = cmsPageRepository.save(byId);
        return save;
    }

    //添加页面，如果已存在则更新页面
    public CmsPageResult save(CmsPage cmsPage) {
        //校验页面是否存在，根据页面名称、站点Id、页面webpath查询
        CmsPage cmsPage1 = cmsPageRepository.findByPageNameAndAndPageWebPathAndSiteId(cmsPage.getPageName(), cmsPage.getPageWebPath(), cmsPage.getSiteId());
        if(cmsPage1 !=null){
            //更新
            return this.edit(cmsPage1.getPageId(),cmsPage);
        }else{
            //添加
            return this.add(cmsPage);
        }
    }

    //一键发布课程详情
    public CmsPostPageResult postPageQuick(CmsPage cmsPage) {
        CmsPageResult save = save(cmsPage);
        CmsPage cmsPage1 = save.getCmsPage();
        if (!save.isSuccess()){
            return new CmsPostPageResult(CommonCode.FAIL,null);
        }
        //页面发布
        ResponseResult responseResult = this.postPage(cmsPage1.getPageId());
        if(!responseResult.isSuccess()){
            return new CmsPostPageResult(CommonCode.FAIL,null);
        }
        //得到页面的url
        //页面url=站点域名+站点webpath+页面webpath+页面名称
        CmsSite cmsSite = findCmsSiteById(cmsPage1.getSiteId());
        if (cmsSite==null){
            return new CmsPostPageResult(CommonCode.FAIL,null);
        }

        String url=cmsSite.getSiteDomain()+cmsSite.getSiteWebPath()+cmsPage1.getPageWebPath()+cmsPage1.getPageName();

         return new CmsPostPageResult(CommonCode.SUCCESS,url);
    }
    //根据id查询站点信息
    public CmsSite findCmsSiteById(String siteId){
        Optional<CmsSite> optional = cmsSiteRepository.findById(siteId);
        if(optional.isPresent()){
            return optional.get();
        }
        return null;
}
}
