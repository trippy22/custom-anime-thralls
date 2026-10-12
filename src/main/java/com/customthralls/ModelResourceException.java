package com.customthralls;

/** A bundled model could not be found, read, parsed, or decoded. */
final class ModelResourceException extends Exception
{
	ModelResourceException(String message)
	{
		super(message);
	}

	ModelResourceException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
