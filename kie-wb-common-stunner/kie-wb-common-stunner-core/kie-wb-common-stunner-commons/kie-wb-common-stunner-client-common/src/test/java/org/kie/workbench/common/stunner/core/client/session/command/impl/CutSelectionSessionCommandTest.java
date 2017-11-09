/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.workbench.common.stunner.core.client.session.command.impl;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.workbench.common.stunner.core.client.event.keyboard.KeyboardEvent.Key;
import org.kie.workbench.common.stunner.core.client.session.command.ClientSessionCommand;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class CutSelectionSessionCommandTest extends BaseSessionCommandKeyboardTest {

    private CutSelectionSessionCommand cutSelectionSessionCommand;

    @Mock
    private CopySelectionSessionCommand copySelectionSessionCommand;

    @Mock
    private DeleteSelectionSessionCommand deleteSelectionSessionCommand;

    @Mock
    private ClientSessionCommand.Callback mainCallback;

    @Before
    public void setUp() throws Exception {
        super.setup();
        this.cutSelectionSessionCommand = getCommand();
    }

    @Test
    public void testExecute() {
        cutSelectionSessionCommand.execute(mainCallback);
        ArgumentCaptor<ClientSessionCommand.Callback> callbackArgumentCaptor = ArgumentCaptor.forClass(ClientSessionCommand.Callback.class);
        verify(copySelectionSessionCommand).execute(callbackArgumentCaptor.capture());

        //success
        callbackArgumentCaptor.getValue().onSuccess();
        verify(deleteSelectionSessionCommand, times(1)).execute(mainCallback);

        //error
        Object error = new Object();
        callbackArgumentCaptor.getValue().onError(error);
        verify(mainCallback, times(1)).onError(error);
    }

    @Override
    protected CutSelectionSessionCommand getCommand() {
        return new CutSelectionSessionCommand(copySelectionSessionCommand, deleteSelectionSessionCommand);
    }

    @Override
    protected Key[] getExpectedKeys() {
        return new Key[]{Key.CONTROL, Key.X};
    }

    @Override
    protected Key[] getUnexpectedKeys() {
        return new Key[]{Key.ESC};
    }
}