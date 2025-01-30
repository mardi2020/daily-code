package org.mardi2020.dependencyinjection.service;

import lombok.extern.slf4j.Slf4j;
import org.mardi2020.dependencyinjection.annotation.MyAutowired;
import org.mardi2020.dependencyinjection.annotation.MyComponent;

@Slf4j
@MyComponent
public class UserService {

    @MyAutowired
    private ItemService itemService;

    public void getHello() {
        log.info("Executing UserService#getHello");
        itemService.sayHello();
    }
}
