package com.xinao.sync.utils;

import com.alibaba.fastjson.JSONObject;
import org.nutz.lang.Lang;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @title: IndexUtil
 * @description:
 * @date: 2020/9/4
 * @author: zwh
 * @copyright: Copyright (c) 2020
 * @version: 1.0
 */

public class IndexUtil {

    public static void main(String[] args) {
        String jar_parent = "";
        try {
            jar_parent = new File(ResourceUtils.getURL("classpath:").getPath())
                    .getParentFile().getParentFile().getParent();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
