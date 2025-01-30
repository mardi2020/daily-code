package org.mardi2020.dependencyinjection;


import lombok.extern.slf4j.Slf4j;
import org.mardi2020.dependencyinjection.annotation.MyAutowired;
import org.mardi2020.dependencyinjection.annotation.MyComponent;
import org.mardi2020.dependencyinjection.config.MyApplicationContextV1;
import org.mardi2020.dependencyinjection.service.UserService;

@Slf4j
@MyComponent
public class DependencyinjectionApplication {

	@MyAutowired
	private UserService userService;

	public static void main(String[] args) {
		MyApplicationContextV1 context = new MyApplicationContextV1("org.mardi2020.dependencyinjection");
		DependencyinjectionApplication app = context.getBean(DependencyinjectionApplication.class);
		app.run();
	}

	public void run() {
		userService.getHello();
	}
}
