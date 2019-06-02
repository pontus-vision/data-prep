// ============================================================================
//
// Copyright (C) 2006-2018 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// https://github.com/Talend/data-prep/blob/master/LICENSE
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================

package org.talend.dataprep.api.type;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.talend.dataprep.api.dataset.ColumnMetadata;
import org.talend.dataquality.semantic.api.CategoryRegistryManager;
import org.talend.dataquality.semantic.classifier.SemanticCategoryEnum;
import org.talend.dataquality.semantic.model.DQCategory;
import org.talend.dataquality.statistics.type.DataTypeEnum;

public class TypeUtils {

    private TypeUtils() {
    }

    /**
     * Compute the dataset metadata columns valid/invalid, empty/count values.
     *
     * @return the dataset column types in DQ libraries.
     * @param columns The Data Prep {@link ColumnMetadata columns} to convert to DQ library's types.
     */
    public static DataTypeEnum[] convert(List<ColumnMetadata> columns) {
        DataTypeEnum[] types = new DataTypeEnum[columns.size()];
        for (int i = 0; i < columns.size(); i++) {
            final String type = columns.get(i).getType();
            types[i] = convert(Type.get(type));
        }
        return types;
    }

    private static DataTypeEnum convert(Type type) {
        switch (type) {
        case ANY:
        case STRING:
            return DataTypeEnum.STRING;
        case NUMERIC:
            return DataTypeEnum.INTEGER;
        case INTEGER:
            return DataTypeEnum.INTEGER;
        case DOUBLE:
        case FLOAT:
            return DataTypeEnum.DOUBLE;
        case BOOLEAN:
            return DataTypeEnum.BOOLEAN;
        case DATE:
            return DataTypeEnum.DATE;
        default:
            return DataTypeEnum.STRING;
        }

    }

    /**
     * @param categoryId A category id from supported {@link SemanticCategoryEnum categories}.
     * @return A display name for the category id or empty string if none found.
     * @see SemanticCategoryEnum
     */
    public static String getDomainLabel(String categoryId) {
        final DQCategory category = CategoryRegistryManager.getInstance().getCategoryMetadataByName(categoryId);
        return category == null ? StringUtils.EMPTY : category.getLabel();
    }

}
