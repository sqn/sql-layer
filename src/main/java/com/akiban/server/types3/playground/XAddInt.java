/**
 * END USER LICENSE AGREEMENT (“EULA”)
 *
 * READ THIS AGREEMENT CAREFULLY (date: 9/13/2011):
 * http://www.akiban.com/licensing/20110913
 *
 * BY INSTALLING OR USING ALL OR ANY PORTION OF THE SOFTWARE, YOU ARE ACCEPTING
 * ALL OF THE TERMS AND CONDITIONS OF THIS AGREEMENT. YOU AGREE THAT THIS
 * AGREEMENT IS ENFORCEABLE LIKE ANY WRITTEN AGREEMENT SIGNED BY YOU.
 *
 * IF YOU HAVE PAID A LICENSE FEE FOR USE OF THE SOFTWARE AND DO NOT AGREE TO
 * THESE TERMS, YOU MAY RETURN THE SOFTWARE FOR A FULL REFUND PROVIDED YOU (A) DO
 * NOT USE THE SOFTWARE AND (B) RETURN THE SOFTWARE WITHIN THIRTY (30) DAYS OF
 * YOUR INITIAL PURCHASE.
 *
 * IF YOU WISH TO USE THE SOFTWARE AS AN EMPLOYEE, CONTRACTOR, OR AGENT OF A
 * CORPORATION, PARTNERSHIP OR SIMILAR ENTITY, THEN YOU MUST BE AUTHORIZED TO SIGN
 * FOR AND BIND THE ENTITY IN ORDER TO ACCEPT THE TERMS OF THIS AGREEMENT. THE
 * LICENSES GRANTED UNDER THIS AGREEMENT ARE EXPRESSLY CONDITIONED UPON ACCEPTANCE
 * BY SUCH AUTHORIZED PERSONNEL.
 *
 * IF YOU HAVE ENTERED INTO A SEPARATE WRITTEN LICENSE AGREEMENT WITH AKIBAN FOR
 * USE OF THE SOFTWARE, THE TERMS AND CONDITIONS OF SUCH OTHER AGREEMENT SHALL
 * PREVAIL OVER ANY CONFLICTING TERMS OR CONDITIONS IN THIS AGREEMENT.
 */

package com.akiban.server.types3.playground;


import com.akiban.server.types3.LazyList;
import com.akiban.server.types3.TConstantValue;
import com.akiban.server.types3.TInputSet;
import com.akiban.server.types3.TInstance;
import com.akiban.server.types3.TOverload;
import com.akiban.server.types3.TOverloadResult;
import com.akiban.server.types3.pvalue.PUnderlying;
import com.akiban.server.types3.pvalue.PValue;
import com.akiban.server.types3.pvalue.PValueSource;
import com.akiban.server.types3.pvalue.PValueTarget;

import java.util.BitSet;
import java.util.Collections;
import java.util.List;

public enum XAddInt implements TOverload {
    INSTANCE;

    @Override
    public String overloadName() {
        return "xadd";
    }

    @Override
    public TOverloadResult resultType() {
        return new TOverloadResult(XInt.TYPE_CLASS);
    }

    @Override
    public List<TInputSet> inputSets() {
        BitSet covering = new BitSet();
        covering.set(0);
        covering.set(1);
        return Collections.singletonList(new TInputSet(XInt.TYPE_CLASS, covering, false));
    }

    @Override
    public void evaluate(List<TInstance> inputInstances, LazyList<PValueSource> inputs, TInstance outputInstance,
                         PValueTarget output)
    {
        if (OverloadUtils.nullsContaminate(output, inputs))
            return;
        int result = inputs.get(0).getInt32() + inputs.get(1).getInt32();
        output.putInt32(result);
    }

    @Override
    public TConstantValue evaluateConstant(LazyList<TConstantValue> inputs) {
        TConstantValue input0 = inputs.get(0);
        if (input0 == null)
            return null;
        TConstantValue input1 = inputs.get(1);
        if (input1 == null)
            return null;
        PValue constValue = new PValue(PUnderlying.INT_32);
        constValue.putInt32(input0.value().getInt32() + input1.value().getInt32());
        return new TConstantValue(XInt.INSTANCE, constValue);
    }
}
