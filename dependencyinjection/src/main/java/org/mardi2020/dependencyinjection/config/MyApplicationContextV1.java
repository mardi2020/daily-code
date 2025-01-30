package org.mardi2020.dependencyinjection.config;

import lombok.extern.slf4j.Slf4j;
import org.mardi2020.dependencyinjection.annotation.MyAutowired;
import org.mardi2020.dependencyinjection.annotation.MyComponent;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.*;

/**
 * 패키지 내 @MyComponent 가 붙은 클래스를 찾아 자동으로 bean 등록
 * 필드에 @MyAutowired 가 붙어있다면 자동으로 의존성 주입
 */
@Slf4j
public class MyApplicationContextV1 {

    private final Map<Class<?>, Object> beans = new HashMap<>();

    /**
     * 1️⃣ 지정된 패키지에서 `@MyComponent`가 붙은 클래스를 찾아 등록
     * 2️⃣ `@MyAutowired`가 붙은 필드에 의존성 주입 수행
     * @param basePackage 스캔할 패키지 명 ex) "org.mardi2020.~~"
     */
    public MyApplicationContextV1(final String basePackage) {
        try {
            Set<Class<?>> componentClasses = findClasses(basePackage);
            log.info("Found {} component classes in {}", componentClasses, basePackage);

            // ✅ @MyComponent 가 붙은 클래스를 자동 등록
            for (Class<?> clazz : componentClasses) {
                registerBean(clazz);
            }

            // ✅ 필드 의존성 자동 주입
            injectDependencies();
            log.info("Application context loaded. Registered beans: {}", beans.keySet());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * @implSpec bean 가져오기
     * @param clazz 찾을 클래스의 타입 (제네릭)
     * @return 등록된 Bean(객체)을 반환, 존재하지 않으면 `null` 반환
     * @param <T> 제네릭 타입 (클래스 타입에 맞춰서)
     */
    public <T> T getBean(Class<T> clazz) {
        return clazz.cast(beans.get(clazz));
    }

    /**
     * @implSpec 하위 패키지까지 포함하여 모든 `@MyComponent` 클래스를 찾음
     * @param basePackage 기본 패키지 경로 ("org.mardi2020.dependencyinjection")
     * @return @MyComponent 가 붙은 클래스 목록
     * @throws IOException 리소스 접근 예외
     */
    private Set<Class<?>> findClasses(String basePackage) throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String path = basePackage.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(path);
        Set<Class<?>> classes = new HashSet<>();

        while (resources.hasMoreElements()) {
            File directory = new File(resources.nextElement().getFile());
            if (directory.exists()) {
                classes.addAll(findClassesInDirectory(directory, basePackage));
            }
        }
        return classes;
    }

    /**
     * @implSpec 주어진 디렉토리 내부의 `.class` 파일을 모두 찾아서 클래스 로드
     * @param directory 탐색할 디렉토리
     * @param packageName 현재 패키지명
     * @return @MyComponent 가 붙은 클래스 목록
     */
    private Set<Class<?>> findClassesInDirectory(File directory, String packageName) {
        Set<Class<?>> classes = new HashSet<>();
        File[] files = directory.listFiles();

        if (files == null) return classes;

        for (File file : files) {
            if (file.isDirectory()) {
                // ✅ 하위 디렉토리 재귀 탐색
                classes.addAll(findClassesInDirectory(file, packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class")) {
                try {
                    String className = packageName + "." + file.getName().replace(".class", "");
                    Class<?> clazz = Class.forName(className);
                    if (clazz.isAnnotationPresent(MyComponent.class)) {
                        classes.add(clazz);
                    }
                } catch (ClassNotFoundException e) {
                    log.error("Class not found: {}", file.getName(), e);
                }
            }
        }
        return classes;
    }

    /**
     * @implSpec 클래스 파일이면 로드하고 @MyComponent 가 있으면 Bean 으로 등록
     * @param clazz class
     */
    public void registerBean(Class<?> clazz) {
        if (beans.containsKey(clazz)) {
            log.warn("⚠️ Warning: {} is already registered as singleton! Skipping registration.", clazz.getSimpleName());
            return;
        }
        try {
            Object instance = clazz.getDeclaredConstructor().newInstance();
            beans.put(clazz, instance);
            log.info("Registered bean: {}", clazz.getSimpleName());
        } catch (Exception e) {
            log.error("Failed to register bean: {}", clazz.getSimpleName(), e);
        }
    }

    /**
     * @implSpec @MyAutowired 가 붙은 필드를 찾아 의존성 주입
     * - `beans` map 에서 해당 타입의 객체를 찾아 주입
     * - Reflection 을 사용하여 private 필드에도 접근 가능
     */
    private void injectDependencies() {
        for (Object bean : beans.values()) {
            for (Field field : bean.getClass().getDeclaredFields()) {
                if (!field.isAnnotationPresent(MyAutowired.class)) { // ✅ @MyAutowired 가 있는 필드만 주입
                    continue;
                }
                field.setAccessible(true); // ✅ private 필드 접근 허용
                Object dependency = beans.get(field.getType()); // ✅ 주입할 객체 가져오기
                if (dependency == null) {
                    continue;
                }
                try {
                    field.set(bean, dependency);
                } catch (IllegalAccessException e) { // 필드 접근 불가
                    log.error("Failed to inject dependency for field: {}", field.getName(), e);
                }
            }
        }
    }
}
