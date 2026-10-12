package com.customthralls;

/** Model downloading, cache access, integrity validation, or decoding failed. */
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
