package com.oracle.oc.saas;

/**
 * @author ke.xiong@oracle.com
 * @version 1.0
 * @date 2020-11-13 14:01
 */
public class KeyValue {
    private String key;
    private String value;

    public String getKey() {
        return key;
    }

    public KeyValue setKey(String key) {
        this.key = key;
        return this;
    }

    public String getValue() {
        return value;
    }

    public KeyValue setValue(String value) {
        this.value = value;
        return this;
    }

    public KeyValue(String key, String value) {
        this.key = key;
        this.value = value;
    }
}
