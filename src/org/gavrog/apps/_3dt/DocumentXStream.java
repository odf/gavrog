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


package org.gavrog.apps._3dt;

import java.awt.Color;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.gavrog.apps._3dt.DisplayList.Item;
import org.gavrog.box.collections.Pair;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.derived.DSCover;
import org.gavrog.joss.geometry.Vector;
import org.gavrog.joss.pgraphs.basic.IEdge;
import org.gavrog.joss.pgraphs.basic.INode;
import org.gavrog.joss.pgraphs.basic.PeriodicGraph;
import org.gavrog.joss.tilings.Tiling;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.SingleValueConverter;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import de.jreality.scene.Transformation;

/**
 */
public class DocumentXStream extends XStream {
	private static DocumentXStream _instance = null;
	
	public static DocumentXStream instance() {
		if (_instance == null) {
			_instance = new DocumentXStream();
		}
		return _instance;
	}
	
	private DocumentXStream() {
    	setMode(XStream.NO_REFERENCES);

    	alias("scene", Document.class);
    	alias("color", Color.class);
    	
    	registerDSymbolConverter();
    	registerColorConverter();
    	registerVectorConverter();
    	registerTransformationConverter();
    	registerPropertiesConverter();
    	registerDocumentConverter();
	}

	private void registerDocumentConverter() {
		registerConverter(new Converter() {
			public boolean canConvert(
			        @SuppressWarnings("rawtypes") final Class clazz)
			{
				return clazz == Document.class;
			}
			public void marshal(final Object value,
					final HierarchicalStreamWriter writer,
					final MarshallingContext context) {
				final Document doc = (Document) value;
				final Tiling til = doc.getTiling();
				final DSCover<Integer> cov = til.getCover();
				
				if (doc.getName() != null) {
					writer.addAttribute("name", doc.getName());
				}
				writer.startNode("symbol");
				context.convertAnother(til.getSymbol().flat());
				writer.endNode();
				writer.startNode("cover");
				context.convertAnother(til.getCover().flat());
				writer.endNode();
				for (final int i: cov.indices()) {
					for (final int D: cov.elements()) {
						final Vector s = til.edgeTranslation(i, D);
						if (!s.isZero()) {
							writer.startNode("edgeShift");
							writer.addAttribute("index", String.valueOf(i));
							writer.addAttribute("element", String.valueOf(D));
							context.convertAnother(s);
							writer.endNode();
						}
					}
				}
				
				writer.startNode("palette");
				context.convertAnother(doc.getPalette());
				writer.endNode();
				for (final DisplayList.Item item: doc) {
					if (item.isTile()) {
						writer.startNode("tile");
						writer.addAttribute("templateNr",
								String.valueOf(item.getTile().getIndex()));
					} else if (item.isFacet()) {
						final Tiling.Facet f = item.getFacet();
						writer.startNode("facet");
						writer.addAttribute("templateNr",
								String.valueOf(f.getTileIndex()));
						writer.addAttribute("index",
								String.valueOf(f.getIndex()));
					} else if (item.isNode()) {
						writer.startNode("node");
						writer.addAttribute("id",
								String.valueOf(item.getNode().id()));
					} else if (item.isEdge()) {
						final IEdge e = item.getEdge();
						writer.startNode("edge");
						writer.addAttribute("source",
								String.valueOf(e.source().id()));
						writer.addAttribute("target",
								String.valueOf(e.target().id()));
						writer.startNode("label");
						context.convertAnother(((PeriodicGraph) e.owner())
								.getShift(e));
						writer.endNode();
					}
					writer.startNode("shift");
					context.convertAnother(item.getShift());
					writer.endNode();
					if (doc.color(item) != null) {
						writer.startNode("color");
						context.convertAnother(doc.color(item));
						writer.endNode();
					}
					writer.endNode();
				}
				for (final Tiling.Facet f: doc.getColoredFacetClasses()) {
					final Color c = doc.getFacetClassColor(f);
					if (c != null) {
						writer.startNode("facet");
						writer.addAttribute("templateNr",
								String.valueOf(f.getTileIndex()));
						writer.addAttribute("index",
								String.valueOf(f.getIndex()));
						writer.startNode("color");
						context.convertAnother(c);
						writer.endNode();
						writer.endNode();
					}
				}
				for (final Tiling.Facet f: doc.getHiddenFacetClasses()) {
					writer.startNode("facet");
					writer.addAttribute("templateNr",
							String.valueOf(f.getTileIndex()));
					writer.addAttribute("index", String.valueOf(f.getIndex()));
					writer.addAttribute("hidden", "true");
					writer.endNode();
				}
				writer.startNode("options");
				context.convertAnother(doc.getProperties());
				writer.endNode();
				writer.startNode("transformation");
				context.convertAnother(doc.getTransformation());
				writer.endNode();
			}
			public Object unmarshal(final HierarchicalStreamReader reader,
					final UnmarshallingContext context) {
				Document doc = null;
				final String name = reader.getAttribute("name");
				DSymbol symbol = null;
				DSymbol cover = null;
				final List<Color> palette = new LinkedList<Color>();
				final List<Object[]> dlist = new LinkedList<Object[]>();
				final Map<Pair<Integer, Integer>, Color> fcolors =
				        new HashMap<Pair<Integer, Integer>, Color>();
				final Set<Pair<Integer, Integer>> fhidden =
				        new HashSet<Pair<Integer, Integer>>();
				Properties props = null;
				Transformation trans = null;
				
				while (reader.hasMoreChildren()) {
					reader.moveDown();
					if ("symbol".equals(reader.getNodeName())) {
						symbol = (DSymbol) context.convertAnother(null,
								DSymbol.class);
					} else if ("cover".equals(reader.getNodeName())) {
						cover = (DSymbol) context.convertAnother(null,
								DSymbol.class);
					} else if ("palette".equals(reader.getNodeName())) {
						while (reader.hasMoreChildren()) {
							reader.moveDown();
							palette.add((Color) context.convertAnother(null,
									Color.class));
							reader.moveUp();
						}
					} else if ("options".equals(reader.getNodeName())) {
						props = (Properties) context.convertAnother(null,
								Properties.class);
					} else if ("transformation".equals(reader.getNodeName())) {
						trans = (Transformation) context.convertAnother(null,
								Transformation.class);
					} else if ("tile".equals(reader.getNodeName())) {
						final Integer number = new Integer(reader
								.getAttribute("templateNr"));
						Vector shift = null;
						Color color = null;
						while (reader.hasMoreChildren()) {
							reader.moveDown();
							if ("shift".equals(reader.getNodeName())) {
								shift = (Vector) context.convertAnother(null,
										Vector.class);
							} else if ("color".equals(reader.getNodeName())) {
								color = (Color) context.convertAnother(null,
										Color.class);
							}
							reader.moveUp();
						}
						dlist.add(new Object[] { "tile", shift, color, number });
					} else if ("node".equals(reader.getNodeName())) {
						final Long number = new Long(reader.getAttribute("id"));
						Vector shift = null;
						Color color = null;
						while (reader.hasMoreChildren()) {
							reader.moveDown();
							if ("shift".equals(reader.getNodeName())) {
								shift = (Vector) context.convertAnother(null,
										Vector.class);
							} else if ("color".equals(reader.getNodeName())) {
								color = (Color) context.convertAnother(null,
										Color.class);
							}
							reader.moveUp();
						}
						dlist.add(new Object[] { "node", shift, color, number });
					} else if ("edge".equals(reader.getNodeName())) {
						final Long source =
							new Long(reader.getAttribute("source"));
						final Long target =
							new Long(reader.getAttribute("target"));
						Vector label = null;
						Vector shift = null;
						Color color = null;
						while (reader.hasMoreChildren()) {
							reader.moveDown();
							if ("label".equals(reader.getNodeName())) {
								label = (Vector) context.convertAnother(null,
										Vector.class);
							} else if ("shift".equals(reader.getNodeName())) {
									shift = (Vector) context.convertAnother(null,
											Vector.class);
							} else if ("color".equals(reader.getNodeName())) {
								color = (Color) context.convertAnother(null,
										Color.class);
							}
							reader.moveUp();
						}
						dlist.add(new Object[] { "edge", shift, color,
								source, target, label });
					} else if ("facet".equals(reader.getNodeName())) {
						final int tile =
							new Integer(reader.getAttribute("templateNr"));
						final int index =
							new Integer(reader.getAttribute("index"));
						final String hidden = reader.getAttribute("hidden");
						if ("true".equalsIgnoreCase(hidden)) {
							fhidden.add(new Pair<Integer, Integer>(tile, index));
						}
						Vector shift = null;
						Color color = null;
						while (reader.hasMoreChildren()) {
							reader.moveDown();
							if ("shift".equals(reader.getNodeName())) {
								shift = (Vector) context.convertAnother(null,
										Vector.class);
							} else if ("color".equals(reader.getNodeName())) {
								color = (Color) context.convertAnother(null,
										Color.class);
							}
							reader.moveUp();
						}
						if (shift == null) {
							fcolors.put(new Pair<Integer, Integer>(tile, index), color);
						} else {
							dlist.add(new Object[] { "facet", shift, color,
									tile, index });
						}
					}
					reader.moveUp();
				}

				if (symbol == null) {
					throw new RuntimeException("No D-Symbol on XML stream.");
				} else {
					if (cover != null) {
						doc = new Document(symbol, name,
						        new DSCover<Integer>(cover, symbol, 1));
					} else {
						doc = new Document(symbol, name);
					}
					doc.setProperties(props);
					for (int i = 0; i < palette.size(); ++i) {
						doc.setTileClassColor(i, palette.get(i));
					}
					for (final Object val[]: dlist) {
						final String kind = (String) val[0];
						final Vector s = (Vector) val[1];
						final Color c = (Color) val[2];
						Item item = null;
						if (kind.equals("tile")) {
							final int id = (Integer) val[3];
							item = doc.add(doc.getTile(id), s);
						} else if (kind.equals("facet")) {
							final int tId = (Integer) val[3];
							final int fId = (Integer) val[4];
							item = doc.add(doc.getTile(tId).facet(fId), s);
						} else if (kind.equals("node")) {
							INode v = (INode) doc.getNet().getNode((Long) val[3]);
							item = doc.add(v, s);
						} else if (kind.equals("edge")) {
							final Tiling.Skeleton net = doc.getNet();
							final INode v = (INode) net.getNode((Long) val[3]);
							final INode w = (INode) net.getNode((Long) val[4]);
							final Vector t = (Vector) val[5];
							final IEdge e = (IEdge) net.getEdge(v, w, t);
							if (e != null) {
								item = doc.add(e, s);
							}
						}
						if (item != null) {
							doc.recolor(item, c);
						}
					}
					for (final Pair<Integer, Integer> item: fcolors.keySet()) {
						final Color c = fcolors.get(item);
						if (c != null) {
							final int tile = item.getFirst();
							final int index = item.getSecond();
							final Tiling.Tile t = doc.getTiles().get(tile);
							final Tiling.Facet f = t.facet(index);
							doc.setFacetClassColor(f, c);
						}
					}
					for (final Pair<Integer, Integer> item: fhidden) {
						final int tile = item.getFirst();
						final int index = item.getSecond();
						final Tiling.Tile t = doc.getTiles().get(tile);
						final Tiling.Facet f = t.facet(index);
						doc.hideFacetClass(f);
					}
					doc.setTransformation(trans);
				}
				
				return doc;
			}
    	});
	}

	private void registerPropertiesConverter() {
		registerConverter(new Converter() {
			public boolean canConvert(
			        @SuppressWarnings("rawtypes") final Class clazz)
			{
				return clazz == Properties.class;
			}
			public void marshal(final Object value,
					final HierarchicalStreamWriter writer,
					final MarshallingContext context) {
				final Properties props = (Properties) value;
				for (final Object key: props.keySet()) {
					writer.startNode("property");
					writer.addAttribute("key", (String) key);
					writer.setValue((String) props.getProperty((String) key));
					writer.endNode();
				}
			}
			public Object unmarshal(final HierarchicalStreamReader reader,
					final UnmarshallingContext context) {
				final Properties props = new Properties();
				while (reader.hasMoreChildren()) {
					reader.moveDown();
					final String key = reader.getAttribute("key");
					final String val = reader.getValue();
					props.setProperty(key, val);
					reader.moveUp();
				}
				return props;
			}
    	});
	}

	private void registerTransformationConverter() {
		registerConverter(new SingleValueConverter() {
			public boolean canConvert(
			        @SuppressWarnings("rawtypes") final Class clazz)
			{
				return clazz == Transformation.class;
			}
			public String toString(final Object value) {
				final double v[] = ((Transformation) value).getMatrix();
				final StringBuffer buf = new StringBuffer(40);
				for (int i = 0; i < v.length; ++i) {
					if (i > 0) {
						buf.append(' ');
					}
					buf.append(v[i]);
				}
				return buf.toString();
			}
			public Object fromString(final String spec) {
				final String fields[] = spec.trim().split("\\s+");
				
				final double v[] = new double[fields.length];
				for (int i = 0; i < fields.length; ++i) {
					v[i] = Double.parseDouble(fields[i]);
				}
				return new Transformation(v);
			}
    	});
	}

	private void registerVectorConverter() {
		registerConverter(new SingleValueConverter() {
			public boolean canConvert(
			        @SuppressWarnings("rawtypes") final Class clazz)
			{
				return clazz == Vector.class;
			}
			public String toString(final Object value) {
				final Vector v = (Vector) value;
				final StringBuffer buf = new StringBuffer(12);
				for (int i = 0; i < v.getDimension(); ++i) {
					if (i > 0) {
						buf.append(' ');
					}
					buf.append(v.get(i).toString());
				}
				return buf.toString();
			}
			public Object fromString(final String spec) {
				final String fields[] = spec.trim().split("\\s+");
				
				final int a[] = new int[fields.length];
				for (int i = 0; i < fields.length; ++i) {
					a[i] = Integer.parseInt(fields[i]);
				}
				return new Vector(a);
			}
    	});
	}

	private void registerColorConverter() {
		registerConverter(new Converter() {
			public boolean canConvert(
			        @SuppressWarnings("rawtypes") final Class clazz)
			{
				return clazz == Color.class;
			}
			public void marshal(final Object value,
					final HierarchicalStreamWriter writer,
					final MarshallingContext context) {
				final Color c = (Color) value;
				writer.addAttribute("red", String.valueOf(c.getRed()));
				writer.addAttribute("green", String.valueOf(c.getGreen()));
				writer.addAttribute("blue", String.valueOf(c.getBlue()));
			}
			public Object unmarshal(final HierarchicalStreamReader reader,
					final UnmarshallingContext context) {
				final int red = Integer.parseInt(reader.getAttribute("red"));
				final int green = Integer.parseInt(reader.getAttribute("green"));
				final int blue = Integer.parseInt(reader.getAttribute("blue"));
				
				return new Color(red, green, blue);
			}
    	});
	}

	private void registerDSymbolConverter() {
		registerConverter(new SingleValueConverter() {
			public boolean canConvert(
			        @SuppressWarnings("rawtypes") final Class clazz)
			{
				return clazz == DSymbol.class;
			}
			public String toString(final Object obj) {
				final String code = obj.toString();
				return code.substring(5, code.length() - 1);
			}
			public Object fromString(final String spec) {
				return new DSymbol(spec);
			}
    	});
	}
}
