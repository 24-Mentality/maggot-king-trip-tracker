package com.maggotkingtriptracker.diagnostic;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.util.Filepath;

/**
 * Appends diagnostic lines to diagnostic.log in the plugin data directory. Lines are queued from the
 * client thread and written by a dedicated background thread, so no disk IO happens on the client thread.
 */
@Slf4j
class DiagnosticLogWriter
{
	static final String FILE_NAME = "diagnostic.log";
	private static final String ROTATED_FILE_NAME = "diagnostic.log.1";
	private static final long MAX_FILE_BYTES = 10L * 1024 * 1024;
	private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

	private final Callable<Filepath> directorySupplier;
	private final Queue<String> pending = new ConcurrentLinkedQueue<>();
	private final AtomicBoolean drainScheduled = new AtomicBoolean();
	private final ExecutorService executor = Executors.newSingleThreadExecutor(r ->
	{
		Thread thread = new Thread(r, "maggot-king-trip-tracker-diagnostic");
		thread.setDaemon(true);
		return thread;
	});

	// Only touched on the executor thread
	private Filepath directory;

	DiagnosticLogWriter(Callable<Filepath> directorySupplier)
	{
		this.directorySupplier = directorySupplier;
	}

	void append(String line)
	{
		pending.add(LocalDateTime.now().format(TIMESTAMP) + ' ' + line);
		if (drainScheduled.compareAndSet(false, true))
		{
			try
			{
				executor.execute(this::drain);
			}
			catch (RejectedExecutionException e)
			{
				// Shutting down; drop the line
				pending.clear();
			}
		}
	}

	void shutDown()
	{
		executor.shutdownNow();
	}

	private void drain()
	{
		drainScheduled.set(false);
		if (pending.isEmpty())
		{
			return;
		}

		try
		{
			Filepath logFile = logFile();
			try (BufferedWriter writer = logFile.openBufferedWriter(StandardOpenOption.CREATE, StandardOpenOption.APPEND))
			{
				String line;
				while ((line = pending.poll()) != null)
				{
					writer.write(line);
					writer.newLine();
				}
			}
		}
		catch (Exception e)
		{
			pending.clear();
			log.warn("Unable to write Maggot King diagnostic log", e);
		}
	}

	private Filepath logFile() throws Exception
	{
		if (directory == null)
		{
			// getPluginDirectory() only creates plugin-data, not this plugin's folder inside it
			Filepath resolved = directorySupplier.call();
			resolved.createDirectories();
			directory = resolved;
		}

		Filepath logFile = directory.joinSegment(FILE_NAME);
		rotateIfNeeded(logFile);
		return logFile;
	}

	private void rotateIfNeeded(Filepath logFile) throws IOException
	{
		if (logFile.exists() && logFile.size() > MAX_FILE_BYTES)
		{
			logFile.moveTo(directory.joinSegment(ROTATED_FILE_NAME), StandardCopyOption.REPLACE_EXISTING);
		}
	}
}
