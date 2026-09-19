package com.itheima;

import com.itheima.mapper.EmpMapper;
import com.itheima.utils.AliyunOSSOperator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class TliasWebManagementApplicationTests {

    @Autowired
    private AliyunOSSOperator aliyunOSSOperator;

    @Test
    public void testListFiles() throws Exception {
        List<String> listFiles = aliyunOSSOperator.listFiles();
        listFiles.stream().forEach(s -> {
            System.out.println(s);
        });
    }

    @Test
    public void testDelFile() throws Exception {
        aliyunOSSOperator.deleteFile("2024/08/1fb9ece8-0328-4d17-8100-fd3eb5e5e417.jpg");
    }
}
