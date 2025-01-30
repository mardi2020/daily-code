## Dependency Injection 따라하기

### 목표
✅ 의존성 주입 컨테이너 (MyApplicationContext) 만들기

✅ 클래스 자동 스캔 (@MyComponent), 객체 생성 및 관리

✅ 의존성 자동 주입 (@MyAutowired) 지원

✅ 싱글톤 객체 관리 (객체가 중복 생성되지 않도록)

### MyApplicationContextV1

- 실행결과: DependencyinjectionApplication, UserService, ItemService 가 빈으로 등록되었음을 확인
```text
[main] INFO org.mardi2020.dependencyinjection.config.MyApplicationContext -- Registered bean: DependencyinjectionApplication
[main] INFO org.mardi2020.dependencyinjection.config.MyApplicationContext -- Registered bean: UserService
[main] INFO org.mardi2020.dependencyinjection.config.MyApplicationContext -- Registered bean: ItemService
[main] INFO org.mardi2020.dependencyinjection.config.MyApplicationContext -- Application context loaded. Registered beans: [class org.mardi2020.dependencyinjection.DependencyinjectionApplication, class org.mardi2020.dependencyinjection.service.UserService, class org.mardi2020.dependencyinjection.service.ItemService]
[main] INFO org.mardi2020.dependencyinjection.DependencyinjectionApplication -- Application started
[main] INFO org.mardi2020.dependencyinjection.service.UserService -- Executing UserService#getHello
[main] INFO org.mardi2020.dependencyinjection.service.ItemService -- hello
```

### 등록된 bean이 singleton인지 확인 결과
```text
[Test worker] INFO org.mardi2020.dependencyinjection.config.MyApplicationContextV1 -- Registered bean: ItemService
[Test worker] INFO org.mardi2020.dependencyinjection.config.MyApplicationContextV1 -- Registered bean: DependencyinjectionApplication
[Test worker] INFO org.mardi2020.dependencyinjection.config.MyApplicationContextV1 -- Registered bean: UserService
[Test worker] INFO org.mardi2020.dependencyinjection.config.MyApplicationContextV1 -- Application context loaded. Registered beans: [class org.mardi2020.dependencyinjection.service.ItemService, class org.mardi2020.dependencyinjection.DependencyinjectionApplication, class org.mardi2020.dependencyinjection.service.UserService]
[Test worker] INFO org.mardi2020.dependencyinjection.BeanTest -- ✅ Singleton Test Passed! All beans are singleton.
```