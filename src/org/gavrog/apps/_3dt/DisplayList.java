/**
   Copyright 2008 Olaf Delgado-Friedrichs

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


package org.gavrog.apps._3dt;

import java.awt.Color;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.gavrog.box.simple.NamedConstant;
import org.gavrog.joss.geometry.Vector;
import org.gavrog.joss.pgraphs.basic.IEdge;
import org.gavrog.joss.pgraphs.basic.INode;
import org.gavrog.joss.pgraphs.basic.PeriodicGraph;
import org.gavrog.joss.tilings.Tiling.Tile;
import org.gavrog.joss.tilings.Tiling.Facet;

import buoy.event.EventSource;

/**
 * @author Olaf Delgado
 * @version $Id: DisplayList.java,v 1.11 2008/03/13 11:25:56 odf Exp $
 */
public class DisplayList extends EventSource implements
		Iterable<DisplayList.Item> {
	
	// --- the types of events that are produced by this class
	private static class EventType extends NamedConstant {
		protected EventType(String name) { super(name); }
	}
	public static EventType BEGIN = new EventType("Begin");
	public static EventType END = new EventType("End");
	public static EventType ADD = new EventType("Add");
	public static EventType DELETE = new EventType("Delete");
	public static EventType RECOLOR = new EventType("Recolor");
	
	// --- helper classes
	private abstract class Template {
		public abstract int hashCode();
		public abstract boolean equals(final Object arg);
		public abstract String toString();
	}
	
	private class TTile extends Template {
		final private Tile tile;
		
		private TTile(final Tile tile) {
			this.tile = tile;
		}

		public Tile getTile() {
			return this.tile;
		}

		public int hashCode() {
			return getTile().hashCode();
		}
		
		public boolean equals(final Object arg) {
			if (arg instanceof TTile) {
				return getTile().equals(((TTile) arg).getTile());
			} else {
				return false;
			}
		}
		
		public String toString() {
			return "T" + getTile().getIndex();
		}
	}
	
	private class TFacet extends Template {
		final private Facet facet;
		
		private TFacet(final Facet facet) {
			this.facet = facet;
		}

		public Facet getFacet() {
			return this.facet;
		}

		public int hashCode() {
			return getFacet().hashCode();
		}
		
		public boolean equals(final Object arg) {
			if (arg instanceof TFacet) {
				return getFacet().equals(((TFacet) arg).getFacet());
			} else {
				return false;
			}
		}
		
		public String toString() {
			return String.format("T%d-F%d", getFacet().getTileIndex(),
					getFacet().getIndex());
		}
	}
	
	private class TNode extends Template {
		final private INode node;
		
		private TNode(final INode node) {
			this.node = node;
		}

		public INode getNode() {
			return this.node;
		}

		public int hashCode() {
			return getNode().hashCode();
		}
		
		public boolean equals(final Object arg) {
			if (arg instanceof TNode) {
				return getNode().equals(((TNode) arg).getNode());
			} else {
				return false;
			}
		}
		
		public String toString() {
			return getNode().toString();
		}
	}
		
	private class TEdge extends Template {
		final private IEdge edge;

		private TEdge(final IEdge edge) {
			this.edge = edge;
		}

		public IEdge getEdge() {
			return this.edge;
		}

		public int hashCode() {
			return getEdge().hashCode();
		}

		public boolean equals(final Object arg) {
			if (arg instanceof TEdge) {
				return getEdge().equals(((TEdge) arg).getEdge());
			} else {
				return false;
			}
		}

		public String toString() {
			return getEdge().toString();
		}
	}
	
	public class Item {
		final private Template template;
		final private Vector shift;
		
		private Item(final Template t, final Vector shift) {
			this.template = t;
			this.shift = shift;
		}
		
		private Item(final Tile tile, final Vector shift) {
			this.template = new TTile(tile);
			this.shift = shift;
		}

		private Item(final Facet facet, final Vector shift) {
			this.template = new TFacet(facet);
			this.shift = shift;
		}

		private Item(final INode node, final Vector shift) {
			this.template = new TNode(node);
			this.shift = shift;
		}

		private Item(final IEdge edge, final Vector shift) {
			this.template = new TEdge(edge);
			this.shift = shift;
		}

		public boolean isTile() {
			return this.template instanceof TTile;
		}
		
		public boolean isFacet() {
			return this.template instanceof TFacet;
		}
		
		public boolean isNode() {
			return this.template instanceof TNode;
		}
		
		public boolean isEdge() {
			return this.template instanceof TEdge;
		}
		
		public Tile getTile() {
			if (isTile()) {
				return ((TTile) this.template).getTile();
			} else {
				throw new RuntimeException("illegal template class " +
						this.template.getClass().getName());
			}
		}

		public Facet getFacet() {
			if (isFacet()) {
				return ((TFacet) this.template).getFacet();
			} else {
				throw new RuntimeException("illegal template class " +
						this.template.getClass().getName());
			}
		}

		public INode getNode() {
			if (isNode()) {
				return ((TNode) this.template).getNode();
			} else {
				throw new RuntimeException("illegal template class " +
						this.template.getClass().getName());
			}
		}
		
		public IEdge getEdge() {
			if (isEdge()) {
				return ((TEdge) this.template).getEdge();
			} else {
				throw new RuntimeException("illegal template class " +
						this.template.getClass().getName());
			}
		}
		
		public Vector getShift() {
			return this.shift;
		}
		
		public int hashCode() {
			if (isEdge()) {
				final IEdge e = getEdge();
				final long a = (Long) e.source().id();
				final long b = (Long) e.target().id();
				final Vector s = ((PeriodicGraph) e.owner()).getShift(e);
				if (a <= b) {
					return (((int) a * 37 + (int) b) * 37 + s.hashCode()) * 37
							+ this.shift.hashCode();
				} else {
					return (((int) b * 37 + (int) a) * 37
							+ s.negative().hashCode()) * 37
							+ this.shift.plus(s).hashCode();
				}
			} else {
				return this.template.hashCode() * 37 + this.shift.hashCode();				
			}
		}
		
		public boolean equals(final Object arg) {
			final Item other = (Item) arg;
			if (isEdge()) {
				if (!other.isEdge()) {
					return false;
				}
				final IEdge e1 = getEdge();
				final IEdge e2 = other.getEdge();
				if (!e1.owner().equals(e2.owner())) {
					return false;
				}
				final long a1 = (Long) e1.source().id();
				final long b1 = (Long) e1.target().id();
				final Vector s1 = ((PeriodicGraph) e1.owner()).getShift(e1);
				final long a2 = (Long) e2.source().id();
				final long b2 = (Long) e2.target().id();
				final Vector s2 = ((PeriodicGraph) e2.owner()).getShift(e2);
				
				return (a1 == a2 && b1 == b2 && s1.equals(s2)
								&& this.shift.equals(other.shift))
						|| (a1 == b2 && b1 == a2 && s1.equals(s2.negative())
								&& this.shift.plus(s1).equals(other.shift));
			} else {
				return other.template.equals(this.template)
						&& other.shift.equals(this.shift);
			}
		}
		
		public String toString() {
			final StringBuffer buf = new StringBuffer(40);
			buf.append(this.template.toString());
			buf.append(" + ");
			buf.append(getShift());
			return buf.toString();
		}
	}
	
	public class Event {
		final private EventType eventType;
		final private Item instance;
		final private Color oldColor;
		final private Color newColor;
		
		protected Event(final EventType type, final Item instance,
				final Color oldColor, final Color newColor) {
			this.eventType = type;
			this.instance = instance;
			this.oldColor = oldColor;
			this.newColor = newColor;
		}

		public Color getOldColor() {
			return this.oldColor;
		}

		public Color getNewColor() {
			return this.newColor;
		}

		public Item getInstance() {
			return this.instance;
		}

		public EventType getEventType() {
			return this.eventType;
		}
		
		public String toString() {
			final StringBuffer buf = new StringBuffer(100);
			buf.append(getEventType());
			buf.append(" ");
			buf.append(getInstance());
			buf.append(", ");
			buf.append(getOldColor());
			buf.append(", ");
			buf.append(getNewColor());
			return buf.toString();
		}
	}
	
	// --- fields
    final private Map<Item, Color> map = new HashMap<Item, Color>();
	
    // --- constructors
	public DisplayList() {
	}
	
	// --- shortcuts for dispatching events
	private void dispatchEvent(final EventType type, final Item inst,
			final Color oldColor, final Color newColor) {
		dispatchEvent(new Event(type, inst, oldColor, newColor));
	}
	
	private void dispatchEvent(final EventType type, final Item inst,
			final Color oldColor) {
		dispatchEvent(type, inst, oldColor, null);
	}
	
	private void dispatchEvent(final EventType type, final Item inst) {
		dispatchEvent(type, inst, null, null);
	}
	
	private void dispatchEvent(final EventType type) {
		dispatchEvent(type, null, null, null);
	}
	
	// --- primitive list modifications
	private Item add(final Item inst) {
		if (!this.map.containsKey(inst)) {
			this.map.put(inst, null);
			dispatchEvent(ADD, inst);
			return inst;
		} else {
			return null;
		}
	}
	
	public Item add(final Tile tile, final Vector shift) {
		return add(new Item(tile, shift));
	}

	public Item add(final Facet facet, final Vector shift) {
		return add(new Item(facet, shift));
	}

	public Item add(final IEdge edge, final Vector shift) {
		dispatchEvent(BEGIN);
		final Item item = add(new Item(edge, shift));
		if (item != null) {
			addIncident(item);
		}
		dispatchEvent(END);
		return item;
	}

	public Item add(final INode node, final Vector shift) {
		return add(new Item(node, shift));
	}

	public boolean remove(final Item item) {
		if (this.map.containsKey(item)) {
			final Color oldColor = color(item);
			this.map.remove(item);
			dispatchEvent(DELETE, item, oldColor);
			return true;
		} else {
			return false;
		}
	}
	
	public boolean recolor(final Item item, final Color newColor) {
		if (this.map.containsKey(item)) {
			final Color oldColor = color(item);
			this.map.put(item, newColor);
			dispatchEvent(RECOLOR, item, oldColor, newColor);
			return true;
		} else {
			return false;
		}
	}

	public boolean recolor(final Tile tile, final Vector shift,
			final Color color) {
		return recolor(new Item(tile, shift), color);
	}
	
	// --- slightly less primitive list modifications
	public Item addNeighbor(final Item item, final int face) {
		final Tile tile = item.getTile();
		final Vector shift = item.getShift();
		final Vector newShift = (Vector) shift.plus(tile.neighborShift(face));
		return add(tile.neighbor(face), newShift);
	}

	public Item addNeighborFacet(final Item item, final int face) {
		final Tile tile = item.getTile();
		final Vector shift = item.getShift();
		final Vector newShift = (Vector) shift.plus(tile.neighborShift(face));
		return add(tile.facet(face).opposite(), newShift);
	}

	@SuppressWarnings("unchecked")
	public int addIncident(final Item item) {
		int count = 0;
		if (item.isEdge()) {
			dispatchEvent(BEGIN);
			final IEdge edge = item.getEdge();
			final Vector shift = item.getShift();
			final INode v = edge.source();
			final INode w = edge.target();
			final Vector s = ((PeriodicGraph) edge.owner()).getShift(edge);
			if (add(v, shift) != null) {
				++count;
			}
			if (add(w, (Vector) shift.plus(s)) != null) {
				++count;
			}
			dispatchEvent(END);
		} else if (item.isNode()) {
			dispatchEvent(BEGIN);
			final INode node = item.getNode();
			final Vector shift = item.getShift();
			final PeriodicGraph net = (PeriodicGraph) node.owner();
			for (final IEdge e: (List<IEdge>) net.allIncidences(node)) {
				if (add(e, shift) != null) {
					++count;
				}
			}
			dispatchEvent(END);
		}
		return count;
	}
	
	@SuppressWarnings("unchecked")
	public int connectToExisting(final Item item) {
		int count = 0;
		if (item.isNode()) {
			dispatchEvent(BEGIN);
			final INode node = item.getNode();
			final Vector shift = item.getShift();
			final PeriodicGraph net = (PeriodicGraph) node.owner();
			for (final IEdge e: (List<IEdge>) net.allIncidences(node)) {
				final Vector s, t;
				final INode w;
				if (e.oriented().source().equals(node)) {
					w = e.oriented().target();
					s = shift;
					t = (Vector) s.plus(net.getShift(e));
				} else {
					w = e.oriented().source();
					t = shift;
					s = (Vector) t.minus(net.getShift(e));
				}
				if (this.map.containsKey(new Item(w, t))) {
					if (add(e, s) != null) {
						++count;
					}
				}
			}
			dispatchEvent(END);
		}
		return count;
	}
	
	public boolean remove(final List<Item> list) {
		if (list.isEmpty()) {
			return false;
		} else {
			dispatchEvent(BEGIN);
			for (Item i: list) {
				remove(i);
			}
			dispatchEvent(END);
			return true;
		}
	}
	
	public boolean removeKind(final Item item) {
		final int kind = item.getTile().getKind();
		final List<Item> list = new LinkedList<Item>();
		for (Item i: this) {
			if (i.isTile() && i.getTile().getKind() == kind) {
				list.add(i);
			}
		}
		return remove(list);
	}
	
	public boolean removeAll() {
		final List<Item> list = new LinkedList<Item>();
		for (Item i: this) {
			list.add(i);
		}
		return remove(list);
	}
	
	public boolean removeAllTiles() {
		final List<Item> list = new LinkedList<Item>();
		for (Item i: this) {
			if (i.isTile()) {
				list.add(i);
			}
		}
		return remove(list);
	}
	
	public boolean removeAllFacets() {
		final List<Item> list = new LinkedList<Item>();
		for (Item i: this) {
			if (i.isFacet()) {
				list.add(i);
			}
		}
		return remove(list);
	}
	
	public boolean removeAllEdges() {
		final List<Item> list = new LinkedList<Item>();
		for (Item i: this) {
			if (i.isEdge()) {
				list.add(i);
			}
		}
		return remove(list);
	}
	
	public boolean removeAllNodes() {
		final List<Item> list = new LinkedList<Item>();
		for (Item i: this) {
			if (i.isNode()) {
				list.add(i);
			}
		}
		return remove(list);
	}
	
	// --- list enquiries
	/* (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	public Iterator<Item> iterator() {
		return this.map.keySet().iterator();
	}
	
	public int size() {
		return this.map.size();
	}
	
	public Color color(final Item inst) {
		return this.map.get(inst);
	}
}
