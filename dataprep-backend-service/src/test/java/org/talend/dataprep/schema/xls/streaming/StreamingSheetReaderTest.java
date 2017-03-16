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

package org.talend.dataprep.schema.xls.streaming;

import java.io.InputStream;
import java.util.Iterator;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.model.SharedStringsTable;
import org.junit.Test;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

public class StreamingSheetReaderTest {

    @Test
    public void randomTest() throws Exception {
        XSSFReader reader = new XSSFReader(
                OPCPackage.open(getClass().getResourceAsStream("../tdp-3459_big-column-metadata.xml")));
        SharedStringsTable sst = reader.getSharedStringsTable();
        Iterator<InputStream> sheets = reader.getSheetsData();

        XMLReader parser = fetchSheetParser(sst);

        System.out.println("Processing new sheet:\n");
        InputStream sheet = sheets.next();
        InputSource sheetSource = new InputSource(sheet);
        parser.parse(sheetSource);
        sheet.close();
        System.out.println("DONE");
    }


    public XMLReader fetchSheetParser(SharedStringsTable sst) throws SAXException {
        XMLReader parser =
                XMLReaderFactory.createXMLReader(
                        "org.apache.xerces.parsers.SAXParser"
                );
        ContentHandler handler = new DefaultHandler() {};
        parser.setContentHandler(handler);
        return parser;
    }


}
