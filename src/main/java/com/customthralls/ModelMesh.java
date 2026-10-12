package com.customthralls;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Mesh geometry and colors based on the format used by the creators-kit plugin
 * (https://github.com/ScreteMonge/creators-kit). Coordinates are in local units where
 * one tile is 128 units, x is east, z is north and y is up with negative values.
 * Array contents belong to one load and are decoded before publication to the client thread.
 */
@Getter
@RequiredArgsConstructor
public final class ModelMesh
{
	private final boolean useVertexColours;
	private final int[][] vertices;
	private final int[][] faces;
	private final double[][] vertexColours;
	private final int[] vertexColourIndex;
	private final double[][] faceColours;
	private final int[] faceColourIndex;
	private final byte[] priorities;
}
