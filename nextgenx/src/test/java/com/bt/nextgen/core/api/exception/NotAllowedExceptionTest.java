package com.bt.nextgen.core.api.exception;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.bt.nextgen.core.api.ApiVersion;

public class NotAllowedExceptionTest
{
	private String message;
	private Throwable cause;

	@Before
	public void setup() throws Exception
	{
		message = "message";
		cause = new Throwable("cause");
	}

	@Test
	public void testConstructor_whenVersion_thenBaseClassHasVersion()
	{
		NotAllowedException ex = new NotAllowedException(ApiVersion.CURRENT_VERSION);
		assertEquals(ApiVersion.CURRENT_VERSION, ex.getApiVersion());
	}

	@Test
	public void testConstructor_whenVersionAndMessage_thenBaseClassHasVersionAndMessage()
	{
		NotAllowedException ex = new NotAllowedException(ApiVersion.CURRENT_VERSION, message);
		assertEquals(ApiVersion.CURRENT_VERSION, ex.getApiVersion());
		assertEquals(message, ex.getMessage());
	}

	@Test
	public void testConstructor_whenVersionAndCause_thenBaseClassHasVersionAndCause()
	{
		NotAllowedException ex = new NotAllowedException(ApiVersion.CURRENT_VERSION, cause);
		assertEquals(ApiVersion.CURRENT_VERSION, ex.getApiVersion());
		assertEquals(cause, ex.getCause());
	}

	@Test
	public void testConstructor_whenVersionAndMessageAndCause_thenBaseClassHasVersionAndMessageAndCause()
	{
		NotAllowedException ex = new NotAllowedException(ApiVersion.CURRENT_VERSION, message, cause);
		assertEquals(ApiVersion.CURRENT_VERSION, ex.getApiVersion());
		assertEquals(message, ex.getMessage());
		assertEquals(cause, ex.getCause());
	}
}