package com.xinao.sync.config;

/**
 * @description:
 * @author: zwh
 * @version: 1.0
 */

public enum DataSourceEnum {
    DB1("db1"),DB2("db2");

    private String value;

    DataSourceEnum(String value){this.value=value;}

    public String getValue() {
        return value;
    }
}
