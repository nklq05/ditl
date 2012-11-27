/*******************************************************************************
 * This file is part of DITL.                                                  *
 *                                                                             *
 * Copyright (C) 2011-2012 John Whitbeck <john@whitbeck.fr>                    *
 *                                                                             *
 * DITL is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU General Public License as published by        *
 * the Free Software Foundation, either version 3 of the License, or           *
 * (at your option) any later version.                                         *
 *                                                                             *
 * DITL is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of              *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the               *
 * GNU General Public License for more details.                                *
 *                                                                             *
 * You should have received a copy of the GNU General Public License           *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.       *
 *******************************************************************************/
package ditl.plausible.cli;

import java.io.IOException;

import org.apache.commons.cli.*;

import ditl.Store.*;
import ditl.WritableStore.AlreadyExistsException;
import ditl.cli.Command;
import ditl.cli.ConvertApp;
import ditl.graphs.EdgeTrace;
import ditl.graphs.cli.GraphOptions;
import ditl.plausible.*;

@Command(pkg="plausible", cmd="edges-to-windowed-edges", alias="e2we")
public class EdgesToWindowedEdges extends ConvertApp {
	
	private GraphOptions graph_options = new GraphOptions(GraphOptions.EDGES);
	private String windowedEdgesOption = "windowed-edges";
	private String windowed_edges_name;
	private long window;

	@Override
	protected void initOptions() {
		super.initOptions();
		graph_options.setOptions(options);
		options.addOption(null, windowedEdgesOption, true, "name of windowed edge trace (default: "+WindowedEdgeTrace.defaultName+")");
	}

	@Override
	protected void parseArgs(CommandLine cli, String[] args)
			throws ParseException, ArrayIndexOutOfBoundsException,
			HelpException {
		super.parseArgs(cli, args);
		graph_options.parse(cli);
		windowed_edges_name = cli.getOptionValue(windowedEdgesOption, WindowedEdgeTrace.defaultName);
		window = Long.parseLong(args[1]);
	}

	@Override
	protected void run() throws IOException, NoSuchTraceException, AlreadyExistsException, LoadTraceException {
		EdgeTrace edges = (EdgeTrace) orig_store.getTrace(graph_options.get(GraphOptions.EDGES));
		WindowedEdgeTrace windowed_edges = (WindowedEdgeTrace) dest_store.newTrace(windowed_edges_name, WindowedEdgeTrace.type, force);
		window *= edges.ticsPerSecond();
		new WindowedEdgeConverter(windowed_edges, edges, window).convert();
	}
	
	@Override
	protected String getUsageString(){
		return "[OPTIONS] STORE WINDOW";
	}
	

}
