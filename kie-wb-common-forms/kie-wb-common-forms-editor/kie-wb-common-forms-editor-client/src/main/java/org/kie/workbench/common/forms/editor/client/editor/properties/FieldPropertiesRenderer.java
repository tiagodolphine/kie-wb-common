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

package org.kie.workbench.common.forms.editor.client.editor.properties;

import java.util.List;
import javax.annotation.PostConstruct;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import org.gwtbootstrap3.client.ui.Modal;
import org.kie.workbench.common.forms.dynamic.service.shared.FormRenderingContext;
import org.kie.workbench.common.forms.dynamic.service.shared.adf.DynamicFormModelGenerator;
import org.kie.workbench.common.forms.editor.client.editor.properties.binding.DataBindingEditor;
import org.kie.workbench.common.forms.editor.client.editor.properties.binding.DynamicFormModel;
import org.kie.workbench.common.forms.editor.client.editor.properties.binding.StaticFormModel;
import org.kie.workbench.common.forms.editor.service.shared.FormEditorRenderingContext;
import org.kie.workbench.common.forms.model.DynamicModel;
import org.kie.workbench.common.forms.model.FieldDefinition;
import org.kie.workbench.common.forms.model.FormModel;
import org.kie.workbench.common.forms.service.shared.FieldManager;

@Dependent
public class FieldPropertiesRenderer implements IsWidget {

    public interface FieldPropertiesRendererView extends IsWidget {

        void setPresenter(FieldPropertiesRenderer presenter);

        void render(FieldPropertiesRendererHelper helper,
                    FormEditorRenderingContext renderingContext,
                    DataBindingEditor editor);

        Modal getPropertiesModal();
    }

    private FieldPropertiesRendererView view;

    private DynamicFormModelGenerator dynamicFormModelGenerator;

    private DataBindingEditor staticDataBindingEditor;

    private DataBindingEditor dynamicDataBindingEditor;

    protected FieldDefinition fieldCopy;

    protected FieldPropertiesRendererHelper helper;

    private FieldManager fieldManager;

    private boolean acceptChanges = false;

    @Inject
    public FieldPropertiesRenderer(FieldPropertiesRendererView view,
                                   DynamicFormModelGenerator dynamicFormModelGenerator,
                                   @StaticFormModel DataBindingEditor staticDataBindingEditor,
                                   @DynamicFormModel DataBindingEditor dynamicDataBindingEditor,
                                   FieldManager fieldManager) {
        this.view = view;
        this.dynamicFormModelGenerator = dynamicFormModelGenerator;
        this.staticDataBindingEditor = staticDataBindingEditor;
        this.dynamicDataBindingEditor = dynamicDataBindingEditor;
        this.fieldManager = fieldManager;
    }

    @PostConstruct
    protected void init() {
        view.setPresenter(this);
    }

    public void render(final FieldPropertiesRendererHelper helper) {
        this.helper = helper;
        this.fieldCopy = resetFieldCopy(helper.getCurrentField());
        this.acceptChanges = false;
        render();
    }

    protected void render() {
        FormRenderingContext context = dynamicFormModelGenerator.getContextForModel(fieldCopy);
        if (context != null) {
            FormEditorRenderingContext renderingContext = new FormEditorRenderingContext(helper.getPath());
            renderingContext.setRootForm(context.getRootForm());
            renderingContext.getAvailableForms().putAll(context.getAvailableForms());
            renderingContext.setModel(fieldCopy);
            doRender(helper,
                     renderingContext);
        }
    }

    public FieldDefinition resetFieldCopy(final FieldDefinition originalField) {
        fieldCopy = fieldManager.getFieldFromProvider(originalField.getFieldType().getTypeName(), originalField.getFieldTypeInfo());
        fieldCopy.copyFrom(originalField);
        fieldCopy.setId(originalField.getId());
        fieldCopy.setName(originalField.getName());
        return fieldCopy;
    }

    public void onPressOk() {
        acceptChanges = true;
    }

    public void onClose() {
        if (acceptChanges) {
            doAcceptChanges();
        } else {
            doCancel();
        }
    }

    private void doAcceptChanges() {
        // apply the changes to the current field
        List<FieldDefinition> fields = helper.getCurrentRenderingContext().getRootForm().getFields();
        fields.remove(helper.getCurrentField());
        fields.add(fieldCopy);

        helper.onPressOk(fieldCopy);
    }

    private void doCancel() {
        helper.onClose();
    }

    public void onFieldTypeChange(final String typeCode) {
        fieldCopy = helper.onFieldTypeChange(fieldCopy,
                                             typeCode);
        render();
    }

    public void onFieldBindingChange(final String bindingExpression) {
        fieldCopy = helper.onFieldBindingChange(fieldCopy,
                                                bindingExpression);
        render();
    }

    protected void doRender(FieldPropertiesRendererHelper helper,
                            FormEditorRenderingContext context) {
        FormModel roodFormModel = helper.getCurrentRenderingContext().getRootForm().getModel();
        final DataBindingEditor editor = roodFormModel instanceof DynamicModel ? dynamicDataBindingEditor : staticDataBindingEditor;
        editor.init(fieldCopy,
                    helper,
                    () -> onFieldBindingChange(editor.getBinding()));
        view.render(helper,
                    context,
                    editor);
    }

    public FieldPropertiesRendererView getView() {
        return view;
    }

    public FieldDefinition getCurrentField() {
        return fieldCopy;
    }

    public List<String> getCompatibleFieldTypes() {
        return helper.getCompatibleFieldTypes(fieldCopy);
    }

    @Override
    public Widget asWidget() {
        return view.asWidget();
    }
}
