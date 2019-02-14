package com.xuecheng.manage_course.service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.xuecheng.framework.domain.course.CourseBase;
import com.xuecheng.framework.domain.course.CourseMarket;
import com.xuecheng.framework.domain.course.CoursePic;
import com.xuecheng.framework.domain.course.Teachplan;
import com.xuecheng.framework.domain.course.ext.CategoryNode;
import com.xuecheng.framework.domain.course.ext.CourseInfo;
import com.xuecheng.framework.domain.course.ext.TeachplanNode;
import com.xuecheng.framework.domain.course.request.CourseListRequest;
import com.xuecheng.framework.domain.course.response.AddCourseResult;
import com.xuecheng.framework.domain.course.response.CourseCode;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.QueryResult;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.manage_course.dao.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CourseService {

    @Autowired
    TeachplanMapper teachplanMapper;

    @Autowired
    TeachplanRepository teachplanRepository;

    @Autowired
    CourseBaseRepository courseBaseRepository;

    @Autowired
    CourseMapper courseMapper;

    @Autowired
    CategoryMapper categoryMapper;
    @Autowired
    CourseMarketRepository courseMarketRepository;
    @Autowired
    CoursePicRepository coursePicRepository;

    //查询课程计划
    public TeachplanNode findTeachplanList(String courseId){
        return teachplanMapper.selectList(courseId);
    }
    //添加课程计划
    @Transactional
    public ResponseResult addTeachplan(Teachplan teachplan){
        //校验课程id和课程计划名称
        if(teachplan == null ||
                StringUtils.isEmpty(teachplan.getCourseid()) ||
                StringUtils.isEmpty(teachplan.getPname())){
            ExceptionCast.Cast(CommonCode.INVALID_PARAM);
        }
        //取出课程id
        String courseid = teachplan.getCourseid();
        //取出父结点id
        String parentid = teachplan.getParentid();
        if (StringUtils.isEmpty(parentid)){
            parentid=getTeachplanRoot(courseid);
        }
        //取出父结点信息
        Optional<Teachplan> teachplanOptional = teachplanRepository.findById(parentid);
        if(!teachplanOptional.isPresent()){
            ExceptionCast.Cast(CommonCode.INVALID_PARAM);
        }
        //父结点
        Teachplan teachplanParent = teachplanOptional.get();
        //父结点级别
        String parentGrade = teachplanParent.getGrade();
        //设置父结点
        teachplan.setParentid(parentid);
        teachplan.setStatus("0");//未发布
        //子结点的级别，根据父结点来判断
        teachplan.setGrade(parentGrade.equals("1")?"2":"3");
        //设置课程id
        teachplan.setCourseid(teachplanParent.getCourseid());
        teachplanRepository.save(teachplan);
        return new ResponseResult(CommonCode.SUCCESS);
    }

    private String getTeachplanRoot(String courseid) {
        //查询顶级节点信息
        List<Teachplan> teachplans = teachplanRepository.findByCourseidAndParentid(courseid, "0");
        if (teachplans==null||teachplans.size()<=0){
            //查询课程基本信息
            Optional<CourseBase> id = courseBaseRepository.findById(courseid);
            if (!id.isPresent()){
                ExceptionCast.Cast(CommonCode.INVALID_PARAM);
            }
            //获取课程基本信息
            CourseBase courseBase = id.get();
            //新增一个根结点
            Teachplan teachplanRoot = new Teachplan();
            teachplanRoot.setCourseid(courseid);
            teachplanRoot.setPname(courseBase.getName());
            teachplanRoot.setParentid("0");
            teachplanRoot.setGrade("1");//1级
            teachplanRoot.setStatus("0");//未发布
            teachplanRepository.save(teachplanRoot);
            return teachplanRoot.getId();
        }
        Teachplan teachplan = teachplans.get(0);
        return teachplan.getId();
    }

    //查询我的课程
    public QueryResponseResult findList(int page, int size, CourseListRequest courseListRequest){
        if(courseListRequest == null){
            //校验查询条件
            courseListRequest = new CourseListRequest();
        }
        if(page<=0){
            page = 0;
        }
        if(size<=0){
            size = 20;
        }
        //设置分页参数
        PageHelper.startPage(page,size);
        Page<CourseInfo> courseListPage = courseMapper.findCourseListPage(courseListRequest);
        QueryResult resou=new QueryResult();
        resou.setList(courseListPage.getResult());
        resou.setTotal(courseListPage.getTotal());
        return new QueryResponseResult(CommonCode.SUCCESS,resou);
    }

    //查询分类信息
    public CategoryNode findCategoryList(){
        CategoryNode categoryNode = categoryMapper.selectList();
        return categoryNode;
    }
    //添加课程基本信息
    public AddCourseResult addCourse(CourseBase courseBase){
        //校验必填项
        if (StringUtils.isEmpty(courseBase.getName())||StringUtils.isEmpty(courseBase.getStudymodel())){
            ExceptionCast.Cast(CommonCode.INVALID_PARAM);
        }
        //课程状态默认为未发布
        courseBase.setStatus("202001");
        courseBaseRepository.save(courseBase);
        return new AddCourseResult(CommonCode.SUCCESS,courseBase.getId());
    }

    //根据课程ID查询课程基本信息
    public CourseBase findByid(String courseId) {
        Optional<CourseBase> id = courseBaseRepository.findById(courseId);
        if (id.isPresent()){
            return id.get();
        }
        return null;
    }

    //更新课程基本信息
    @Transactional
    public ResponseResult saveBase( String id, CourseBase courseBase){
        CourseBase one = this.findByid(id);
        if(one == null){
            //抛出异常
            ExceptionCast.Cast(CommonCode.INVALID_PARAM);
        }
        //修改课程信息
        one.setName(courseBase.getName());
        one.setMt(courseBase.getMt());
        one.setSt(courseBase.getSt());
        one.setGrade(courseBase.getGrade());
        one.setStudymodel(courseBase.getStudymodel());
        one.setUsers(courseBase.getUsers());
        one.setDescription(courseBase.getDescription());
        CourseBase save = courseBaseRepository.save(one);
        return new ResponseResult(CommonCode.SUCCESS);
    }

    //根据ID查询课程营销信息
    public CourseMarket getCourseMarketById(String courseId) {
        Optional<CourseMarket> id = courseMarketRepository.findById(courseId);
        if (!id.isPresent()){
            return null;
        }
        return id.get();
    }

    //更新营销计划,如果没有就创建营销计划
    @Transactional
    public ResponseResult updateCourseMarket(String id, CourseMarket courseMarket) {
        CourseMarket one = this.getCourseMarketById(id);
        if (one==null){
            //添加课程营销信息
            one = new CourseMarket();
            BeanUtils.copyProperties(courseMarket, one);
            //设置课程id
            one.setId(id);
            courseMarketRepository.save(one);
        }else {
            one.setCharge(courseMarket.getCharge());
            one.setStartTime(courseMarket.getStartTime());//课程有效期，开始时间
            one.setEndTime(courseMarket.getEndTime());//课程有效期，结束时间
            one.setPrice(courseMarket.getPrice());
            one.setQq(courseMarket.getQq());
            one.setValid(courseMarket.getValid());
            courseMarketRepository.save(one);
        }
        return new ResponseResult(CommonCode.SUCCESS);
    }


    //添加课程图片
    @Transactional
    public ResponseResult saveCoursePic(String courseId,String pic){
        //查询课程图片
        Optional<CoursePic> picOptional = coursePicRepository.findById(courseId);
        CoursePic coursePic = null;
        if(picOptional.isPresent()){
            coursePic = picOptional.get();
        }
        //没有课程图片则新建对象
        if(coursePic == null){
            coursePic = new CoursePic();
        }
        coursePic.setCourseid(courseId);
        coursePic.setPic(pic);
        //保存课程图片
        coursePicRepository.save(coursePic);
        return new ResponseResult(CommonCode.SUCCESS);
    }

    //查询根据ID课程图片
    public CoursePic findCoursepic(String courseId) {
        Optional<CoursePic> byId = coursePicRepository.findById(courseId);
        if (!byId.isPresent()){
            return null;
        }
        return byId.get();
    }


    //删除课程图片
    @Transactional
    public ResponseResult deleteCoursePic(String courseId) {
        //执行删除
        coursePicRepository.deleteById(courseId);
        return new ResponseResult(CommonCode.SUCCESS);
    }
}
