package com.customthralls;

/** A model ID is unknown or its generated geometry cannot be decoded. */
final class ModelDataException extends Exception
{
	ModelDataException(String message)
	{
		super(message);
	}

	ModelDataException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
