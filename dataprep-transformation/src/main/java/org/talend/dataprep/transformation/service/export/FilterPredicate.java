package org.talend.dataprep.transformation.service.export;

import java.io.Serializable;
import java.util.function.Function;
import java.util.function.Predicate;

import org.talend.dataprep.api.dataset.RowMetadata;
import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.api.filter.FilterService;
import org.talend.dataprep.transformation.actions.Providers;

public class FilterPredicate implements Function<RowMetadata, Predicate<DataSetRow>>, Serializable {

    private final String filter;

    public FilterPredicate(String filter) {
        this.filter = filter;
    }

    @Override
    public Predicate<DataSetRow> apply(RowMetadata rowMetadata) {
        return Providers.get(FilterService.class).build(filter, rowMetadata);
    }
}
