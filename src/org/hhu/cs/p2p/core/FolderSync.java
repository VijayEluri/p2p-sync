package org.hhu.cs.p2p.core;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.log4j.Logger;
import org.hhu.cs.p2p.local.LocalIndex;
import org.hhu.cs.p2p.local.LocalIndexWatcher;
import org.hhu.cs.p2p.net.NetworkClient;
import org.hhu.cs.p2p.net.NetworkService;
import org.hhu.cs.p2p.remote.RemoteIndex;

import uk.co.flamingpenguin.jewel.cli.ArgumentValidationException;
import uk.co.flamingpenguin.jewel.cli.Cli;
import uk.co.flamingpenguin.jewel.cli.CliFactory;

/**
 * FolderSync creates a service that watches a directory for changes and
 * communicates these changes with other computers running the same service on a
 * local directory of their own.
 * 
 * The goal is to sync directories with the help of p2p technology.
 * 
 * @author Oliver Schrenk <oliver.schrenk@uni-duesseldorf.de>
 * 
 */
public class FolderSync {

	private static Logger logger = Logger.getLogger(FolderSync.class);

	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		logger.info("Starting application.");

		logger.info("Parsing arguments.");
		Cli<StartupArguments> cli = null;
		StartupArguments parsedArguments;
		try {
			cli = CliFactory.createCli(StartupArguments.class);
			parsedArguments = cli.parseArguments(args);
		} catch (ArgumentValidationException e) {
			logger.fatal(cli.getHelpMessage(), e);
			return;
		}

		logger.info("Validating arguments.");
		Options options;
		try {
			options = new OptionsBuilder().setArguments(parsedArguments)
					.build();
		} catch (IllegalArgumentException e) {
			logger.fatal("Startup failed.", e);
			return;
		}

		// redirect hazelcast logging output
		System.setProperty("hazelcast.logging.type", "log4j");
		Registry registry = Registry.getInstance();
		try {
			Path rootDirectory = options.getWatchDirectory();

			// will block for initial indexing
			LocalIndex localIndex = new LocalIndex(rootDirectory);

			// will only produce
			LocalIndexWatcher directoryWatcher = new LocalIndexWatcher(
					localIndex, rootDirectory);

			// producer, consumer
			RemoteIndex remoteIndex = new RemoteIndex();

			// change service is needed right away, start it
			ChangeService changeService = new ChangeService(localIndex, remoteIndex);
			new Thread(changeService).start();

			NetworkClient networkClient = new NetworkClient(rootDirectory);
			
			registry.setLocalIndex(localIndex);
			registry.setRemoteIndex(remoteIndex);
			registry.setChangeService(changeService);
			registry.setNetworkClient(networkClient);

			// startup sequence
			new Startup().run();

			new Thread(directoryWatcher).start();
			new NetworkService(rootDirectory).start();

		} catch (IOException e) {
			logger.fatal(e);
		}
	}
}
