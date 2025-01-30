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

        // 2️⃣ 자동 등록된 Bean 가져오기
        UserService userService1 = context.getBean(UserService.class);
        ItemService itemService1 = context.getBean(ItemService.class);

        // 3️⃣ 동일한 Bean인지 확인 (Singleton 보장)
        assertSame(userService1, context.getBean(UserService.class), "UserService should be singleton");
        assertSame(itemService1, context.getBean(ItemService.class), "ItemService should be singleton");

        // 4️⃣ registerBean()을 사용하여 UserService 를 새로 등록
        context.registerBean(UserService.class);

        // 5️⃣ 새롭게 등록된 Bean 과 기존 Bean 이 같은지 확인 (Singleton 이 유지되는지 확인)
        UserService userService2 = context.getBean(UserService.class);
        assertSame(userService1, userService2,
                "UserService singleton should be preserved, but was replaced!");
    }
}
