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
package com.akiban.direct;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO - Total hack that this is static - need to a way to get this into the
 * context for JDBCResultSet.
 * 
 * @author peter
 * 
 */
public class Direct {

    private final static Map<Class<?>, Class<? extends AbstractDirectObject>> classMap = new HashMap<>();
    private final static ThreadLocal<Map<Class<?>, AbstractDirectObject>> instanceMap = new ThreadLocal<Map<Class<?>, AbstractDirectObject>>() {

        @Override
        protected Map<Class<?>, AbstractDirectObject> initialValue() {
            return new HashMap<Class<?>, AbstractDirectObject>();
        }
    };
    
    private final static ThreadLocal<DirectContextImpl> contextThreadLocal = new ThreadLocal<>();

    public static void registerDirectObjectClass(final Class<?> iface, final Class<? extends AbstractDirectObject> impl) {
        classMap.put(iface, impl);
    }

    /**
     * TODO - for now this clears everything!
     */

    public static void unregisterDirectObjectClasses() {
        classMap.clear();
        instanceMap.remove();

    }

    /**
     * Return a thread-private instance of an entity object of the registered
     * for a given Row, or null if there is none.
     */
    public static AbstractDirectObject objectForRow(final Class<?> c) {
        AbstractDirectObject o = instanceMap.get().get(c);
        if (o == null) {
            try {
                Class<? extends AbstractDirectObject> cl = classMap.get(c);
                o = cl.newInstance();
            } catch (InstantiationException | IllegalAccessException | ClassCastException e) {
                throw new RuntimeException(e);
            }
            if (o != null) {
                instanceMap.get().put(c, o);
            }
        }
        return o;
    }
    
    public static void enter(DirectContextImpl dc) {
        contextThreadLocal.set(dc);
        dc.enter();
    }
    
    public static DirectContextImpl getContext() {
        return contextThreadLocal.get();
    }
    
    public static void leave() {
        DirectContextImpl dc = contextThreadLocal.get();
        contextThreadLocal.remove();
        assert dc != null : "enter() was not called before leave()";
        dc.leave();
    }

}