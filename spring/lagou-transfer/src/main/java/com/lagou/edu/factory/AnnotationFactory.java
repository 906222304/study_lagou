package com.lagou.edu.factory;

import com.lagou.edu.annotation.AutoWired;
import com.lagou.edu.annotation.Service;
import com.lagou.edu.annotation.Transactional;
import com.lagou.edu.utils.ConnectionUtils;
import com.lagou.edu.utils.TransactionManager;

import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class AnnotationFactory {

    private TransactionManager transactionManager = new TransactionManager();

    public AnnotationFactory() {
        transactionManager.setConnectionUtils(new ConnectionUtils());
    }

    //1.获取指定包下的所有类
    public HashMap<String,Object> getPackageClass(String packageName){

        HashMap<String,Object> hashMap = new HashMap<>();

        ArrayList<String> arrayList = new ArrayList<>();
        // 获取包的名字 并进行替换
        String packagePath = packageName.replace('.','/');
        Enumeration<URL> dirs;
        try {
            dirs = Thread.currentThread().getContextClassLoader().getResources(packagePath);
            while (dirs.hasMoreElements()) {
                //获取下一个元素
                URL url = dirs.nextElement();
                //获取协议名称
                String protocol = url.getProtocol();
                if ("file".equals(protocol)) {
                    //获取包下面的所有类
                    String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
                    arrayList = getClassByPackageFilePath(filePath, packageName, arrayList);
                }
            }
            if (arrayList.size() != 0) {
                //遍历arraylist
                for (String className : arrayList) {
                    if (!className.contains("$") && !className.contains("annotation") && !className.contains("Servlet") && !className.contains("BeanFactory")
                            && !className.contains("DruidUtils") && !className.contains("JsonUtils") && !className.contains("AnnotationFactory")) {
                        Class<?> aClass = Class.forName(className);
                        //将存在@Service注解的类存储到Map中
                        setServiceClass(hashMap, aClass);
                        //将实现了接口的类存储到Map中，便于@AutoWired注入
                        setImplementClass(hashMap, aClass);
                    }
                }
                //将存在@AutoWired注解的属性设置到对应的类中，并存储到Map中
                setAllAutoWiredClass(hashMap);
                //扫描存在@Transactional
                setTransactionalClass(hashMap);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hashMap;
    }

    private void setTransactionalClass(HashMap<String, Object> hashMap) throws ClassNotFoundException {
        if (hashMap != null) {
            for (Map.Entry<String, Object> transactionEntry : hashMap.entrySet()) {
                // 1.使用反射机制,获取当前类的所有属性
                Class<?> transactionClass = transactionEntry.getValue().getClass();
                //2.判断当前类属性是否存在注解
                Transactional transactional = transactionClass.getAnnotation(Transactional.class);
                if (transactional != null) {
                    //从容器中取出该类
                    Object transaction = transactionEntry.getValue();
                    //为有@Transactional注解的类创建代理对象
                    Object jdkProxyTransaction = Proxy.newProxyInstance(transaction.getClass().getClassLoader(), transaction.getClass().getInterfaces(),
                            new InvocationHandler() {
                                @Override
                                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                                    Object result = null;

                                    try {
                                        // 开启事务(关闭事务的自动提交)
                                        transactionManager.beginTransaction();

                                        result = method.invoke(transaction, args);

                                        // 提交事务

                                        transactionManager.commit();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        // 回滚事务
                                        transactionManager.rollback();

                                        // 抛出异常便于上层servlet捕获
                                        throw e;

                                    }

                                    return result;
                                }
                            });
                    //将创建代理对象后的类重新放入容器中
                    hashMap.put(transactionEntry.getKey(), jdkProxyTransaction);
                }
            }
        }
    }

    /**
     * 扫描所有的类，存在@AutoWired注解时进行注入
     * @param hashMap
     */
    private void setAllAutoWiredClass(HashMap<String, Object> hashMap) throws ClassNotFoundException, IllegalAccessException {

        if (hashMap != null) {
            for (Map.Entry<String, Object> objectEntry : hashMap.entrySet()) {
              /*  // 1.使用反射机制,获取当前类的所有属性
                Class<?> autoWiredClass = objectEntry.getValue().getClass();
                Field[] declaredFields = autoWiredClass.getDeclaredFields();
                //2.判断当前类属性是否存在注解
                for (Field declaredField : declaredFields) {
                    AutoWired autoWired = declaredField.getAnnotation(AutoWired.class);
                    if (autoWired != null) {
                        //获取全限定类名
                        String autoWiredName = declaredField.getType().getName();
                        //根据全限定类名从容器中取出
                        Object o = hashMap.get(autoWiredName);
                        getAutoWired(o, hashMap ,true);
                    }
                }*/
                getAutoWired(objectEntry.getValue(), hashMap, true);
            }
        }

    }

    /**
     * 迭代注入@AutoWired
     * @param object
     * @param hashMap
     * @param bollean
     * @throws ClassNotFoundException
     * @throws IllegalAccessException
     */
    private void getAutoWired(Object object, HashMap<String, Object> hashMap , Boolean bollean) throws ClassNotFoundException, IllegalAccessException {
        Class<?> autoWiredClass = object.getClass();
        Field[] declaredFields = autoWiredClass.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            AutoWired autoWired = declaredField.getAnnotation(AutoWired.class);
            if (autoWired != null) {
                bollean = true;
            } else {
                bollean = false;
            }
            //获取全限定类名
            String allAutoWiredName = declaredField.getType().getName();
            //根据全限定名从容器中取出
            Object o = hashMap.get(allAutoWiredName);
            if (o != null && bollean) {
                getAutoWired(o, hashMap ,true);
            }
            if (o != null) {
                declaredField.setAccessible(true);
                //设置@AutoWired属性到类中
                declaredField.set(object, o);
                hashMap.put(allAutoWiredName, o);
            } else if (hashMap.get(declaredField.getName()) != null && autoWired != null) {
                //全限定类名不存在但存在该类名的key值时
                declaredField.setAccessible(true);
                //设置@AutoWired属性到类中
                declaredField.set(object, hashMap.get(declaredField.getName()));
                hashMap.put(declaredField.getName(), hashMap.get(declaredField.getName()));
            }
        }
    }

    /**
     * 如果类实现接口，将接口作为key，类为value存储到Map中
     * @param hashMap
     * @param aClass
     */
    private void setImplementClass(HashMap<String, Object> hashMap, Class<?> aClass) throws IllegalAccessException, InstantiationException {
        Class<?>[] interfaces = aClass.getInterfaces();
        if (interfaces != null) {
            for (Class<?> anInterface : interfaces) {
                hashMap.put(anInterface.getName(), aClass.newInstance());
            }
        }
    }

    /**
     * 将有@service注解的类存储到map中
     * @param hashMap
     * @param aClass
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public void setServiceClass(HashMap<String, Object> hashMap, Class<?> aClass) throws InstantiationException, IllegalAccessException {
        Service serviceAnnotation = aClass.getAnnotation(Service.class);
        if (serviceAnnotation != null && serviceAnnotation.value() != null) {
            hashMap.put(serviceAnnotation.value(), aClass.newInstance());
        } else if (!aClass.isInterface()){
            String simpleName = aClass.getSimpleName();
            hashMap.put(toLowerCaseFirstOne(simpleName), aClass.newInstance());
        }
    }

    /**
     * 将首字母转换成小写
     * @param simpleName
     * @return
     */
    private String toLowerCaseFirstOne(String simpleName) {

        if(Character.isLowerCase(simpleName.charAt(0)))
            return simpleName;
        else
            return (new StringBuilder()).append(Character.toLowerCase(simpleName.charAt(0))).append(simpleName.substring(1)).toString();

    }

    /**
     * 获取路径下所有的类名
     * @param filePath
     * @param packageName
     * @param arrayList
     * @return
     */
    private ArrayList<String> getClassByPackageFilePath(String filePath, String packageName, ArrayList<String> arrayList) {

        File UrlFile = new File(filePath);
        File[] files = UrlFile.listFiles(new FileFilter() {
            //自定义过滤规则，为文件夹或者为.class结尾的文件
            @Override
            public boolean accept(File pathname) {
                return (UrlFile.isDirectory()) || (UrlFile.getName().endsWith(".class"));
            }
        });
        for (File file : files) {
            if (file.isDirectory()) {
                String path = file.getPath();
                getClassByPackageFilePath(file.getPath(),packageName + "." + file.getName(),arrayList);
            } else {
                String className = file.getName().substring(0,file.getName().length() - 6);
                arrayList.add(packageName + "." + className);
            }
        }
        return arrayList;

    }

}
