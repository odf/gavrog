/*
Copyright 2005 Olaf Delgado-Friedrichs

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package org.gavrog.joss.pgraphs.io;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;

import junit.framework.TestCase;

import org.gavrog.joss.pgraphs.basic.PeriodicGraph;
import org.gavrog.joss.pgraphs.io.Archive.Entry;

/**
 * @author Olaf Delgado
 * @version $Id: TestArchive.java,v 1.2 2008/04/18 02:45:54 odf Exp $
 */
public class TestArchive extends TestCase {
    final PeriodicGraph srs = NetParser.stringToNet(""
            + "PERIODIC_GRAPH\n"
            + "  1 2  0 0 0\n"
            + "  1 3  0 0 0\n"
            + "  1 4  0 0 0\n"
            + "  2 3  1 0 0\n"
            + "  2 4  0 1 0\n"
            + "  3 4  0 0 1\n"
            + "END\n");
    final String srs_key = "3 1 2 0 0 0 1 3 0 0 0 1 4 0 0 0 2 3 0 1 0 2 4 1 0 0 3 4 0 0 1";
    final String keyVersion = srs.invariantVersion;
    final String srs_name = "srs";
    final String srs_digest = "d01d26b1ad1122626f6c4c98415129f8";
    
    final String srs_entry = ""
        + "key      " + srs_key + "\n"
        + "version  " + keyVersion + "\n"
        + "id       " + srs_name + "\n"
        + "checksum " + srs_digest + "\n"
        + "ref      " + "\n"
        + "desc     " + "\n"
        + "end\n";
    
    public void testEntryChecksum() {
        final Entry entry1 = new Entry(srs_key, keyVersion, srs_name);
        assertEquals(srs_digest, entry1.getDigestString());
        
        final Entry entry2 = new Entry(srs, srs_name);
        assertEquals(srs_digest, entry2.getDigestString());
    }
    
    public void testToString() {
        assertEquals(srs_entry, new Entry(srs, srs_name).toString());
    }
    
    public void testEntryRead() {
        final BufferedReader input = new BufferedReader(new StringReader(srs_entry));
        final Entry entry = Entry.read(input);
        assertEquals(srs_name, entry.getName());
        assertEquals(srs_key, entry.getKey());
        assertEquals(srs_digest, entry.getDigestString());
        assertEquals(keyVersion, entry.getKeyVersion());
        assertNull(Entry.read(input));
    }
    
    public void testArchiveRead() {
        final Archive rcsr = new Archive("1.0");
        final String path = "org/gavrog/apps/systre/rcsr.arc";
        final InputStream stream = ClassLoader.getSystemResourceAsStream(path);
        rcsr.addAll(new BufferedReader(new InputStreamReader(stream)));
        
        final String key = srs.getSystreKey();
        final Entry entry = rcsr.get(key);
        assertEquals(srs_name, entry.getName());
        assertEquals(srs_key, entry.getKey());
        assertEquals(srs_digest, entry.getDigestString());
        assertEquals(keyVersion, entry.getKeyVersion());
        assertEquals(rcsr.get(srs_name), entry);
    }
}
