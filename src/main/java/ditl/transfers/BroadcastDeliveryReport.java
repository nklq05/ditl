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
package ditl.transfers;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import ditl.Listener;
import ditl.Report;
import ditl.ReportFactory;

public class BroadcastDeliveryReport extends Report
        implements MessageTrace.Handler, BufferTrace.Handler {

    private final Map<Integer, List<Long>> delivery_times = new HashMap<Integer, List<Long>>();
    private final Map<Integer, Long> ctimes = new HashMap<Integer, Long>();

    public BroadcastDeliveryReport(OutputStream out) throws IOException {
        super(out);
        appendComment("msg_id | delivery times");
    }

    public static final class Factory implements ReportFactory<BroadcastDeliveryReport> {
        @Override
        public BroadcastDeliveryReport getNew(OutputStream out) throws IOException {
            return new BroadcastDeliveryReport(out);
        }
    };

    @Override
    public Listener<Message> messageListener() {
        // Note: this class deliberately only listens to MessageEvents to
        // prevent any weird side effects with creation times
        return null;
    }

    @Override
    public Listener<MessageEvent> messageEventListener() {
        return new Listener<MessageEvent>() {
            @Override
            public void handle(long time, Collection<MessageEvent> events) throws IOException {
                for (final MessageEvent mev : events) {
                    final Integer msgId = mev.msgId;
                    if (mev.isNew()) {
                        delivery_times.put(msgId, new LinkedList<Long>());
                        ctimes.put(msgId, time);
                    } else if (ctimes.containsKey(msgId)) {
                        print(msgId);
                        delivery_times.remove(msgId);
                        ctimes.remove(msgId);
                    }
                }
            }
        };
    }

    private void print(Integer msgId) throws IOException {
        final StringBuffer buffer = new StringBuffer();
        buffer.append(msgId);
        for (final Long t : delivery_times.get(msgId))
            buffer.append(" " + t);
        append(buffer.toString());
    }

    @Override
    public Listener<BufferEvent> bufferEventListener() {
        return new Listener<BufferEvent>() {
            @Override
            public void handle(long time, Collection<BufferEvent> events)
                    throws IOException {
                for (final BufferEvent event : events) {
                    final Integer msgId = event.msgId;
                    final List<Long> deliveries = delivery_times.get(msgId);
                    if (deliveries != null)
                        if (event.type == BufferEvent.Type.ADD) {
                            final Long dtime = time - ctimes.get(msgId);
                            deliveries.add(dtime);
                        }
                }
            }
        };
    }

    @Override
    public Listener<Buffer> bufferListener() {
        return null;
    }
}
