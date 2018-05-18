package com.dogcamera.utils;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class BuildProperties {

    private static BuildProperties instance;

    public static BuildProperties getInstance() throws IOException {
        if (instance == null) {
            instance = new BuildProperties();
        }
        return instance;
    }

    private final Properties properties;

    private BuildProperties() throws IOException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(new File(Environment.getRootDirectory(), "build.prop"));
            properties = new Properties();
            properties.load(fis);
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
    }

    public boolean containsKey(final Object key) {
        return properties.containsKey(key);
    }

    public boolean containsValue(final Object value) {
        return properties.containsValue(value);
    }

    public String getProperty(final String name) {
        return properties.getProperty(name);
    }

    public String getProperty(final String name, final String defaultValue) {
        return properties.getProperty(name, defaultValue);
    }

    public Set<Map.Entry<Object, Object>> entrySet() {
        return properties.entrySet();
    }

    public boolean isEmpty() {
        return properties.isEmpty();
    }

    public Enumeration keys() {
        return properties.keys();
    }

    public Set keySet() {
        return properties.keySet();
    }

    public int size() {
        return properties.size();
    }

    public Collection values() {
        return properties.values();
    }
}

