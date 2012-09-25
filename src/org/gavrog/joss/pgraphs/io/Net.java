package org.gavrog.joss.pgraphs.io;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.gavrog.box.collections.FilteredIterator;
import org.gavrog.box.collections.IteratorAdapter;
import org.gavrog.box.collections.Pair;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.generators.InputIterator;
import org.gavrog.joss.pgraphs.basic.IEdge;
import org.gavrog.joss.pgraphs.basic.IGraphElement;
import org.gavrog.joss.pgraphs.basic.INode;
import org.gavrog.joss.pgraphs.basic.PeriodicGraph;
import org.gavrog.joss.tilings.Tiling;

/**
 * Encapsulates a net with extra information picked up by the parser.
 */
public class Net extends PeriodicGraph {
	final private String name;
	final private String givenGroup;
	final private Map nodeToName = new HashMap();
	final private Map nodeInfo = new HashMap();
	final private List errors = new ArrayList();
	final private List<String> warnings = new ArrayList<String>();
	
	public Net(final int dim, final String name, final String group) {
		super(dim);
		this.name = name;
		this.givenGroup = group;
	}

	public Net(final PeriodicGraph graph, final String name, final String group) {
        super(graph.getDimension());
        final Map old2new = new HashMap();
        for (final Iterator nodes = graph.nodes(); nodes.hasNext();) {
            final INode v = (INode) nodes.next();
            old2new.put(v, newNode());
        }
        for (final Iterator edges = graph.edges(); edges.hasNext();) {
            final IEdge e = (IEdge) edges.next();
            final INode v = (INode) old2new.get(e.source());
            final INode w = (INode) old2new.get(e.target());
            newEdge(v, w, graph.getShift(e));
        }
		this.name = name;
		this.givenGroup = group;
	}

	public String getGivenGroup() {
		return givenGroup;
	}

	public String getName() {
		return name;
	}
	
	public void setNodeInfo(final INode v, final Object key, final Object value) {
		assert(this.hasNode(v));
		this.nodeInfo.put(new Pair(v, key), value);
	}
	
	public Object getNodeInfo(final INode v, final Object key) {
		assert(this.hasNode(v));
		return this.nodeInfo.get(new Pair(v, key));
	}
	
	public String getNodeName(final INode v) {
		assert(this.hasNode(v));
		return (String) this.nodeToName.get(v);
	}
	
	public Map getNodeToNameMap() {
		final Map res = new HashMap();
		res.putAll(this.nodeToName);
		return res;
	}
	
	public INode newNode() {
		final INode v = super.newNode();
		this.nodeToName.put(v, "V" + v.id());
		return v;
	}
	
	public INode newNode(final String name) {
		final INode v = super.newNode();
		this.nodeToName.put(v, name);
		return v;
	}
	
	public void delete(final INode x) {
		this.nodeToName.remove(x);
		super.delete(x);
	}
    
    public static class IllegalFileNameException extends RuntimeException {
        public IllegalFileNameException(final String msg) {
            super(msg);
        }
    }
    
    public boolean isOk() {
    	return this.errors.isEmpty();
    }
    
    public Iterator getErrors() {
    	return this.errors.iterator();
    }
    
    public void logError(final Exception ex) {
    	this.errors.add(ex);
    }
    
	public Iterator<String> getWarnings() {
		return warnings.iterator();
	}

    public void addWarning(final String text) {
    	warnings.add(text);
    }
    
   public static Iterator iterator(final String filePath)
            throws FileNotFoundException {
        
        final String extension = filePath
                .substring(filePath.lastIndexOf('.') + 1);

        final BufferedReader reader;
        reader = new BufferedReader(new FileReader(filePath));

        if ("cgd".equals(extension) || "pgr".equals(extension)) {
            final NetParser parser = new NetParser(reader);
            
            return new Iterator() {
                public boolean hasNext() {
                    return !parser.atEnd();
                }

                public Object next() {
                    return parser.parseNet();
                }

                public void remove() {
                    throw new UnsupportedOperationException("not supported");
                }
            };
        } else if ("ds".equals(extension) || "tgs".equals(extension)) {
            return new FilteredIterator(new InputIterator(reader)) {
                public Object filter(Object x) {
                    final DelaneySymbol ds = (DelaneySymbol) x;
                    final PeriodicGraph graph = new Tiling(ds).getSkeleton();
                    final String group = (ds.dim() == 3) ? "P1" : "p1";
                    return new Net(graph, null, group);
                }
            };
        } else if ("arc".equals(extension)) {
            return new IteratorAdapter() {
                protected Object findNext() throws NoSuchElementException {
                    final Archive.Entry entry = Archive.Entry.read(reader);
                    if (entry == null) {
                        throw new NoSuchElementException("at end");
                    }
                    final String key = entry.getKey();
                    final PeriodicGraph graph = PeriodicGraph
                            .fromInvariantString(key);
                    final String group = (graph.getDimension() == 3) ? "P1"
                            : "p1";
                    return new Net(graph, entry.getName(), group);
                }
            };
        } else {
            throw new IllegalFileNameException("Unrecognized extension \"."
                    + extension + "\"");
        }
    }
}