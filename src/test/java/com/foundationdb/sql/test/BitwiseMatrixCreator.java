/**
 * Copyright (C) 2009-2013 FoundationDB, LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.foundationdb.sql.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Date;
import java.util.Random;

/*
 * creates tests for bitwise math operations
 * */
public class BitwiseMatrixCreator implements Runnable {

    StringBuffer data = new StringBuffer();
    StringBuffer sql_data = new StringBuffer();
    StringBuffer verify = new StringBuffer();
    Random r;
    private int counter = 0;

    /**
     * @param args
     */
    public static void main(String[] args) {
        BitwiseMatrixCreator b = new BitwiseMatrixCreator();
        b.run();
    }

    @Override
    public void run() {
        r = new Random(452342435);
        verify.append("# generated by com.foundationdb.sql.test.BitwiseMatrixCreator on "
                + new Date() + System.getProperty("line.separator"));
        verify.append("---" + System.getProperty("line.separator")
                + "- Include: all-bitwise-matrix-schema.yaml"
                + System.getProperty("line.separator"));

        data.append("# generated by com.foundationdb.sql.test.BitwiseMatrixCreator on "
                + new Date() + System.getProperty("line.separator"));
        data.append("# Create a table with all supported data types"
                + System.getProperty("line.separator") + "---"
                + System.getProperty("line.separator")
                + "- CreateTable: bitwise_matrix (" + "        id integer,"
                + "        source1 bigint unsigned,"
                + "        source2 bigint unsigned,"
                + "        bitwise_and bigint," + "        bitwise_or bigint,"
                + "        bitwise_xor bigint,"
                + "        bitwise_rightshift varchar(255),"
                + "        bitwise_leftshift varchar(255),"
                + "        bitwise_invert int" + "        )"
                + System.getProperty("line.separator"));
        sql_data.append("use test;" + System.getProperty("line.separator"));
        sql_data.append("drop Table bitwise_matrix;"
                + System.getProperty("line.separator"));
        sql_data.append("Create Table bitwise_matrix (" + "        id integer,"
                + "        source1 bigint unsigned,"
                + "        source2 bigint unsigned,"
                + "        bitwise_and bigint," + "        bitwise_or bigint,"
                + "        bitwise_xor bigint,"
                + "        bitwise_rightshift varchar(255),"
                + "        bitwise_leftshift varchar(255),"
                + "        bitwise_invert bigint" + "        );"
                + System.getProperty("line.separator"));

        for (int x = 0; x < 25; x++) {
            recordSQL();
        }

        data.append("..." + System.getProperty("line.separator"));
        verify.append("..." + System.getProperty("line.separator"));

        try {
            // will drop the files in the same branch as where this ciode is running
            // sql gets dropped in the root directory of branch
            String path = System.getProperty("user.dir")
                    + "/src/test/resources/com/foundationdb/sql/pg/yaml/functional/";
            System.out.println(path);
            save(path + "all-bitwise-matrix-schema.yaml", data);
            save("all-bitwise-matrix-schema.sql", sql_data);
            save(path + "test-bitwise-matrix.yaml", verify);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private String genSQL(boolean sql, BigInteger source1, BigInteger source2) {
        String database = "bitwise_matrix";
        if (sql) {
            database = "test." + database;
        }

        return "insert into " + database + " (" + "id," + "source1,"
                + "source2," + "bitwise_and," + "bitwise_or," + "bitwise_xor,"
                + "bitwise_rightshift," + "bitwise_leftshift,"
                + "bitwise_invert" + ") values (" + (counter) + "," + source1
                + "," + source2 + "," + (source1.and(source2)).toString() + ","
                + (source1.or(source2)).toString() + ","
                + (source1.xor(source2)).toString() + ",'"
                + (source1.shiftRight(source2.intValue())) + "','"
                + (source1.shiftLeft(source2.intValue())) + "',"
                + (source1.andNot(source2)) + " )";
    }

    private String returnYAMLselects() {
        return "---"
                + System.getProperty("line.separator")
                + "- Statement: select id,source1,source2,BITAND(source1,source2),BITXOR(source1,source2),BITOR(source1,source2) from bitwise_matrix where "
                + "((BITAND(source1,source2) != bitwise_and) or "
                + "(BITXOR(source1,source2) != bitwise_xor) or "
                + "(BITOR(source1,source2) != bitwise_or)) and id="
                + counter
                + System.getProperty("line.separator")
                + "- row_count: 0"
                + System.getProperty("line.separator")
                + "---"
                + System.getProperty("line.separator")
                + "- Statement: select id,source1,source2 from bitwise_matrix where "
                + "((RIGHTSHIFT(source1,source2) != bitwise_rightshift) or "
                + "(LEFTSHIFT(source1,source2) != bitwise_leftshift)) and id="
                + counter + System.getProperty("line.separator")
                + "- row_count: 0" + System.getProperty("line.separator");
    }

    private void recordSQL() {
        counter++;
        BigInteger source1 = BigInteger.valueOf(r.nextInt(10));
        BigInteger source2 = BigInteger.valueOf(r.nextInt(10));
        data.append("---" + System.getProperty("line.separator")
                + "- Statement: " + genSQL(false, source1, source2)
                + System.getProperty("line.separator"));
        verify.append(returnYAMLselects());
        sql_data.append(genSQL(true, source1, source2) + ";"
                + System.getProperty("line.separator"));
    }

    private void save(String filename, StringBuffer data) throws IOException {
        try {
            // Create file
            FileWriter fstream = new FileWriter(filename);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(data.toString());
            // Close the output stream
            out.close();
        } catch (Exception e) {// Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }
        File f = new File(filename);
        System.out.println(f.getCanonicalPath());
        System.out.println(data.toString());

    }

}