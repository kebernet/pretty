/**
 * Copyright 2013, Robert Cooper, Reach Health
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 */
package com.reachcall.pretty.shell;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author kebernet
 */
public class ShellThread extends Thread {

	private static final Logger LOG = Logger.getLogger(ShellThread.class.getCanonicalName());

	private ShellTask clientTask;

	public ShellThread(ShellTask runnable, String threadName) {
		super(runnable, threadName);
		this.clientTask = runnable;
	}

	@SuppressWarnings("deprecation")
	public void kill() {
		interrupt();
		clientTask.closeSocket();
		try {
			join(500);
		} catch (InterruptedException e) {
			// Restore interrupt thread flag
			currentThread().interrupt();
		}

		if (isAlive()) {
			LOG.log(Level.WARNING, "{0} not responding to interrupts ... forcibly stopping", getName());
			stop();
		}
	}

}