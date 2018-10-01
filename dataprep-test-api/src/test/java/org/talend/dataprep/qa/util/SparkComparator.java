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

package org.talend.dataprep.qa.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.IOUtils;

public class SparkComparator {

    /**
     * Comparator for Spark disordered files. Read the rows of the files, order them in the same way then compare the 2
     * lists of rows.
     * 
     * @param input1
     * @param input2
     * @return true if the files have the same rows once those rows are ordered
     */
    public static boolean compareTwoFile(InputStream input1, InputStream input2) {
        boolean equalFile = false;

        try {
            List<String> input1Lines = IOUtils.readLines(input1, "UTF-8");
            List<String> input2Lines = IOUtils.readLines(input2, "UTF-8");

            Collections.sort(input1Lines);
            Collections.sort(input2Lines);

            equalFile = Arrays.equals(input1Lines.toArray(), input1Lines.toArray());

        } catch (IOException e) {
            e.printStackTrace();
        }

        return equalFile;
    }

}
