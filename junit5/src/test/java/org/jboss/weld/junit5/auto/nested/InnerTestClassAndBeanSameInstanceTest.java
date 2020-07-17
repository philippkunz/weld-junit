package org.jboss.weld.junit5.auto.nested;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.util.Nonbinding;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Qualifier;

import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.jboss.weld.literal.NamedLiteral;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

/**
 * @see <a href="https://github.com/weld/weld-junit/issues/103">https://github.com/weld/weld-junit/issues/103</a>
 */
@EnableAutoWeld
public class InnerTestClassAndBeanSameInstanceTest {

    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    public @interface FieldProduced { }

    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    public @interface MethodProduced {
        @Nonbinding String value() default "";
    }

    interface Bean {
        default String ping() {
            // use hashCode to verify same instance in assertSame thereby ignoring proxies
            return "ping" + Objects.hashCode(this);
        }
    }

    static void assertSameBean(Bean bean1, Bean bean2) {
        assertEquals(bean1.ping(), bean2.ping());
    }

    @PostConstruct
    void pc() {
        System.out.println("pc1");
    }

    @PreDestroy
    void pd() {
        System.out.println("pd1");
    }

    @Nested
//    @Named("innerNested")
//    @ApplicationScoped
//    @TestInstance(Lifecycle.PER_CLASS)
    class Inner {

        @Produces @FieldProduced
        Bean theBean = new Bean() {
        };

        @Produces @MethodProduced
        Bean produceBean(InnerTestClassAndBeanSameInstanceTest someParam, InjectionPoint injectionPoint) {
            System.out.println("PRODUCER METHOD CALLED - someParam = " + someParam + ", injectionPoint = " + injectionPoint);
            Annotated annotated = injectionPoint.getAnnotated();
            Set<Annotation> qualifiers = injectionPoint.getQualifiers();
            System.out.println("produceBean:qualifiers = " + qualifiers);
            System.out.println("produceBean:annotated = " + annotated);
            if (annotated != null) {
                String value = annotated.getAnnotation(MethodProduced.class).value();
                System.out.println("producer @MethodProduced value = " + value);
            }
            return theBean;
        }

        @PostConstruct
        void pc() {
            System.out.println("pc2");
        }

        @PreDestroy
        void pd() {
            System.out.println("pd2");
        }

        @Nested
//        @Named("innererNesteder")
//        @RequestScoped
        class InnerInner {

            @PostConstruct
            void pc() {
                System.out.println("pc3");
            }

            @PreDestroy
            void pd() {
                System.out.println("pd3");
            }

            @Inject @FieldProduced
            Bean fieldProducedNestedInjectBean;
            @Inject @MethodProduced("gugus")
            Bean methodProducedNestedInjectBean;

            @Inject
            Inner inner;
            @Inject
            InnerTestClassAndBeanSameInstanceTest t;

            @Test
            void test(@MethodProduced("param") Bean methodProducedNestedInjectBean2) {
                assertNotNull(inner);
                assertNotNull(t);
                assertSame(t, InnerTestClassAndBeanSameInstanceTest.this);
                assertSame(inner, Inner.this);
                assertSameBean(theBean, fieldProducedNestedInjectBean);
                assertSameBean(theBean, methodProducedNestedInjectBean);
                assertSameBean(theBean, methodProducedNestedInjectBean2);
                
            }

        }

    }

}
