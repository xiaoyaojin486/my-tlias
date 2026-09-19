package com.itheima.controller;

import com.itheima.pojo.Result;
import com.itheima.utils.AliyunOSSOperator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Slf4j
@RestController
public class UploadController {

    /*@PostMapping("/upload")
    public Result upload(String username, Integer age, MultipartFile file) throws IOException {
        log.info("上传文件：{},{},{}",username,age,file);
        //1. 构建新的文件名
        String originalFilename = file.getOriginalFilename(); //1.1.1.1.1.jpg
        String extName = originalFilename.substring(originalFilename.lastIndexOf(".")); //.jpg
        String newFileName = UUID.randomUUID().toString() + extName;

        //2. 将file存储到 D:/images/ 目录下
        file.transferTo(new File("D:/images/" + newFileName));
        return Result.success();
    }*/

    @Autowired
    private AliyunOSSOperator aliyunOSSOperator;

    /**
     * 文件上传
     */
    @PostMapping("/upload")
    public Result upload(MultipartFile file) throws Exception {
        log.info("上传文件：{}",file.getOriginalFilename());
        //调用aliyun OSS进行文件上传
        String url = aliyunOSSOperator.upload(file.getBytes(), file.getOriginalFilename());
        //返回结果
        return Result.success(url);
    }
}
