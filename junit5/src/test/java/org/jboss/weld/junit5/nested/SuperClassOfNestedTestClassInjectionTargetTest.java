package org.jboss.weld.junit5.nested;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests a {@code @BeforeEach} method in superclass when actual test is in the inner class.
 * {@code @BeforeEach} should still be invoked for nested classes. For the below scenario to work,
 * we need to make the parent class an injection target as well.
 */
@ExtendWith(WeldJunit5Extension.class)
public class SuperClassOfNestedTestClassInjectionTargetTest {

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

    @WeldSetup
    WeldInitiator weld = WeldInitiator.of(MyBean1.class);

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
    class MyNestedTest {

        @WeldSetup
        WeldInitiator weld = WeldInitiator.of(MyBean2.class);

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
        class TwiceNestedTest {

            @WeldSetup
            WeldInitiator weld = WeldInitiator.of(MyBean3.class);

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
