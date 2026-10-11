package com.customthralls;

import lombok.Getter;
import lombok.Setter;

/**
 * A generated mesh based on the JSON format used by the creators-kit plugin
 * (https://github.com/ScreteMonge/creators-kit). Coordinates are in local units where
 * one tile is 128 units, x is east, z is north and y is up with negative values.
 */
@Getter
@Setter
class BlenderModel
{
	/** Optional storage encoding; the resource reader restores absolute geometry before use. */
	private String geometryEncoding;
	private boolean useVertexColours;
	private int[][] vertices;
	private int[][] faces;
	private double[][] vertexColours;
	private int[] vertexColourIndex;
	private double[][] faceColours;
	private int[] faceColourIndex;
	private byte[] priorities;
}
