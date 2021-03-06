/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api;

import junit.framework.TestCase;

import org.junit.Test;

public class ObjectIdTest extends TestCase {

    @Test
    public void testIsNull() {
        assertTrue(ObjectId.NULL.isNull());
    }

    @Test
    public void testEquals() {
        ObjectId id1 = ObjectId.forString("some content");
        ObjectId id2 = ObjectId.forString("some content");
        assertNotSame(id1, id2);
        assertEquals(id1, id2);
        assertFalse(id1.equals(ObjectId.forString("some other content")));
    }

    @Test
    public void testToStringAndValueOf() {
        ObjectId id1 = ObjectId.forString("some content");
        String stringRep = id1.toString();
        ObjectId valueOf = ObjectId.valueOf(stringRep);
        assertEquals(id1, valueOf);
    }

    @Test
    public void testByeN() {
        ObjectId oid = new ObjectId(new byte[] { 00, 01, 02, 03, (byte) 0xff, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0 });
        assertEquals(0, oid.byteN(0));
        assertEquals(1, oid.byteN(1));
        assertEquals(2, oid.byteN(2));
        assertEquals(3, oid.byteN(3));
        assertEquals(255, oid.byteN(4));
    }
}
