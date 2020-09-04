package com.xinao.sync.service;

import com.xinao.sync.entity.xinao.UnitType;
import com.xinao.sync.service.gas.GasAreaCommunityService;
import com.xinao.sync.service.gas.GasBookNoService;
import com.xinao.sync.service.xinao.UnitTypeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @title: XinAoDataService
 * @description: 同步服务
 * @date: 2020/9/4
 * @author: zwh
 * @copyright: Copyright (c) 2020
 * @version: 1.0
 */
@Service
@Slf4j
public class XinAoDataService {

    @Autowired
    UnitTypeService unitTypeService;

    @Autowired
    GasAreaCommunityService areaCommunityService;

    @Autowired
    GasBookNoService bookNoService;

    /**
     * @description: 同步方法
     * @date: 2020/9/4
     * @author: zwh
     */
    public void dataSync(){
        // TODO 测试改回卡  确认表类型种类

    }
}
