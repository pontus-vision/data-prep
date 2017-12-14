package org.talend.dataprep.parameters;

public class UISchemaParameter extends Parameter {

    private final String uiSchema;

    public UISchemaParameter(String uiSchema) {
        this.uiSchema = uiSchema;
    }

    public String getUISchema() {
        return uiSchema;
    }
}
