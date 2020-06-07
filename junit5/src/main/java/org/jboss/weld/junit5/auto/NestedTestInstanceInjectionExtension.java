/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.weld.junit5.auto;

import static java.util.Arrays.stream;
import static org.junit.platform.commons.support.AnnotationSupport.findAnnotatedMethods;
import static org.junit.platform.commons.support.AnnotationSupport.findRepeatableAnnotations;
import static org.junit.platform.commons.support.AnnotationSupport.isAnnotated;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.inject.Qualifier;
import javax.inject.Singleton;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstances;
import org.junit.platform.commons.support.HierarchyTraversalMode;

/**
 * Extension class that enables injection of an externally provided
 * test instance which is not a valid CDI bean such as a
 * {@link org.junit.jupiter.api.Nested Nested} inner test class instance.
 */
public class NestedTestInstanceInjectionExtension implements Extension {

    private final TestInstances testInstances;

    NestedTestInstanceInjectionExtension(ExtensionContext context) {
        this.testInstances = context.getRequiredTestInstances();
    }

    public void afterBeanDiscovery(@Observes AfterBeanDiscovery event, BeanManager manager) {
        Set<Class<?>> excludeBeanClasses =
                testInstances.getAllInstances().stream()
                .flatMap(testInstance -> findRepeatableAnnotations(testInstance.getClass(), ExcludeBeanClasses.class).stream())
                .flatMap(ann -> stream(ann.value()))
                .distinct()
                .collect(Collectors.toSet());

        for (Object testInstance : testInstances.getAllInstances()) {
            for (Field field : ClassScanning.findAllFieldsInHierarchy(testInstance.getClass())) {
                if (!field.isAnnotationPresent(Produces.class)) {
                    continue;
                }
                if (field.isAnnotationPresent(ExcludeBean.class)) {
                    continue;
                }
                if (excludeBeanClasses.stream().filter(c -> c.isAssignableFrom(field.getType())).count() > 0) {
                    continue;
                }
                event.addBean()
                        .addType(field.getType())
                        .addQualifiers(getQualifiers(field.getAnnotations()))
                        .scope(Singleton.class)
                        .produceWith(instance -> {
                            try {
                                return field.get(testInstance);
                            } catch (IllegalAccessException e) {
                                // In case we cannot get to the field, we need to set accessibility as well
                                return AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
                                    field.setAccessible(true);
                                    try {
                                        return field.get(testInstance);
                                    } catch (ReflectiveOperationException ex) {
                                        throw new RuntimeException(ex.getMessage(), ex);
                                    }
                                });
                            }
                        });
            }
            for (Method method : findAnnotatedMethods(testInstance.getClass(), Produces.class, HierarchyTraversalMode.BOTTOM_UP)) {
                if (method.isAnnotationPresent(ExcludeBean.class)) {
                    continue;
                }
                if (excludeBeanClasses.stream().filter(c -> c.isAssignableFrom(method.getReturnType())).count() > 0) {
                    continue;
                }
                event.addBean()
                        .addType(method.getReturnType())
                        .addQualifiers(getQualifiers(method.getAnnotations()))
                        .scope(Singleton.class)
                        .produceWith(instance -> {
                            try {
                                return method.invoke(testInstance);
                            } catch (IllegalAccessException e) {
                                // In case we cannot get to the field, we need to set accessibility as well
                                return AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
                                    try {
                                        method.setAccessible(true);
                                        return method.invoke(testInstance);
                                    } catch (ReflectiveOperationException ex) {
                                        throw new RuntimeException(ex.getMessage(), ex);
                                    }
                                });
                            } catch (ReflectiveOperationException ex) {
                                throw new RuntimeException(ex.getMessage(), ex);
                            }
                        });
            }
            // TODO: disposer methods
        }
    }

    private Set<Annotation> getQualifiers(Annotation[] annotations) {
        return Arrays.stream(annotations)
                .filter(ann -> isAnnotated(ann.annotationType(), Qualifier.class))
                .collect(Collectors.toSet());
    }

}
