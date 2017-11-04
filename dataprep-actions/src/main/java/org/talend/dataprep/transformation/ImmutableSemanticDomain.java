/*
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 *
 * This source code is available under agreement available at
 * https://github.com/Talend/data-prep/blob/master/LICENSE
 *
 * You should have received a copy of the agreement
 * along with this program; if not, write to Talend SA
 * 9 rue Pages 92150 Suresnes, France
 */

package org.talend.dataprep.transformation;

public class ImmutableSemanticDomain {

    private final String id;

    private final String label;

    private final float frequency;

    public ImmutableSemanticDomain(String id, String label, float frequency) {
        this.id = id;
        this.label = label;
        this.frequency = frequency;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public float getFrequency() {
        return frequency;
    }
}
