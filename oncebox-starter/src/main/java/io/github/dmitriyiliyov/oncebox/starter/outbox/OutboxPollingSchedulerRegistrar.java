package io.github.dmitriyiliyov.oncebox.starter.outbox;

import io.github.dmitriyiliyov.oncebox.core.ContinuableTaskDecorator;
import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxPollingScheduler;
import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxProcessor;
import io.github.dmitriyiliyov.oncebox.core.polling.OnceboxScheduleStrategy;
import io.github.dmitriyiliyov.oncebox.starter.*;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;

import java.util.concurrent.ScheduledExecutorService;


public class OutboxPollingSchedulerRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware, BeanFactoryAware {

    private Environment environment;
    private BeanFactory beanFactory;

    @Override
    public void setEnvironment(@NonNull Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setBeanFactory(@NonNull BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void registerBeanDefinitions(@NonNull AnnotationMetadata importingClassMetadata, @NonNull BeanDefinitionRegistry registry) {

        Binder binder = Binder.get(environment);
        OutboxProperties properties = binder.bind(
                "oncebox",
                Bindable.of(OnceboxProperties.class)
                ).orElseThrow(() -> new IllegalStateException("oncebox properties not specified"))
                .getOutbox();
        properties.applyDefaults();

        for (OutboxProperties.EventProperties event : properties.getEvents().values()) {

            String beanName = BeanNameUtils.toBeanName(
                    event.getEventType(),
                    "OutboxPollingScheduler"
            );

            RootBeanDefinition definition = new RootBeanDefinition(OutboxPollingScheduler.class);
            definition.setInstanceSupplier(() -> {

                ScheduledExecutorService executor = beanFactory.getBean(
                        "onceboxScheduledExecutorService",
                        ScheduledExecutorService.class
                );

                OutboxProcessor processor = beanFactory.getBean(OutboxProcessor.class);

                OnceboxScheduleStrategyListenerSupplier listenerSupplier = beanFactory.getBean(
                        OnceboxScheduleStrategyListenerSupplier.class
                );

                ContinuableTaskDecoratorSupplier decoratorSupplier = beanFactory.getBean(
                        ContinuableTaskDecoratorSupplier.class
                );

                OnceboxScheduleStrategy strategy = OnceboxScheduleStrategyFactory.create(
                        event.getEventType(),
                        event.getPolling(),
                        executor,
                        listenerSupplier
                );

                ContinuableTaskDecorator decorator = decoratorSupplier.supply(event.getEventType());

                return new OutboxPollingScheduler(event, strategy, processor, decorator);
            });

            registry.registerBeanDefinition(beanName, definition);
        }
    }
}