/*
Copyright 2012 Olaf Delgado-Friedrichs

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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gavrog.box.collections.Pair;
import org.gavrog.box.simple.DataFormatException;
import org.gavrog.joss.pgraphs.basic.PeriodicGraph;

/**
 * A class to represent an archive of periodic nets.
 */
public class Archive {
    final String keyVersion;
    final private Map<String, Entry> byKey;
    final private Map<String, Entry> byName;
    private boolean errorOnOverwrite = false;
    
    /**
     * Represents an individual archive entry.
     */
    public static class Entry {
        final static char hexDigit[] = new char[] {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
    
        final private String key;
        final private String keyVersion;
        final private String name;
        private String description;
        private String reference;
        private String url;
        
        /**
         * Constructs an entry with explicit values.
         * 
         * @param key the invariant key describing the structure.
         * @param version the version of the key generation process used.
         * @param name the name of the structure.
         */
        public Entry(final String key, final String version, final String name)
        {
            this.key = key;
            this.keyVersion = version;
            this.name = name;
        }
        
        /**
         * Constructs an entry representing a periodic graph.
         * 
         * @param G the graph to encode.
         * @param name the name for the graph.
         */
        public Entry(final PeriodicGraph G, final String name) {
            this(G.getSystreKey(), G.invariantVersion, name);
        }
        
        /**
         * @return Returns the key.
         */
        public String getKey() {
            return key;
        }
        
        /**
         * @return Returns the name.
         */
        public String getName() {
            return name;
        }
        
        /**
         * @return Returns the version string.
         */
        public String getKeyVersion() {
            return keyVersion;
        }
        
        /**
         * Returns a digest string determined by the "md5" algorithm. This
         * string can be used to assert the integrity of this entry when read
         * from a file.
         * 
         * @return the digest string.
         */
        public String getDigestString() {
            try {
                final MessageDigest md = MessageDigest.getInstance("MD5");
                final StringBuffer buf = new StringBuffer(100);
                buf.append(key);
                buf.append("\n");
                buf.append(keyVersion);
                buf.append("\n");
                buf.append(name);
                md.update(buf.toString().getBytes());
                final byte digest[] = md.digest();
                final StringBuffer result = new StringBuffer(digest.length * 2);
                for (int i =0; i < digest.length; ++i) {
                    result.append(hexDigit[(digest[i] & 0xf0) >>> 4]);
                    result.append(hexDigit[digest[i] & 0x0f]);
                }
                return result.toString();
            } catch (GeneralSecurityException ex) {
                throw new RuntimeException(ex);
            }
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            final StringBuffer buf = new StringBuffer(200);
            buf.append("key      ");
            buf.append(getKey());
            buf.append("\n");
            buf.append("version  ");
            buf.append(getKeyVersion());
            buf.append("\n");
            buf.append("id       ");
            buf.append(getName());
            buf.append("\n");
            buf.append("checksum ");
            buf.append(getDigestString());
            buf.append("\n");
            buf.append("ref      ");
            if (reference != null) {
                buf.append(getReference());
            }
            buf.append("\n");
            buf.append("desc     ");
            if (getDescription() != null) {
                buf.append(getDescription());
            }
            buf.append("\n");
            if (getURL() != null) {
                buf.append("url      ");
                buf.append(getURL());
                buf.append("\n");
            }
            buf.append("end\n");
            return buf.toString();
        }
        
        /**
         * Reads an entry from a stream.
         * @param input represents the input stream.
         * @return the entry read or null if the stream is at its end.
         */
        public static Entry read(final BufferedReader input) {
            String line;
            final Map<String, String> fields = new HashMap<String, String>();
            while (true) {
                try {
                    line = input.readLine();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
                if (line == null) {
                    break;
                }
                line = line.trim().replaceAll("\\s+", " ");
                if (line.length() == 0) {
                    continue;
                }
                int k = line.indexOf(" ");
                final String tag;
                final String arg;
                if (k < 0) {
                    tag = line;
                    arg = null;
                } else {
                    tag = line.substring(0, k);
                    arg = line.substring(k + 1);
                }
                if (tag.equals("end")) {
                    final String key = fields.get("key");
                    final String version = fields.get("version");
                    final String name = fields.get("id");
                    final String checksum = fields.get("checksum");
                    final Entry entry = new Entry(key, version, name);
                    if (!entry.getDigestString().equals(checksum)) {
                        throw new DataFormatException("checksum mismatch for entry '"
								+ name + "'.");
                    }
                    entry.setDescription(fields.get("desc"));
                    entry.setReference(fields.get("ref"));
                    entry.setURL(fields.get("url"));
                    return entry;
                } else {
                    fields.put(tag, arg);
                }
            }
            return null;
        }
        
        /**
         * @return the current value of description.
         */
        public String getDescription() {
            return this.description;
        }
        
        /**
         * @param description The new value for description.
         */
        public void setDescription(String description) {
            this.description = description;
        }
        
        /**
         * @return the current value of reference.
         */
        public String getReference() {
            return this.reference;
        }
        
        /**
         * @param reference The new value for reference.
         */
        public void setReference(String reference) {
            this.reference = reference;
        }
        
        /**
         * @return the current value of url.
         */
        public String getURL() {
            return this.url;
        }
        
        /**
         * @param url The new value for url.
         */
        public void setURL(String url) {
            this.url = url;
        }
    }
    
    /**
     * Creates a new empty instance.
     * 
     * @param keyVersion the key creation version to be used for this archive.
     */
    public Archive(final String keyVersion) {
        this.keyVersion = keyVersion;
        this.byKey = new LinkedHashMap<String, Entry>();
        this.byName = new HashMap<String, Entry>();
    }
    
    /**
     * Returns the number of entries in this archive.
     * @return the number of entries.
     */
    public int size() {
        return this.byKey.size();
    }
    
    /**
     * @return Returns the version string.
     */
    public String getKeyVersion() {
        return keyVersion;
    }
    
    /**
     * Removes all entries from this archive.
     */
    public void clear() {
        this.byKey.clear();
        this.byName.clear();
    }
    
    /**
     * Adds the given entry to the archive.
     * 
     * @param entry the new entry.
     */
    public void add(final Entry entry) {
        final String version = entry.getKeyVersion();
        final String key = entry.getKey();
        final String name = entry.getName();
		if (!version.equals(getKeyVersion())) {
			throw new IllegalArgumentException("entry '" + name
					+ "' has key of version " + version + ", but " + getKeyVersion()
					+ " is required.");
		}
        if (this.errorOnOverwrite) {
			if (this.byKey.containsKey(key)) {
				final String clashing = ((Entry) this.byKey.get(key)).getName();
				if (!clashing.equals(name)) {
					throw new IllegalArgumentException("identical keys for entries '"
							+ clashing + "' and '" + name + "'");
				}
			} else if (this.byName.containsKey(name)) {
				throw new IllegalArgumentException("multiple entries for id '" + name
						+ "'");
			}
		}
        this.byKey.put(key, entry);
        this.byName.put(name, entry);
    }
    
    /**
     * Adds an entry for a given periodic graph to the archive.
     * 
     * @param G the periodic graph to add.
     * @param name the name to use for that graph.
     * @return the entry added.
     */
    public Entry add(final PeriodicGraph G, final String name) {
        final Entry entry = new Entry(G, name);
        add(entry);
        return entry;
    }
    
    /**
     * Removes an entry.
     * 
     * @param entry the entry to remove.
     */
    public void delete(final Entry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("null argument");
        }
        final String key = entry.getKey();
        final String name = entry.getName();
        if (entry != getByKey(key)) {
            throw new IllegalArgumentException("no such entry");
        }
        this.byKey.remove(key);
        this.byName.remove(name);
    }
    
    /**
     * Retrieves an entry.
     * @param key the key for the entry to get.
     * @return the entry with the given key or null.
     */
    public Entry getByKey(final String key) {
        return this.byKey.get(key);
    }
    
    /**
     * Retrieves an entry.
     * @param name the name for the entry to get.
     * @return the entry with the given name or null.
     */
    public Entry getByName(final String name) {
        return this.byName.get(name);
    }
    
    /**
     * Retrieves an entry. If an entry exists with the given argument as its
     * key, that entry is returned. Otherwise, the method tries to find an entry
     * with the given argument as its name.
     * 
     * @param keyOrName the key or name to look for.
     * @return the corresponding entry or null.
     */
    public Entry get(final String keyOrName) {
        final Entry e = getByKey(keyOrName);
        if (e != null) {
            return e;
        } else {
            return getByName(keyOrName);
        }
    }
    
    /**
     * Adds all archive entries read from a stream.
     * @param input represents the input stream.
     */
    public void addAll(final BufferedReader input) {
        while (true) {
            final Entry entry = Entry.read(input);
            if (entry == null) {
                return;
            } else {
                add(entry);
            }
        }
    }
    
    /**
     * Adds all archive entries read from a stream.
     * @param input represents the input stream.
     */
    public void addAll(final Reader input) {
        addAll(new BufferedReader(input));
    }
    
    /**
     * Retrieves the set of all Systre keys present in this archive. 
     * @return the set of Systre keys.
     */
    public Set<String> keySet() {
        return this.byKey.keySet();
    }

	public boolean getErrorOnOverwrite() {
		return errorOnOverwrite;
	}

	public void setErrorOnOverwrite(boolean errorOnOverwrite) {
		this.errorOnOverwrite = errorOnOverwrite;
	}
	
	public static void main(final String args[]) {
		if (args.length < 2) {
			System.err.println("Usage: Archive command file arguments");
			return;
		}
		final String cmd = args[0];
		final String filename = args[1];
		final Archive arc = new Archive("1.0");
		if ("check".equalsIgnoreCase(cmd)) {
			arc.setErrorOnOverwrite(true);
		}
		
		try {
			arc.addAll(new FileReader(filename));
		} catch (FileNotFoundException ex) {
			System.err.println("Could not find file \"" + filename + "\".");
			return;
		} catch (Exception ex) {
			System.err.println(ex.getMessage() + " - in archive \"" + filename + "\".");
			return;
		}
		
		final int n = arc.size();
		System.err.println("Read " + n + " entr" + (n == 1 ? "y" : "ies")
				+ " from archive " + filename + ".");
		
		if ("rename".equalsIgnoreCase(cmd)) {
			if (args.length < 4) {
				System.err.println("Usage: Archive rename file old1 new1 ...");
				return;
			}
			for (int i = 2; i < args.length; i += 2) {
				final String oldid = args[i];
				final String newid = args[i+1];
				final Entry e = arc.getByName(oldid);
				if (e == null) {
					System.err.println("Warning: could not find entry \"" + oldid + "\".");
				} else {
					final Entry f = new Entry(e.key, e.keyVersion, newid);
					arc.delete(e);
					arc.add(f);
				}
			}
			int count = 0;
			for (final String key: arc.keySet()) {
				final Entry entry = arc.getByKey(key);
				System.out.println(entry.toString());
				++count;
			}
			System.err.println("Wrote " + count + " entr" + (count == 1 ? "y" : "ies")
					+ " to standard output.");
		} else if ("compare".equalsIgnoreCase(cmd)){
			if (args.length < 3) {
				System.err.println("Usage: Archive compare oldfile newfile");
				return;
			}
			final Archive newArc = new Archive("1.0");
			final String newName = args[2];
			try {
				newArc.addAll(new FileReader(newName));
			} catch (FileNotFoundException ex) {
				System.err.println("Could not find file \"" + newName + "\".");
				return;
			} catch (Exception ex) {
				System.err.println(ex.getMessage() + " - in archive \"" + newName + "\".");
				return;
			}
			final int m = newArc.size();
			System.err.println("Read " + m + " entr" + (m == 1 ? "y" : "ies")
					+ " from archive " + newName + ".");
			
			final List<String> deleted = new LinkedList<String>();
			final List<Pair<String, String>> renamed =
			        new LinkedList<Pair<String, String>>();
			final List<String> added = new LinkedList<String>();
			final List<String> changed = new LinkedList<String>();
			
			for (final String key: arc.keySet()) {
				final Entry oldEntry = arc.getByKey(key);
				final Entry newEntry = newArc.getByKey(key);
				final String name = oldEntry.name;
				if (newEntry == null) {
					deleted.add(name);
				} else if (!newEntry.name.equals(name)) {
					renamed.add(new Pair<String, String>(name, newEntry.name));
				}
				final Entry sameName = newArc.getByName(name);
				if (sameName != null && !sameName.key.equals(key)) {
					changed.add(name);
				}
			}			
            for (final String key: newArc.keySet()) {
				final Entry oldEntry = arc.getByKey(key);
				final Entry newEntry = newArc.getByKey(key);
				if (oldEntry == null) {
					added.add(newEntry.name);
				}
			}
			
			printList(changed, "Changed");
			printList(renamed, "Renamed");
			printList(deleted, "Deleted");
			printList(added, "Added");
		}
	}
	
	private static <T> void printList(final List<T> list, final String heading) {
		if (list.size() > 0) {
			System.err.print(heading + ": ");
			for (final T item: list) {
				System.err.print(" " + item);
			}
		}
		System.err.println();
	}
}
