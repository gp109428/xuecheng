package com.xuecheng.learning.client;



import com.xuecheng.framework.domain.media.TeachplanMediaPub;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


@FeignClient(value = "xc-service-search")
public interface CourseSearchClient {
	@GetMapping(value="/getmedia/{teachplanId}")
	public TeachplanMediaPub getmedia(@PathVariable("teachplanId") String teachplanId);
}