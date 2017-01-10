/*
 * MIT License
 *
 * Copyright (c) 2016 EPAM Systems
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.epam.catgenome.manager.externaldb.bindings;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang.math.RandomUtils;
import org.junit.Test;

import com.epam.catgenome.manager.externaldb.bindings.dbsnp.ObjectFactory;
import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;

/**
 * Source:      DbsnpBindingTest
 * Created:     04.10.16, 13:20
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * @author Mikhail Miroliubov
 */
public class ExternalDBBindingTest {
    private static final String TEST_BIGINTEGER = "1111";

    @Test
    public void testDbsnpBindings() throws InvocationTargetException, IllegalAccessException, InstantiationException {
        ObjectFactory factory = new ObjectFactory();
        processFactory(factory);
    }

    @Test
    public void testRcsbPdbBindings() throws InvocationTargetException, IllegalAccessException, InstantiationException {
        com.epam.catgenome.manager.externaldb.bindings.rcsbpbd.ObjectFactory factory = new com.epam.catgenome
                .manager.externaldb.bindings.rcsbpbd.ObjectFactory();

        processFactory(factory);
    }

    @Test
    public void testUniprotBindings() throws InvocationTargetException, IllegalAccessException, InstantiationException {
        com.epam.catgenome.manager.externaldb.bindings.uniprot.ObjectFactory factory = new com.epam.catgenome
                .manager.externaldb.bindings.uniprot.ObjectFactory();

        processFactory(factory);
    }

    private void processFactory(Object factory) throws InvocationTargetException, IllegalAccessException,
            InstantiationException {
        List<Object> factoryObjects = new ArrayList<>();
        Map<Class, Object> objectMap = new HashMap<>();

        for (Method m : factory.getClass().getDeclaredMethods()) {
            if (Modifier.isPublic(m.getModifiers())) {
                Object o;
                if (m.getParameterCount() == 0) {
                    o = m.invoke(factory);
                } else {
                    Object[] params = new Object[m.getParameterCount()];
                    for (int i = 0; i < m.getParameterCount(); i++) {
                        params[i] = createParam(m.getParameterTypes()[i]);
                    }

                    o = m.invoke(factory, params);
                }

                factoryObjects.add(o);
                objectMap.put(o.getClass(), o);
            }
        }

        for (Object o : factoryObjects) {
            for (Method m : o.getClass().getDeclaredMethods()) {
                if (Modifier.isPublic(m.getModifiers())) {
                    if (m.getParameterTypes().length > 0) {
                        Class type = m.getParameterTypes()[0];
                        if (objectMap.containsKey(type)) {
                            m.invoke(o, objectMap.get(type));
                        } else {
                            Object param = createParam(type);

                            m.invoke(o, param);
                        }
                    } else {
                        m.invoke(o);
                    }
                }
            }
        }
    }

    private Object createParam(Class type) throws IllegalAccessException, InvocationTargetException,
            InstantiationException {
        Object param;
        if (type == String.class) {
            param = "test";
        } else if (type == Integer.class || type == Integer.TYPE) {
            param = RandomUtils.nextInt();
        } else if (type == Long.class || type == Long.TYPE) {
            param = RandomUtils.nextLong();
        } else if (type == Float.class || type == Float.TYPE) {
            param = RandomUtils.nextFloat();
        } else if (type == Double.class || type == Double.TYPE) {
            param = RandomUtils.nextDouble();
        } else if (type == Boolean.class || type == Boolean.TYPE) {
            param = RandomUtils.nextBoolean();
        } else if (type == BigInteger.class) {
            param = new BigInteger(TEST_BIGINTEGER);
        } else if (type == List.class) {
            param = new ArrayList<>();
        } else if (type == XMLGregorianCalendar.class) {
            param = new XMLGregorianCalendarImpl();
        } else {
            Constructor[] constructors = type.getConstructors();
            param = constructors[0].newInstance();
        }

        return param;
    }
}
