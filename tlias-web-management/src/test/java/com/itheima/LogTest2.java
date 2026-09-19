package com.itheima;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogTest2 {
    //定义日志记录器 - 固定代码
    private static final Logger log = LoggerFactory.getLogger(LogTest2.class);

    @Test
    public void testLog(){
        log.warn("warn message ....");
        log.error("error message ....");

        log.trace("trace message ....");

        log.debug("debug message ....");
        log.info("info message .....");
    }

}
