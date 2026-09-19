package com.itheima;

import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;

public class UrlTest {

    @Test
    public void testUrl() throws Exception {
        String urlStr = "https://java422-web-ai.oss-cn-beijing.aliyuncs.com/2024/08/2689f3f0-6769-42a0-bc02-1ce2286b041e.jpg";
        URL url = new URL(urlStr);
        String path = url.getPath();
        System.out.println(path.substring(1));
    }

}
