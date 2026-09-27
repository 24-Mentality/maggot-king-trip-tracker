package com.maggotkingtriptracker.persistence;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.maggotkingtriptracker.model.AccountHistory;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.util.Filepath;

/**
 * Reads and writes one history-&lt;accountHash&gt;.json per account in the plugin data folder.
 * All disk IO runs on the supplied executor; callbacks are invoked on that executor thread.
 */
@Slf4j
public class HistoryStore
{
	private final Gson gson;
	private final Callable<Filepath> directorySupplier;
	private final ExecutorService executor;

	// Only touched on the executor thread
	private Filepath directory;

	public HistoryStore(Gson gson, Callable<Filepath> directorySupplier, ExecutorService executor)
	{
		this.gson = gson;
		this.directorySupplier = directorySupplier;
		this.executor = executor;
	}

	public void load(long accountHash, Consumer<LoadResult> callback)
	{
		submit(() -> callback.accept(readHistory(accountHash)));
	}

	/**
	 * @param json the history already serialized on the client thread
	 */
	public void save(long accountHash, String json)
	{
		submit(() -> writeHistory(accountHash, json));
	}

	/**
	 * Writes a user-chosen export file. The callback gets null on success, or the error.
	 */
	public void writeFile(Filepath file, String content, Consumer<Exception> callback)
	{
		submit(() ->
		{
			try
			{
				file.write(content.getBytes(StandardCharsets.UTF_8),
					StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
				callback.accept(null);
			}
			catch (Exception e)
			{
				log.warn("Unable to write export", e);
				callback.accept(e);
			}
		});
	}

	/**
	 * Reads a user-chosen history export. The callback gets the parsed history, or null and the error.
	 */
	public void readHistoryFile(Filepath file, BiConsumer<AccountHistory, Exception> callback)
	{
		submit(() ->
		{
			try (Reader reader = file.openBufferedReader())
			{
				AccountHistory history = gson.fromJson(reader, AccountHistory.class);
				if (history == null || history.getTrips() == null)
				{
					throw new IOException("Not a Maggot King Trip Tracker export");
				}
				callback.accept(history, null);
			}
			catch (Exception e)
			{
				log.warn("Unable to read import", e);
				callback.accept(null, e);
			}
		});
	}

	private void submit(Runnable task)
	{
		try
		{
			executor.execute(task);
		}
		catch (RejectedExecutionException e)
		{
			log.debug("Plugin shutting down; skipped history IO");
		}
	}

	private LoadResult readHistory(long accountHash)
	{
		try
		{
			Filepath file = directory().joinSegment(fileName(accountHash));
			if (!file.exists())
			{
				return new LoadResult(emptyHistory(accountHash), false);
			}

			AccountHistory history;
			try (Reader reader = file.openBufferedReader())
			{
				history = gson.fromJson(reader, AccountHistory.class);
			}
			catch (JsonParseException e)
			{
				Filepath backup = directory().joinSegment(fileName(accountHash) + ".corrupt-" + System.currentTimeMillis());
				log.warn("Maggot King history for this account is unreadable; moving it to {}", backup.getFileName(), e);
				file.moveTo(backup);
				return new LoadResult(emptyHistory(accountHash), false);
			}

			if (history == null)
			{
				return new LoadResult(emptyHistory(accountHash), false);
			}
			if (history.getTrips() == null)
			{
				history.setTrips(new ArrayList<>());
			}
			if (history.getEggPops() == null)
			{
				history.setEggPops(new ArrayList<>());
			}
			if (history.getPolishOutcomes() == null)
			{
				history.setPolishOutcomes(new HashMap<>());
			}
			if (history.getSchemaVersion() <= 0)
			{
				history.setSchemaVersion(AccountHistory.CURRENT_SCHEMA_VERSION);
			}
			history.setAccountHash(accountHash);

			// A file from a newer plugin version is shown but never overwritten
			boolean readOnly = history.getSchemaVersion() > AccountHistory.CURRENT_SCHEMA_VERSION;
			return new LoadResult(history, readOnly);
		}
		catch (Exception e)
		{
			log.warn("Unable to load Maggot King history", e);
			return new LoadResult(emptyHistory(accountHash), true);
		}
	}

	private void writeHistory(long accountHash, String json)
	{
		try
		{
			Filepath target = directory().joinSegment(fileName(accountHash));
			Filepath temp = directory().joinSegment(fileName(accountHash) + ".tmp");
			temp.write(json.getBytes(StandardCharsets.UTF_8),
				StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
			try
			{
				temp.moveTo(target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			}
			catch (IOException e)
			{
				temp.moveTo(target, StandardCopyOption.REPLACE_EXISTING);
			}
		}
		catch (Exception e)
		{
			log.warn("Unable to save Maggot King history", e);
		}
	}

	private Filepath directory() throws Exception
	{
		if (directory == null)
		{
			Filepath resolved = directorySupplier.call();
			resolved.createDirectories();
			directory = resolved;
		}
		return directory;
	}

	private static String fileName(long accountHash)
	{
		return "history-" + accountHash + ".json";
	}

	private static AccountHistory emptyHistory(long accountHash)
	{
		AccountHistory history = new AccountHistory();
		history.setAccountHash(accountHash);
		return history;
	}

	@Value
	public static class LoadResult
	{
		AccountHistory history;
		/**
		 * True if the file must not be overwritten (newer schema, or it could not be read).
		 */
		boolean readOnly;
	}
}
