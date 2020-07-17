/*
 * JBoss, Home of Professional Open Source
 * Copyright 2018, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
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

import static org.jboss.weld.util.reflection.Reflections.cast;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.event.ObservesAsync;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.AfterTypeDiscovery;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanAttributes;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.EventContext;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.InjectionTargetFactory;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessBean;
import javax.enterprise.inject.spi.ProcessBeanAttributes;
import javax.enterprise.inject.spi.ProcessInjectionPoint;
import javax.enterprise.inject.spi.ProcessInjectionTarget;
import javax.enterprise.inject.spi.ProcessManagedBean;
import javax.enterprise.inject.spi.ProcessObserverMethod;
import javax.enterprise.inject.spi.ProcessProducer;
import javax.enterprise.inject.spi.ProcessProducerField;
import javax.enterprise.inject.spi.ProcessProducerMethod;
import javax.enterprise.inject.spi.ProcessSyntheticAnnotatedType;
import javax.enterprise.inject.spi.ProcessSyntheticBean;
import javax.enterprise.inject.spi.ProcessSyntheticObserverMethod;
import javax.enterprise.inject.spi.Producer;
import javax.enterprise.inject.spi.ProducerFactory;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.print.attribute.standard.OrientationRequested;

import org.jboss.classfilewriter.ClassFile;
import org.jboss.weld.bean.ProducerMethod;
import org.jboss.weld.bean.proxy.util.SimpleProxyServices;
import org.jboss.weld.injection.ForwardingInjectionTarget;
import org.jboss.weld.injection.producer.BeanInjectionTarget;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.security.GetProtectionDomainAction;
import org.jboss.weld.util.annotated.ForwardingAnnotatedConstructor;
import org.jboss.weld.util.annotated.ForwardingAnnotatedType;
import org.jboss.weld.util.bytecode.ClassFileUtils;
import org.jboss.weld.util.bytecode.ConstructorUtils;

/**
 * Extension that makes test classes appear like regular beans even though instances are created by JUnit.
 * This includes injection into all test instances.
 * Proper handling of all other CDI annotations such as {@link javax.enterprise.inject.Produces &#064;Produces} is supported only on top level test classes.
 */
public class TestInstanceInjectionExtension implements Extension {

    static final boolean VERBOSE = false;

//    private static final AnnotationLiteral<Singleton> SINGLETON_LITERAL = new AnnotationLiteral<Singleton>() {};

//    private Map<Class<?>, ?> testInstancesByClass;
    private List<?> testInstances;

    TestInstanceInjectionExtension(List<?> testInstances) {
//        this.testInstancesByClass = testInstances.stream().collect(Collectors.toMap(Object::getClass, Function.identity()));
        this.testInstances = testInstances;
    }

    void addNestedProducersDisposersObservers(@Observes BeforeBeanDiscovery beforeBeanDiscovery) {
        if (VERBOSE) System.out.println("beforeBeanDiscovery = " + beforeBeanDiscovery);
    }
    void addNestedProducersDisposersObservers(@Observes ProcessAnnotatedType processAnnotatedType) {
        if (VERBOSE) System.out.println("processAnnotatedType = " + processAnnotatedType);
    }
    void addNestedProducersDisposersObservers(@Observes ProcessSyntheticAnnotatedType processSyntheticAnnotatedType) {
        if (VERBOSE) System.out.println("processSyntheticAnnotatedType = " + processSyntheticAnnotatedType);
    }
    <T> void addNestedProducersDisposersObservers(@Observes AfterTypeDiscovery afterTypeDiscovery, BeanManager beanManager) {
        if (VERBOSE) System.out.println("afterTypeDiscovery = " + afterTypeDiscovery);
    }
    void addNestedProducersDisposersObservers(@Observes ProcessInjectionTarget processInjectionTarget) {
        if (VERBOSE) System.out.println("processInjectionTarget = " + processInjectionTarget + "   .getInjectionTarget() = " + processInjectionTarget.getInjectionTarget());
    }
    void addNestedProducersDisposersObservers(@Observes ProcessProducer processProducer) {
        if (VERBOSE) System.out.println("processProducer = " + processProducer + "   .getProducer() = " + processProducer.getProducer());
    }
    void addNestedProducersDisposersObservers(@Observes ProcessInjectionPoint processInjectionPoint) {
        if (VERBOSE) System.out.println("processInjectionPoint = " + processInjectionPoint + "   .getInjectionPoint() = " + processInjectionPoint.getInjectionPoint());
    }
    void addNestedProducersDisposersObservers(@Observes ProcessBeanAttributes processBeanAttributes) {
        if (VERBOSE) System.out.println("processBeanAttributes = " + processBeanAttributes + "   .getBeanAttributes() = " + processBeanAttributes.getBeanAttributes());
    }
    void addNestedProducersDisposersObservers(@Observes ProcessBean processBean) {
        if (VERBOSE) System.out.println("processBean = " + processBean);
        if (VERBOSE) System.out.println("processBean.getAnnotated() = " + processBean.getAnnotated());
        if (VERBOSE) System.out.println("processBean.getBean() = " + processBean.getBean());
        if (VERBOSE) System.out.println("processBean.getClass() = " + processBean.getClass());
    }
    void addNestedProducersDisposersObservers(@Observes ProcessManagedBean processManagedBean) {
        if (VERBOSE) System.out.println("processManagedBean = " + processManagedBean);
        if (VERBOSE) System.out.println("processManagedBean.getAnnotated() = " + processManagedBean.getAnnotated());
    }
    void addNestedProducersDisposersObservers(@Observes ProcessProducerMethod processProducerMethod) {
        if (VERBOSE) System.out.println("processProducerMethod = " + processProducerMethod);
        if (VERBOSE) System.out.println("processProducerMethod.getAnnotatedProducerMethod() = " + processProducerMethod.getAnnotatedProducerMethod());
        if (VERBOSE) System.out.println("processProducerMethod.getAnnotatedDisposedParameter() = " + processProducerMethod.getAnnotatedDisposedParameter());
    }
    void addNestedProducersDisposersObservers(@Observes ProcessProducerField processProducerField) {
        if (VERBOSE) System.out.println("processProducerField = " + processProducerField);
        if (VERBOSE) System.out.println("processProducerField.getAnnotatedProducerField() = " + processProducerField.getAnnotatedProducerField());
    }
    void addNestedProducersDisposersObservers(@Observes ProcessSyntheticBean processSyntheticBean) {
        if (VERBOSE) System.out.println("processSyntheticBean = " + processSyntheticBean);
        if (VERBOSE) System.out.println("processSyntheticBean.getAnnotated() = " + processSyntheticBean.getAnnotated());
    }
    void addNestedProducersDisposersObservers(@Observes ProcessObserverMethod processObserverMethod) {
        if (VERBOSE) System.out.println("processObserverMethod = " + processObserverMethod);
    }
    void addNestedProducersDisposersObservers(@Observes ProcessSyntheticObserverMethod processSyntheticObserverMethod) {
        if (VERBOSE) System.out.println("processSyntheticObserverMethod = " + processSyntheticObserverMethod);
    }
    void addNestedProducersDisposersObservers(@Observes AfterDeploymentValidation afterDeploymentValidation) {
        if (VERBOSE) System.out.println("afterDeploymentValidation = " + afterDeploymentValidation);
    }

//    static class NoArgConstructor<X> extends ForwardingAnnotatedConstructor<X> {
//        AnnotatedConstructor<X> originalAnnotatedConstructor;
//        NoArgConstructor(AnnotatedConstructor<X> originalAnnotatedConstructor) {
//            this.originalAnnotatedConstructor = originalAnnotatedConstructor;
//        }
//        @Override
//        protected AnnotatedConstructor<X> delegate() {
//            return originalAnnotatedConstructor;
//        }
//        @Override
//        public Constructor<X> getJavaMember() {
//            try {
//                return (Constructor<X>) Object.class.getConstructor();
//            } catch (NoSuchMethodException | SecurityException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//                return null;
//            }
//        }
//    }
//    static class NoArgConstructorsAnnotatedType<X> extends ForwardingAnnotatedType<X> {
//        AnnotatedType<X> originalAnnotatedType;
//        NoArgConstructorsAnnotatedType(AnnotatedType<X> originalAnnotatedType) {
//            this.originalAnnotatedType = originalAnnotatedType;
//        }
//        @Override
//        public AnnotatedType<X> delegate() {
//            return originalAnnotatedType;
//        }
//        @Override
//        public Set<AnnotatedConstructor<X>> getConstructors() {
//            return super.getConstructors().stream().map(c -> new NoArgConstructor(c)).collect(Collectors.toSet());
//        }
//        @Override
//        public Class<X> getJavaClass() {
//            return (Class<X>) Object.class;
//        }
//    }
//    static class ObjectAnnotatedType<X> extends ForwardingAnnotatedType<X> {
//        AnnotatedType<X> originalAnnotatedType;
//        ObjectAnnotatedType(AnnotatedType<X> originalAnnotatedType) {
//            this.originalAnnotatedType = originalAnnotatedType;
//        }
//        @Override
//        public AnnotatedType<X> delegate() {
//            return originalAnnotatedType;
//        }
//        @Override
//        public Class<X> getJavaClass() {
//            return (Class<X>) Object.class;
//        }
//    }
    
    static class ZauberInjectionTargetFactory<X> implements InjectionTargetFactory<X> {
        InjectionTarget<?> injectionTarget;
        Supplier<Object> instanceSupplier;
        ZauberInjectionTargetFactory(InjectionTarget<?> injectionTarget, Supplier<Object> instanceSupplier) {
            this.injectionTarget = injectionTarget;
            this.instanceSupplier = instanceSupplier;
        }
        @Override
        public InjectionTarget<X> createInjectionTarget(Bean<X> bean) {
            return new InjectionTarget<X>() {

                @Override
                public X produce(CreationalContext<X> ctx) {
                    return (X) instanceSupplier.get();
                }

                @Override
                public void dispose(X instance) {
                    // JUnit will
                }

                @Override
                public Set<InjectionPoint> getInjectionPoints() {
                    Set<InjectionPoint> ips = injectionTarget.getInjectionPoints();
                    return ips;
                }

                @Override
                public void inject(X instance, CreationalContext<X> ctx) {
                    // TODO Auto-generated method stub
                    System.out.println("INJECTINJECTINJECTINJECTINJECT");
                    InjectionTarget it = injectionTarget;
                    it.inject((Object) instance, (CreationalContext) ctx);
                }

                @Override
                public void postConstruct(X instance) {
                    // TODO Auto-generated method stub
                    ((InjectionTarget<Object>) injectionTarget).postConstruct(instance);
                }

                @Override
                public void preDestroy(X instance) {
                    // TODO Auto-generated method stub
                    ((InjectionTarget<Object>) injectionTarget).preDestroy(instance);
                }
                
            };
        }
    }

    void addNestedProducersDisposersObservers(@Observes AfterBeanDiscovery afterBeanDiscovery, BeanManager beanManager) {
        if (VERBOSE) System.out.println("afterBeanDiscovery = " + afterBeanDiscovery);
        
        for (Object testInstance : testInstances) {
            Class<?> beanClass = testInstance.getClass();
            
            
            
//            Class<?> noArgBeanClass = beanClass;
//            try {
//                beanClass.getConstructor();
//            } catch (NoSuchMethodException e) {
//                String className = beanClass.getName();
//                ClassFile classFile = new ClassFile(className + "$NoArgTestInstance", className);
//                ConstructorUtils.addDefaultConstructor(classFile, Collections.emptyList(), false);
//                ProtectionDomain domain = AccessController.doPrivileged(new GetProtectionDomainAction(beanClass));
//                noArgBeanClass = ClassFileUtils.toClass(classFile, beanClass, new SimpleProxyServices(), domain);
//            }
            
            
            
//            System.out.println("addBean beanClass = " + beanClass);
            AnnotatedType<?> originalAnnotatedType = beanManager.createAnnotatedType(beanClass);
//            AnnotatedType<?> annotatedType = new NoArgConstructorsAnnotatedType<>(originalAnnotatedType);
            AnnotatedType<?> annotatedType = originalAnnotatedType;
            
            
//            System.out.println("annotatedType.getConstructors() = " + annotatedType.getConstructors());
            
            
            InjectionTargetFactory<?> originalInjectionTargetFactory = beanManager.getInjectionTargetFactory(annotatedType);
            InjectionTarget<?> originalInjectionTarget = /*cast(*/ originalInjectionTargetFactory.createInjectionTarget(null);
            
            
            InjectionTargetFactory<?> injectionTargetFactory = new ZauberInjectionTargetFactory<>(originalInjectionTarget, () -> testInstance);
            
            
//            InjectionTarget<?> injectionTarget = originalInjectionTarget;
            InjectionTarget<?> injectionTarget = injectionTargetFactory.createInjectionTarget(null);

//              CreationalContext<T> ctx = beanManager.createCreationalContext();
//              beanManager.createBean(beanAttributes, beanClass, injectionTargetFactory);
              
              
              
              
              
            BeanAttributes<?> beanAttributes = beanManager.createBeanAttributes(annotatedType);
//            System.out.println("beanClass = " + beanClass);
//            System.out.println("annotatedType = " + annotatedType);
//            System.out.println("beanAttributes = " + beanAttributes);
//            System.out.println("beanAttributes.getScope() = " + beanAttributes.getScope());
//            System.out.println("injectionTarget = " + injectionTarget);
//            System.out.println("injectionTarget.getInjectionPoints() = " + injectionTarget.getInjectionPoints());
            Named named = annotatedType.getAnnotation(Named.class);
            String name = named == null ? null : named.value();
            Class<? extends Annotation> scope = beanAttributes.getScope();
            if (Dependent.class.isAssignableFrom(scope) || Dependent.class == scope) {
                scope = Singleton.class;
            }
            Set<InjectionPoint> injectionPoints = injectionTarget.getInjectionPoints();
            
            afterBeanDiscovery.addBean()
                .addInjectionPoints(injectionPoints.toArray(InjectionPoint[]::new))
                .addQualifiers(beanAttributes.getQualifiers())
                .addStereotypes(beanAttributes.getStereotypes())
                .addTypes(beanAttributes.getTypes())
                .alternative(beanAttributes.isAlternative()) // there most certainly must not be an alternative to a test class - but then why not
                .name(name)
                .beanClass(beanClass)
                .scope(scope)
                .createWith(c -> {
                    System.out.println("createWith = " + c);
                    Object instance = ((InjectionTarget<Object>) injectionTarget).produce(c);
                    ((InjectionTarget<Object>) injectionTarget).inject(instance, c);
                    ((InjectionTarget<Object>) injectionTarget).postConstruct(instance);
                    return instance;
                })
                .destroyWith((i, c) -> {
                    System.out.println("destroyWith = " + i + ", " + c);
                    ((InjectionTarget<Object>) injectionTarget).preDestroy(i);
                    c.release();
                })
//                    .produceWith(i -> {
//                        System.out.println("produceWith");
////                        ((InjectionTarget<Object>) injectionTarget).inject(testInstance, i);
////                        ((InjectionTarget<Object>) injectionTarget).postConstruct(testInstance);
//                        return testInstance;
//                    })
//                    .disposeWith((o, i) -> {
////                        ((InjectionTarget<Object>) injectionTarget).preDestroy(testInstance);
//                    })
                    
                    ;
            
            
            
//          ProducerMethod.of(attributes, method, declaringBean, disposalMethod, beanManager, services)
//          // org.jboss.weld.bootstrap.AbstractBeanDeployer.createProducerMethod(AbstractClassBean<X>, EnhancedAnnotatedMethod<T, ? super X>)
            // afterBeanDiscovery.addObserverMethod()
            
            
            List<Method> disposers = new LinkedList<>();
            
            
            
            for (Field field : Arrays.stream(beanClass.getDeclaredFields()).filter(f -> f.isAnnotationPresent(Produces.class)).collect(Collectors.toList())) {
                
                Class<?> producesFieldClass = field.getType();
                System.out.println("produces field = " + field);
                
                AnnotatedType<?> producesFieldAnnotatedType = beanManager.createAnnotatedType(producesFieldClass);
//                InjectionTargetFactory<?> producesFieldOriginalInjectionTargetFactory = beanManager.getInjectionTargetFactory(producesFieldAnnotatedType);
//                InjectionTarget<?> producesFieldOriginalInjectionTarget = /*cast(*/ producesFieldOriginalInjectionTargetFactory.createInjectionTarget(null);
                Supplier<?> supplier = () -> {
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
                };
//                InjectionTargetFactory<?> producesFieldInjectionTargetFactory = new ZauberInjectionTargetFactory<Object>((InjectionTarget<Object>) producesFieldOriginalInjectionTarget, (Supplier<Object>) supplier);
//                InjectionTarget<?> producesFieldInjectionTarget = producesFieldInjectionTargetFactory.createInjectionTarget(null);
                

//                System.out.println("field = " + field);
//                System.out.println("field.getType() = " + field.getType());
//                System.out.println("field.getType().getInterfaces() = " + Arrays.stream(field.getType().getInterfaces()).collect(Collectors.toList()));
//                System.out.println("field.getType().getClasses() = " + Arrays.stream(field.getType().getClasses()).collect(Collectors.toList()));
                
                BeanAttributes<?> producesFieldBeanAttributes = beanManager.createBeanAttributes(producesFieldAnnotatedType);
                
//                System.out.println("producesFieldBeanAttributes.getQualifiers() = " + producesFieldBeanAttributes.getQualifiers());
                List<Annotation> producesFieldQualifiers = Arrays.stream(field.getAnnotations()).filter(a -> beanManager.isQualifier(a.annotationType())).collect(Collectors.toList());
                Set<Class<? extends Annotation>> producesFieldStereotypes = Arrays.stream(field.getAnnotations()).filter(a -> beanManager.isStereotype(a.annotationType())).map(a -> a.annotationType()).collect(Collectors.toSet());
//                System.out.println("producesFieldQualifiers = " + producesFieldQualifiers);
                Class<? extends Annotation> producesFieldScope = Arrays.stream(field.getAnnotations()).map(a -> a.annotationType()).filter(at -> beanManager.isScope(at)).findAny().orElse(null); // TODO: unique statt findAny
                if (producesFieldScope == null) producesFieldScope = Dependent.class;
                
                afterBeanDiscovery.addBean()
//                        .addInjectionPoints(producesFieldInjectionTarget.getInjectionPoints().toArray(InjectionPoint[]::new))
//                        .addQualifiers(producesFieldBeanAttributes.getQualifiers())
                        .addQualifiers(producesFieldQualifiers.toArray(Annotation[]::new))
//                        .addStereotypes(producesFieldBeanAttributes.getStereotypes())
                        .addStereotypes(producesFieldStereotypes)
                        .addTypes(producesFieldBeanAttributes.getTypes())
                        .alternative(producesFieldBeanAttributes.isAlternative())
//                        .name(name)
                        .beanClass(producesFieldClass)
                        .scope(producesFieldScope)
                        .createWith(c -> {
                            System.out.println("produces field createWith = " + field + ", " + c);
//                            Object instance = ((InjectionTarget<Object>) producesFieldInjectionTarget).produce(c);
//                            ((InjectionTarget<Object>) producesFieldInjectionTarget).inject(instance, c);
//                            ((InjectionTarget<Object>) producesFieldInjectionTarget).postConstruct(instance);
//                            return instance;
                            return supplier.get();
                        })
                        .destroyWith((i, c) -> {
                            
//                            disposers.stream().filter(predicate)
                            
                            System.out.println("produces field destroyWith = " + i + ", " + c);
//                            ((InjectionTarget<Object>) producesFieldInjectionTarget).preDestroy(i);
                            c.release();
                        });
            }
            
            /*
                List<Class<? extends Annotation>> scopes = Arrays.stream(field.getDeclaredAnnotations())
//                        .filter(ann -> beanManager.isScope(ann.getClass()))
                        .filter(ann -> {
                            return ann.annotationType().isAnnotationPresent(NormalScope.class) || ann.annotationType().isAnnotationPresent(Scope.class);
                        })
                        .map(ann -> {
//                            try {
                                if (ann.getClass().getSimpleName().matches("^\\$Proxy\\d+$")) {
                                    switch (ann.toString().replaceAll("^@(.*)\\(\\)", "$1")) {
                                    case "javax.enterprise.context.SessionScoped":
                                        return SessionScoped.class;
                                    case "javax.enterprise.context.ConversationScoped":
                                        return ConversationScoped.class;
                                    default:
                                        return null;
                                    }
//                                    ann = (Annotation) Class.forName().getConstructor().newInstance();
                                }
                                return ann.getClass();
//                            } catch (ReflectiveOperationException e) {
//                                throw new RuntimeException(e);
//                            }
                        }).collect(Collectors.toList());
                System.out.println("field scopes = " + scopes);
                scopes.forEach(scopeClass -> {
                    beanConfigurator.scope(scopeClass);
                });
            }
            
            // TODO: remove field type bean class to auto-prevent another instantiation? same for produces methods
            */
            
            
            for (Method producerMethod : Arrays.stream(beanClass.getDeclaredMethods()).filter(m -> m.isAnnotationPresent(Produces.class)).collect(Collectors.toList())) {
                System.out.println("produces method = " + producerMethod);
                Class<?> producesMethodClass = producerMethod.getReturnType();

                AnnotatedType<?> producesMethodAnnotatedType = beanManager.createAnnotatedType(producesMethodClass);
//                InjectionTargetFactory<?> producesMethodOriginalInjectionTargetFactory = beanManager.getInjectionTargetFactory(producesMethodAnnotatedType);
//                InjectionTarget<?> producesMethodOriginalInjectionTarget = /*cast(*/ producesMethodOriginalInjectionTargetFactory.createInjectionTarget(null);
                Function<Instance<Object>, Object> supplier = (i) -> {
                    // BEGIN: invoke method with injected parameters
                    Object[] methodParams = Arrays.stream(producerMethod.getParameters())
                            .map(p -> {
                                Class<?> t = p.getType();
//                                System.out.println("parameter type = " + t + ", t.getClass = " + t.getClass());
                                if (t.isAssignableFrom(InjectionPoint.class)) {
//                                    System.out.println("InjectionPoint parameter");
//                                    Set<Bean<?>> ips = beanManager.getBeans(InjectionPoint.class);
//                                    System.out.println("ips = " + ips);
//                                    Bean<? extends Object> ipBean = beanManager.resolve(ips);
//                                    return null;
                                    Instance<InjectionPoint> ipi = i.select(InjectionPoint.class);
                                    InjectionPoint ip = ipi.get();
                                    return ip;
                                    
                                } else {
                                    Set<Bean<?>> beans = beanManager.getBeans(t,
                                            Arrays.stream(p.getDeclaredAnnotations())
                                          .filter(ann -> beanManager.isQualifier(ann.getClass()))
    //                                        .filter(ann -> ann.annotationType().isAnnotationPresent(Qualifier.class))
                                            .toArray(Annotation[]::new));
                                    Bean<? extends Object> bean = beanManager.resolve(beans);
                                    CreationalContext<?> creationalContext = beanManager.createCreationalContext(bean); // TODO: dispose
                                    Object reference = beanManager.getReference(bean, p.getType(), creationalContext);
                                    return reference;
                                }
                            })
                            .toArray(Object[]::new);
                    try {
                        return producerMethod.invoke(testInstance, methodParams);
                    } catch (IllegalAccessException e) {
                        // In case we cannot get to the method, we need to set accessibility as well
                        return AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
                            try {
                                producerMethod.setAccessible(true);
                                return producerMethod.invoke(testInstance, methodParams);
                            } catch (ReflectiveOperationException ex) {
                                throw new RuntimeException(ex.getMessage(), ex);
                            }
                        });
                    } catch (ReflectiveOperationException ex) {
                        throw new RuntimeException(ex.getMessage(), ex);
                    }
                    // END: invoke method with injected parameters
                };
//                InjectionTargetFactory<?> producesFieldInjectionTargetFactory = new ZauberInjectionTargetFactory<Object>((InjectionTarget<Object>) producesFieldOriginalInjectionTarget, (Supplier<Object>) supplier);
//                InjectionTarget<?> producesFieldInjectionTarget = producesFieldInjectionTargetFactory.createInjectionTarget(null);
                

                BeanAttributes<?> producesMethodBeanAttributes = beanManager.createBeanAttributes(producesMethodAnnotatedType);
                
                List<Annotation> producesMethodQualifiers = Arrays.stream(producerMethod.getAnnotations()).filter(a -> beanManager.isQualifier(a.annotationType())).collect(Collectors.toList());
                Set<Class<? extends Annotation>> producesMethodStereotypes = Arrays.stream(producerMethod.getAnnotations()).filter(a -> beanManager.isStereotype(a.annotationType())).map(a -> a.annotationType()).collect(Collectors.toSet());
//                System.out.println("producesFieldQualifiers = " + producesFieldQualifiers);
                Class<? extends Annotation> producesMethodScope = Arrays.stream(producerMethod.getAnnotations()).map(a -> a.annotationType()).filter(at -> beanManager.isScope(at)).findAny().orElse(null); // TODO: unique statt findAny
                if (producesMethodScope == null) producesMethodScope = Dependent.class;
                
                afterBeanDiscovery.addBean()
//                        .addInjectionPoints(producesMethodInjectionTarget.getInjectionPoints().toArray(InjectionPoint[]::new))
//                        .addQualifiers(producesMethodBeanAttributes.getQualifiers())
                        .addQualifiers(producesMethodQualifiers.toArray(Annotation[]::new))
//                        .addStereotypes(producesMethodBeanAttributes.getStereotypes())
                        .addStereotypes(producesMethodStereotypes)
                        .addTypes(producesMethodBeanAttributes.getTypes())
                        .alternative(producesMethodBeanAttributes.isAlternative())
//                        .name(name)
                        .beanClass(producesMethodClass)
                        .scope(producesMethodScope)
//                        .createWith(c -> {
//                            System.out.println("produces method createWith = " + method + ", " + c);
//                            
////                            Set<Bean<?>> ips = beanManager.getBeans(InjectionPoint.class);
////                            Bean<? extends Object> ipBean = beanManager.resolve(ips);
////                            InjectionPoint injectionPoint = (InjectionPoint) beanManager.getReference(ipBean, InjectionPoint.class, c);
////                            System.out.println("createWith:InjectionPoint = " + injectionPoint);
//                            
////                            Object instance = ((InjectionTarget<Object>) producesMethodInjectionTarget).produce(c);
////                            ((InjectionTarget<Object>) producesMethodInjectionTarget).inject(instance, c);
////                            ((InjectionTarget<Object>) producesMethodInjectionTarget).postConstruct(instance);
////                            return instance;
//                            return supplier.get();
//                        })
//                        .destroyWith((i, c) -> {
//                            System.out.println("produces method destroyWith = " + i + ", " + c);
////                            ((InjectionTarget<Object>) producesMethodInjectionTarget).preDestroy(i);
//                            c.release();
//                        })
                        .produceWith((Instance<Object> i) -> {
                            Instance<InjectionPoint> ipi = i.select(InjectionPoint.class);
                            InjectionPoint ip = ipi.get();
                            System.out.println("produceWith:InjectionPoint = " + ip);
                            return supplier.apply(i);
                        })
                        .disposeWith((o, i) -> {
                            for (Method method : disposers) {
                                if (!method.getDeclaringClass().equals(beanClass)) continue;
                                
                                Object[] methodParams = Arrays.stream(method.getParameters())
                                        .map(p -> {
                                            Class<?> t = p.getType();
//                                            System.out.println("parameter type = " + t + ", t.getClass = " + t.getClass());
                                            if (p.isAnnotationPresent(Disposes.class)
                                                    && p.getType().isAssignableFrom(producesMethodClass)
                                                    && Arrays.stream(method.getDeclaredAnnotations())
                                              .filter(a -> beanManager.isQualifier(a.annotationType()))
                                              .filter(a -> !producesMethodQualifiers.contains(a)).count() == 0) {
                                                return o;
                                            } else
                                                // BEGIN: invoke method with injected parameters
                                            if (t.isAssignableFrom(InjectionPoint.class)) {
//                                                System.out.println("InjectionPoint parameter");
//                                                Set<Bean<?>> ips = beanManager.getBeans(InjectionPoint.class);
//                                                System.out.println("ips = " + ips);
//                                                Bean<? extends Object> ipBean = beanManager.resolve(ips);
//                                                return null;
                                                Instance<InjectionPoint> ipi = i.select(InjectionPoint.class);
                                                InjectionPoint ip = ipi.get();
                                                return ip;
                                                
                                            } else {
                                                Set<Bean<?>> beans = beanManager.getBeans(t,
                                                        Arrays.stream(p.getDeclaredAnnotations())
                                                      .filter(ann -> beanManager.isQualifier(ann.getClass()))
                //                                        .filter(ann -> ann.annotationType().isAnnotationPresent(Qualifier.class))
                                                        .toArray(Annotation[]::new));
                                                Bean<? extends Object> bean = beanManager.resolve(beans);
                                                CreationalContext<?> creationalContext = beanManager.createCreationalContext(bean); // TODO: dispose
                                                Object reference = beanManager.getReference(bean, p.getType(), creationalContext);
                                                return reference;
                                            }
                                        })
                                        .toArray(Object[]::new);
                                try {
                                    method.invoke(testInstance, methodParams);
                                } catch (IllegalAccessException e) {
                                    // In case we cannot get to the method, we need to set accessibility as well
                                    AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                                        try {
                                            method.setAccessible(true);
                                            method.invoke(testInstance, methodParams);
                                            return null;
                                        } catch (ReflectiveOperationException ex) {
                                            throw new RuntimeException(ex.getMessage(), ex);
                                        }
                                    });
                                } catch (ReflectiveOperationException ex) {
                                    throw new RuntimeException(ex.getMessage(), ex);
                                }
                                // END: invoke method with injected parameters
                            }
                        })
                        ;
            }
            
            for (Method disposerMethod : Arrays.stream(beanClass.getDeclaredMethods())
                    .filter(m -> Arrays.stream(m.getParameters())
                            .filter(at -> at.isAnnotationPresent(Disposes.class)).count() > 0)
                    .collect(Collectors.toList())) {
                System.out.println("disposer method " + disposerMethod);
                
                disposers.add(disposerMethod);
                
                
            }
            
            for (Method observerMethod : Arrays.stream(beanClass.getDeclaredMethods())
                    .filter(m -> Arrays.stream(m.getParameters())
                            .filter(p -> p.isAnnotationPresent(Observes.class) || p.isAnnotationPresent(ObservesAsync.class)).count() > 0)
                    .collect(Collectors.toList())) {
                System.out.println("observer method " + observerMethod);
                
                for (Parameter p : observerMethod.getParameters()) {
                    if (!p.isAnnotationPresent(Observes.class) && !p.isAnnotationPresent(ObservesAsync.class)) continue;
                    
                    afterBeanDiscovery.addObserverMethod()
                        .observedType(p.getType())
                        .qualifiers(Arrays.stream(p.getAnnotations()).filter(a -> beanManager.isQualifier(a.getClass())).collect(Collectors.toSet()))
                        .async(p.isAnnotationPresent(ObservesAsync.class))
                        .beanClass(beanClass)
                        .observedType(p.getType())
                        .notifyWith((EventContext<Object> eventContext) -> {
                            System.out.println("NOTIFY");
                            Object event = eventContext.getEvent();
                            
                            try {
                                observerMethod.invoke(testInstance, event);
                            } catch (IllegalAccessException e) {
                                // In case we cannot get to the method, we need to set accessibility as well
                                AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                                    try {
                                        observerMethod.setAccessible(true);
                                        observerMethod.invoke(testInstance, event);
                                        return null;
                                    } catch (ReflectiveOperationException ex) {
                                        throw new RuntimeException(ex.getMessage(), ex);
                                    }
                                });
                            } catch (ReflectiveOperationException ex) {
                                throw new RuntimeException(ex.getMessage(), ex);
                            }
                            
                        })
                    ;
                    
                }
            }
            
            
        }
    }

}
