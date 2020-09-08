package com.xinao.sync.controller;

import com.alibaba.fastjson.JSONObject;
import com.xinao.sync.service.XinAoDataService;
import com.xinao.sync.service.gas.GasUseTypeService;
import com.xinao.sync.service.xinao.GastypeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @title: IndexController
 * @description: index
 * @date: 2020/9/4
 * @author: zwh
 * @copyright: Copyright (c) 2020
 * @version: 1.0
 */
@RestController
@Slf4j
public class IndexController {
    @Autowired
    GastypeService gastypeService;
    @Autowired
    GasUseTypeService useTypeService;
    @Autowired
    XinAoDataService dataService;

    /**
     * @description: 测试多数据源
     * @date: 2020/9/7
     * @author: zwh
     */
    @RequestMapping("/index")
    public String index(){
        try {
            gastypeService.list();
            useTypeService.getList();
            return "success";
        } catch (Exception e) {
            log.error("异常:{}",e.getMessage());
            return "error";
        }
    }

    /**
     * @description: 同步接口
     * @date: 2020/9/7
     * @author: zwh
     */
    @RequestMapping("/sync")
    public String sync(Integer id){
        long start = System.currentTimeMillis() / 1000;
        String result=dataService.dataSync(id);
        long end = System.currentTimeMillis() / 1000;
        return "<hr/>"+result+"<hr/>"+String.format("耗时:%d秒",end-start);
    }
}
