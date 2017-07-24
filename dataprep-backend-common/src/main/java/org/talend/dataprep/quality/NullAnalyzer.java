// ============================================================================
// Copyright (C) 2006-2016 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// https://github.com/Talend/data-prep/blob/master/LICENSE
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================

package org.talend.dataprep.quality;

import java.util.Collections;
import java.util.List;

import org.talend.dataquality.common.inference.Analyzer;
import org.talend.dataquality.common.inference.Analyzers;

public class NullAnalyzer implements Analyzer<Analyzers.Result> {

    public static final Analyzer<Analyzers.Result> INSTANCE = new NullAnalyzer();

    private static final long serialVersionUID = 1L;

    private NullAnalyzer() {
    }

    @Override
    public void init() {
        // Nothing to do
    }

    @Override

    public boolean analyze(String... strings) {
        // Nothing to do
        return true;
    }

    @Override
    public void end() {
        // Nothing to do
    }

    @Override
    public List<Analyzers.Result> getResult() {
        return Collections.emptyList();
    }

    @Override
    public Analyzer<Analyzers.Result> merge(Analyzer<Analyzers.Result> analyzer) {
        return this;
    }

    @Override
    public void close() throws Exception {
        // Nothing to do
    }
}
