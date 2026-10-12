package com.customthralls;

import com.customthralls.generated.GeneratedModels;

/** Allocates and decodes generated Java arrays on a worker thread. */
final class BundledModels
{
	ModelMesh load(String modelId) throws ModelDataException
	{
		ModelMesh model = modelId == null ? null : GeneratedModels.create(modelId);
		if (model == null)
		{
			throw new ModelDataException("Unknown bundled model: " + modelId);
		}
		try
		{
			decodeDeltaRows(model.getVertices(), CustomModelLoader.MAX_VERTICES);
			decodeDeltaRows(model.getFaces(), CustomModelLoader.MAX_FACES);
			for (int[] face : model.getFaces())
			{
				for (int index : face)
				{
					if (index < 0 || index >= model.getVertices().length)
					{
						throw new ModelDataException("Invalid face index");
					}
				}
			}
		}
		catch (ModelDataException | ArithmeticException e)
		{
			throw new ModelDataException("Invalid generated geometry for " + modelId + ": " + e.getMessage(), e);
		}
		return model;
	}

	/** In-place column-wise prefix sums; every generated factory returns fresh arrays. */
	static void decodeDeltaRows(int[][] rows, int limit) throws ModelDataException
	{
		if (rows == null || rows.length == 0 || rows.length > limit)
		{
			throw new ModelDataException("Invalid geometry size");
		}
		int[] previous = new int[3];
		for (int[] row : rows)
		{
			if (row == null || row.length != 3)
			{
				throw new ModelDataException("Invalid geometry row");
			}
			for (int column = 0; column < 3; column++)
			{
				row[column] = Math.addExact(previous[column], row[column]);
				previous[column] = row[column];
			}
		}
	}
}
