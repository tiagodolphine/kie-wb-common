/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.workbench.common.widgets.configresource.client.widget.unbound;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import org.appformer.project.datamodel.imports.Import;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Label;
import org.gwtbootstrap3.client.ui.constants.ButtonSize;
import org.gwtbootstrap3.client.ui.constants.ButtonType;
import org.gwtbootstrap3.client.ui.constants.IconType;
import org.gwtbootstrap3.client.ui.gwt.ButtonCell;
import org.gwtbootstrap3.client.ui.gwt.CellTable;
import org.kie.workbench.common.widgets.configresource.client.resources.i18n.ImportConstants;
import org.uberfire.client.mvp.LockRequiredEvent;
import org.uberfire.ext.widgets.common.client.common.BusyPopup;

public class ImportsWidgetViewImpl
        extends Composite
        implements ImportsWidgetView {

    interface Binder
            extends UiBinder<Widget, ImportsWidgetViewImpl> {

    }

    private static Binder uiBinder = GWT.create( Binder.class );

    @UiField
    Button addImportButton;

    @UiField(provided = true)
    CellTable<Import> table = new CellTable<Import>();

    @Inject
    private AddImportPopup addImportPopup;

    @Inject
    private javax.enterprise.event.Event<LockRequiredEvent> lockRequired;

    private List<Import> importTypes = new ArrayList<Import>();
    private ListDataProvider<Import> dataProvider = new ListDataProvider<Import>();
    private final Command addImportCommand = makeAddImportCommand();

    private ImportsWidgetView.Presenter presenter;

    private boolean isReadOnly = false;

    public ImportsWidgetViewImpl() {
        setup();
        initWidget( uiBinder.createAndBindUi( this ) );

        //Disable until content is loaded
        addImportButton.setEnabled( false );
    }

    private void setup() {
        //Setup table
        table.setStriped( true );
        table.setCondensed( true );
        table.setBordered( true );
        table.setEmptyTableWidget( new Label( ImportConstants.INSTANCE.noImportsDefined() ) );

        //Columns
        final TextColumn<Import> importTypeColumn = new TextColumn<Import>() {

            @Override
            public String getValue( final Import importType ) {
                return importType.getType();
            }
        };

        final ButtonCell deleteImportButton = new ButtonCell( IconType.TRASH, ButtonType.DANGER, ButtonSize.SMALL );
        final Column<Import, String> deleteImportColumn = new Column<Import, String>( deleteImportButton ) {
            @Override
            public String getValue( final Import importType ) {
                return ImportConstants.INSTANCE.remove();
            }
        };
        deleteImportColumn.setFieldUpdater( new FieldUpdater<Import, String>() {
            public void update( final int index,
                                final Import importType,
                                final String value ) {
                if ( isReadOnly ) {
                    return;
                }
                if ( Window.confirm( ImportConstants.INSTANCE.promptForRemovalOfImport0( importType.getType() ) ) ) {
                    dataProvider.getList().remove( index );
                }
            }
        } );

        table.addColumn( importTypeColumn,
                         new TextHeader( ImportConstants.INSTANCE.importType() ) );
        table.addColumn( deleteImportColumn,
                         ImportConstants.INSTANCE.remove() );

        //Link data
        dataProvider.addDataDisplay( table );
        dataProvider.setList( importTypes );
    }

    @Override
    public void init( final ImportsWidgetView.Presenter presenter ) {
        this.presenter = presenter;
    }

    @Override
    public void setContent( final List<Import> importTypes,
                            final boolean isReadOnly ) {
        this.importTypes = importTypes;
        this.dataProvider.setList( importTypes );
        this.addImportButton.setEnabled( !isReadOnly );
        this.isReadOnly = isReadOnly;
    }

    @UiHandler("addImportButton")
    public void onClickAddImportButton( final ClickEvent event ) {
        addImportPopup.setCommand( addImportCommand );
        addImportPopup.show();
    }

    private Command makeAddImportCommand() {
        return new Command() {

            @Override
            public void execute() {
                final Import importType = new Import( addImportPopup.getImportType() );
                dataProvider.getList().add( importType );
                lockRequired.fire( new LockRequiredEvent() );
            }
        };
    }

    @Override
    public void showBusyIndicator( final String message ) {
        BusyPopup.showMessage( message );
    }

    @Override
    public void hideBusyIndicator() {
        BusyPopup.close();
    }
}
