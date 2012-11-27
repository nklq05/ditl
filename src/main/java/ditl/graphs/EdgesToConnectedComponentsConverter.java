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
package ditl.graphs;

import java.io.*;
import java.util.*;

import ditl.*;



public final class EdgesToConnectedComponentsConverter implements Converter {

	private AdjacencySet.Edges adjacency = new AdjacencySet.Edges();
	private Map<Integer,Group> cc_map = new HashMap<Integer, Group>();
	private StatefulWriter<GroupEvent,Group> group_writer;
	private StatefulReader<EdgeEvent,Edge> edge_reader;
	private int counter = 0;
	private GroupTrace _ccs;
	private EdgeTrace _edges;
	
	public EdgesToConnectedComponentsConverter( GroupTrace ccs, EdgeTrace edges){ 
		_ccs = ccs;
		_edges = edges;
	}
	
	private void merge(long time, Group cc1, Group cc2) throws IOException {
		cc1._members.addAll(cc2._members);
		for ( Integer i : cc2._members )
			cc_map.put(i, cc1);
		group_writer.append(time, new GroupEvent(cc2.gid(), GroupEvent.Type.LEAVE, cc2._members));
		delCC(time, cc2.gid());
		group_writer.append(time, new GroupEvent(cc1.gid(), GroupEvent.Type.JOIN, cc2._members));
	}
	
	private void checkSplit(long time, Integer i, Group cc) throws IOException {
		LinkedList<Integer> toVisit = new LinkedList<Integer>();
		Set<Integer> visited = new HashSet<Integer>();
		toVisit.add(i);
		visited.add(i);
		while( ! toVisit.isEmpty() ){
			Integer j = toVisit.pop();
			for ( Integer k : adjacency.getNext(j) ){
				if ( ! visited.contains(k) ){
					visited.add(k);
					toVisit.add(k);
				}
			}
		}
		if ( visited.size() != cc.size() ){ // the CC has split!
			int half = cc.size() / 2;
			Group ncc = newCC(time);
			cc._members.removeAll(visited);
			if ( visited.size() > half ){ // those in 'visited' remain in current cc, the others go to ncc
				ncc._members = cc._members;
				cc._members = visited;
			} else { // all those in 'visited' go to ncc
				ncc._members = visited;
			}
			group_writer.append(time, new GroupEvent(cc.gid(), GroupEvent.Type.LEAVE, ncc._members));
			for ( Integer j : ncc._members )
				cc_map.put(j, ncc);
			group_writer.append(time, new GroupEvent(ncc.gid(), GroupEvent.Type.JOIN, ncc._members));
		}
	}
	
	private Group newCC(long time) throws IOException {
		Integer gid = counter++;
		group_writer.append(time, new GroupEvent(gid, GroupEvent.Type.NEW));
		return new Group(gid);
	}
	
	private void delCC(long time, Integer gid) throws IOException {
		group_writer.append(time, new GroupEvent(gid, GroupEvent.Type.DELETE) );
	}
	
	private void addEdge(long time, Edge e) throws IOException {
		Group cc, occ;
		if ( ! cc_map.containsKey(e.id1) ){ 
			if ( ! cc_map.containsKey(e.id2) ){ // new isolated edge
				cc = newCC(time);
				cc._members.add(e.id1);
				cc._members.add(e.id2);
				cc_map.put(e.id1, cc);
				cc_map.put(e.id2, cc);
				group_writer.append(time, 
						new GroupEvent(cc.gid(), GroupEvent.Type.JOIN, new Integer[]{e.id1,e.id2}));
			} else { // add id1 to id2's cc
				cc = cc_map.get(e.id2);
				cc._members.add(e.id1);
				cc_map.put(e.id1, cc);
				group_writer.append(time, new GroupEvent(cc.gid(), GroupEvent.Type.JOIN, Collections.singleton(e.id1)));
			}
		} else {
			if ( ! cc_map.containsKey(e.id2) ){ // add id2 to id1's cc
				cc = cc_map.get(e.id1);
				cc._members.add(e.id2);
				cc_map.put(e.id2, cc);
				group_writer.append(time, new GroupEvent(cc.gid(), GroupEvent.Type.JOIN, Collections.singleton(e.id2)));
			} else {
				cc = cc_map.get(e.id1);
				occ = cc_map.get(e.id2);
				if ( cc != occ ){ // id1 and id2 belong to different ccs => merge
					if ( cc.size() > occ.size() )
						merge(time, cc, occ);
					else
						merge(time, occ, cc);
				}
			}
		}
	}
	
	private void removeSingleton(long time, Integer i) throws IOException {
		Group cc = cc_map.get(i);
		cc._members.remove(i);
		cc_map.remove(i);
		group_writer.append(time, new GroupEvent(cc.gid(), GroupEvent.Type.LEAVE, new Integer[]{i}));
		if ( cc._members.isEmpty() )
			delCC(time, cc.gid());
	}
	
	private void removeEdge(long time, Edge e) throws IOException {
		boolean singleton1 = (adjacency.getNext(e.id1).isEmpty());
		boolean singleton2 = (adjacency.getNext(e.id2).isEmpty());
		if ( singleton1 ) // id1 has become a singleton
			removeSingleton(time, e.id1);
		if ( singleton2 ) // id2 has become a singleton
			removeSingleton(time, e.id2);
		if ( ! singleton1 && ! singleton2 )
			checkSplit( time, e.id1, cc_map.get(e.id1) );
	}

	public void handleEvents(long time, Collection<EdgeEvent> events) throws IOException {
		Deque<EdgeEvent> down_events = new LinkedList<EdgeEvent>();
		for ( EdgeEvent eev : events ){
			Edge e = eev.edge();
			if ( eev.isUp() ){
				adjacency.add(e);
				addEdge(time, e);
			} else {
				down_events.addLast(eev);
			}
		}
		while ( ! down_events.isEmpty() ){
			Edge dl = down_events.poll().edge();
			adjacency.remove(dl);
			removeEdge(time, dl);
		}
	}
	
	private void setInitState(long time) throws IOException{
		Set<Group> initCCs = new HashSet<Group>();
		LinkedList<Integer> toVisit = new LinkedList<Integer>(adjacency.vertices());
		LinkedList<Integer> toVisitInCC = new LinkedList<Integer>();
		Set<Integer> visited = new HashSet<Integer>(toVisit.size()*2);
		while ( ! toVisit.isEmpty() ){
			Integer i = toVisit.pop();
			if ( ! visited.contains(i) ){
				Set<Integer> neighbs = adjacency.getNext(i); 
				if ( ! neighbs.isEmpty() ){
					Group g = new Group(counter++);
					initCCs.add(g);
					visited.add(i);
					g._members.add(i);
					toVisitInCC.clear();
					for ( Integer k : neighbs )
						if ( ! visited.contains(k) ){
							toVisitInCC.add(k);
							visited.add(k);
						}
					while ( ! toVisitInCC.isEmpty() ){
						Integer j = toVisitInCC.pop();
						g._members.add(j);
						for ( Integer k : adjacency.getNext(j) ){
							if ( ! visited.contains(k) ){
								toVisitInCC.add(k);
								visited.add(k);
							}
						}
					}
				}
			}
		}
		for ( Group g : initCCs ){
			Group h = new Group(g.gid(), new HashSet<Integer>(g._members)); 
			for ( Integer i : h._members )
				cc_map.put(i, h);
		}
		
		group_writer.setInitState(time, initCCs);
	}


	@Override
	public void convert() throws IOException {
		edge_reader = _edges.getReader();
		group_writer = _ccs.getWriter();
		long minTime = _edges.minTime();
		edge_reader.seek(minTime);
		Collection<Edge> initEdges = edge_reader.referenceState();
		for ( Edge e : initEdges )
			adjacency.add(e);
		setInitState(minTime);
		while ( edge_reader.hasNext() ){
			long time = edge_reader.nextTime();
			handleEvents(time, edge_reader.next());
		}
		group_writer.setPropertiesFromTrace(_edges);
		group_writer.close();
		edge_reader.close();
	}

}
