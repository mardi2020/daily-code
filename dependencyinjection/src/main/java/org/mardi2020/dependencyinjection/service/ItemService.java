package org.mardi2020.dependencyinjection.service;

import lombok.extern.slf4j.Slf4j;
import org.mardi2020.dependencyinjection.annotation.MyComponent;

@Slf4j
@MyComponent
public class ItemService {

    public void sayHello() {
        log.info("hello");
    }
}
