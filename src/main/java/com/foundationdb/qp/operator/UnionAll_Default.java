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

package com.foundationdb.qp.operator;

import com.foundationdb.ais.model.UserTable;
import com.foundationdb.qp.row.HKey;
import com.foundationdb.qp.row.Row;
import com.foundationdb.qp.rowtype.RowType;
import com.foundationdb.server.error.AkibanInternalException;
import com.foundationdb.server.explain.*;
import com.foundationdb.server.types.TInstance;
import com.foundationdb.server.types.value.ValueSource;
import com.foundationdb.util.ArgumentValidation;
import com.foundationdb.util.Strings;
import com.foundationdb.util.tap.InOutTap;

import com.google.common.base.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 <h1>Overview</h1>

 UnionAll_Default generates an output stream containing all the rows of both input streams. There are no
 guarantees on output order, and duplicates are not eliminated.

 <h1>Arguments</h1>

 <li><b>Operator input1:</b> Source of first input stream. 
 <li><b>RowType input1Type:</b> Type of rows in first input stream. 
 <li><b>Operator input2:</b> Source of second input stream. 
 <li><b>RowType input2Type:</b> Type of rows in second input stream. 
 <li><b>boolean openBoth:</b> Whether to open both input cursors at the same time rather than as needed. 

 <h1>Behavior</h1>

 The output from UnionAll_Default is formed by concatenating the first and second input streams.

 <h1>Output</h1>

 Rows of the first input stream followed by rows of the second input stream.

 <h1>Assumptions</h1>

 input1Type and input2Type are union-compatible. This means input1Type == input2Type or they have the same
 number of fields, and that corresponding field types match.

 <h1>Performance</h1>

 This operator does no IO.

 <h1>Memory Requirements</h1>

 None.

 */

final class UnionAll_Default extends Operator {
    @Override
    public List<Operator> getInputOperators() {
        return Collections.unmodifiableList(inputs);
    }

    @Override
    public RowType rowType() {
        return outputRowType;
    }

    @Override
    public String describePlan() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0, end = inputs.size(); i < end; ++i) {
            Operator input = inputs.get(i);
            sb.append(input);
            if (i + 1 < end)
                sb.append(Strings.nl()).append("UNION ALL").append(Strings.nl());
        }
        return sb.toString();
    }

    @Override
    protected Cursor cursor(QueryContext context, QueryBindingsCursor bindingsCursor) {
        return new Execution(context, bindingsCursor);
    }

    UnionAll_Default(Operator input1, RowType input1Type, Operator input2, RowType input2Type, boolean openBoth) {
        ArgumentValidation.notNull("first input", input1);
        ArgumentValidation.notNull("first input type", input1Type);
        ArgumentValidation.notNull("second input", input2);
        ArgumentValidation.notNull("second input type", input2Type);
        this.outputRowType = rowType(input1Type, input2Type);
        this.inputs = Arrays.asList(input1, input2);
        this.inputTypes = Arrays.asList(input1Type, input2Type);
        ArgumentValidation.isEQ("inputs.size", inputs.size(), "inputTypes.size", inputTypes.size());
        this.openBoth = openBoth;
    }

    // for use in this package (in ctor and unit tests)

    static RowType rowType(RowType rowType1, RowType rowType2) {
        if (rowType1 == rowType2)
            return rowType1;
        if (rowType1.nFields() != rowType2.nFields())
            throw notSameShape(rowType1, rowType2);
        return rowTypeNew(rowType1, rowType2);
    }

    private static RowType rowTypeNew(RowType rowType1, RowType rowType2) {
        TInstance[] types = new TInstance[rowType1.nFields()];
        for(int i=0; i<types.length; ++i) {
            TInstance tInst1 = rowType1.typeInstanceAt(i);
            TInstance tInst2 = rowType2.typeInstanceAt(i);
            if (Objects.equal(tInst1, tInst2))
                types[i] = tInst1;
            else if (tInst1 == null)
                types[i] = tInst2;
            else if (tInst2 == null)
                types[i] = tInst1;
            else
                throw notSameShape(rowType1, rowType2);
        }
        return rowType1.schema().newValuesType(types);
    }

    private static IllegalArgumentException notSameShape(RowType rt1, RowType rt2) {
        return new IllegalArgumentException(String.format("RowTypes not of same shape: %s (%s), %s (%s)",
                rt1, tInstanceOf(rt1),
                rt2, tInstanceOf(rt2)
        ));
    }

    private static String tInstanceOf (RowType rt) {
        TInstance[] result = new TInstance[rt.nFields()];
        for (int i = 0; i < result.length; ++i) {
            result[i] = rt.typeInstanceAt(i);
        }
        return Arrays.toString(result);
    }
    
    // Class state
    
    private static final InOutTap TAP_OPEN = OPERATOR_TAP.createSubsidiaryTap("operator: UnionAll_Default open"); 
    private static final InOutTap TAP_NEXT = OPERATOR_TAP.createSubsidiaryTap("operator: UnionAll_Default next");
    private static final Logger LOG = LoggerFactory.getLogger(UnionAll_Default.class);

    // Object state

    private final List<? extends Operator> inputs;
    private final List<? extends RowType> inputTypes;
    private final RowType outputRowType;
    private final boolean openBoth;

    @Override
    public CompoundExplainer getExplainer(ExplainContext context)
    {
        Attributes att = new Attributes();
        
        att.put(Label.NAME, PrimitiveExplainer.getInstance(getName()));
        att.put(Label.UNION_OPTION, PrimitiveExplainer.getInstance("ALL"));
        
        for (Operator op : inputs)
            att.put(Label.INPUT_OPERATOR, op.getExplainer(context));
        for (RowType type : inputTypes)
            att.put(Label.INPUT_TYPE, type.getExplainer(context));
       
        att.put(Label.OUTPUT_TYPE, outputRowType.getExplainer(context));
        
        att.put(Label.PIPELINE, PrimitiveExplainer.getInstance(openBoth));

        return new CompoundExplainer(Type.UNION, att);
    }

    private class Execution extends OperatorCursor {

        @Override
        public void open() {
            TAP_OPEN.in();
            try {
                CursorLifecycle.checkIdle(this);
                idle = false;
                if (openBoth) {
                    for (int i = 0; i < cursors.length; i++) {
                        cursors[i].open();
                    }
                }
            } finally {
                TAP_OPEN.out();
            }
        }

        @Override
        public Row next() {
            if (TAP_NEXT_ENABLED) {
                TAP_NEXT.in();
            }
            try {
                if (CURSOR_LIFECYCLE_ENABLED) {
                    CursorLifecycle.checkIdleOrActive(this);
                }
                Row outputRow;
                if (currentCursor == null) {
                    outputRow = nextCursorFirstRow();
                }
                else {
                    outputRow = currentCursor.next();
                    if (outputRow == null) {
                        currentCursor.close();
                        outputRow = nextCursorFirstRow();
                    }
                }
                if (outputRow == null) {
                    close();
                    idle = true;
                }
                else {
                    outputRow = wrapped(outputRow);
                }
                if (LOG_EXECUTION) {
                    LOG.debug("UnionAll_Default: yield {}", outputRow);
                }
                return outputRow;
            } finally {
                if (TAP_NEXT_ENABLED) {
                    TAP_NEXT.out();
                }
            }
        }

        @Override
        public void close() {
            CursorLifecycle.checkIdleOrActive(this);
            if (currentCursor != null) {
                currentCursor.close();
                currentCursor = null;
            }
            if (openBoth) {
                while (++inputOperatorsIndex < inputs.size()) {
                    cursors[inputOperatorsIndex].close();
                }
            }
            inputOperatorsIndex = -1;
            currentInputRowType = null;
            idle = true;
        }

        @Override
        public void destroy()
        {
            close();
            for (Cursor cursor : cursors) {
                if (cursor != null) {
                    cursor.destroy();
                }
            }
            destroyed = true;
        }

        @Override
        public boolean isIdle()
        {
            return !destroyed && idle;
        }

        @Override
        public boolean isActive()
        {
            return !destroyed && !idle;
        }

        @Override
        public boolean isDestroyed()
        {
            return destroyed;
        }

        @Override
        public void openBindings() {
            bindingsCursor.openBindings();
            for (int i = 0; i < cursors.length; i++) {
                cursors[i].openBindings();
            }
        }

        @Override
        public QueryBindings nextBindings() {
            QueryBindings bindings = bindingsCursor.nextBindings();
            for (int i = 0; i < cursors.length; i++) {
                QueryBindings other = cursors[i].nextBindings();
                assert (bindings == other);
            }
            return bindings;
        }

        @Override
        public void closeBindings() {
            bindingsCursor.closeBindings();
            for (int i = 0; i < cursors.length; i++) {
                cursors[i].closeBindings();
            }
        }

        @Override
        public void cancelBindings(QueryBindings bindings) {
            for (int i = 0; i < cursors.length; i++) {
                cursors[i].cancelBindings(bindings);
            }
            bindingsCursor.cancelBindings(bindings);
        }

        private Execution(QueryContext context, QueryBindingsCursor bindingsCursor)
        {
            super(context);
            MultipleQueryBindingsCursor multiple = new MultipleQueryBindingsCursor(bindingsCursor);
            this.bindingsCursor = multiple;
            cursors = new Cursor[inputs.size()];
            for (int i = 0; i < cursors.length; i++) {
                cursors[i] = inputs.get(i).cursor(context, multiple.newCursor());
            }
        }

        /**
         * Opens as many cursors as it takes to get one that returns a first row. Whichever is the first cursor
         * to return a non-null row, that cursor is saved as this.currentCursor. If no cursors remain that have
         * a next row, returns null.
         * @return the first row of the next cursor that has a non-null row, or null if no such cursors remain
         */
        private Row nextCursorFirstRow() {
            while (++inputOperatorsIndex < inputs.size()) {
                Cursor nextCursor = cursors[inputOperatorsIndex];
                if (!openBoth) {
                    nextCursor.open();
                }
                Row nextRow = nextCursor.next();
                if (nextRow == null) {
                    nextCursor.close();
                }
                else {
                    currentCursor = nextCursor;
                    this.currentInputRowType = inputTypes.get(inputOperatorsIndex);
                    return nextRow;
                }
            }
            return null;
        }

        private Row wrapped(Row inputRow) {
            assert inputRow != null;
            if (!inputRow.rowType().equals(currentInputRowType)) {
                throw new WrongRowTypeException(inputRow, currentInputRowType);
            }
            if (currentInputRowType == outputRowType) {
                return inputRow;
            }
            MasqueradingRow row = new MasqueradingRow(outputRowType, inputRow);
            return row;
        }

        private final QueryBindingsCursor bindingsCursor;
        private int inputOperatorsIndex = -1; // right before the first operator
        private Cursor[] cursors;
        private Cursor currentCursor;
        private RowType currentInputRowType;
        private boolean idle = true;
        private boolean destroyed = false;
    }

    static class WrongRowTypeException extends AkibanInternalException {
        public WrongRowTypeException(Row row, RowType expected) {
            super(row + ": expected row type " + expected + " but was " + row.rowType());
        }
    }

    private static class MasqueradingRow implements Row {

        @Override
        public int compareTo(Row row, int leftStartIndex, int rightStartIndex, int fieldCount)
        {
            return delegate.compareTo(row, leftStartIndex, rightStartIndex, fieldCount);
        }

        @Override
        public RowType rowType() {
            return rowType; // Note! Not a delegate
        }

        @Override
        public HKey hKey() {
            return delegate.hKey();
        }

        @Override
        public HKey ancestorHKey(UserTable table)
        {
            return delegate.ancestorHKey(table);
        }

        @Override
        public boolean ancestorOf(Row that) {
            return delegate.ancestorOf(that);
        }

        @Override
        public boolean containsRealRowOf(UserTable userTable) {
            throw new UnsupportedOperationException(getClass().toString());
        }

        @Override
        public Row subRow(RowType subRowType) {
            return delegate.subRow(subRowType);
        }

        @Override
        public ValueSource value(int index) {
            return delegate.value(index);
        }

        @Override
        public String toString() {
            return delegate.toString() + " of type " + rowType;
        }

        private MasqueradingRow(RowType rowType, Row wrapped) {
            this.rowType = rowType;
            this.delegate = wrapped;
        }

        private final Row delegate;
        private final RowType rowType;
    }
}