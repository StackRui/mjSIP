/*
 * Copyright (c) 2023 Bernhard Haumacher et al. All Rights Reserved.
 */
package org.mjsip.config;

import java.io.File;

import org.kohsuke.args4j.ClassParser;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.ParserProperties;
import org.zoolu.util.ConfigFile;

/**
 * Utility for parsing command line options to multiple beans using args4j.
 */
public class OptionParser {

	/**
	 * Parses the given command line arguments and fills the given bean objects.
	 */
	public static MetaConfig parseOptions(String[] args, String defaultConfigFile, Object...beans) {
		MetaConfig metaConfig = new MetaConfig();
		ParserProperties options = ParserProperties.defaults().withUsageWidth(120);
		CmdLineParser parser = new CmdLineParser(metaConfig, options);
		for (Object bean : beans) {
			new ClassParser().parse(bean, parser);
		}
		
		try {
			parser.parseArgument(args);
			
			String argFile = metaConfig.configFile;
			
			File file;
			if (argFile != null && !argFile.isBlank()) {
				if ("none".equals(argFile.toLowerCase())) {
					file = null;
				} else {
					file = new File(argFile);
				}
			} else if (defaultConfigFile != null) {
				String fileName = System.getProperty("user.home") + "/" + defaultConfigFile;
				file = new File(fileName);
				if (!file.exists()) {
					file = null;
				}
			} else {
				file = null;
			}
			
			if (file!= null) {
				ConfigFile configFile = new ConfigFile(file);
				parser.parseArgument(configFile.toArguments());
				
				// Parse arguments again to make sure they have more precedence.
				parser.parseArgument(args);
			}
	
			if (metaConfig.help) {
				parser.printUsage(System.err);
				System.exit(1);
			}
		} catch (CmdLineException ex) {
			parser.printUsage(System.err);
			System.out.println(ex.getMessage());
			System.exit(1);
		}
		
		return metaConfig;
	}

}