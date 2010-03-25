package org.hhu.cs.p2p.remote;

import java.nio.file.Paths;

import org.apache.log4j.Logger;
import org.hhu.cs.p2p.core.Registry;
import org.hhu.cs.p2p.index.Attributes;
import org.hhu.cs.p2p.index.Change;
import org.hhu.cs.p2p.index.ChangeType;
import org.hhu.cs.p2p.index.Direction;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;

/**
 * @author Oliver
 * 
 */
public class RemoteIndexWatcher implements EntryListener<String, Attributes> {

	private static Logger logger = Logger.getLogger(RemoteIndexWatcher.class);

	public void entryAdded(EntryEvent<String, Attributes> event) {

		// if we are responsible, we can ignore the request
		if (event.getValue().getAddress().equals(
				Registry.getInstance().getAddress())) {
			if (logger.isTraceEnabled()) {
				logger.trace(String.format(
						"Request came from this machine. Ignoring %1s", event
								.getKey()));
			}
			return;
		}

		logger.info(String.format("Adding entry %1s", event.getKey()));
		Registry.getInstance().getChangeService().accept(
				new Change(Paths.get(event.getKey()), event.getValue()
						.getAddress(), ChangeType.CREATE, Direction.PULL));

	}

	public void entryRemoved(EntryEvent<String, Attributes> event) {
		logger.info(String.format("Deleting entry %1s", event.getKey()));
	}

	public void entryUpdated(EntryEvent<String, Attributes> event) {
		logger.info(String.format("Updating entry %1s", event.getKey()));
		// handle like entry added
	}

	@Override
	public void entryEvicted(EntryEvent<String, Attributes> event) {
		logger.info(String.format("Evecting entry %1s", event.getKey()));
	}

}