package io.github.dmitriyiliyov.oncebox.starter.registration;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

/**
 * Starts a real Spring Boot application and stops it right after every configuration class, auto-configurations
 * included, has registered its bean definitions - the point where every bean condition has been evaluated. No bean
 * is created, so no database or broker is reached.
 */
final class RegisteredBeans {

    private static final List<String> BASE_PROPERTIES = List.of(
            "spring.datasource.url=jdbc:postgresql://localhost:1/oncebox",
            "oncebox.outbox.sender.type=kafka",
            "oncebox.outbox.events.registration-event.topic=registration"
    );

    private final ConfigurableListableBeanFactory beanFactory;
    private final Throwable startupFailure;

    private RegisteredBeans(ConfigurableListableBeanFactory beanFactory, Throwable startupFailure) {
        this.beanFactory = beanFactory;
        this.startupFailure = startupFailure;
    }

    static RegisteredBeans of(Class<?> application, String... properties) {
        return start(new SpringApplicationBuilder(application), properties);
    }

    // the application sees the classpath without the given classes, as if their jars were not in the build
    static RegisteredBeans withoutClasses(Class<?> application, Class<?>... hidden) {
        return start(
                new SpringApplicationBuilder(application)
                        .resourceLoader(new DefaultResourceLoader(new FilteredClassLoader(hidden)))
        );
    }

    private static RegisteredBeans start(SpringApplicationBuilder builder, String... properties) {
        AtomicReference<ConfigurableListableBeanFactory> captured = new AtomicReference<>();
        try {
            builder
                    .web(WebApplicationType.NONE)
                    .properties(Stream.concat(BASE_PROPERTIES.stream(), Stream.of(properties)).toArray(String[]::new))
                    .initializers(context -> context.addBeanFactoryPostProcessor((BeanFactoryPostProcessor) bf -> {
                        captured.set(bf);
                        throw new RegistrationCaptured();
                    }))
                    .run()
                    .close();
        } catch (Throwable e) {
            return captured.get() != null ? new RegisteredBeans(captured.get(), null) : new RegisteredBeans(null, e);
        }
        throw new IllegalStateException("The context was refreshed past bean factory post-processing");
    }

    Throwable startupFailure() {
        return startupFailure;
    }

    List<String> namesOf(Class<?> type) {
        return List.of(beanFactory().getBeanNamesForType(type, true, false));
    }

    boolean contains(String beanName) {
        return beanFactory().containsBeanDefinition(beanName);
    }

    BeanDefinition definition(String beanName) {
        return beanFactory().getBeanDefinition(beanName);
    }

    private ConfigurableListableBeanFactory beanFactory() {
        if (beanFactory == null) {
            throw new IllegalStateException("The application failed to start", startupFailure);
        }
        return beanFactory;
    }

    private static final class RegistrationCaptured extends RuntimeException {

        private RegistrationCaptured() {
            super("bean definitions captured", null, false, false);
        }
    }
}
