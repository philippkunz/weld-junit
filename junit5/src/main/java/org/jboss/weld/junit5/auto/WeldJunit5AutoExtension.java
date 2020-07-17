/*
 * JBoss, Home of Professional Open Source
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldJunitEnricher;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

import static org.jboss.weld.junit5.ExtensionContextUtils.getExplicitInjectionInfoFromStore;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.inject.Instance;

/**
 * An alternative to {@link WeldJunit5Extension} allowing to fully leverage annotation based configuration approach.
 * When used, the extension will attempt to resolve all beans used in your test class and automatically adds them to
 * Weld container while bootstrapping it.
 * 
 * There is quite a few annotations which can be used to configure it further still:
 * @see ActivateScopes
 * @see AddBeanClasses
 * @see AddEnabledDecorators
 * @see AddEnabledInterceptors
 * @see AddExtensions
 * @see AddPackages
 * @see EnableAlternatives
 * @see EnableAlternativeStereotypes
 *
 * Note that this approach cannot be combined with {@link WeldJunit5Extension}, choose one or the other approach, not both.
 *
 * @see EnableAutoWeld
 * @see WeldJunitEnricher
 */
public class WeldJunit5AutoExtension extends WeldJunit5Extension {

    @Override
    protected void weldInit(ExtensionContext context, Weld weld, WeldInitiator.Builder weldInitiatorBuilder) {

        List<?> testInstances = context.getRequiredTestInstances().getAllInstances();
        List<Class<?>> testClasses = testInstances.stream().map(Object::getClass).collect(Collectors.toList());

        ClassScanning.scanForRequiredBeanClasses(testClasses, weld, getExplicitInjectionInfoFromStore(context));

//        weld.addBeanClasses(testInstances.stream().toArray(Class[]::new));
        weld.addBeanClass(DummyBean.class);
        weld.addExtension(new TestInstanceInjectionExtension(testInstances));

        testClasses.stream()
                .map(testClass -> AnnotationSupport.findRepeatableAnnotations(testClass, ActivateScopes.class))
                .flatMap(ann -> ann.stream().map(ActivateScopes::value))
                .forEach(weldInitiatorBuilder::activate);

    }

    protected WeldContainer initWeldContainer(WeldInitiator initiator, ExtensionContext context) {


        // this ensures the test class is injected into
        // in case of nested tests, this also injects into any outer classes
        initiator.addObjectsToInjectInto(context.getRequiredTestInstances().getAllInstances().stream().collect(Collectors.toSet()));

        WeldContainer weldContainer = initiator.initWeld(context.getRequiredTestClass());

//        // this ensures the test class is injected into
//        // in case of nested tests, this also injects into any outer classes
//        System.out.println("going to resolve testInstanceInstances");
//        List<Instance<?>> testInstances = context.getRequiredTestInstances().getAllInstances().stream().map(Object::getClass).map(weldContainer::select).collect(Collectors.toList());
//        List<?> testInstanceInstances = testInstances.stream().map(Instance::get).collect(Collectors.toList());
//        System.out.println("testInstanceInstances = " + testInstanceInstances);
//        // TODO: dispose testInstanceInstances

        return weldContainer;
    }

}
