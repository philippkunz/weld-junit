package org.jboss.weld.junit5.auto.nested;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.jboss.weld.junit5.auto.WeldJunit5AutoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests a {@code @BeforeEach} method in superclass when actual test is in the inner class.
 * {@code @BeforeEach} should still be invoked for nested classes. For the below scenario to work,
 * we need to make the parent class an injection target as well.
 *
 * @see org.jboss.weld.junit5.nested.SuperClassOfNestedTestClassInjectionTargetTest
 */
@ExtendWith(WeldJunit5AutoExtension.class)
@AddBeanClasses(AutoSuperClassOfNestedTestClassInjectionTargetTest.MyBean1.class)
public class AutoSuperClassOfNestedTestClassInjectionTargetTest {

    interface MyBean {
        default String ping() {
            return getClass().getSimpleName();
        }
    }

    static class MyBean1 implements MyBean {
    }

    static class MyBean2 extends MyBean1 {
    }

    static class MyBean3 extends MyBean2 {
    }

    @Inject
    MyBean myBean;

    @Inject
    Instance<MyBean1> myBean1;

    @Inject
    Instance<MyBean2> myBean2;

    @Inject
    Instance<MyBean3> myBean3;

    MyBean injectedInBeforeEach;

    Set<Class<? extends MyBean>> resolvedBeansFoundInBeforeEach = new HashSet<>();

    @BeforeEach
    void setup() {
        assertNotNull(myBean);
        injectedInBeforeEach = myBean;

        resolvedBeansFoundInBeforeEach.clear();
        if (myBean1.isResolvable()) resolvedBeansFoundInBeforeEach.add(MyBean1.class);
        if (myBean2.isResolvable()) resolvedBeansFoundInBeforeEach.add(MyBean2.class);
        if (myBean3.isResolvable()) resolvedBeansFoundInBeforeEach.add(MyBean3.class);
    }

    @Test
    void testClass() {
        assertEquals(MyBean1.class.getSimpleName(), myBean.ping());
        assertEquals(MyBean1.class.getSimpleName(), injectedInBeforeEach.ping());

        assertTrue(resolvedBeansFoundInBeforeEach.contains(MyBean1.class));
        assertFalse(resolvedBeansFoundInBeforeEach.contains(MyBean2.class));
        assertFalse(resolvedBeansFoundInBeforeEach.contains(MyBean3.class));
    }

    @Nested
    @AddBeanClasses(MyBean2.class)
    class MyNestedTest {

        @Inject
        MyBean myNestedBean;

        @Test
        void testNestedClass() {
            assertEquals(MyBean2.class.getSimpleName(), myBean.ping());
            assertEquals(MyBean2.class.getSimpleName(), injectedInBeforeEach.ping());
            assertEquals(MyBean2.class.getSimpleName(), myNestedBean.ping());

            assertTrue(resolvedBeansFoundInBeforeEach.contains(MyBean1.class));
            assertTrue(resolvedBeansFoundInBeforeEach.contains(MyBean2.class));
            assertFalse(resolvedBeansFoundInBeforeEach.contains(MyBean3.class));
        }

        @Nested
        @AddBeanClasses(MyBean3.class)
        class TwiceNestedTest {

            @Inject
            MyBean myTwiceNestedBean;

            @Test
            void testTwiceNestedClass() {
                assertEquals(MyBean3.class.getSimpleName(), myBean.ping());
                assertEquals(MyBean3.class.getSimpleName(), injectedInBeforeEach.ping());
                assertEquals(MyBean3.class.getSimpleName(), myNestedBean.ping());
                assertEquals(MyBean3.class.getSimpleName(), myTwiceNestedBean.ping());

                assertTrue(resolvedBeansFoundInBeforeEach.contains(MyBean1.class));
                assertTrue(resolvedBeansFoundInBeforeEach.contains(MyBean2.class));
                assertTrue(resolvedBeansFoundInBeforeEach.contains(MyBean3.class));
            }

        }

    }

}
