package org.mardi2020.dependencyinjection;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mardi2020.dependencyinjection.config.MyApplicationContextV1;
import org.mardi2020.dependencyinjection.service.ItemService;
import org.mardi2020.dependencyinjection.service.UserService;

import static org.junit.jupiter.api.Assertions.assertSame;

@Slf4j
public class BeanTest {

    @Test
    @DisplayName("Bean 이 싱글톤인지 확인한다")
    void singleton_bean_test() {
        // 1️⃣ ApplicationContext 생성
        MyApplicationContextV1 context = new MyApplicationContextV1("org.mardi2020.dependencyinjection");

        // 2️⃣ 같은 타입의 Bean 을 여러 번 가져오기
        UserService userService1 = context.getBean(UserService.class);
        UserService userService2 = context.getBean(UserService.class);

        ItemService itemService1 = context.getBean(ItemService.class);
        ItemService itemService2 = context.getBean(ItemService.class);

        // 3️⃣ 동일한 객체인지 확인 (Singleton 검증)
        assertSame(userService1, userService2, "UserService should be singleton");
        assertSame(itemService1, itemService2, "ItemService should be singleton");

        log.info("✅ Singleton Test Passed! All beans are singleton.");
    }
}
