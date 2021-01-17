package com.lagou.util;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * 自定义类加载器
 * 定义自已的类加载器分为两步：
 * 1、继承java.lang.Class
 * 2、重写父类的findClass方法
 */
public class MiniCatClassLoader extends ClassLoader {
    @Override
    public Class<?> findClass(String classPath) throws ClassNotFoundException {
        try (InputStream in = new FileInputStream(classPath)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int i = 0;
            while ((i = in.read()) != -1) {
                out.write(i);
            }
            byte[] byteArray = out.toByteArray();
            return defineClass(byteArray, 0, byteArray.length);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;

    }
}
