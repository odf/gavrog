/*
   Copyright 2013 Olaf Delgado-Friedrichs

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.gavrog.box.collections.FilteredIterator;
import org.gavrog.box.collections.IteratorAdapter;
import org.gavrog.box.collections.Pair;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.generators.InputIterator;
import org.gavrog.joss.geometry.Point;
import org.gavrog.joss.pgraphs.basic.IEdge;
import org.gavrog.joss.pgraphs.basic.INode;
import org.gavrog.joss.pgraphs.basic.PeriodicGraph;
import org.gavrog.joss.tilings.FaceList;
import org.gavrog.joss.tilings.Tiling;

/**
 * Encapsulates a net with extra information picked up by the parser.
 */
public class Net extends PeriodicGraph {
	final private String name;
	final private String givenGroup;
	final private Map<INode, String> nodeToName = new HashMap<INode, String>();
	final private Map<Pair<INode, Object>, Object> nodeInfo =
	        new HashMap<Pair<INode, Object>, Object>();
	final private List<Exception> errors = new ArrayList<Exception>();
	final private List<String> warnings = new ArrayList<String>();
	
	public Net(final int dim, final String name, final String group) {
		super(dim);
		this.name = name;
		this.givenGroup = group;
	}

	public Net(
	        final PeriodicGraph graph,
	        final String name,
	        final String group)
	{
        super(graph.getDimension());
        final Map<INode, INode> old2new = new HashMap<INode, INode>();
        for (final INode v: graph.nodes()) {
            old2new.put(v, newNode());
        }
        for (final IEdge e: graph.edges()) {
            final INode v = old2new.get(e.source());
            final INode w = old2new.get(e.target());
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
		this.nodeInfo.put(new Pair<INode, Object>(v, key), value);
	}
	
	public Object getNodeInfo(final INode v, final Object key) {
		assert(this.hasNode(v));
		return this.nodeInfo.get(new Pair<INode, Object>(v, key));
	}
	
	public String getNodeName(final INode v) {
		assert(this.hasNode(v));
		return (String) this.nodeToName.get(v);
	}
	
	public Map<INode, String> getNodeToNameMap() {
	    return new HashMap<INode, String>(this.nodeToName);
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
        private static final long serialVersionUID = 4776009175735945241L;

        public IllegalFileNameException(final String msg) {
            super(msg);
        }
    }
    
    public boolean isOk() {
    	return this.errors.isEmpty();
    }
    
    public Iterator<Exception> getErrors() {
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
    
    public static Iterator<Net> iterator(final String filePath)
            throws FileNotFoundException {
        
        final String extension = filePath
                .substring(filePath.lastIndexOf('.') + 1);

        final BufferedReader reader;
        reader = new BufferedReader(new FileReader(filePath));

        if ("cgd".equals(extension) || "pgr".equals(extension)) {
            final NetParser parser = new NetParser(reader);
            
            return new Iterator<Net>() {
                public boolean hasNext() {
                    return !parser.atEnd();
                }

                public Net next() {
                    return extract(parser);
                }

                public void remove() {
                    throw new UnsupportedOperationException("not supported");
                }
            };
        } else if ("ds".equals(extension) || "tgs".equals(extension)) {
            return new FilteredIterator<Net, DSymbol>(
                    new InputIterator(reader)) {
                public Net filter(final DSymbol ds) {
                    final PeriodicGraph graph = new Tiling(ds).getSkeleton();
                    final String group = (ds.dim() == 3) ? "P1" : "p1";
                    return new Net(graph, null, group);
                }
            };
        } else if ("arc".equals(extension)) {
            return new IteratorAdapter<Net>() {
                protected Net findNext() throws NoSuchElementException {
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

    /**
     * @param parser
     * @return
     */
    private static Net extract(final NetParser parser) {
        final GenericParser.Block data = parser.parseDataBlock();
        if (data.getType().toLowerCase().equals("tiling")) {
            final FaceList fl = new FaceList(data);
            final Tiling til = new Tiling(fl.getSymbol());
            final Map<Integer, Point> pos = fl.getPositions();
            final Tiling.Skeleton skel = til.getSkeleton();
    
            final Net net = new Net(
                    skel.getDimension(),
                    data.getEntriesAsString("name"),
                    "P1");
    
            final Map<INode, INode> old2new =
                    new HashMap<INode, INode>();
            for (final INode v: skel.nodes()) {
                old2new.put(v, net.newNode());
            }
            for (final IEdge e: skel.edges()) {
                final INode v = old2new.get(e.source());
                final INode w = old2new.get(e.target());
                net.newEdge(v, w, skel.getShift(e));
            }
    
            for (final int D: pos.keySet())
                net.setNodeInfo(
                        old2new.get(skel.nodeForChamber(D)),
                        NetParser.POSITION,
                        pos.get(D));
            return net;
        }
        else {
            return parser.parseNet(data);
        }
    }
}