/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.workbench.common.screens.examples.backend.server;

import javax.enterprise.event.Event;

import org.guvnor.common.services.project.events.NewProjectEvent;
import org.guvnor.common.services.shared.metadata.MetadataService;
import org.guvnor.structure.backend.config.ConfigurationFactoryImpl;
import org.guvnor.structure.organizationalunit.OrganizationalUnitService;
import org.guvnor.structure.repositories.EnvironmentParameters;
import org.guvnor.structure.repositories.RepositoryService;
import org.guvnor.structure.server.config.ConfigGroup;
import org.guvnor.structure.server.config.ConfigItem;
import org.guvnor.structure.server.config.ConfigType;
import org.guvnor.structure.server.config.ConfigurationFactory;
import org.guvnor.structure.server.repositories.RepositoryFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.workbench.common.screens.examples.model.ExampleRepository;
import org.kie.workbench.common.services.shared.project.KieProjectService;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.uberfire.io.IOService;
import org.uberfire.mocks.EventSourceMock;
import org.uberfire.rpc.SessionInfo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ExamplesServiceImplCheckNoIndexConfigTest {

    @Mock
    private IOService ioService;

    private ConfigurationFactory configurationFactory = spy(new ConfigurationFactoryImpl());

    @Mock
    private RepositoryFactory repositoryFactory;

    @Mock
    private KieProjectService projectService;

    @Mock
    private RepositoryService repositoryService;

    @Mock
    private OrganizationalUnitService ouService;

    @Mock
    private MetadataService metadataService;

    @Spy
    private Event<NewProjectEvent> newProjectEvent = new EventSourceMock<NewProjectEvent>() {
        @Override
        public void fire(final NewProjectEvent event) {
            //Do nothing. Default implementation throws an exception.
        }
    };

    @Mock
    private SessionInfo sessionInfo;

    private ExamplesServiceImpl service;

    @Before
    public void setup() {
        service = spy(new ExamplesServiceImpl(ioService,
                                              configurationFactory,
                                              repositoryFactory,
                                              projectService,
                                              repositoryService,
                                              ouService,
                                              metadataService,
                                              newProjectEvent,
                                              sessionInfo));
    }

    @Test
    public void testCheckRepositoryConfig_NoIndex() {
        final ConfigGroup configGroup = new ConfigGroup();
        when(configurationFactory.newConfigGroup(any(ConfigType.class),
                                                 anyString(),
                                                 anyString())).thenReturn(configGroup);

        service.getProjects(new ExampleRepository("https://github.com/guvnorngtestuser1/guvnorng-playground.git"));

        final ConfigItem item = configGroup.getConfigItem(EnvironmentParameters.AVOID_INDEX);
        assertNotNull(item);

        assertEquals("true", item.getValue());
    }
}
