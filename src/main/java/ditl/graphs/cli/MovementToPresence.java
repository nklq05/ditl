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
package ditl.graphs.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;

import ditl.cli.App;
import ditl.cli.ConvertApp;
import ditl.graphs.MovementToPresenceConverter;
import ditl.graphs.MovementTrace;
import ditl.graphs.PresenceTrace;

@App.Cli(pkg = "graphs", cmd = "movement-to-presence", alias = "m2p")
public class MovementToPresence extends ConvertApp {

    private final GraphOptions.CliParser graph_options = new GraphOptions.CliParser(GraphOptions.MOVEMENT, GraphOptions.PRESENCE);

    @Override
    protected void initOptions() {
        super.initOptions();
        graph_options.setOptions(options);
    }

    @Override
    protected void parseArgs(CommandLine cli, String[] args)
            throws ParseException, ArrayIndexOutOfBoundsException,
            HelpException {
        super.parseArgs(cli, args);
        graph_options.parse(cli);
    }

    @Override
    protected void run() throws Exception {
        final MovementTrace movement = orig_store.getTrace(graph_options.get(GraphOptions.MOVEMENT));
        final PresenceTrace presence = dest_store.newTrace(graph_options.get(GraphOptions.PRESENCE), PresenceTrace.class, force);
        new MovementToPresenceConverter(presence, movement).convert();
    }
}
