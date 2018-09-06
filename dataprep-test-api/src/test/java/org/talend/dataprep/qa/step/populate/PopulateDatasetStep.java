// ============================================================================
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

package org.talend.dataprep.qa.step.populate;

import au.com.bytecode.opencsv.CSVWriter;
import cucumber.api.DataTable;
import cucumber.api.java.en.Given;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.talend.dataprep.qa.config.DataPrepStep;
import org.talend.dataprep.qa.step.ActionStep;
import org.talend.dataprep.qa.step.DatasetStep;
import org.talend.dataprep.qa.step.PreparationStep;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.talend.dataprep.qa.config.FeatureContext.removeSuffixName;

/**
 * Step dealing with dataset.
 */
public class PopulateDatasetStep extends DataPrepStep {

    @Autowired
    private DatasetStep datasetStep;

    @Autowired
    private PreparationStep preparationStep;

    @Autowired
    private ActionStep actionStep;

    @Given("^I upload \"(.*)\" times the dataset \"(.*)\" with name \"(.*)\"$") //
    public void givenIUploadTheDataSetNTime(Integer nbTime, String fileName, String name) throws IOException {
        for (int i = 0; i < nbTime; i++) {
            String finalDatasetName = i + "_" + name;
            datasetStep.givenIUploadTheDataSet(fileName, finalDatasetName);
        }
    }

    @Given("^I create \"(.*)\" preparation \"(.*)\" with random dataset and \"(.*)\" steps with parameters:$")
    public void givenICreatePrepWithNStepNTime(Integer nbTime, String preparationName, Integer nbStep,
            DataTable dataTable) throws IOException {
        for (int i = 0; i < nbTime; i++) {
            String datasetName = removeSuffixName(getRandomDatasetName());
            String finalPrepName = i + "_" + preparationName;
            preparationStep.givenICreateAPreparation(finalPrepName, datasetName);
            preparationStep.loadPreparationMultipleTimes(1, finalPrepName);
            for (int j = 0; j < (nbStep / 2); j++) {
                iCreateMultipleStepForPrep(finalPrepName, dataTable);
            }
        }
    }

    private void iCreateMultipleStepForPrep(String finalPrepName, DataTable dataTable) throws IOException {
        actionStep.whenIAddAStepToAPreparation("uppercase", finalPrepName, dataTable);
        preparationStep.loadPreparationMultipleTimes(1, finalPrepName);
        actionStep.whenIAddAStepToAPreparation("lowercase", finalPrepName, dataTable);
        preparationStep.loadPreparationMultipleTimes(1, finalPrepName);
    }

    private String getRandomDatasetName() {
        List<String> listDataset = context.getDatasetNames();
        Random rand = new Random();
        return listDataset.get(rand.nextInt(listDataset.size()));
    }

    @Given("^I upload a random dataset with (\\d+) columns and (\\d+) rows with name \"([^\"]*)\"$")
    public void iUploadARandomDatasetWithColumnsAndRowsWithName(int nbColumns, int nbRows, String datasetName) throws Throwable {
        Path tempFile = generateRandomDataset(nbColumns, nbRows, datasetName);
        System.out.println("tempFile. = " + tempFile);
        datasetStep.givenIUploadTheDataSet(tempFile.toString(), datasetName);
    }

    private Path generateRandomDataset(int nbColumns, int nbRows, String datasetName) throws IOException {
        Path tempFile = Files.createTempFile(datasetName, ".csv");
        FileWriter fileWriter = new FileWriter(tempFile.toFile());
        CSVWriter writer = new CSVWriter(fileWriter);

        List<String[]> datas = new ArrayList<>(nbRows + 1);

        datas.add(generateHeader(nbColumns));

        for (int i = 0; i < nbRows; i++) {
            datas.add(generateData(nbColumns));
        }

        writer.writeAll(datas);
        writer.close();

        return tempFile;
    }

    private String[] generateData(int nbColumns) {
        String[] data = new String[nbColumns];
        for (int i = 0; i < nbColumns; i++) {
            data[i] = RandomStringUtils.randomAlphabetic(10);
        }
        return data;
    }

    private String[] generateHeader(int nbColumns) {
        String[] header = new String[nbColumns];
        for (int i = 0; i < nbColumns; i++) {
            header[i] = "col" + i;
        }
        return header;
    }

}
