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


package org.gavrog.joss.graphics;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gavrog.box.simple.NamedConstant;


/**
 * Implements Catmull-Clark subdivision surfaces.
 */
public class Surface {
	private static class Target extends NamedConstant {
		protected Target(String name) { super(name); }
	}
	private static class Attribute extends NamedConstant {
		protected Attribute(String name) { super(name); }
	}
	
	final public static Target FACE = new Target("face");
	final public static Target VERTEX = new Target("vertex");
	
	final public static Attribute CONVEX = new Attribute("convex");
	final public static Attribute NORMAL = new Attribute("normal");
	final public static Attribute TAG = new Attribute("tag");

	private static class AttributeKey {
		final Target targetType;
		final int index;
		final Attribute attribute;
		
		public AttributeKey(final Target type, final int idx,
				final Attribute key) {
			this.targetType = type;
			this.index = idx;
			this.attribute = key;
		}
		
		public int hashCode() {
			return (this.targetType.hashCode() * 37 + this.index) * 37
					+ this.attribute.hashCode();
		}
		
		public boolean equals(final Object x) {
			final AttributeKey other = (AttributeKey) x;
			return this.targetType.equals(other.targetType)
					&& this.index == other.index
					&& this.attribute.equals(other.attribute);
		}
	}
	
    final public double[][] vertices;
    final public int[][] faces;
    final public int[] fixed;
    final private Map<AttributeKey, Object> attributes;
    
    public Surface(
			final double[][] vertices, final int[][] faces, final int fixed[]) {
        this.vertices = (double[][]) vertices.clone();
        this.faces = (int[][]) faces.clone();
        this.fixed = (int[]) fixed.clone();
        this.attributes = new HashMap<AttributeKey, Object>();
    }
    
    public void setAttribute(final Target targetType, final int targetIndex,
			final Attribute attributeKey, final Object attributeValue) {
		this.attributes.put(new AttributeKey(targetType, targetIndex,
				attributeKey), attributeValue);
	}

    public void setAttribute(final Object targetType, final int targetIndex,
			final Object attributeKey, final boolean attributeValue) {
		setAttribute(targetType, targetIndex, attributeKey, new Boolean(
				attributeValue));
	}

	public Object getAttribute(final Target targetType, final int targetIndex,
			final Attribute attributeKey) {
		return this.attributes.get(new AttributeKey(targetType, targetIndex,
				attributeKey));
	}
    
	public boolean getBooleanAttribute(final Target targetType,
			final int targetIndex, final Attribute attributeKey) {
		final Object val = getAttribute(targetType, targetIndex, attributeKey);
		return val == null || ((Boolean) val).booleanValue();
	}
    
    public void computeNormals() {
		final int nv = this.vertices.length;
		final double vertexNormals[][] = new double[nv][3];
		final int nf = this.faces.length;

		for (int i = 0; i < nf; ++i) {
			final int[] face = this.faces[i];
			final int n = face.length;
			final double normal[] = new double[] { 0.0, 0.0, 0.0 };
			for (int j = 0; j < n; ++j) {
				final double p[] = this.vertices[face[j]];
				final double q[] = this.vertices[face[(j + 1) % n]];
				Vec.plus(normal, normal, Vec.crossProduct(null, p, q));
			}
			Vec.normalized(normal, normal);
			setAttribute(FACE, i, NORMAL, normal);

			for (int j = 0; j < n; ++j) {
				final int v = face[j];
				Vec.plus(vertexNormals[v], vertexNormals[v], normal);
			}
		}
		for (int i = 0; i < nv; ++i) {
			final double normal[] = new double[3];
			Vec.normalized(normal, vertexNormals[i]);
			setAttribute(VERTEX, i, NORMAL, normal);
		}
	}
    
    public double[][] getFaceNormals() {
    	final int nf = this.faces.length;
    	final double normals[][] = new double[nf][3];
    	for (int i = 0; i < nf; ++i) {
    		final double n[] = (double[]) getAttribute(FACE, i, NORMAL);
    		for (int j = 0; j < 3; ++j) {
    			normals[i][j] = n[j];
    		}
    	}
    	return normals;
    }
    
    public double[][] getVertexNormals() {
    	final int nv = this.vertices.length;
    	final double normals[][] = new double[nv][3];
    	for (int i = 0; i < nv; ++i) {
    		final double n[] = (double[]) getAttribute(VERTEX, i, NORMAL);
    		for (int j = 0; j < 3; ++j) {
    			normals[i][j] = n[j];
    		}
    	}
    	return normals;
    }
    
    public void tagAll(final Object tag) {
    	for (int i = 0; i < faces.length; ++i) {
    		setAttribute(FACE, i, TAG, tag);
    	}
    }

    public Set<String> faceTags() {
    	final Set<String> res = new HashSet<String>();
    	
    	for (int i = 0; i < faces.length; ++i) {
    		final String tag = (String) getAttribute(FACE, i, TAG);
    		if (tag != null) res.add(tag);
    	}
    	return res;
    }
    
    public static Surface concatenation(final Surface parts[]) {
    	final int n = parts.length;
        int newNF = 0;
        int newNV = 0;
        for (int i = 0; i < n; ++i) {
            newNF += parts[i].faces.length;
            newNV += parts[i].vertices.length;
        }
        final double newVerts[][] = new double[newNV][];
        final int newFixed[] = new int[newNV];
        final int newFaces[][] = new int[newNF][];
        final int mapV[][] = new int[n][];
        final int mapF[][] = new int[n][];
        
        int offsetV = 0;
        int offsetF = 0;
        for (int i = 0; i < n; ++i) {
            final double verts[][] = parts[i].vertices;
            final int faces[][] = parts[i].faces;
            final int nv = verts.length;
            final int nf = faces.length;
            mapV[i] = new int[nv];
            mapF[i] = new int[nf];
            for (int j = 0; j < nv; ++j) {
            	mapV[i][j] = offsetV + j;
            }
            for (int j = 0; j < nf; ++j) {
            	mapF[i][j] = offsetF + j;
            }
            System.arraycopy(verts, 0, newVerts, offsetV, nv);
            System.arraycopy(parts[i].fixed, 0, newFixed, offsetV, nv);
            for (int j = 0; j < faces.length; ++j) {
                final int face[] = faces[j];
                final int newFace[] = new int[face.length];
                newFaces[j + offsetF] = newFace;
                for (int k = 0; k < face.length; ++k) {
                    newFace[k] = face[k] + offsetV;
                }
            }
            offsetV += nv;
            offsetF += nf;
        }
        
        final Surface surf = new Surface(newVerts, newFaces, newFixed);
        
        // --- copy attributes
		for (int i = 0; i < n; ++i) {
			final Surface part = parts[i];
			for (final AttributeKey key: part.attributes.keySet()) {
				final int newIndex;
				if (key.targetType == FACE) {
					newIndex = mapF[i][key.index];
				} else {
					newIndex = mapV[i][key.index];
				}
				if (newIndex >= 0) {
					surf.setAttribute(key.targetType, newIndex, key.attribute,
							part.attributes.get(key));
				}
			}
		}
        return surf;
    }
    
    private boolean equal(final Object a, final Object b) {
        if (a == null) {
            return b == null;
        } else {
            return a.equals(b);
        }
    }
    
    public Surface extract(final Object tag) {
        final int nf = this.faces.length;
        final int nv = this.vertices.length;
        
        // --- determine which vertices appear on faces with the right tag
        final boolean usedV[] = new boolean[nv];
        final boolean usedF[] = new boolean[nf];
        int newNV = 0;
        int newNF = 0;
        for (final AttributeKey key: attributes.keySet()) {
        	if (key.targetType.equals(FACE) && key.attribute.equals(TAG)
					&& equal(tag, attributes.get(key))) {
        		final int i = key.index;
                final int face[] = this.faces[i];
                for (int j = 0; j < face.length; ++j) {
                    final int v = face[j];
                    if (!usedV[v]) {
                        ++newNV;
                    }
                    usedV[v] = true;
                }
                if (!usedF[i]) {
                	++newNF;
                }
            	usedF[i] = true;
        	}
        }
        
        // --- collect used vertices and map old vertex numbers to new ones
        final int mapV[] = new int[nv];
        final double newVerts[][] = new double[newNV][3];
        final int newFixed[] = new int[newNV];
        int k = 0;
        for (int i = 0; i < nv; ++i) {
            if (usedV[i]) {
                mapV[i] = k;
                Vec.copy(newVerts[k], this.vertices[i]);
                newFixed[k] = this.fixed[i];
                ++k;
            } else {
            	mapV[i] = -1;
            }
        }
        
        // --- map and collect faces
        final int mapF[] = new int[nf];
        final int newFaces[][] = new int[newNF][];
        final Object newTags[] = new Object[newNF];
        k = 0;
        for (int i = 0; i < nf; ++i) {
            if (usedF[i]) {
            	mapF[i] = k;
                final int face[] = this.faces[i];
                final int newFace[] = new int[face.length];
                for (int j = 0; j < face.length; ++j) {
                    newFace[j] = mapV[face[j]];
                }
                newFaces[k] = newFace;
                newTags[k] = tag;
                ++k;
            } else {
            	mapF[i] = -1;
            }
        }
        
        // --- make new surface
        final Surface surf = new Surface(newVerts, newFaces, newFixed);
        
        // --- extract attributes
        for (final AttributeKey key: this.attributes.keySet()) {
        	final int newIndex;
        	if (key.targetType == FACE) {
        		newIndex = mapF[key.index];
        	} else {
        		newIndex = mapV[key.index];
        	}
        	if (newIndex >= 0) {
        		surf.setAttribute(key.targetType, newIndex, key.attribute,
        				this.attributes.get(key));
        	}
        }
        
        return surf;
    }
    
    public Surface subdivision() {
        // --- shortcuts
        final int nv = this.vertices.length;
        final int nf = this.faces.length;

        // --- initialize array to hold map from edges to running numbers
        final int[][] edgeToIndex = new int[nv][nv];
        for (int i = 0; i < nv; ++i) {
            for (int j = 0; j < nv; ++j) {
                edgeToIndex[i][j] = -1;
            }
        }
        
        // --- count edges and map endpoints to running numbers
        int ne = 0;
        int neInterior = 0;
        for (int i = 0; i < nf; ++i) {
            final int[] face = this.faces[i];
            final int n = face.length;
            for (int j = 0; j < n; ++j) {
                final int v = face[j];
                final int w = face[(j + 1) % n];
                if (edgeToIndex[v][w] < 0) {
                    edgeToIndex[v][w] = edgeToIndex[w][v] = ne;
                    ++ne;
                } else {
                    ++neInterior;
                }
            }
        }
        
        // --- create arrays for new surface
        final double newVertices[][] = new double[nf + ne + nv][3];
        final int newFaces[][] = new int[ne + neInterior][4];
        final int newFixed[] = new int[newVertices.length];
        final boolean newConvex[] = new boolean[newVertices.length];
        
        // --- create mappings from old to (lists of) new components
        final int mapF[][] = new int[nf][];
        
        // --- make the new faces
        int facesMade = 0;
        for (int i = 0; i < nf; ++i) {
            final int[] face = this.faces[i];
            final int n = face.length;
            mapF[i] = new int[n];
            for (int j = 0; j < n; ++j) {
                final int u = face[j];
                final int v = face[(j + 1) % n];
                final int w = face[(j + 2) % n];
                final int k = edgeToIndex[u][v];
                final int k1 = edgeToIndex[v][w];
                newFaces[facesMade] = new int[] { i, nf + k, nf + ne + v,
						nf + k1 };
                mapF[i][j] = facesMade;
                ++facesMade;
                
                final boolean cu = getBooleanAttribute(VERTEX, u, CONVEX);
                final boolean cv = getBooleanAttribute(VERTEX, v, CONVEX);
                boolean convex = newConvex[nf + k] = (cu || cv);
                
                final int fu = this.fixed[u];
                final int fv = this.fixed[v];
                if (convex && (fu > 0 && fv <= 0 || fv > 0 && fu <= 0)) {
                    newFixed[nf + k] = 0;
                } else {
                    newFixed[nf + k] = Math.min(fu, fv) - 1;
                }
                
                newFixed[nf + ne + u] = this.fixed[u] - 1;
            }
        }
        
        // --- create arrays to hold temporary data
        final double[][] vertexTmp = new double[newVertices.length][3];
        final int[] vertexDeg = new int[newVertices.length];
        
        // --- find positions for face points
        for (int k = 0; k < newFaces.length; ++k) {
        	final int[] face = newFaces[k];
            final double p[] = vertexTmp[face[0]];
            final double q[] = this.vertices[(face[2] - nf - ne)];
            p[0] += q[0];
            p[1] += q[1];
            p[2] += q[2];
            ++vertexDeg[face[0]];
        }
        for (int i = 0; i < nf; ++i) {
            final double p[] = newVertices[i];
            final double q[] = vertexTmp[i];
            final int d = vertexDeg[i];
            p[0] = q[0] / d;
            p[1] = q[1] / d;
            p[2] = q[2] / d;
        }
        
        // --- find positions for edge points
        for (int k = 0; k < newFaces.length; ++k) {
        	final int[] face = newFaces[k];
            final int e1 = face[1];
            final int e2 = face[3];
            final double p1[] = vertexTmp[e1];
            final double p2[] = vertexTmp[e2];
            final double q1[] = this.vertices[face[2] - nf - ne];
            final double q2[] = newVertices[face[0]];
            for (int i = 0; i < 3; ++i) {
                p1[i] += q1[i];
                p2[i] += q1[i];
            }
            ++vertexDeg[e1];
            ++vertexDeg[e2];
            if (newFixed[e1] < 0) {
                for (int i = 0; i < 3; ++i) {
                    p1[i] += q2[i];
                }
                ++vertexDeg[e1];
            }
            if (newFixed[e2] < 0) {
                for (int i = 0; i < 3; ++i) {
                    p2[i] += q2[i];
                }
                ++vertexDeg[e2];
            }
        }
        for (int i = 0; i < ne; ++i) {
            final double p[] = newVertices[nf + i];
            final double q[] = vertexTmp[nf + i];
            final int d = vertexDeg[nf + i];
            p[0] = q[0] / d;
            p[1] = q[1] / d;
            p[2] = q[2] / d;
        }
        
        // --- adjust positions for original vertices
        for (int k = 0; k < newFaces.length; ++k) {
        	final int[] face = newFaces[k];
            final int v = face[2];
            if (newFixed[v] >= 0) {
                continue;
            }
            final double[] p = vertexTmp[v];
            final double[] r = newVertices[face[0]];
            final double[] q1 = newVertices[face[1]];
            final double[] q2 = newVertices[face[3]];
            for (int nu = 0; nu < 3; ++nu) {
                p[nu] += 2 * q1[nu] + 2 * q2[nu] - r[nu];
            }
            ++vertexDeg[v];
        }
        for (int i = 0; i < nv; ++i) {
            final int v = nf + ne + i;
            final double p[] = newVertices[v];
            final double q[] = this.vertices[i];
            p[0] = q[0];
            p[1] = q[1];
            p[2] = q[2];
            if (newFixed[v] < 0) {
                final double[] r = vertexTmp[v];
                final int d = vertexDeg[v];
                for (int nu = 0; nu < 3; ++nu) {
                    p[nu] = ((d - 3) * p[nu] + r[nu] / d) / d;
                }
            }
        }
        
        Surface surf = new Surface(newVertices, newFaces, newFixed);

        // --- copy attributes
        for (final AttributeKey key: this.attributes.keySet()) {
			final Target type = key.targetType;
			final Attribute attr = key.attribute;
			final Object val = this.attributes.get(key);
			if (key.targetType == FACE) {
				final int o2n[] = mapF[key.index];
				for (int j = 0; j < o2n.length; ++j) {
					surf.setAttribute(type, o2n[j], attr, val);
				}
			} else {
				surf.setAttribute(type, key.index + nf + ne, attr, val);
			}
		}
        // --- add convex attributes
        for (int i = 0; i < newVertices.length; ++i) {
        	if (newConvex[i]) {
        		setAttribute(VERTEX, i, CONVEX, CONVEX);
        	}
        }
        
        return surf;
    }

    public static Surface fromOutline(final double corners[][],
    		final int fixBorder) {
    	final List<double[]> vertices = new ArrayList<double[]>();
    	final List<int[]> faces = new ArrayList<int[]>();
    	final double tmp[] = new double[3];
    	int startInner = 0;
    	for (int i = 0; i < corners.length; ++i) {
    		vertices.add(corners[i]);
    	}
    	
    	while (true) {
            final int startNew = vertices.size();
			final int n = startNew - startInner;
			if (n <= 4) {
				break;
			}
			
            // --- compute an average normal vector and face center
			final double normal[] = new double[3];
			final double center[] = new double[3];
			for (int i = 0; i < n; ++i) {
				Vec.crossProduct(tmp, (double[]) vertices.get(i + startInner),
						(double[]) vertices.get((i + 1) % n + startInner));
				Vec.plus(normal, normal, tmp);
				Vec.plus(center, center, (double[]) vertices.get(i + startInner));
			}
			// --- normalize both vectors
			Vec.normalized(normal, normal);
			Vec.times(center, 1.0 / n, center);

			// --- determine if vertices lie above, on or below the middle plane
			final int upDown[] = new int[n];
			int nOnMiddle = 0;
			for (int i = 0; i < n; ++i) {
				Vec.minus(tmp, (double[]) vertices.get(i + startInner),
						center);
				final double d0 = Vec.innerProduct(normal, tmp);
				if (d0 > 0.1) {
					upDown[i] = 1;
                } else if (d0 < -0.1) {
                    upDown[i] = -1;
                } else {
                    upDown[i] = 0;
                    ++nOnMiddle;
                }
            }
            if (nOnMiddle == n) {
                break;
            }

            // --- determine where the middle plane is crossed
            final boolean changes[] = new boolean[n];
            int nChanging = 0;
            for (int i0 = 0; i0 < n; ++i0) {
                int i1 = (i0 + 1) % n;
                if (upDown[i0] != upDown[i1]) {
                    changes[i0] = true;
                    ++nChanging;
                } else {
                    changes[i0] = false;
                }
            }
            if (nChanging < 4) {
                break;
            }
            
            // --- add inner vertices
            final int back[] = new int[startNew + n];
			final int forw[] = new int[startNew + n];
			for (int i = startInner; i < back.length; ++i) {
				back[i] = forw[i] = -1;
			}
			
			for (int i0 = 0; i0 < n; ++i0) {
				if (i0 == 0 && changes[n-1] && changes[0] && upDown[0] == 0) {
					continue;
				}
				final int i1 = (i0 + 1) % n;
                final int k = vertices.size();
                final double c[] = new double[3];
                final int a;
                final int b;
				if (changes[i0]) {
					if (changes[i1] && upDown[i1] == 0) {
						a = i0;
						b = (i1 + 1) % n;
						Vec.linearCombination(tmp, 0.5, (double[]) vertices
								.get(i0 + startInner), 0.5, (double[]) vertices
								.get(i1 + startInner));
						Vec.linearCombination(c, 0.667, tmp, 0.333,
								(double[]) vertices.get(b + startInner));
						++i0;
					} else {
						a = i0;
						b = i1;
						Vec.linearCombination(c, 0.5, (double[]) vertices
								.get(i0 + startInner), 0.5, (double[]) vertices
								.get(i1 + startInner));
					}
				} else if (!changes[i1]) {
					a = b = i1;
					Vec.copy(c, (double[]) vertices.get(i1 + startInner));
				} else {
					continue;
                }
				back[k] = a + startInner;
				forw[k] = b + startInner;
				forw[a + startInner] = k;
				back[b + startInner] = k;
                final double v[] = new double[3];
                Vec.minus(tmp, c, center);
                Vec.complementProjection(tmp, tmp, normal);
                Vec.linearCombination(tmp, 0.5, tmp, 1, center);
                Vec.linearCombination(v, 0.667, tmp, 0.333, c);
                vertices.add(v);
			}
			
            // --- add new faces pointing outward
			for (int i = startInner; i < startNew; ++i) {
				if (back[i] >= 0 && forw[i] >= 0) {
					final int b = back[i];
					final int f = forw[i];
					if (b != f) {
						faces.add(new int[] { i, f, b });
					}
				}
			}
            
            // --- add new faces pointing inward
			for (int i = startNew; i < vertices.size(); ++i) {
				if (back[i] >= 0 && forw[i] >= 0) {
					final int b = back[i];
					final int f = forw[i];
					if (b != f) {
						final int m = (b + 1 - startInner) % n + startInner;
						if (m == f) {
							faces.add(new int[] { i, b, f });
						} else {
							final double u[] = new double[3];
							final double v[] = new double[3];
							final double w[] = new double[3];
							Vec.minus(u, (double[]) vertices.get(b),
									(double[]) vertices.get(m));
							Vec.minus(v, (double[]) vertices.get(i),
									(double[]) vertices.get(m));
							Vec.minus(w, (double[]) vertices.get(f),
									(double[]) vertices.get(m));
							final double angle = Vec.angle(u, v)
									+ Vec.angle(v, w);
							if (angle > 0.75 * Math.PI) {
								faces.add(new int[] { i, b, m });
								faces.add(new int[] { i, m, f });
							} else {
								faces.add(new int[] { i, b, m, f });
							}
						}
					}
				}
			}
            
            // --- add remaining new faces
			for (int i0 = startInner; i0 < startNew; ++i0) {
                if (forw[i0] < 0 || forw[i0] == back[i0]) {
					if (back[i0] < 0) {
						continue;
					}
					final int i1 = (i0 + 1 - startInner) % n + startInner;
					final int b = back[i0];
					final int f;
					if (back[i1] >= 0) {
						f = back[i1];
					} else {
						f = forw[i1];
					}
					faces.add(new int[] { i0, i1, f, b });
				}
			}
			
			// --- prepare for the next step if any
            final int x = vertices.size() - startNew;
			startInner = startNew;
			if (x == n) {
				break;
			}
    	}
    	// --- add final inner face
    	final int inner[] = new int[vertices.size() - startInner];
    	for (int i = startInner; i < vertices.size(); ++i) {
    		inner[i - startInner] = i;
    	}
    	faces.add(inner);

    	// --- construct a subdivision surface to return
    	final double pos[][] = new double[vertices.size()][];
    	vertices.toArray(pos);
    	final int idcs[][] = new int[faces.size()][];
    	faces.toArray(idcs);
    	final int fixed[] = new int[vertices.size()];
    	for (int i = 0; i < corners.length; ++i) {
    		fixed[i] = fixBorder;
    	}
    	
    	return new Surface(pos, idcs, fixed);
	}
    
    public void write(final OutputStream target) throws IOException {
    	write(new OutputStreamWriter(target));
    }
    
    public void write(final OutputStream target,
    		final int startIndex, final String prefix,
    		final double transform[], final boolean invert) throws IOException {
    	write(new OutputStreamWriter(target),
    			startIndex, prefix, transform, invert);
    }
    
    public void write(final Writer target) throws IOException {
    	write(target, 1, "",
    			new double[] { 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0 }, false);
    }
    
    public void write(final Writer target,
    		final int startIndex, final String prefix,
    		final double transform[], final boolean invert) throws IOException {
    	final double t[] = transform;
    	final BufferedWriter out = new BufferedWriter(target);
    	for (int i = 0; i < vertices.length; ++i) {
    		final double v[] = vertices[i];
    		out.write(String.format("v %f %f %f\n",
    				v[0] * t[ 0] + v[1] * t[ 1] + v[2] * t[ 2] + t[ 3],
    				v[0] * t[ 4] + v[1] * t[ 5] + v[2] * t[ 6] + t[ 7],
    				v[0] * t[ 8] + v[1] * t[ 9] + v[2] * t[10] + t[11]));
    	}
    	computeNormals();
    	final double normals[][] = getVertexNormals();
    	for (int i = 0; i < normals.length; ++i) {
    		final double n[] = normals[i];
    		if (invert) {
    			out.write(String.format("vn %f %f %f\n", -n[0], -n[1], -n[2]));
    		} else {
    			out.write(String.format("vn %f %f %f\n", n[0], n[1], n[2]));
    		}
    	}
    	final Map<String, List<Integer>> mats =
    		new HashMap<String, List<Integer>>();
    	for (int i = 0; i < faces.length; ++i) {
    		String m = (String) getAttribute(FACE, i, TAG);
    		if (m == null) m = "default";
    		if (mats.get(m) == null) mats.put(m, new ArrayList<Integer>());
    		mats.get(m).add(i);
    	}
    	for (final String m: mats.keySet()) {
    		out.write(String.format("usemtl %s%s\n", prefix, m));
    		for (final int i: mats.get(m)) {
    			final int f[] = faces[i];
    			out.write("f ");
    			if (invert) {
					for (int j = f.length-1; j > -1; --j) {
						final int k = f[j] + startIndex;
						out.write(String.format(" %d//%d", k, k));
					}
				} else {
					for (int j = 0; j < f.length; ++j) {
						final int k = f[j] + startIndex;
						out.write(String.format(" %d//%d", k, k));
					}
				}
    			out.write("\n");
    		}
    	}
    	out.flush();
    }
    
    public static void main(final String args[]) {
        final double v[][] = { { 0, 0, 0 }, { 0, 0, 1 }, { 0, 1, 0 },
                { 0, 1, 1 }, { 1, 0, 0 }, { 1, 0, 1 }, { 1, 1, 0 }, { 1, 1, 1 } };
        final int f[][] = { { 0, 1, 3, 2 }, { 5, 4, 6, 7 }, { 1, 0, 4, 5 },
                { 2, 3, 7, 6 }, { 0, 2, 6, 4 }, { 3, 1, 5, 7 } };
        final int fixed[] = new int[8];
        final Object tag[] = { "left", "right", "bottom", "top", "back",
				"front" };
        
        Surface surf = new Surface(v, f, fixed);
        for (int i = 0; i < tag.length; ++i) {
        	surf.setAttribute(FACE, i, TAG, tag[i]);
        }
        surf = surf.subdivision();
        surf.computeNormals();
		try {
			surf.write(System.out);
			surf.write(System.out,
					surf.vertices.length + 1, "second_",
					new double[] { 1.1, 0, 0, 1, 0, 1.1, 0, 0, 0, 0, 1.1, 0 },
					false);
		} catch (IOException ex) {
			ex.printStackTrace(System.err);
		}
    }
}
