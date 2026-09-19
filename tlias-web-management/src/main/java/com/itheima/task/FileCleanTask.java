package com.itheima.task;

import com.itheima.mapper.EmpMapper;
import com.itheima.utils.AliyunOSSOperator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件清理任务类
 */
@Slf4j
@Component
public class FileCleanTask {

    @Autowired
    private EmpMapper empMapper;
    @Autowired
    private AliyunOSSOperator aliyunOSSOperator;
    
    /**
     *  -- @Scheduled(cron = "0/5 * * * * ?") // 0秒开始，每5秒执行一次
     *  -- @Scheduled(cron = "0-5 * * * * ?") // 0秒开始至5秒，每秒执行一次
     *  -- @Scheduled(cron = "0,5 * * * * ?") // 0秒 和 5秒，各执行一次
     *  -- @Scheduled(cron = "0/5 * * * * ? *") //错误; cron支持7个域，但是在springTask中只支持 6 个域
     */
    @Scheduled(cron = "0 0 2 * * ?") //每天凌晨两点执行一次
//    @Scheduled(cron = "0/10 * * * * ?")
    public void clean() throws Exception {
        log.info("文件清理任务开始执行 .... ");
        //1. 查询数据库中员工的头像对应的图片文件列表 dbFiles
        List<String> urlList = empMapper.listFiles();
        List<String> dbFiles = urlList.stream().map(url -> { // https://java422-web-ai.oss-cn-beijing.aliyuncs.com/2024/08/2689f3f0-6769-42a0-bc02-1ce2286b041e.jpg
            //将url中的这部分路径("2024/08/2689f3f0-6769-42a0-bc02-1ce2286b041e.jpg")截取出来
            try {
                return new URL(url).getPath().substring(1);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }).toList();

        //2. 查询阿里云OSS中的所有文件 ossFiles
        List<String> ossFiles = aliyunOSSOperator.listFiles();
        if(!CollectionUtils.isEmpty(ossFiles)){
            //3. 计算待删除文件(垃圾文件) - 在ossFiles中存在， 但是dbFiles中不存在的数据。
            List<String> deleteFiles = ossFiles.stream().filter(ossFile -> !dbFiles.contains(ossFile)).toList();

            //4. 删除垃圾文件
            if(!CollectionUtils.isEmpty(deleteFiles)){
                deleteFiles.forEach(file -> {
                    try {
                        log.info("{} 文件被删除 ....", file);
                        aliyunOSSOperator.deleteFile(file);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
    }

}
