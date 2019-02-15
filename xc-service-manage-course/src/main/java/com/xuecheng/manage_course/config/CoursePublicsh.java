package com.xuecheng.manage_course.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "course-publish")
public class CoursePublicsh {
    String siteId;
    String templateId;
    String previewUrl;
    String pageWebPath;
    String pagePhysicalPath;
    String dataUrlPre;

}
