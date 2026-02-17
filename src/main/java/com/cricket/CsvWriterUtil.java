package com.cricket;

import java.io.FileWriter;

import com.opencsv.CSVWriter;

public class CsvWriterUtil {

    private CSVWriter writer;

    public void open(String fileName) throws Exception {
        writer = new CSVWriter(new FileWriter(fileName));
    }

    // 🔥 Generic header (any number of columns)
    public void writeHeader(String[] header) {
        writer.writeNext(header);
    }

    // 🔥 Generic row (any number of columns)
    public void writeRow(Object... values) {

        String[] row = new String[values.length];

        for (int i = 0; i < values.length; i++) {
            row[i] = String.valueOf(values[i]);
        }

        writer.writeNext(row);
    }

    public void close() throws Exception {
        writer.close();
    }
}