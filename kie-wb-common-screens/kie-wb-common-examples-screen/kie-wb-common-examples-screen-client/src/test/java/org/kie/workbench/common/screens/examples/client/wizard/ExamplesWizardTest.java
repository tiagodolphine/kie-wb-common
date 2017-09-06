/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
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

package org.kie.workbench.common.screens.examples.client.wizard;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.enterprise.event.Event;

import org.guvnor.common.services.project.context.ProjectContextChangeEvent;
import org.jboss.errai.common.client.api.Caller;
import org.jboss.errai.ui.client.local.spi.TranslationService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.workbench.common.screens.examples.client.wizard.model.ExamplesWizardModel;
import org.kie.workbench.common.screens.examples.client.wizard.pages.targetrepository.TargetRepositoryPage;
import org.kie.workbench.common.screens.examples.client.wizard.pages.project.ProjectPage;
import org.kie.workbench.common.screens.examples.client.wizard.pages.sourcerepository.SourceRepositoryPage;
import org.kie.workbench.common.screens.examples.model.ExampleOrganizationalUnit;
import org.kie.workbench.common.screens.examples.model.ExampleRepository;
import org.kie.workbench.common.screens.examples.model.ExampleTargetRepository;
import org.kie.workbench.common.screens.examples.model.ExamplesMetaData;
import org.kie.workbench.common.screens.examples.service.ExamplesService;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.uberfire.client.callbacks.Callback;
import org.uberfire.ext.widgets.common.client.common.BusyIndicatorView;
import org.uberfire.ext.widgets.core.client.wizards.WizardView;
import org.uberfire.mocks.CallerMock;
import org.uberfire.mocks.EventSourceMock;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ExamplesWizardTest {

    private static final String EXAMPLE_REPOSITORY1 = "https://github.com/guvnorngtestuser1/guvnorng-playground.git";
    private static final String EXAMPLE_ORGANIZATIONAL_UNIT1 = "ou1";
    private static final String EXAMPLE_ORGANIZATIONAL_UNIT2 = "ou2";
    private final WizardView mockView = mock(WizardView.class);
    private final ExampleRepository repository = new ExampleRepository(EXAMPLE_REPOSITORY1);
    private final Set<ExampleOrganizationalUnit> organizationalUnits = new HashSet<ExampleOrganizationalUnit>() {{
        add(new ExampleOrganizationalUnit(EXAMPLE_ORGANIZATIONAL_UNIT1));
        add(new ExampleOrganizationalUnit(EXAMPLE_ORGANIZATIONAL_UNIT2));
    }};
    @Mock
    private SourceRepositoryPage sourceRepositoryPage;
    @Mock
    private ProjectPage projectPage;
    @Mock
    private TargetRepositoryPage organizationalUnitPage;
    @Mock
    private BusyIndicatorView busyIndicatorView;
    private ExamplesService examplesService = mock(ExamplesService.class);
    private Caller<ExamplesService> examplesServiceCaller = new CallerMock<ExamplesService>(examplesService);
    @Spy
    private Event<ProjectContextChangeEvent> event = new EventSourceMock<ProjectContextChangeEvent>() {
        @Override
        public void fire(final ProjectContextChangeEvent event) {
            //Do nothing. Default implementation throws an exception.
        }
    };
    @Mock
    private TranslationService translator;
    @Captor
    private ArgumentCaptor<ExampleRepository> repositoryArgumentCaptor;
    @Mock
    private Callback<Boolean> callback;
    private ExamplesMetaData metaData = new ExamplesMetaData(repository,
                                                             organizationalUnits);

    private ExamplesWizard wizard;

    @Before
    public void setup() {
        wizard = new ExamplesWizard(sourceRepositoryPage,
                                    projectPage,
                                    organizationalUnitPage,
                                    busyIndicatorView,
                                    examplesServiceCaller,
                                    event,
                                    translator) {
            {
                this.view = mockView;
            }
        };
        when(examplesService.getMetaData()).thenReturn(metaData);
    }

    @Test
    public void testStart() {
        final ArgumentCaptor<ExamplesWizardModel> modelArgumentCaptor = ArgumentCaptor.forClass(ExamplesWizardModel.class);

        wizard.start();
        verify(sourceRepositoryPage,
               times(1)).initialise();
        verify(projectPage,
               times(1)).initialise();
        verify(organizationalUnitPage,
               times(1)).initialise();
        verify(sourceRepositoryPage,
               times(1)).setModel(modelArgumentCaptor.capture());
        verify(projectPage,
               times(1)).setModel(modelArgumentCaptor.getValue());
        verify(organizationalUnitPage,
               times(1)).setModel(modelArgumentCaptor.getValue());
        verify(sourceRepositoryPage,
               times(1)).setPlaygroundRepository(repositoryArgumentCaptor.capture());

        assertEquals(repository,
                     repositoryArgumentCaptor.getValue());
    }

    @Test
    public void testClose() {
        wizard.close();

        verify(sourceRepositoryPage,
               times(1)).destroy();
        verify(projectPage,
               times(1)).destroy();
        verify(organizationalUnitPage,
               times(1)).destroy();
    }

    @Test
    public void testGetPageWidget() {
        wizard.getPageWidget(0);
        verify(sourceRepositoryPage,
               times(1)).prepareView();
        verify(sourceRepositoryPage,
               times(1)).asWidget();

        wizard.getPageWidget(1);
        verify(projectPage,
               times(1)).prepareView();
        verify(projectPage,
               times(1)).asWidget();

        wizard.getPageWidget(2);
        verify(organizationalUnitPage,
               times(1)).prepareView();
        verify(organizationalUnitPage,
               times(1)).asWidget();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testIsComplete_RepositoryPageIncomplete() {
        doAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(final InvocationOnMock invocation) throws Throwable {
                final Callback<Boolean> callback = (Callback<Boolean>) invocation.getArguments()[0];
                callback.callback(false);
                return null;
            }
        }).when(sourceRepositoryPage).isComplete(any(Callback.class));

        wizard.isComplete(callback);

        verify(callback,
               times(1)).callback(eq(true));
        verify(callback,
               times(1)).callback(eq(false));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testIsComplete_ProjectPageIncomplete() {
        doAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(final InvocationOnMock invocation) throws Throwable {
                final Callback<Boolean> callback = (Callback<Boolean>) invocation.getArguments()[0];
                callback.callback(false);
                return null;
            }
        }).when(projectPage).isComplete(any(Callback.class));

        wizard.isComplete(callback);

        verify(callback,
               times(1)).callback(eq(true));
        verify(callback,
               times(1)).callback(eq(false));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testIsComplete_OrganizationalUnitPageIncomplete() {
        doAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(final InvocationOnMock invocation) throws Throwable {
                final Callback<Boolean> callback = (Callback<Boolean>) invocation.getArguments()[0];
                callback.callback(false);
                return null;
            }
        }).when(organizationalUnitPage).isComplete(any(Callback.class));

        wizard.isComplete(callback);

        verify(callback,
               times(1)).callback(eq(true));
        verify(callback,
               times(1)).callback(eq(false));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testIsComplete_AllPagesComplete() {
        doAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(final InvocationOnMock invocation) throws Throwable {
                final Callback<Boolean> callback = (Callback<Boolean>) invocation.getArguments()[0];
                callback.callback(true);
                return null;
            }
        }).when(sourceRepositoryPage).isComplete(any(Callback.class));
        doAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(final InvocationOnMock invocation) throws Throwable {
                final Callback<Boolean> callback = (Callback<Boolean>) invocation.getArguments()[0];
                callback.callback(true);
                return null;
            }
        }).when(projectPage).isComplete(any(Callback.class));
        doAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(final InvocationOnMock invocation) throws Throwable {
                final Callback<Boolean> callback = (Callback<Boolean>) invocation.getArguments()[0];
                callback.callback(true);
                return null;
            }
        }).when(organizationalUnitPage).isComplete(any(Callback.class));

        wizard.isComplete(callback);

        verify(callback,
               times(1)).callback(eq(true));
        verify(callback,
               never()).callback(eq(false));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testOnComplete() {
        wizard.start();
        wizard.complete();

        verify(busyIndicatorView,
               times(1)).showBusyIndicator(any(String.class));
        verify(busyIndicatorView,
               times(1)).hideBusyIndicator();
        verify(examplesService,
               times(1)).setupExamples(any(ExampleOrganizationalUnit.class),
                                       any(ExampleTargetRepository.class),
                                       anyString(),
                                       any(List.class));
        verify(event,
               times(1)).fire(any(ProjectContextChangeEvent.class));
    }

    @Test
    public void testSetDefaultTargetOrganizationalUnit() {
        wizard.setDefaultTargetOrganizationalUnit("testOU");

        ExampleOrganizationalUnit targetOrganizationalUnit = wizard.getModel().getTargetOrganizationalUnit();

        assertNotNull(targetOrganizationalUnit);
        assertEquals("testOU",
                     targetOrganizationalUnit.getName());
    }

    @Test
    public void testSetDefaultTargetRepository() {
        wizard.setDefaultTargetRepository("testRepository");

        ArgumentCaptor<ExampleTargetRepository> targetRepositoryArgumentCaptor = ArgumentCaptor.forClass(ExampleTargetRepository.class);

        verify(organizationalUnitPage,
               times(1)).setTargetRepository(targetRepositoryArgumentCaptor.capture());

        ExampleTargetRepository capturedRepository = targetRepositoryArgumentCaptor.getValue();

        assertNotNull(capturedRepository);
        assertEquals("testRepository",
                     capturedRepository.getAlias());
    }
}
